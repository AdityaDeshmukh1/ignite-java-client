# Ignite ACID Transaction Tests

This project demonstrates testing of ACID properties (Atomicity, Consistency, Isolation, Durability) using Apache Ignite's native Java API with transactional caches.

## Features

- Configures Ignite with persistence enabled.
- Runs isolated tests for each ACID property.
- Validates transactional behavior on a partitioned transactional cache.

## Setup

- Requires Java 11+ and Maven.
- Apache Ignite dependencies managed via Maven.
- Run `IgniteNativeACIDTest.java` to execute tests.

## Usage

```bash
mvn compile exec:java -Dexec.mainClass=com.example.IgniteNativeACIDTest

