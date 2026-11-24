import yori.build.configureAndroid
import yori.build.configureTest

plugins {
    id("com.android.library")
}

android {
    configureAndroid(this)
    configureTest()
}
