import io.github.keiyoushi.gradle.api.ContentWarning
import io.github.keiyoushi.gradle.tasks.SignExtensionJarTask

plugins {
    alias(ns.plugins.extension)
}

val pdfResources by configurations.creating

dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    pdfResources("com.tom-roush:pdfbox-android:2.0.27.0@aar")
    testImplementation(libs.junit)
    testImplementation(libs.bundles.common)
    testImplementation(libs.tachiyomi.lib.v16)
}

// Extensions cannot load PDFBox assets from the host application's AssetManager.
val unpackPdfResources by tasks.registering(Copy::class) {
    from(zipTree(pdfResources.singleFile)) {
        include("assets/**")
        eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("generated/pdf-resources"))
}

val mergePdfJarResources by tasks.registering(Jar::class) {
    dependsOn("createReleaseExtensionJar", unpackPdfResources)
    archiveFileName.set("with-pdf-resources.jar")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/pdf-jar"))
    from(zipTree(layout.buildDirectory.file("intermediates/extension_jar/release/unsigned.jar")))
    from(layout.buildDirectory.dir("generated/pdf-resources"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        tasks.named<SignExtensionJarTask>("signReleaseExtensionJar") {
            inputJar.set(mergePdfJarResources.flatMap { it.archiveFile })
        }
    }
}

android {
    sourceSets.named("main") {
        resources.directories.add(layout.buildDirectory.dir("generated/pdf-resources").get().asFile.path)
    }
    sourceSets.named("test") {
        java.directories.add("test")
        kotlin.directories.add("test")
        resources.directories.add("test-resources")
    }
    testOptions.unitTests.isReturnDefaultValues = true
}

tasks.matching { it.name.startsWith("ksp") && it.name.contains("UnitTest") }.configureEach {
    enabled = false
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("JavaRes") }.configureEach {
    dependsOn(unpackPdfResources)
}

keiyoushi {
    name = "Witch Cult Translations"
    versionCode = 1
    contentWarning = ContentWarning.SAFE
    libVersion = "1.6"

    source {
        baseUrl = "https://witchculttranslation.com"
        lang = "en"
    }

    deeplink {
        path("/..*")
    }
}
