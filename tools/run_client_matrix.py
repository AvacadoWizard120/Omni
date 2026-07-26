#!/usr/bin/env python3
import argparse
import datetime as dt
import json
import os
import re
import subprocess
import sys
import time
import traceback
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SETTINGS = ROOT / "settings.gradle.kts"
DEFAULT_RESULTS_DIR = ROOT / "build" / "run-client-matrix"
LAUNCHABLE_LOADERS = {"fabric", "quilt", "forge", "neoforge", "legacyfabric"}
UNLAUNCHABLE_TARGETS = {
    "1.12.2-forge": "standalone Forge 1.12.2 coremod build has no Gradle runClient task",
    "1.12.2-liteloader": "LiteLoader artifact build has no Gradle runClient task",
}


def parse_targets(settings_path):
    targets = []
    mc_call = re.compile(r'\bmc\("(?P<version>[^"]+)"(?P<loaders>.*?)\)\s*$')
    with settings_path.open("r", encoding="utf-8") as handle:
        for line in handle:
            line_without_comment = line.split("//", 1)[0]
            match = mc_call.search(line_without_comment)
            if not match:
                continue
            version = match.group("version")
            loaders = re.findall(r'"([^"]+)"', match.group("loaders"))
            for loader in loaders:
                target = f"{version}-{loader}"
                targets.append(
                    {
                        "target": target,
                        "minecraft": version,
                        "loader": loader,
                        "task": f":{target}:runClient",
                        "launchable": is_launchable(target, loader),
                        "skip_reason": skip_reason(target, loader),
                    }
                )
    return targets


def is_launchable(target, loader):
    return target not in UNLAUNCHABLE_TARGETS and loader in LAUNCHABLE_LOADERS


def skip_reason(target, loader):
    if target in UNLAUNCHABLE_TARGETS:
        return UNLAUNCHABLE_TARGETS[target]
    if loader not in LAUNCHABLE_LOADERS:
        return f"{loader} is not wired to a Gradle runClient task"
    return ""


def parse_target_name(value):
    if "-" not in value:
        return None, None
    minecraft, loader = value.rsplit("-", 1)
    return minecraft, loader


def apply_filters(targets, args):
    filtered = targets
    if args.loader:
        loaders = {loader.lower() for loader in args.loader}
        filtered = [item for item in filtered if item["loader"].lower() in loaders]
    if args.version:
        versions = set(args.version)
        filtered = [item for item in filtered if item["minecraft"] in versions]
    if args.only:
        filtered = [item for item in filtered if any(part in item["target"] for part in args.only)]
    if args.skip:
        filtered = [item for item in filtered if not any(part in item["target"] for part in args.skip)]
    if args.from_target:
        filtered = slice_from_target(filtered, args.from_target, include=True)
    if args.after_target:
        filtered = slice_from_target(filtered, args.after_target, include=False)
    if args.resume:
        filtered = skip_previous_successes(filtered, args.results_dir / "latest.json")
    if args.limit is not None:
        filtered = filtered[: args.limit]
    return filtered


def slice_from_target(targets, target, include):
    for index, item in enumerate(targets):
        if item["target"] == target:
            return targets[index if include else index + 1 :]
    raise SystemExit(f"Target {target!r} was not found in the matrix.")


