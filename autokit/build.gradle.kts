plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.github.halilozel1903.autokit"
    compileSdk = 37

    defaultConfig {
        // The Car App Library needs 23+.
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Robolectric loads the library's drawables for CarIcons.
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":autokit-core"))
    api(libs.androidx.car.app)
    implementation(libs.androidx.core)

    testImplementation(libs.androidx.car.app.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured (Maven Central); JitPack and local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    // JitPack serves artifacts under com.github.<user>.<repo>.
    val jitpackGroup = "com.github.halilozel1903.android-auto-kit".takeIf { System.getenv("JITPACK") == "true" }
    coordinates(groupId = jitpackGroup, artifactId = "android-auto-kit")
    pom {
        name.set("Android Auto Kit")
        description.set("Android Auto apps without the boilerplate: Car App Library templates for media, lists, grids, panes, messages and navigation from a small declarative model, with a base CarAppService, Session and Screen.")
    }
}
