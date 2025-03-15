import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile



plugins {
	kotlin("jvm") version "2.1.10" // Kotlin plugin
	id("fabric-loom") version "1.9.1" // Fabric Loom plugin
	id("maven-publish") // Maven publishing plugin
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set(project.property("archives_base_name") as String)
}


repositories {
	// Essential Maven repository
	maven(url = "https://repo.essential.gg/repository/maven-public")
	maven (url ="https://maven.shedaniel.me/" )
	maven ( url =  "https://maven.terraformersmc.com/releases/" )
	maven (url= "https://maven.fabricmc.net/")
	// Add other repositories here if needed
}

dependencies {
	// Minecraft and mappings
	minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
	mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")

	// Fabric Loader and Kotlin
	modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
	modImplementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
	// Fabric API (optional but recommended)
	modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
	// UniversalCraft
	modImplementation(include("gg.essential:universalcraft-1.21.4-fabric:373")!!)
	//modImplementation(include("gg.essential:universalcraft-standalone:373")!!)
	// Vigilance
	modImplementation(include("gg.essential:vigilance:306")!!)
	//modImplementation("gg.essential:essential-1.20.6-fabric:17141+gd6f4cfd3a8")
	// UniversalCraft dependency
	modImplementation(include("gg.essential:elementa:685")!!)

	implementation("com.google.code.gson:gson:2.10.1")



	// Config UI (for ModMenu integration)
	modImplementation("net.fabricmc.fabric-api:fabric-renderer-api-v1:1.21.4+api+build")

	modImplementation(include("org.nanohttpd:nanohttpd:2.3.1")!!)


}

tasks.processResources {
	inputs.property("version", project.version)

	filesMatching("fabric.mod.json") {
		expand(
			"version" to project.version,
			"minecraft_version" to project.property("minecraft_version"),
			"loader_version" to project.property("loader_version"),
			"kotlin_loader_version" to project.property("kotlin_loader_version")
		)
	}
}




tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8" // Ensure UTF-8 encoding
	options.release.set(21) // Target Java 21
}

tasks.withType<KotlinCompile>().configureEach {
	compilerOptions {
		jvmTarget.set(JvmTarget.JVM_21) // Target JVM 21 for Kotlin
	}
}

java {
	withSourcesJar() // Generate sources JAR
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName.get()}" }
	}
}

// Configure Maven publishing
publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	repositories {
		// Add repositories to publish to here
		// Example: Maven Central or a private Maven repository
	}
}


