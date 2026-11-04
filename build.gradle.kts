// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // Keep your existing plugins here (like android application, kotlin android, etc.)
    id("com.google.dagger.hilt.android") version "2.60.1" apply false

    // ADD THIS LINE (Match this version to your Kotlin version!)
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
}