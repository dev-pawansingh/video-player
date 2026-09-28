plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.pawansingh.vp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pawansingh.vp"
        minSdk = 23
        targetSdk = 37
        versionCode = 14
        versionName = "2.3"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
    implementation ("com.github.bumptech.glide:glide:5.0.9")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-datasource:1.4.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation ("com.github.TutorialsAndroid:FilePicker:v9.0.1")
    implementation ("com.github.bullheadandplato:AndroidEqualizer:2.2")
    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")

    //implementation ("com.google.android.gms:play-services-ads:23.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation ("com.google.android.play:app-update:2.1.0")
}