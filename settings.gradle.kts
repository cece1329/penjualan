pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TAMBAHKAN BARIS DI BAWAH INI
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "penjualan"
include(":app")