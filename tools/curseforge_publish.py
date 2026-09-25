#!/usr/bin/env python3
import argparse
import json
import mimetypes
import os
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CONFIG = ROOT / "curseforge-publish.local.json"
DEFAULT_RELEASE_DIR = ROOT / "build" / "libs" / "1.2-OVERHAUL"
PLAN_PATH = ROOT / "build" / "curseforge-upload-plan.json"
RESULTS_PATH = ROOT / "build" / "curseforge-upload-results.json"
UPLOAD_ENDPOINT = "https://minecraft.curseforge.com/api/projects/{project_id}/upload-file"
LEGACY_VERSIONS_ENDPOINT = "https://minecraft.curseforge.com/api/game/versions"
CORE_MINECRAFT_VERSIONS_ENDPOINT = "https://api.curseforge.com/v1/minecraft/version"
PUBLIC_FILES_ENDPOINT = "https://www.curseforge.com/api/v1/mods/{project_id}/files"


LOADERS = {
    "fabric": "Fabric",
    "quilt": "Quilt",
    "forge": "Forge",
    "neoforge": "NeoForge",
    "liteloader": "LiteLoader",
}

LOADERS_WITHOUT_CURSEFORGE_TAG = {
    "liteloader",
}

# Legacy Fabric artifacts are Modrinth-only. Keep the internal loader token
# explicitly denied here so it can never fall through as a Fabric upload.
CURSEFORGE_EXCLUDED_LOADERS = {
    "legacyfabric",
}


ARTIFACT_RE = re.compile(r"^omni-(?P<release>.+)\+(?P<target>.+)\.(?P<extension>jar|litemod)$")


def load_json(path):
    if not path.exists():
        return {}
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def read_text(path):
    with Path(path).open("r", encoding="utf-8") as handle:
        return handle.read()


def load_results(path):
    if not path.exists():
        return []
    try:
        data = load_json(path)
    except (OSError, json.JSONDecodeError):
        return []
    return data if isinstance(data, list) else []


def successful_files(results):
    return {
        str(item.get("file"))
        for item in results
        if isinstance(item, dict) and item.get("ok") and item.get("file")
    }


def http_json(url, headers=None, timeout=60):
    req = urllib.request.Request(
        url,
        headers={
            "Accept": "application/json",
            "User-Agent": "Omni CurseForge publisher",
            **(headers or {}),
        },
    )
    with urllib.request.urlopen(req, timeout=timeout) as response:
        data = response.read()
    return json.loads(data.decode("utf-8"))


def parse_artifact_name(file_name):
    match = ARTIFACT_RE.match(file_name)
    if not match:
        return None
    target = match.group("target")
    if "-" not in target:
        return None
    minecraft, loader = target.rsplit("-", 1)
    if loader in CURSEFORGE_EXCLUDED_LOADERS:
        return None
    if loader not in LOADERS:
        return None
    return {
        "release": match.group("release"),
        "extension": match.group("extension"),
        "minecraft": minecraft,
        "loader": loader,
        "loader_display": LOADERS[loader],
        "loader_upper": LOADERS[loader].upper(),
    }


def parse_jar(path):
    item = parse_artifact_name(path.name)
    if not item:
        return None
    return {
        **item,
        "file": path,
        "file_name": path.name,
        "size": path.stat().st_size,
    }


def collect_jars(release_dir):
    jars = []
    for path in sorted(Path(release_dir).iterdir()):
        if path.suffix not in {".jar", ".litemod"}:
            continue
        entry = parse_jar(path)
        if entry:
            jars.append(entry)
    return jars


def normalize_name(value):
    return re.sub(r"[\s_-]+", "", str(value).lower())


def extract_versions(raw):
    data = raw.get("data", raw) if isinstance(raw, dict) else raw
    if not isinstance(data, list):
        raise ValueError("CurseForge game versions response was not a list")

    versions = {}
    for item in data:
        if not isinstance(item, dict):
            continue
        name = item.get("name") or item.get("versionString")
        version_id = item.get("id") or item.get("gameVersionId")
        if name is None or version_id is None:
            continue
        versions.setdefault(str(name), int(version_id))
    return versions


def fetch_legacy_versions(token):
    return extract_versions(http_json(LEGACY_VERSIONS_ENDPOINT, {"X-Api-Token": token}))


def fetch_core_minecraft_versions():
    raw = http_json(CORE_MINECRAFT_VERSIONS_ENDPOINT)
    data = raw.get("data", [])
    return {item["versionString"]: item["gameVersionId"] for item in data if "versionString" in item}


