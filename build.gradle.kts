/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

plugins {
	java
	`maven-publish`
	`java-library`

	id("pl.allegro.tech.build.axion-release") version "1.15.1"
}

repositories {
	mavenLocal()
	mavenCentral()
}

group = "nl.lifely.webdav-servlet"
project.version = scmVersion.version

java {
	java.sourceCompatibility = JavaVersion.VERSION_17
	java.targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
	compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

	implementation("org.slf4j:slf4j-api:2.0.9")

	testImplementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
	testImplementation("junit:junit:4.13.2")
	testImplementation("org.jmock:jmock:2.12.0")
	testImplementation("org.slf4j:slf4j-simple:2.0.10")
}

publishing {
	publications {
		create<MavenPublication>("webdav-servlet") {
			from(components["java"])
			// Include any other artifacts here, like javadocs
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