/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
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
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
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
