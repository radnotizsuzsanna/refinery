/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.jmh")
}

dependencies {
	implementation(libs.eclipseCollections.api)
    implementation(project(mapOf("path" to ":refinery-interpreter")))
    runtimeOnly(libs.eclipseCollections)
}
