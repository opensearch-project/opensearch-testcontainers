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
  id("com.diffplug.spotless") version "7.0.4"
}

buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("gradle.plugin.org.ec4j.gradle:editorconfig-gradle-plugin:0.0.3")
    classpath("com.diffplug.spotless:spotless-plugin-gradle:7.0.4")
    classpath("net.researchgate:gradle-release:3.1.0")
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
  maven {
    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
  }
  maven {
    url = uri("https://aws.oss.sonatype.org/content/repositories/snapshots/")
  }
}

dependencies {
  implementation("org.testcontainers:testcontainers:1.21.3")
  testImplementation(platform("org.junit:junit-bom:5.13.1"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("ch.qos.logback:logback-classic:1.5.18")
  testImplementation("org.opensearch.client:opensearch-rest-client:3.0.0")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21

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
    leadingTabsToSpaces()
    endWithNewline()

    removeUnusedImports()
    importOrder()
    palantirJavaFormat()
  }
}

configure<ReleaseExtension> {
  with(git) {
    requireBranch.set("main")
    versionPropertyFile.set("version.properties")
  }
}

publishing {
  repositories{
    if (version.toString().endsWith("SNAPSHOT")) {
      maven("https://central.sonatype.com/repository/maven-snapshots/") {
        name = "snapshotRepo"
        credentials {
            username = System.getenv("SONATYPE_USERNAME")
            password = System.getenv("SONATYPE_PASSWORD")
        }
      }
    }
    maven(rootProject.layout.buildDirectory.dir("repository")) {
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
