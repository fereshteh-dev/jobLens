# ADR-0004: Maven for the backend build

Date: 2026-09-21

## Context

User decision (asked explicitly per the brief's "ask before big decisions" rule).

## Decision

Maven, with the standard `spring-boot-starter-parent` (4.1.1) as parent POM.

## Why

Matches start.spring.io's default, keeps ArchUnit/JaCoCo/Spotless/Error Prone plugin
configuration declarative and well-documented, and avoids adding Gradle/Kotlin-DSL build
logic surface to a project whose explicit goal is not to over-engineer. Gradle's incremental
build speed matters less here — one backend module, no large multi-module build.
