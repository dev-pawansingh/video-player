plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pawansingh.vp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pawansingh.vp"
        minSdk = 23
        targetSdk = 36
        versionCode = 13
        versionName = "2.2"
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    implementation ("com.google.android.exoplayer:exoplayer:2.14.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.14.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation ("com.github.TutorialsAndroid:FilePicker:v9.0.1")
    implementation ("com.github.bullheadandplato:AndroidEqualizer:2.2")
    implementation("androidx.activity:activity:1.11.0")

    //implementation ("com.google.android.gms:play-services-ads:23.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation ("com.google.android.play:app-update:2.1.0")
}