def skip_previous_successes(targets, latest_path):
    if not latest_path.exists():
        return targets
    try:
        raw = json.loads(latest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return targets
    successes = {item["target"] for item in raw.get("results", []) if item.get("status") == "ok"}
    return [item for item in targets if item["target"] not in successes]


def exit_code_values(value):
    values = {value}
    if value < 0:
        values.add((1 << 32) + value)
        values.add(value & 0xFF)
    return values


def accepted_exit_codes(values):
    accepted = set()
    for value in values:
        accepted.update(exit_code_values(value))
    return accepted


def now_iso():
    return dt.datetime.now(dt.timezone.utc).astimezone().isoformat(timespec="seconds")


def timestamp_id():
    return dt.datetime.now().strftime("%Y%m%d-%H%M%S")


def safe_log_name(target):
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", target) + ".log"


def relative(path):
    try:
        return str(path.relative_to(ROOT))
    except ValueError:
        return str(path)


def write_results(path, payload):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def command_for(args, task):
    if os.name == "nt" and args.gradle.suffix.lower() in {".bat", ".cmd"}:
        command = ["cmd.exe", "/d", "/c", str(args.gradle), task]
    else:
        command = [str(args.gradle), task]
    if args.console_plain:
        command.extend(["--console", "plain"])
    command.extend(args.gradle_arg or [])
    return command


def run_target(item, args, run_dir, ok_codes):
    target = item["target"]
    log_path = run_dir / "logs" / safe_log_name(target)
    log_path.parent.mkdir(parents=True, exist_ok=True)
    command = command_for(args, item["task"])
    started = time.monotonic()
    result = {
        "target": target,
        "minecraft": item["minecraft"],
        "loader": item["loader"],
        "task": item["task"],
        "command": command,
        "started_at": now_iso(),
        "log": relative(log_path),
    }

    print(flush=True)
    print(f"=== {target} ===", flush=True)
    print("Command:", " ".join(command), flush=True)
    print(f"Close Minecraft normally to continue to the next target. Log: {relative(log_path)}", flush=True)

    with log_path.open("w", encoding="utf-8", errors="replace") as log:
        log.write(f"Command: {' '.join(command)}\n")
        log.write(f"Started: {result['started_at']}\n\n")
        try:
            process = subprocess.Popen(
                command,
                cwd=ROOT,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
                stdin=subprocess.DEVNULL,
            )
            assert process.stdout is not None
            for line in process.stdout:
                print(line, end="", flush=True)
                log.write(line)
            exit_code = process.wait()
        except KeyboardInterrupt:
            result.update(
                {
                    "status": "interrupted",
                    "ended_at": now_iso(),
                    "duration_seconds": round(time.monotonic() - started, 3),
                }
            )
            print("\nInterrupted by user.", flush=True)
            raise

    result["exit_code"] = exit_code
    result["ended_at"] = now_iso()
    result["duration_seconds"] = round(time.monotonic() - started, 3)
    result["status"] = "ok" if exit_code in ok_codes else "failed"
    print(f"=== {target} exited with {exit_code} ({result['status']}) ===", flush=True)
    return result


def skipped_result(item):
    return {
        "target": item["target"],
        "minecraft": item["minecraft"],
        "loader": item["loader"],
        "task": item["task"],
        "status": "skipped",
        "reason": item["skip_reason"],
    }


def print_plan(targets, include_skipped, try_unlaunchable=False):
    if try_unlaunchable:
        runnable = targets
        skipped = []
    else:
        runnable = [item for item in targets if item["launchable"]]
        skipped = [item for item in targets if not item["launchable"]]
    print(f"Runnable targets: {len(runnable)}", flush=True)
    for item in runnable:
        print(f"- {item['task']}", flush=True)
    if include_skipped and skipped:
        print(flush=True)
        print(f"Skipped targets: {len(skipped)}", flush=True)
        for item in skipped:
            print(f"- {item['target']}: {item['skip_reason']}", flush=True)


def summarize(results):
    counts = {}
    for item in results:
        counts[item["status"]] = counts.get(item["status"], 0) + 1
    print(flush=True)
    print("Summary:", flush=True)
    for status in sorted(counts):
        print(f"- {status}: {counts[status]}", flush=True)


def main():
    parser = argparse.ArgumentParser(description="Queue Omni Gradle runClient targets one at a time.")
    parser.add_argument("--settings", type=Path, default=DEFAULT_SETTINGS)
    parser.add_argument("--gradle", default=ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    parser.add_argument("--results-dir", type=Path, default=DEFAULT_RESULTS_DIR)
    parser.add_argument("--loader", action="append", help="Only run this loader. Can be repeated.")
    parser.add_argument("--version", action="append", help="Only run this Minecraft version. Can be repeated.")
    parser.add_argument("--only", action="append", default=[], help="Only run targets containing this substring.")
    parser.add_argument("--skip", action="append", default=[], help="Skip targets containing this substring.")
    parser.add_argument("--from-target", help="Start at this target, inclusive, e.g. 1.20.4-neoforge.")
    parser.add_argument("--after-target", help="Start after this target, e.g. resume after 1.20.4-neoforge.")
    parser.add_argument("--limit", type=int, help="Run at most this many filtered targets.")
    parser.add_argument("--resume", action="store_true", help="Skip targets marked ok in the latest results file.")
    parser.add_argument("--try-unlaunchable", action="store_true", help="Try targets that are known not to expose runClient.")
    parser.add_argument("--continue-on-failure", action="store_true", help="Keep going when a run returns an exit code not listed by --ok-exit-code.")
    parser.add_argument("--dry-run", action="store_true", help="Print the queue without launching Minecraft.")
    parser.add_argument("--ok-exit-code", type=int, action="append", default=[0], help="Accepted exit code. Repeat to add more.")
    parser.add_argument("--delay", type=float, default=0.0, help="Seconds to wait between successful runs.")
    parser.add_argument("--gradle-arg", action="append", default=[], help="Extra argument passed to every Gradle invocation.")
    parser.add_argument("--no-console-plain", dest="console_plain", action="store_false", default=True)
    args = parser.parse_args()

    args.settings = args.settings if args.settings.is_absolute() else ROOT / args.settings
    args.results_dir = args.results_dir if args.results_dir.is_absolute() else ROOT / args.results_dir
    args.gradle = Path(args.gradle)
    if not args.gradle.is_absolute():
        args.gradle = ROOT / args.gradle

    targets = apply_filters(parse_targets(args.settings), args)
    if not args.try_unlaunchable:
        run_queue = [item for item in targets if item["launchable"]]
        skipped = [item for item in targets if not item["launchable"]]
    else:
        run_queue = targets
        skipped = []

    if args.dry_run:
        print_plan(
            targets if args.try_unlaunchable else run_queue + skipped,
            include_skipped=not args.try_unlaunchable,
            try_unlaunchable=args.try_unlaunchable,
        )
        return 0

    if not run_queue and not skipped:
        print("No targets matched the requested filters.", file=sys.stderr, flush=True)
        return 1

    run_id = timestamp_id()
    run_dir = args.results_dir / run_id
    run_payload = {
        "run_id": run_id,
        "started_at": now_iso(),
        "ok_exit_codes": sorted(accepted_exit_codes(args.ok_exit_code)),
        "results": [],
    }
    latest_path = args.results_dir / "latest.json"
    run_path = run_dir / "results.json"

    for item in skipped:
        result = skipped_result(item)
        run_payload["results"].append(result)
        print(f"Skipping {item['target']}: {item['skip_reason']}", flush=True)

    ok_codes = accepted_exit_codes(args.ok_exit_code)
    exit_status = 0
    try:
        for index, item in enumerate(run_queue, start=1):
            print(f"\nQueue {index}/{len(run_queue)}", flush=True)
            result = run_target(item, args, run_dir, ok_codes)
            run_payload["results"].append(result)
            run_payload["updated_at"] = now_iso()
            write_results(run_path, run_payload)
            write_results(latest_path, run_payload)

            if result["status"] != "ok":
                exit_status = 1
                if not args.continue_on_failure:
                    print("Exit code was not accepted; stopping.", flush=True)
                    break
            elif args.delay > 0 and index < len(run_queue):
                time.sleep(args.delay)
    except KeyboardInterrupt:
        run_payload["updated_at"] = now_iso()
        write_results(run_path, run_payload)
        write_results(latest_path, run_payload)
        summarize(run_payload["results"])
        return 130

    run_payload["ended_at"] = now_iso()
    write_results(run_path, run_payload)
    write_results(latest_path, run_payload)
    summarize(run_payload["results"])
    print(f"Results: {relative(run_path)}", flush=True)
    print(f"Latest results: {relative(latest_path)}", flush=True)
    return exit_status


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception:
        DEFAULT_RESULTS_DIR.mkdir(parents=True, exist_ok=True)
        error_path = DEFAULT_RESULTS_DIR / "last-error.log"
        error_text = traceback.format_exc()
        error_path.write_text(error_text, encoding="utf-8")
        print(error_text, file=sys.stderr, flush=True)
        raise
