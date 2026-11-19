// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

plugins {
    kotlin("jvm")
    application
}

val feedUsername = providers.environmentVariable("ENV_VSTS_MVN_CRED_USERNAME")
    .orElse(providers.gradleProperty("vstsUsername"))
val feedToken = providers.environmentVariable("ENV_VSTS_MVN_CRED_ACCESSTOKEN")
    .orElse(providers.gradleProperty("vstsMavenAccessToken"))

repositories.withType<MavenArtifactRepository>().forEach {
    if (it.url.host == "identitydivision.pkgs.visualstudio.com") {
        it.credentials {
            username = feedUsername.orNull
            password = feedToken.orNull
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.json:json:${rootProject.extra["jsonVersion"]}")
    testImplementation("junit:junit:${rootProject.extra["junitVersion"]}")
}

application {
    mainClass.set("com.microsoft.identity.tools.flightgovernance.MainKt")
}
