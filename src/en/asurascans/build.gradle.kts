import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(ns.plugins.extension)
}

android {
    sourceSets.named("test") {
        java.directories.add("test")
        kotlin.directories.add("test")
    }
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.bundles.common)
    testImplementation(libs.tachiyomi.lib.v16)
}

// Source metadata is generated from main; unit tests declare no @Source class.
tasks.matching { it.name.startsWith("ksp") && it.name.contains("UnitTest") }.configureEach {
    enabled = false
}

keiyoushi {
    name = "Asura Scans"
    versionCode = 1
    contentWarning = ContentWarning.SAFE
    libVersion = "1.6"

    source {
        baseUrl = "https://asurascans.com"
        lang = "en"
    }

    deeplink {
        path("/novels/..*")
    }
}
