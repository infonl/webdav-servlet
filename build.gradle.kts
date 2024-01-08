import org.gradle.internal.impldep.org.junit.platform.launcher.EngineFilter.includeEngines

/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

plugins {
	java
	jacoco
	`maven-publish`
	`java-library`

	id("pl.allegro.tech.build.axion-release") version "1.16.1"
}

repositories {
	mavenLocal()
	mavenCentral()
}

group = "nl.info.webdav"
project.version = scmVersion.version

java {
	java.sourceCompatibility = JavaVersion.VERSION_17
	java.targetCompatibility = JavaVersion.VERSION_17
}

val junitVersion = "5.10.1"
val jakartaServletVersion = "6.0.0"

dependencies {
	compileOnly("jakarta.servlet:jakarta.servlet-api:$jakartaServletVersion")

	implementation("org.slf4j:slf4j-api:2.0.10")

	testImplementation("jakarta.servlet:jakarta.servlet-api:$jakartaServletVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testImplementation("org.jmock:jmock:2.12.0")
	testImplementation("org.slf4j:slf4j-simple:2.0.10")
	
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

java {
	withJavadocJar()
	withSourcesJar()
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = "webdav-servlet"
			from(components["java"])
			versionMapping {
				usage("java-api") {
					fromResolutionOf("runtimeClasspath")
				}
				usage("java-runtime") {
					fromResolutionResult()
				}
			}
			pom {
				name = "WebDAV Servlet"
				description = "A simple WebDAV servlet"
				url = "https://github.com/infonl/webdav-servlet"
				licenses {
					license {
						name = "The Apache License, Version 2.0"
						url = "https://www.apache.org/licenses/LICENSE-2.0"
					}
				}
				developers {
					developer {
						id = "edgarvonk"
						name = "Edgar Vonk"
						email = "edgar@info.nl"
					}
					developer {
						id = "bas-info-nl"
						name = "Bas de Wit"
						email = "bas@info.nl"
					}
					developer {
						id = "jorann"
						name = "Jorann de Waaij"
						email = "joran@lifely.nl"
					}
					developer {
						id = "RickWoltheus"
						name = "Rick Woltheus"
						email = "rick@lifely.nl"
						scm {
							connection = "scm:git:https://github.com/infonl/webdav-servlet.git"
							developerConnection =
								"scm:git:https://github.com/infonl/webdav-servlet.git"
							url = "https://github.com/infonl/webdav-servlet"
						}
					}
				}
			}
		}
	}
	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/infonl/webdav-servlet")
			version = "$version"
			credentials {
				username = System.getenv("GITHUB_ACTOR")
				password = System.getenv("GITHUB_TOKEN")
			}
		}
	}
}


tasks {
	val sourcesJar by creating(Jar::class) {
		archiveClassifier.set("sources")
		from(sourceSets.main.get().allSource)
	}

	val javadocJar by creating(Jar::class) {
		dependsOn.add(javadoc)
		archiveClassifier.set("javadoc")
		from(javadoc)
	}

	artifacts {
		archives(sourcesJar)
		archives(javadocJar)
		archives(jar)
	}

	test {
		useJUnitPlatform()
		
		finalizedBy("jacocoTestReport") // report is always generated after tests run
	}

	jacocoTestReport {
		dependsOn("test") // tests are required to run before generating the report
	}
}