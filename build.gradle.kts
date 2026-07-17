/*
 * Copyright 2026 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    java
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.git.properties)
}

group = "dev.waterdog.waterdogpe"
version = "2.0.4-SNAPSHOT"
description = "Brand new Minecraft: Bedrock Edition proxy created by authors of well-known Waterdog proxy"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenLocal() // locally built protocol/raknet snapshots take priority, like with Maven
    mavenCentral()
    maven("https://repo.opencollab.dev/maven-releases/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
    maven("https://repo.waterdog.dev/main")
}

dependencies {
    implementation(libs.bugsnag)
    implementation(libs.bstats.base)
    implementation(libs.yamler.core)
    implementation(libs.snakeyaml)
    implementation(libs.gson)
    implementation(libs.fastutil)
    implementation(libs.commons.lang3)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    implementation(libs.disruptor)
    implementation(libs.jline.asProvider())
    implementation(libs.jline.terminal.asProvider())
    implementation(libs.jline.terminal.jna)
    implementation(libs.jline.reader)
    implementation(libs.terminalconsoleappender) {
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
        exclude(group = "org.jline")
    }
    implementation(libs.bedrock.codec) {
        exclude(group = "io.netty", module = "netty-buffer")
    }
    implementation(libs.bedrock.connection)
    implementation(libs.netty.transport.raknet)
    // Pulled in transitively by netty-transport-raknet for PROXY protocol support;
    // pinned here to keep it aligned with the rest of Netty.
    implementation(libs.netty.codec.haproxy)
    implementation(variantOf(libs.netty.transport.native.epoll) { classifier("linux-x86_64") })
    implementation(variantOf(libs.netty.transport.native.kqueue) { classifier("osx-x86_64") })
    implementation(libs.nimbus.jose.jwt)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gitProperties {
    failOnNoGitDirectory = false
    keys = listOf("git.branch", "git.commit.id", "git.commit.id.abbrev", "git.commit.time", "git.build.version")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "dev.waterdog.waterdogpe.WaterdogPE"
}

tasks.shadowJar {
    archiveFileName = "Waterdog.jar"
    archiveClassifier = ""
    manifest.attributes["Multi-Release"] = "true"
    // FFM terminal provider needs Java 22+; strip it like the old shade filter did
    exclude("org/jline/terminal/impl/ffm/**")
    relocate("org.bstats", "dev.waterdog")
    transform(Log4j2PluginsCacheFileTransformer::class.java)
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

// publish the shaded jar as the main artifact with the full dependency pom,
// matching the old maven-shade behavior
listOf(configurations.apiElements, configurations.runtimeElements).forEach { elements ->
    elements.configure {
        outgoing.artifacts.removeIf { it.buildDependencies.getDependencies(null).contains(tasks.jar.get()) }
        outgoing.artifact(tasks.shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "waterdog"
            url = uri(
                if (version.toString().endsWith("-SNAPSHOT")) "https://repo.waterdog.dev/snapshots"
                else "https://repo.waterdog.dev/releases"
            )
            credentials(PasswordCredentials::class)
        }
    }
}