/*
 * Copyright OpenSearch Contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

import net.researchgate.release.ReleaseExtension
import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
  java
  `maven-publish`
  eclipse
  idea
  id("org.ec4j.editorconfig") version "0.0.3"
  id("com.diffplug.spotless") version "6.12.0"
}

buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("gradle.plugin.org.ec4j.gradle:editorconfig-gradle-plugin:0.0.3")
    classpath("com.diffplug.spotless:spotless-plugin-gradle:6.12.0")
    classpath("net.researchgate:gradle-release:3.0.2")
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
  implementation("org.testcontainers:testcontainers:1.17.6") 
  testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
  testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.1")
  testImplementation("ch.qos.logback:logback-classic:1.4.5")
  testImplementation("org.opensearch.client:opensearch-rest-client:2.4.1")
}

group = "org.opensearch"
val build = Properties().apply {
  load(FileInputStream(File(rootProject.rootDir, "version.properties")))
}

// Detect version from version.properties and align it with the build settings
var isSnapshot = "true" == System.getProperty("build.snapshot", "true")
var buildVersion = build.getProperty("version")
if (isSnapshot && !buildVersion.endsWith("SNAPSHOT")) {
  buildVersion = buildVersion + "-SNAPSHOT"
} else if (!isSnapshot && buildVersion.endsWith("SNAPSHOT")) {
  throw GradleException("Expecting release (non-SNAPSHOT) build but version is not set accordingly: " + buildVersion)
}
  
// Check if tag release version (if provided) matches the version from build settings 
val tagVersion = System.getProperty("build.version", buildVersion)
if (!buildVersion.equals(tagVersion)) {
  throw GradleException("The tagged version " + tagVersion + " does not match the build version " + buildVersion)
}

version = buildVersion 
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

publishing {
  repositories{
    if (version.toString().endsWith("SNAPSHOT")) {
      maven("https://aws.oss.sonatype.org/content/repositories/snapshots/") {
        name = "snapshotRepo"
        credentials(PasswordCredentials::class)
      }
    }
    maven("${rootProject.buildDir}/repository") {
      name = "localRepo"
    }
  }

publications {
  create<MavenPublication>("publishMaven") {
    from(components["java"])
      pom {
        name.set("OpenSearch Testcontainers integration")
        packaging = "jar"
        artifactId = "opensearch-testcontainers"
        description.set("OpenSearch Testcontainers integration")
        url.set("https://github.com/opensearch-project/opensearch-testcontainers/")
        scm {
          connection.set("scm:git@github.com:opensearch-project/opensearch-testcontainers.git")
          developerConnection.set("scm:git@github.com:opensearch-project/opensearch-testcontainers.git")
          url.set("git@github.com:opensearch-project/opensearch-testcontainers.git")
        }
        licenses {
          license {
            name.set("The Apache License, Version 2.0")
            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
          }
        }
        developers {
          developer {
            name.set("opensearch-project")
            url.set("https://www.opensearch.org")
            inceptionYear.set("2022")
          }
        }
      }
    }
  }
}
