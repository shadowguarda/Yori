plugins {
    id("yori.android.library")
    kotlin("android")
    alias(kotlinx.plugins.serialization)
}

android {
    namespace = "yori.core.archive"
}

dependencies {
    implementation(libs.jsoup)
    implementation(libs.libarchive)
    implementation(libs.unifile)
}