def fetch_existing_files(project_id):
    files = []
    index = 0
    page_size = 50
    while True:
        query = urllib.parse.urlencode({"index": index, "pageSize": page_size})
        url = f"{PUBLIC_FILES_ENDPOINT.format(project_id=project_id)}?{query}"
        raw = http_json(url, timeout=60)
        page = raw.get("data", [])
        if not isinstance(page, list):
            raise ValueError("CurseForge existing files response was not a list")
        files.extend(item for item in page if isinstance(item, dict))

        pagination = raw.get("pagination", {})
        total = pagination.get("totalCount")
        if not page or total is None or len(files) >= int(total):
            break
        index += len(page)
    return files


def looks_like_minecraft_version(value):
    return bool(re.match(r"^(?:\d+\.)+\d+$", str(value)))


def existing_loader_version_pairs(files):
    pairs = set()
    for item in files:
        parsed = parse_artifact_name(str(item.get("fileName", "")))
        if parsed:
            pairs.add((parsed["loader"], parsed["minecraft"]))

        versions = {str(value) for value in item.get("gameVersions", [])}
        minecraft_versions = {value for value in versions if looks_like_minecraft_version(value)}
        for loader, loader_display in LOADERS.items():
            if loader_display in versions:
                for minecraft in minecraft_versions:
                    pairs.add((loader, minecraft))
    return pairs


def existing_file_names(files):
    return {str(item.get("fileName")) for item in files if item.get("fileName")}


def filter_existing_files(plan, existing_files, skip_pairs):
    file_names = existing_file_names(existing_files)
    pairs = existing_loader_version_pairs(existing_files)
    kept = []
    skipped_names = []
    skipped_pairs = []
    for item in plan:
        if item["file_name"] in file_names:
            skipped_names.append(item)
            continue
        if skip_pairs and (item["loader"], item["minecraft"]) in pairs:
            skipped_pairs.append(item)
            continue
        kept.append(item)
    return kept, skipped_names, skipped_pairs


def find_version_id(name, versions, candidates):
    explicit = versions.get(name)
    if explicit is not None:
        return explicit

    normalized = {normalize_name(key): value for key, value in versions.items()}
    for candidate in candidates:
        hit = normalized.get(normalize_name(candidate))
        if hit is not None:
            return hit
    return None


def resolve_ids(plan, config, token):
    game_overrides = {str(k): int(v) for k, v in config.get("game_version_ids", {}).items()}
    loader_overrides = {str(k).lower(): int(v) for k, v in config.get("loader_ids", {}).items()}
    environment_overrides = {
        normalize_name(k): int(v) for k, v in config.get("environment_ids", {}).items()
    }
    core_minecraft_versions = fetch_core_minecraft_versions()
    legacy_versions = fetch_legacy_versions(token) if token else {}

    errors = []
    environment_names = config.get("environments", ["Client", "Server"])
    if isinstance(environment_names, str):
        environment_names = [environment_names]
    environment_ids = []
    for environment_name in environment_names:
        environment_name = str(environment_name)
        environment_id = environment_overrides.get(normalize_name(environment_name))
        if environment_id is None:
            environment_id = find_version_id(
                environment_name,
                legacy_versions,
                [environment_name],
            )
        if environment_id is None:
            errors.append(f"could not resolve environment tag {environment_name!r}")
        else:
            environment_ids.append(environment_id)
    if not environment_names:
        errors.append("at least one CurseForge environment tag is required")

    for item in plan:
        minecraft = item["minecraft"]
        loader = item["loader"]
        loader_display = item["loader_display"]

        minecraft_id = game_overrides.get(minecraft)
        if minecraft_id is None:
            minecraft_id = core_minecraft_versions.get(minecraft)
        if minecraft_id is None:
            minecraft_id = find_version_id(
                minecraft,
                legacy_versions,
                [minecraft, f"Minecraft {minecraft}"],
            )

        loader_id = loader_overrides.get(loader)
        if loader_id is None:
            loader_id = find_version_id(
                loader_display,
                legacy_versions,
                [loader_display, loader, loader_display.replace("Neo", "Neo ")],
            )

        item["curseforge_game_version_ids"] = [
            x for x in [minecraft_id, loader_id, *environment_ids] if x is not None
        ]
        if minecraft_id is None:
            errors.append(f"{item['file_name']}: could not resolve Minecraft version tag {minecraft!r}")
        if loader_id is None and loader not in LOADERS_WITHOUT_CURSEFORGE_TAG:
            errors.append(f"{item['file_name']}: could not resolve loader tag {loader_display!r}")

    return errors


