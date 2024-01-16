
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "io.github.rysanekrivera.powerfulcoroutines"
    compileSdk = 34

    defaultConfig {

        aarMetadata {
            minCompileSdk = 21
        }

        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}

publishing {

    publications {
        create<MavenPublication>("releaseAar") {
            groupId = "io.github.rysanekrivera"
            artifactId ="powerful-coroutines"
            version = "1.0.0"

            pom {
                name.set("Powerful Coroutines")
                description.set("This library enhances the power of coroutines and intends to enhance it's power by having better error handling, being network aware, waiting for the internet connection before executing the calls and also doing all these things and being lifecycle aware and memory efficient.")
                url.set("https://github.com/rysanekrivera/PowerfulCoroutines")

                licenses {
                    license {
                        name.set("Apache-2.0") // Replace with your license
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer{
                        id.set("RysanekRivera")
                        name.set("Rysanek Rivera")
                        email.set("rysanekmobiledeveloper@gmail.com")
                    }
                }
            }
        }
    }

    repositories {
      mavenCentral()
    }
}