import argparse
import gzip
import hashlib
import json
import shutil
from pathlib import Path

from google.protobuf import json_format

import index_pb2


def build_index(root: Path, output: Path, repository: str, tag: str, certificate: Path):
    output.mkdir(parents=True, exist_ok=True)
    base = f"https://github.com/{repository}/releases/download/{tag}"
    index = index_pb2.Index(
        name="Tsundoku Extensions",
        badgeLabel="CE",
        signingKey=hashlib.sha256(certificate.read_bytes()).hexdigest(),
        contact=index_pb2.Contact(website=f"https://github.com/{repository}"),
        extensionList=index_pb2.ExtensionList(),
    )
    metadata = sorted(root.glob("src/*/*/build/keiyoushi-source-info.json"))
    if not metadata:
        raise ValueError("No release metadata found; run assembleRelease first")
    for path in metadata:
        info = json.loads(path.read_text())
        apk, = path.parent.glob("outputs/apk/release/*.apk")
        jar, = path.parent.glob("outputs/jar/release/*.jar")
        for asset in (apk, jar):
            shutil.copy2(asset, output / asset.name)
        icon = root / "core/src/main/res/mipmap-xhdpi/ic_launcher.png"
        module_icon = path.parent.parent / "res/mipmap-xhdpi/ic_launcher.png"
        if module_icon.exists():
            icon = module_icon
        icon_name = f"{info['module']}.png"
        shutil.copy2(icon, output / icon_name)
        extension = index.extensionList.extensions.add(
            name=info["name"], packageName=info["packageName"],
            extensionLib=info["extensionLib"], versionCode=info["versionCode"],
            versionName=info["versionName"], contentWarning=info["contentWarning"],
            isNovel=True,
            resources=index_pb2.Resources(
                apkUrl=f"{base}/{apk.name}", jarUrl=f"{base}/{jar.name}",
                iconUrl=f"{base}/{icon_name}",
            ),
        )
        for source in info["sources"]:
            extension.sources.add(
                id=int(source["id"]), name=source["name"],
                language=source["lang"], homeUrl=source["baseUrl"],
                mirrorUrls=source.get("mirrorUrls", []),
            )
    (output / "index.pb").write_bytes(gzip.compress(index.SerializeToString(deterministic=True), mtime=0))
    (output / "index.json").write_text(json_format.MessageToJson(index, preserving_proto_field_name=True) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--repository", required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--certificate", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("dist"))
    args = parser.parse_args()
    build_index(Path(__file__).resolve().parents[1], args.output, args.repository, args.tag, args.certificate)
