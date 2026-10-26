# Tsundoku Extensions

A small native Kotlin extension repository based on [NovelSourcery](https://github.com/NovelSourcery/extensions-source). Currently includes Asura Scans novels.

## Login

1. Install the extension in Tsundoku and open Asura Scans.
2. Open the source in WebView and sign in to your Asura account at `https://asurascans.com/login`.
3. Return to Tsundoku and refresh the novel or retry the chapter.

The extension uses Tsundoku's existing WebView cookie jar. Asura stores `access_token` as a host-only cookie on `asurascans.com`; the extension reads it for each API request and sends it as `Authorization: Bearer …` to `https://api.asurascans.com`. Other cookies continue through Tsundoku's normal cookie handling. Account responses bypass the HTTP cache.

If the session expires, reopen WebView and sign in again. Token refresh is left to the website. Premium chapters remain visible in the chapter list; access is determined by Asura for the signed-in account.

## Build

Use Java 25 and an Android SDK with platform 34. The Gradle wrapper and dependency versions are retained from the upstream snapshot.

```sh
./gradlew spotlessApply
./gradlew :src:en:asurascans:testDebugUnitTest :src:en:asurascans:lintRelease :src:en:asurascans:assembleRelease
```

APKs and JARs are written under `src/en/asurascans/build/outputs/`. Local builds use the Android debug signing key unless `signingkey.jks` is present.

## GitHub releases

The workflow checks builds on pull requests and pushes. The **Publish release** option creates a signed APK/JAR and Tsundoku repository index. To initialize release publishing, install `gh` and a JDK (`keytool`), authenticate GitHub CLI as **mookoan**, and run:

```sh
gh auth login
bash scripts/setup-publishing.sh
```

The script generates a signing keystore at `~/.local/share/tsunext/signingkey.jks` if none exists, configures the repository Actions secrets (`SIGNING_KEY`, `KEY_STORE_PASSWORD`, `ALIAS`, `KEY_PASSWORD`), and dispatches the publish workflow. It refuses to silently replace an existing GitHub signing secret if the local keystore is missing. **Back up the keystore and its password securely:** future extension updates must use the same certificate. Never commit the keystore, password, or repository secrets.

For subsequent releases, increase `keiyoushi.versionCode` in `src/en/asurascans/build.gradle.kts` and run:

```sh
gh workflow run build.yml --repo mookoan/tsunext --ref main -f publish=true
```

Or run the **Build** workflow manually with **Publish release** selected. The repository URL to add in Tsundoku is:

```text
https://github.com/mookoan/tsunext/releases/latest/download/index.pb
```

The GitHub repository must be public for Tsundoku to download its release assets without GitHub authentication.

## Add another source

Run `python3 ext-bootstrap.py -n 'Source Name' -l en -u https://example.com`, implement the generated source, and add its Gradle tasks to `.github/workflows/build.yml`. Only modules present under `src/` are loaded. Shared themes and optional libraries can be copied from upstream when needed.

See [UPSTREAM.md](UPSTREAM.md) for provenance and licenses. This repository is unaffiliated with Asura, Tsundoku, and NovelSourcery.
