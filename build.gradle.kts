plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.opencode"
version = "1.0.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
        localPlatformArtifacts()
    }
}

dependencies {
    intellijPlatform {
        // Use 2024.1 for better stability and to avoid Java 25 parsing bug
        intellijIdeaCommunity("2024.1")
        
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        bundledPlugin("Git4Idea")
        instrumentationTools()
        pluginVerifier()
    }
}

intellijPlatform {
    projectName.set("OpenCode")
    
    pluginConfiguration {
        id.set("com.opencode.plugin")
        name.set("OpenCode AI")
        vendor {
            name.set("OpenCode")
        }
        
        description.set("""
            Expert AI Code Reviewer for Android Studio.
            Integrates with local OpenCode server for privacy-first, streaming reviews.
        """.trimIndent())
        
        changeNotes.set("Fixed compatibility with Android Studio 2025.3")
}

    publishing {
        val envFile = file(".env")
        if (envFile.exists()) {
             val env = envFile.readLines().associate { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to ""
            }
            token.set(env["PUBLISH_TOKEN"])
        }
    }
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}
