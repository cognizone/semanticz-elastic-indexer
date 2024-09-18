# Elastic Indexer

The 'elastic-indexer' is a Java-based tool that helps in indexing and searching documents in an Elasticsearch cluster using simple and bulk methods.

## Prerequisites

- Java 11 or higher
- Gradle 8.5 or higher for building the project

## Installation

### From Maven Central

To include `elastic-indexer` in your project, add the following dependency:

#### For Gradle users:

```gradle
dependencies {
    implementation 'zone.cogni.semanticz:elastic-indexer:1.0.0'
}
```

#### For Maven users:

```xml
<dependency>
    <groupId>zone.cogni.semanticz</groupId>
    <artifactId>elastic-indexer</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Building From Source

To build the project from source:

1. Clone the repository:
   ```bash
   git clone https://github.com/cognizone/semanticz-elastic-indexer.git
   cd elastic-indexer
   ```

2. Build with Gradle:
   ```bash
   gradle clean build
   ```

The build artifacts will be stored in the `build/libs` directory.

## Usage

Here’s how you can use the library to convert RDF data using SHACL:

```java

```

## Running Tests

Run unit tests using Gradle:

```bash
gradle test
```

## License

This project is licensed under the Apache-2.0 license - see the `LICENSE` file for details.
