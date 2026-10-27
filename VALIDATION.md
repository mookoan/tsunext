# Validation

Asura's public API was checked on October 26, 2026: listing (`limit=2`, `offset=0`, `sort=popular`), details for `shadow-slave` (3,160 chapters), and public chapter 1. The responses match the Kotlin DTO fields. The live login page sets a host-only `access_token` cookie and uses Bearer authentication for API requests.

Premium access requires an on-device test with a signed-in Asura account. No account credentials were used during development.

Completed checks:

- Kotlin release compilation and APK/JAR assembly pass.
- Four session tests pass: current cookie lookup, token changes and logout, URL decoding, cache behavior, and API origin restrictions.
- Two Python index publisher tests pass.
- The index publisher also produces an index from the actual release metadata and certificate.
- Release lint reports 0 errors and 2 warnings in the upstream generated manifest (`AppLinkWarning`, `DataExtractionRules`).
- APK and JAR signatures verify. The bundled test artifacts use the local Android debug certificate; configure a stable release signing key before publishing updates.

The bundled test APK/JAR are in `artifacts/` inside the downloadable archive. Install the APK in Tsundoku, open Asura Scans in WebView, sign in, and retry a premium novel chapter. The release workflow creates an installable repository index after signing secrets are configured.

The requested Jev quality-control review did not run. Automatic approval review rejected sending the README to the external TypeSafe service because that disclosure was not explicitly authorized. No quality score is available.

## Witch Cult Translations — October 27, 2026

- Release APK/JAR assembly and seven regression tests pass. Release lint reports 0 errors and 7 warnings (generated-manifest warnings, dependency warnings, and inline dependency-version suggestions).
- Three additional temporary live-data tests passed against the fetched WCT table, representative pages from all four supported hosts, and WCT's first Arc 4 PDF collection. The live index produced 520 reading entries across Arcs 1–10, including 8 unique PDF collections. The Arc 4 sample contained approximately 105,000 words and extracted through the same Android PDF library used by the extension.
- TranslationChicken link traversal was checked against Arc 6 chapters 33 (2 parts), 34 (4 parts), and 35 (2 parts, followed by WCT's separately indexed third part).
- Both packaged extension formats contain PDFBox classpath resources. The JAR adds those resources before signing; it does not depend on assets in the host application.
- A smoke test against the finished release JAR extracted 619,197 characters from the live Arc 4 sample PDF.
- WCT currently indexes Arc 2 chapters 1–19 and Arc 3 chapters 1–13, plus an Arc 3 interlude PDF. Chapters marked “Coming Soon” and Mega-only extras are not represented as readable chapters.
- Android device installation and reader navigation still need an on-device check.
