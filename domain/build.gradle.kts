plugins {
    id("yori.android.library")
    kotlin("multiplatform")
    alias(kotlinx.plugins.serialization)
}

kotlin {
    androidTarget()
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.source.api)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.test)
                implementation(kotlinx.coroutines.test)
            }
        }
        androidMain {
            dependencies {
            }
        }
    }
}

android {
    namespace = "yori.domain"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}
