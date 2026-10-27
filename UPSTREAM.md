# Upstream code

Build framework, core, compiler, and Gradle configuration: [NovelSourcery/extensions-source](https://github.com/NovelSourcery/extensions-source), commit `7e0ad39d1ab702a82c1f6adbb17c71d81bdef348`, Apache-2.0. The source tree was reduced to one extension; the project name, publication workflow, and index publisher were changed.

Asura API paths, DTO fields, and chapter cleanup were adapted from [LNReader/lnreader-plugins](https://github.com/LNReader/lnreader-plugins/blob/9a2948bb4bedbbc181f1636b9a9276e079bff113/plugins/english/asurascans.ts), commit `9a2948bb4bedbbc181f1636b9a9276e079bff113`, MIT. Its license is retained in `licenses/LNReader-MIT.txt`. The Kotlin implementation adds authentication using the host app's WebView cookie jar.

Whitespace was normalized in the copied Windows wrapper, compiler source, and build properties. No shared runtime behavior was changed.

Witch Cult Translations is a new Kotlin implementation of WCT's published chapter index. It does not use LNReader's Arc 5 cutoff. PDF text extraction uses [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android), Apache-2.0, with its resources packaged on the extension classpath so the host app does not need PDFBox assets.

The Witch Cult Translations icon is reused from LNReader/lnreader-plugins under its retained MIT license.
