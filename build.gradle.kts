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
	alias(libs.plugins.spotless)
}

repositories {
	mavenLocal()
	mavenCentral()
}

group = "nl.info.webdav"
project.version = scmVersion.version

dependencyLocking {
	lockAllConfigurations()
}

java {
	java.sourceCompatibility = JavaVersion.VERSION_21
	java.targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
	compileOnly(libs.jakarta.servlet.api)

	testImplementation(libs.jakarta.servlet.api)
	testImplementation(libs.junit.jupiter.params)
	testImplementation(libs.junit.jupiter.api)
	testImplementation(libs.jmock)

	testRuntimeOnly(libs.junit.jupiter.engine)
	testRuntimeOnly(libs.junit.platform)
}

java {
	withJavadocJar()
	withSourcesJar()
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
	format("misc") {
		target("*.gradle", ".gitattributes", ".gitignore")

		trimTrailingWhitespace()
		indentWithSpaces()
		endWithNewline()
	}
	java {
		targetExcludeIfContentContains("Apache Software Foundation")

		removeUnusedImports()
		importOrderFile("config/importOrder.txt")

		formatAnnotations()

		// Latest supported version:
		// https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatter
		eclipse(libs.versions.spotless.eclipse.formatter.get()).configFile("config/webdav-servlet.xml")
	}
}

nexusPublishing {
	repositories {
		sonatype()
	}
	repositories {
		// see https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#configuration
		sonatype {
			nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
			snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
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
						id = "felixcicatt"
						name = "Felix Cornelissen"
						email = "felix@info.nl"
					}
				}
				scm {
					connection = "scm:git:https://github.com/infonl/webdav-servlet.git"
					developerConnection = "scm:git:https://github.com/infonl/webdav-servlet.git"
					url = "https://github.com/infonl/webdav-servlet"
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
