/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
	java
	`maven-publish`
	`java-library`
}

repositories {
	mavenLocal()
	mavenCentral()
}

group = "net.sf.webdav-servlet"
version = "2.1.2"

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