def validate_core_versions(plan):
    known = fetch_core_minecraft_versions()
    missing = sorted({item["minecraft"] for item in plan if item["minecraft"] not in known})
    return missing


def display_name_for(item, config):
    template = config.get(
        "display_name_template",
        "[{loader_upper}] Omni {release}",
    )
    return template.format(**item)


def metadata_for(item, config):
    return {
        "changelog": item["changelog"],
        "changelogType": config.get("changelog_type", "markdown"),
        "displayName": display_name_for(item, config),
        "gameVersions": item["curseforge_game_version_ids"],
        "releaseType": config.get("release_type", "release"),
    }


def multipart_body(fields, files):
    boundary = f"----OmniCurseForge{int(time.time() * 1000)}"
    chunks = []

    for name, value, content_type in fields:
        chunks.append(f"--{boundary}\r\n".encode())
        chunks.append(
            f'Content-Disposition: form-data; name="{name}"\r\n'
            f"Content-Type: {content_type}\r\n\r\n".encode()
        )
        chunks.append(value.encode("utf-8") if isinstance(value, str) else value)
        chunks.append(b"\r\n")

    for name, path in files:
        mime = mimetypes.guess_type(path.name)[0] or "application/java-archive"
        chunks.append(f"--{boundary}\r\n".encode())
        chunks.append(
            f'Content-Disposition: form-data; name="{name}"; filename="{path.name}"\r\n'
            f"Content-Type: {mime}\r\n\r\n".encode()
        )
        chunks.append(path.read_bytes())
        chunks.append(b"\r\n")

    chunks.append(f"--{boundary}--\r\n".encode())
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def upload_file(project_id, token, item, config):
    metadata = metadata_for(item, config)
    body, content_type = multipart_body(
        [("metadata", json.dumps(metadata), "application/json")],
        [("file", item["file"])],
    )
    url = UPLOAD_ENDPOINT.format(project_id=project_id)
    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Accept": "application/json",
            "Content-Type": content_type,
            "Content-Length": str(len(body)),
            "User-Agent": "Omni CurseForge publisher",
            "X-Api-Token": token,
        },
    )
    with urllib.request.urlopen(req, timeout=180) as response:
        response_body = response.read().decode("utf-8", "replace")
        if not response_body.strip().startswith("{"):
            raise RuntimeError(
                f"CurseForge returned HTTP {response.status} but not JSON. "
                f"Final URL was {response.geturl()!r}."
            )
        return {
            "status": response.status,
            "reason": response.reason,
            "body": json.loads(response_body),
        }


def public_plan(plan, config):
    clean = []
    for item in plan:
        copy = {k: v for k, v in item.items() if k != "file"}
        copy["path"] = str(item["file"])
        copy["display_name"] = display_name_for(item, config)
        clean.append(copy)
    return clean


