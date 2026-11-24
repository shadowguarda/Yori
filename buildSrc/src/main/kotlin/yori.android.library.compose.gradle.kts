import yori.build.configureCompose

plugins {
    id("com.android.library")
}

android {
    configureCompose(this)
}
