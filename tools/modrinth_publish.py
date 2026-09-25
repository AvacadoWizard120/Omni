#!/usr/bin/env python3
import argparse
import hashlib
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
DEFAULT_CONFIG = ROOT / "modrinth-publish.local.json"
DEFAULT_RELEASE_DIR = ROOT / "build" / "libs" / "1.2-OVERHAUL"
PLAN_PATH = ROOT / "build" / "modrinth-upload-plan.json"
RESULTS_PATH = ROOT / "build" / "modrinth-upload-results.json"
API_BASE = "https://api.modrinth.com/v2"
CREATE_VERSION_ENDPOINT = f"{API_BASE}/version"
LOADER_TAG_ENDPOINT = f"{API_BASE}/tag/loader"
GAME_VERSION_TAG_ENDPOINT = f"{API_BASE}/tag/game_version"
VERSION_FILE_ENDPOINT = f"{API_BASE}/version_file/{{hash}}?algorithm=sha512"
USER_AGENT = "AvacadoWizard120/Omni-Modrinth-Publisher"
TRANSIENT_HTTP_STATUS = {429, 500, 502, 503, 504}
HTTP_RETRY_DELAYS = (1, 2, 4, 8, 16)


LOADERS = {
    "fabric": "Fabric",
    "quilt": "Quilt",
    "forge": "Forge",
    "neoforge": "NeoForge",
    "liteloader": "LiteLoader",
    "legacyfabric": "Legacy Fabric",
}