def main():
    parser = argparse.ArgumentParser(description="Dry-run or upload Omni artifacts to CurseForge.")
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--release-dir", type=Path)
    parser.add_argument("--project-id")
    parser.add_argument("--token")
    parser.add_argument("--token-env")
    parser.add_argument("--changelog")
    parser.add_argument("--changelog-file", type=Path)
    parser.add_argument("--release-type", choices=["release", "beta", "alpha"])
    parser.add_argument("--upload", action="store_true", help="Upload files; omit for a dry run.")
    parser.add_argument("--validate", action="store_true", help="Resolve CurseForge IDs using the tokened author API.")
    parser.add_argument("--no-skip-existing", action="store_true", help="Do not skip already published file names or prior local successes.")
    parser.add_argument("--skip-existing-pairs", action="store_true", help="Also skip any artifact whose loader/Minecraft pair already exists.")
    parser.add_argument("--only", action="append", default=[], help="Substring filter for artifact filenames.")
    args = parser.parse_args()

    config = load_json(args.config)
    if args.release_dir:
        config["release_dir"] = str(args.release_dir)
    if args.project_id:
        config["project_id"] = args.project_id
    if args.token_env:
        config["token_env"] = args.token_env
    if args.changelog:
        config["changelog"] = args.changelog
    if args.changelog_file:
        config["changelog"] = read_text(args.changelog_file)
    if args.release_type:
        config["release_type"] = args.release_type

    token = args.token or config.get("token") or os.environ.get(config.get("token_env", "CURSEFORGE_API_TOKEN"))
    project_id = config.get("project_id")
    release_dir = Path(config.get("release_dir", DEFAULT_RELEASE_DIR))
    if not release_dir.is_absolute():
        release_dir = ROOT / release_dir

    plan = collect_jars(release_dir)
    if args.only:
        plan = [item for item in plan if any(part in item["file_name"] for part in args.only)]

    if not plan:
        print(f"No publishable Omni artifacts found in {release_dir}", file=sys.stderr)
        return 1

    changelog = config.get("changelog", "").strip()
    if not changelog:
        print("Missing changelog. Put it in the config or pass --changelog/--changelog-file.", file=sys.stderr)
        return 1
    for item in plan:
        item["changelog"] = changelog

    missing_core = validate_core_versions(plan)
    if missing_core:
        print("CurseForge Core API does not know these Minecraft versions:", ", ".join(missing_core), file=sys.stderr)
        return 1

    if project_id and bool(config.get("skip_existing", True)) and not args.no_skip_existing:
        existing_files = fetch_existing_files(project_id)
        before = len(plan)
        skip_existing_pairs = bool(config.get("skip_existing_pairs", False)) or args.skip_existing_pairs
        plan, skipped_names, skipped_pairs = filter_existing_files(plan, existing_files, skip_existing_pairs)
        skipped = before - len(plan)
        if skipped:
            print(
                f"Skipped {skipped} existing CurseForge file(s) "
                f"({len(skipped_names)} by file name, "
                f"{len(skipped_pairs)} by loader/Minecraft pair) "
                f"after checking {len(existing_files)} remote file(s)."
            )

    previous_results = load_results(RESULTS_PATH) if bool(config.get("skip_existing", True)) and not args.no_skip_existing else []
    previous_successes = successful_files(previous_results)
    if previous_successes:
        before = len(plan)
        plan = [item for item in plan if item["file_name"] not in previous_successes]
        skipped = before - len(plan)
        if skipped:
            print(f"Skipped {skipped} CurseForge file(s) listed as successful in the local results file.")

    if args.upload or args.validate:
        if not token:
            print("Missing CurseForge token. Set CURSEFORGE_API_TOKEN or pass --token.", file=sys.stderr)
            return 1
        errors = resolve_ids(plan, config, token)
        if errors:
            for error in errors:
                print(error, file=sys.stderr)
            print(
                "Add explicit game_version_ids/loader_ids/environment_ids to the local config "
                "if CurseForge uses unexpected tag names.",
                file=sys.stderr,
            )
            return 1

    PLAN_PATH.parent.mkdir(parents=True, exist_ok=True)
    PLAN_PATH.write_text(json.dumps(public_plan(plan, config), indent=2), encoding="utf-8")

    print(f"Prepared {len(plan)} CurseForge upload(s).")
    print(f"Plan written to {PLAN_PATH}")
    for item in plan:
        ids = item.get("curseforge_game_version_ids", [])
        id_text = f" ids={ids}" if ids else ""
        print(f"- {item['file_name']} -> Minecraft {item['minecraft']} / {item['loader_display']}{id_text}")

    if not plan:
        print("Nothing to upload after existing-file filtering.")
        RESULTS_PATH.write_text(json.dumps([item for item in previous_results if item.get("ok")], indent=2), encoding="utf-8")
        return 0

    if not args.upload:
        print("Dry-run only. Re-run with --upload after reviewing the plan.")
        return 0

    if not project_id:
        print("Missing project_id. Put it in the config or pass --project-id.", file=sys.stderr)
        return 1
    if not str(project_id).isdigit():
        print("project_id must be the numeric CurseForge Project ID, not the project slug.", file=sys.stderr)
        return 1

    results = []
    for index, item in enumerate(plan, start=1):
        print(f"[{index}/{len(plan)}] Uploading {item['file_name']}...")
        try:
            result = upload_file(project_id, token, item, config)
            results.append({"file": item["file_name"], "ok": True, "result": result})
            print(f"  OK {result['status']} {result['reason']}")
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", "replace")
            results.append({"file": item["file_name"], "ok": False, "status": error.code, "body": body})
            print(f"  FAILED HTTP {error.code}: {body}", file=sys.stderr)
            break
        except Exception as error:
            results.append({"file": item["file_name"], "ok": False, "error": repr(error)})
            print(f"  FAILED {error!r}", file=sys.stderr)
            break

    RESULTS_PATH.write_text(
        json.dumps([item for item in previous_results if item.get("ok")] + results, indent=2),
        encoding="utf-8",
    )
    print(f"Results written to {RESULTS_PATH}")
    return 0 if all(item["ok"] for item in results) and len(results) == len(plan) else 1


if __name__ == "__main__":
    raise SystemExit(main())
