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