MODRINTH_LOADER_TAG_OVERRIDES = {
    "legacyfabric": "legacy-fabric",
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


def http_json(url, headers=None, method="GET", data=None, timeout=60):
    request_method = method.upper()
    for attempt in range(len(HTTP_RETRY_DELAYS) + 1):
        req = urllib.request.Request(
            url,
            data=data,
            method=request_method,
            headers={
                "Accept": "application/json",
                "User-Agent": USER_AGENT,
                **(headers or {}),
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=timeout) as response:
                body = response.read()
            break
        except urllib.error.HTTPError as error:
            retryable = request_method == "GET" and error.code in TRANSIENT_HTTP_STATUS
            if not retryable or attempt == len(HTTP_RETRY_DELAYS):
                raise
            retry_after = error.headers.get("Retry-After")
            delay = int(retry_after) if retry_after and retry_after.isdigit() else HTTP_RETRY_DELAYS[attempt]
            print(f"Modrinth HTTP {error.code}; retrying GET in {delay}s...", file=sys.stderr)
            time.sleep(delay)
        except urllib.error.URLError as error:
            if request_method != "GET" or attempt == len(HTTP_RETRY_DELAYS):
                raise
            delay = HTTP_RETRY_DELAYS[attempt]
            print(f"Modrinth network error {error.reason!r}; retrying GET in {delay}s...", file=sys.stderr)
            time.sleep(delay)
    if not body:
        return None
    return json.loads(body.decode("utf-8"))


def quote_path(value):
    return urllib.parse.quote(str(value), safe="")


def project_endpoint(project_id):
    return f"{API_BASE}/project/{quote_path(project_id)}"


def project_versions_endpoint(project_id):
    return f"{project_endpoint(project_id)}/version?include_changelog=false"


def parse_artifact(path):
    match = ARTIFACT_RE.match(path.name)
    if not match:
        return None
    target = match.group("target")
    if "-" not in target:
        return None
    minecraft, loader = target.rsplit("-", 1)
    if loader not in LOADERS:
        return None
    loader_tag = MODRINTH_LOADER_TAG_OVERRIDES.get(loader, loader)
    return {
        "file": path,
        "file_name": path.name,
        "release": match.group("release"),
        "extension": match.group("extension"),
        "minecraft": minecraft,
        "loader": loader_tag,
        "version_loader": loader,
        "loader_display": LOADERS[loader],
        "loader_upper": LOADERS[loader].upper(),
        "size": path.stat().st_size,
    }


def collect_artifacts(release_dir):
    artifacts = []
    for path in sorted(Path(release_dir).iterdir()):
        if path.suffix not in {".jar", ".litemod"}:
            continue
        artifact = parse_artifact(path)
        if artifact:
            artifacts.append(artifact)
    return artifacts


def template_value(config, key, default, item):
    return config.get(key, default).format(**item)


def hydrate_plan(plan, config):
    for item in plan:
        item["name"] = template_value(
            config,
            "name_template",
            "[{loader_upper}] Omni {release} for Minecraft {minecraft}",
            item,
        )
        version_item = {**item, "loader": item["version_loader"]}
        item["version_number"] = template_value(
            config,
            "version_number_template",
            "{release}+{minecraft}-{loader}",
            version_item,
        )
        item["changelog"] = config.get("changelog", "").strip()
    return plan


def fetch_known_loaders():
    raw = http_json(LOADER_TAG_ENDPOINT)
    return {item["name"] for item in raw if isinstance(item, dict) and "name" in item}


def fetch_known_game_versions():
    raw = http_json(GAME_VERSION_TAG_ENDPOINT)
    return {item["version"] for item in raw if isinstance(item, dict) and "version" in item}


def validate_tags(plan):
    known_loaders = fetch_known_loaders()
    known_versions = fetch_known_game_versions()
    missing_loaders = sorted({item["loader"] for item in plan if item["loader"] not in known_loaders})
    missing_versions = sorted({item["minecraft"] for item in plan if item["minecraft"] not in known_versions})

    errors = []
    if missing_loaders:
        errors.append("Unknown Modrinth loader tags: " + ", ".join(missing_loaders))
    if missing_versions:
        errors.append("Unknown Modrinth game version tags: " + ", ".join(missing_versions))
    return errors


def auth_headers(token):
    return {"Authorization": token} if token else {}


def resolve_project_id(project_id, token=None):
    project = http_json(project_endpoint(project_id), headers=auth_headers(token))
    resolved = project.get("id")
    if not resolved:
        raise RuntimeError(f"Modrinth project {project_id!r} did not return an id")
    return resolved


def fetch_existing_versions(project_id, token=None):
    versions = http_json(project_versions_endpoint(project_id), headers=auth_headers(token))
    return [item for item in versions if isinstance(item, dict)]


def existing_version_numbers(versions):
    return {
        item["version_number"]
        for item in versions
        if "version_number" in item
    }


def existing_loader_version_pairs(versions):
    pairs = set()
    for item in versions:
        for loader in item.get("loaders", []):
            for minecraft in item.get("game_versions", []):
                pairs.add((str(loader), str(minecraft)))
    return pairs


def file_sha512(path):
    digest = hashlib.sha512()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def fetch_version_by_file_hash(file_hash):
    try:
        return http_json(VERSION_FILE_ENDPOINT.format(hash=quote_path(file_hash)))
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def filter_existing_file_hashes(plan):
    kept = []
    skipped = []
    for item in plan:
        file_hash = file_sha512(item["file"])
        existing = fetch_version_by_file_hash(file_hash)
        if existing:
            item["existing_file_hash"] = file_hash
            item["existing_file_version_id"] = existing.get("id")
            item["existing_file_project_id"] = existing.get("project_id")
            item["existing_file_version_number"] = existing.get("version_number")
            skipped.append(item)
            continue
        kept.append(item)
    return kept, skipped


def filter_existing_versions(plan, versions, skip_pairs):
    numbers = existing_version_numbers(versions)
    pairs = existing_loader_version_pairs(versions)

    kept = []
    skipped_numbers = []
    skipped_pairs = []
    for item in plan:
        if item["version_number"] in numbers:
            skipped_numbers.append(item)
            continue
        if skip_pairs and (item["loader"], item["minecraft"]) in pairs:
            skipped_pairs.append(item)
            continue
        kept.append(item)
    return kept, skipped_numbers, skipped_pairs


def metadata_for(item, config, project_id):
    metadata = {
        "name": item["name"],
        "version_number": item["version_number"],
        "changelog": item["changelog"],
        "dependencies": config.get("dependencies", []),
        "game_versions": [item["minecraft"]],
        "version_type": config.get("version_type", "release"),
        "loaders": [item["loader"]],
        "featured": bool(config.get("featured", False)),
        "project_id": project_id,
        "file_parts": ["file"],
        "primary_file": "file",
    }
    environment = config.get("environment", "client_and_server")
    if environment:
        metadata["environment"] = environment
    requested_status = config.get("requested_status")
    if requested_status:
        metadata["requested_status"] = requested_status
    status = config.get("status")
    if status:
        metadata["status"] = status
    return metadata


def multipart_body(fields, files):
    boundary = f"----OmniModrinth{int(time.time() * 1000)}"
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


def upload_version(project_id, token, item, config):
    metadata = metadata_for(item, config, project_id)
    body, content_type = multipart_body(
        [("data", json.dumps(metadata), "application/json")],
        [("file", item["file"])],
    )
    return http_json(
        CREATE_VERSION_ENDPOINT,
        method="POST",
        data=body,
        timeout=180,
        headers={
            "Authorization": token,
            "Content-Type": content_type,
            "Content-Length": str(len(body)),
        },
    )


def public_plan(plan):
    clean = []
    for item in plan:
        copy = {k: v for k, v in item.items() if k != "file"}
        copy["path"] = str(item["file"])
        clean.append(copy)
    return clean


def print_plan(plan):
    for item in plan:
        print(
            f"- {item['file_name']} -> "
            f"{item['name']} / version {item['version_number']} / "
            f"{item['minecraft']} {item['loader']}"
        )


def main():
    parser = argparse.ArgumentParser(description="Dry-run or upload Omni artifacts to Modrinth.")
    parser.add_argument("--config", type=Path, default=DEFAULT_CONFIG)
    parser.add_argument("--release-dir", type=Path)
    parser.add_argument("--project-id")
    parser.add_argument("--token")
    parser.add_argument("--token-env")
    parser.add_argument("--changelog")
    parser.add_argument("--changelog-file", type=Path)
    parser.add_argument("--version-type", choices=["release", "beta", "alpha"])
    parser.add_argument("--requested-status", choices=["listed", "archived", "draft", "unlisted"])
    parser.add_argument("--upload", action="store_true", help="Upload files; omit for a dry run.")
    parser.add_argument("--validate-tags", action="store_true", help="Validate loader and game version tags against Modrinth.")
    parser.add_argument("--no-skip-existing", action="store_true", help="Do not skip already published version numbers or file hashes.")
    parser.add_argument("--skip-existing-pairs", action="store_true", help="Also skip any artifact whose loader/Minecraft pair already exists.")
    parser.add_argument("--only", action="append", default=[], help="Substring filter for artifact filenames.")
    parser.add_argument("--limit", type=int, help="Limit how many planned artifacts are processed.")
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
    if args.version_type:
        config["version_type"] = args.version_type
    if args.requested_status:
        config["requested_status"] = args.requested_status

    token = args.token or config.get("token") or os.environ.get(config.get("token_env", "MODRINTH_TOKEN"))
    project_id = config.get("project_id")
    release_dir = Path(config.get("release_dir", DEFAULT_RELEASE_DIR))
    if not release_dir.is_absolute():
        release_dir = ROOT / release_dir

    if not release_dir.exists():
        print(f"Release dir does not exist: {release_dir}", file=sys.stderr)
        return 1

    plan = collect_artifacts(release_dir)
    if args.only:
        plan = [item for item in plan if any(part in item["file_name"] for part in args.only)]
    if args.limit is not None:
        plan = plan[: args.limit]
    hydrate_plan(plan, config)

    if not plan:
        print(f"No publishable Omni artifacts found in {release_dir}", file=sys.stderr)
        return 1
    if not config.get("changelog", "").strip():
        print("Missing changelog. Put it in the config or pass --changelog/--changelog-file.", file=sys.stderr)
        return 1

    skip_existing = bool(config.get("skip_existing", True)) and not args.no_skip_existing
    skip_existing_pairs = bool(config.get("skip_existing_pairs", False)) or args.skip_existing_pairs

    if args.validate_tags or args.upload:
        errors = validate_tags(plan)
        if errors:
            for error in errors:
                print(error, file=sys.stderr)
            print("Upload canceled: Modrinth does not recognize one or more loader or game-version tags.", file=sys.stderr)
            return 1
        print("Modrinth loader/game version tags validated.")

    if not project_id:
        if args.upload:
            print("Missing project_id. Put the Modrinth project id or slug in the config or pass --project-id.", file=sys.stderr)
            return 1
    else:
        if skip_existing:
            existing = fetch_existing_versions(project_id, token)
            before = len(plan)
            plan, skipped_numbers, skipped_pairs = filter_existing_versions(plan, existing, skip_existing_pairs)
            skipped = before - len(plan)
            if skipped:
                print(
                    "Skipped "
                    f"{skipped} existing Modrinth version(s) "
                    f"({len(skipped_numbers)} by version number, "
                    f"{len(skipped_pairs)} by loader/Minecraft pair)."
                )

    if skip_existing:
        before = len(plan)
        plan, skipped_hashes = filter_existing_file_hashes(plan)
        skipped = before - len(plan)
        if skipped:
            print(f"Skipped {skipped} Modrinth file(s) already known by SHA-512 hash.")

    PLAN_PATH.parent.mkdir(parents=True, exist_ok=True)
    PLAN_PATH.write_text(json.dumps(public_plan(plan), indent=2), encoding="utf-8")

    print(f"Prepared {len(plan)} Modrinth version upload(s).")
    print(f"Plan written to {PLAN_PATH}")
    print_plan(plan)

    if not plan:
        print("Nothing to upload after existing-version filtering.")
        RESULTS_PATH.write_text("[]\n", encoding="utf-8")
        return 0

    if not args.upload:
        print("Dry-run only. Re-run with --upload after reviewing the plan.")
        return 0

    if not token:
        print("Missing Modrinth token. Set MODRINTH_TOKEN, put token in the local config, or pass --token.", file=sys.stderr)
        return 1

    resolved_project_id = resolve_project_id(project_id, token)

    results = []
    for index, item in enumerate(plan, start=1):
        print(f"[{index}/{len(plan)}] Uploading {item['file_name']}...")
        try:
            result = upload_version(resolved_project_id, token, item, config)
            results.append({"file": item["file_name"], "ok": True, "version_id": result.get("id"), "result": result})
            print(f"  OK version_id={result.get('id')}")
        except urllib.error.HTTPError as error:
            body = error.read().decode("utf-8", "replace")
            results.append({"file": item["file_name"], "ok": False, "status": error.code, "body": body})
            print(f"  FAILED HTTP {error.code}: {body}", file=sys.stderr)
            break
        except Exception as error:
            results.append({"file": item["file_name"], "ok": False, "error": repr(error)})
            print(f"  FAILED {error!r}", file=sys.stderr)
            break

    RESULTS_PATH.write_text(json.dumps(results, indent=2), encoding="utf-8")
    print(f"Results written to {RESULTS_PATH}")
    return 0 if all(item["ok"] for item in results) and len(results) == len(plan) else 1


if __name__ == "__main__":
    raise SystemExit(main())
