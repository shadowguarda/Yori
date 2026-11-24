plugins {
    id("yori.android.library")
    kotlin("multiplatform")
    alias(kotlinx.plugins.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(projects.domain)
                api(libs.bundles.db)
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.bundles.db.android)
                implementation(projects.source.api)
            }
        }
    }
}

android {
    namespace = "yori.data"
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("yori.data")
            dialect(libs.sqldelight.dialects.sql)
            schemaOutputDirectory.set(project.file("./src/commonMain/sqldelight"))
        }
    }
}
