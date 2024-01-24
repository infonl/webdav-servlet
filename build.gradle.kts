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
	signing

	alias(libs.plugins.axion.release)
	alias(libs.plugins.nexus.publish.plugin)
}

repositories {
	mavenLocal()
	mavenCentral()
}

group = "nl.info.webdav"
project.version = scmVersion.version

dependencyLocking {
	lockAllConfigurations()
	lockMode = LockMode.STRICT
}

java {
	java.sourceCompatibility = JavaVersion.VERSION_17
	java.targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
	compileOnly(libs.jakarta.servlet.api)

	implementation(libs.slf4j.api)

	testImplementation(libs.jakarta.servlet.api)
	testImplementation(libs.junit.jupiter.params)
	testImplementation(libs.junit.jupiter.api)
	testImplementation(libs.jmock)
	testImplementation(libs.slf4j.simple)
	
	testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
	withJavadocJar()
	withSourcesJar()
}

nexusPublishing {
	repositories {
		sonatype {
			// only for users registered in Sonatype after 24 Feb 2021
			nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
			snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
		}
	}
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

signing {
	// signing is done as part of the publishing to Nexus using the Gradle Nexus Publish Plugin
	// see: https://github.com/gradle-nexus/publish-plugin

	// Signing requires an OpenPGP keypair and the ORG_GRADLE_PROJECT_signingKey
	// and ORG_GRADLE_PROJECT_signingPassword environment variables to be provided (by our GitHub workflow).
	// See: https://docs.gradle.org/current/userguide/signing_plugin.html
	val signingKey = findProperty("signingKey") as String?
	val signingPassword = findProperty("signingPassword") as String?
	useInMemoryPgpKeys(signingKey, signingPassword)
	sign(publishing.publications["mavenJava"])
}

tasks {
	check {
		dependsOn("jacocoTestReport")
	}

	test {
		useJUnitPlatform()
	}

	jacocoTestReport {
		dependsOn("test")

		reports {
			xml.required = true
			html.required = false
		}
	}
}