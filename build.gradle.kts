/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.researchgate.release.ReleaseExtension

plugins {
  java
  `maven-publish`
  eclipse
  idea
  id("org.ec4j.editorconfig") version "0.0.3"
  id("com.diffplug.spotless") version "6.8.0"
}

buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("gradle.plugin.org.ec4j.gradle:editorconfig-gradle-plugin:0.0.3")
    classpath("com.diffplug.spotless:spotless-plugin-gradle:6.8.0")
    classpath("net.researchgate:gradle-release:3.0.0")
  }
}

apply(plugin = "org.ec4j.editorconfig")
apply(plugin = "com.diffplug.spotless")
apply(plugin = "net.researchgate.release")

repositories {
  mavenLocal()
  maven {
    url = uri("https://repo.maven.apache.org/maven2/")
  }
}

dependencies {
  implementation("org.testcontainers:testcontainers:1.17.3") 
  testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
  testImplementation("ch.qos.logback:logback-classic:1.2.11")
  testImplementation("org.opensearch.client:opensearch-rest-client:2.0.1")
}

group = "org.opensearch"
version = "2.0.0-SNAPSHOT"
description = "Testcontainers for Opensearch"
java.sourceCompatibility = JavaVersion.VERSION_11

java {
  withSourcesJar()
  withJavadocJar()
}

publishing {
  publications.create<MavenPublication>("maven") {
    from(components["java"])
  }
}

tasks.withType<JavaCompile>() {
  options.encoding = "UTF-8"
}

tasks.test {
  useJUnitPlatform()
  reports {
    junitXml.required.set(true)
    html.required.set(true)
  }
}

spotless {
  java {
    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()

    removeUnusedImports()
    importOrder()
    palantirJavaFormat()
  }
}

configure<ReleaseExtension> {
  with(git) {
    requireBranch.set("main")
  }
}
