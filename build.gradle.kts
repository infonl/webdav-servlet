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
}

repositories {
	mavenLocal()
	mavenCentral()
}

group = "net.sf.webdav-servlet"
//version = "2.1-dev"

java {
	java.sourceCompatibility = JavaVersion.VERSION_17
	java.targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
	compileOnly("javax.servlet:servlet-api:2.4")

	implementation("org.slf4j:slf4j-api:1.4.3")
	implementation("org.slf4j:slf4j-log4j12:1.4.3")
	implementation("log4j:log4j:1.2.12")

	testImplementation("javax.servlet:servlet-api:2.4")
	testImplementation("junit:junit:4.4")
	testImplementation("org.jmock:jmock:2.4.0")
	testImplementation("org.springframework:spring-webmvc:2.5.2")
	testImplementation("org.springframework:spring-mock:2.0.7")
}

//test {
//  include '**/*Test.class'
//  exclude '**/net/sf/webdav/testutil/**/*Test.class'
//}
//

//libs {
//	archive_jar.enabled=true
//
//	jar(classifier: 'sources') {
//		fileSet(dir: 'src/main/java')
//	}
//
//	jar(classifier: 'javadoc') {
//		fileSet(dir: new File(buildDir, "docs/javadoc"))
//	}
//}
