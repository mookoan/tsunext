import gzip
import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

import index_pb2

spec = importlib.util.spec_from_file_location("build_index", Path(__file__).with_name("build-index.py"))
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class IndexTest(unittest.TestCase):
    def test_release_index_keeps_source_ids_and_uses_own_certificate(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            build = root / "src/en/asurascans/build"
            build.mkdir(parents=True)
            metadata = {
                "name": "Asura Scans", "module": "en.asurascans",
                "packageName": "eu.kanade.tachiyomi.novelextension.en.asurascans",
                "extensionLib": "1.6", "versionCode": 1, "versionName": "1.6.1",
                "contentWarning": 1,
                "sources": [{"id": 8123456789012345678, "name": "Asura Scans", "lang": "en", "baseUrl": "https://asurascans.com"}],
            }
            (build / "keiyoushi-source-info.json").write_text(json.dumps(metadata))
            for kind in ("apk", "jar"):
                asset = build / f"outputs/{kind}/release/asura.{kind}"
                asset.parent.mkdir(parents=True)
                asset.write_bytes(b"test artifact")
            icon = root / "core/src/main/res/mipmap-xhdpi/ic_launcher.png"
            icon.parent.mkdir(parents=True)
            icon.write_bytes(b"test icon")
            certificate = root / "certificate.der"
            certificate.write_bytes(b"own certificate")
            output = root / "dist"
            publisher.build_index(root, output, "example/extensions", "test-tag", certificate)
            index = index_pb2.Index.FromString(gzip.decompress((output / "index.pb").read_bytes()))
            self.assertEqual(hashlib.sha256(b"own certificate").hexdigest(), index.signingKey)
            extension, = index.extensionList.extensions
            self.assertTrue(extension.isNovel)
            self.assertEqual(8123456789012345678, extension.sources[0].id)
            self.assertEqual("https://github.com/example/extensions/releases/download/test-tag/asura.jar", extension.resources.jarUrl)
            self.assertTrue((output / "asura.apk").is_file())
            self.assertTrue((output / "en.asurascans.png").is_file())
            self.assertEqual("8123456789012345678", json.loads((output / "index.json").read_text())["extensionList"]["extensions"][0]["sources"][0]["id"])

    def test_missing_build_fails(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            cert = root / "cert.der"
            cert.write_bytes(b"certificate")
            with self.assertRaises(ValueError):
                publisher.build_index(root, root / "dist", "owner/repo", "tag", cert)


if __name__ == "__main__":
    unittest.main()
