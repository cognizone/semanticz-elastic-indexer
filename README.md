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

To index RDF data into Elasticsearch using the `rdf2jsonld` and `elastic-indexer` libraries, add the following dependencies to your project:

### Gradle

```gradle
implementation "zone.cogni.semanticz:rdf2jsonld:{rdf2jsonld.version}"
implementation "zone.cogni.semanticz:elastic-indexer:{rdf2jsonld.version}"
```

### Maven

```xml
<dependency>
  <groupId>zone.cogni.semanticz</groupId>
  <artifactId>rdf2jsonld</artifactId>
  <version>{rdf2jsonld.version}</version>
</dependency>
<dependency>
  <groupId>zone.cogni.semanticz</groupId>
  <artifactId>elastic-indexer</artifactId>
  <version>{rdf2jsonld.version}</version>
</dependency>
```

### Indexing a Document Using a SHACL Model

Here's how you can index a single RDF document into Elasticsearch using a SHACL model for transformation:

```java
public void indexOne(String uri, Model data, Model shaclModel) {
    // Convert the RDF data to JSON-LD using the SHACL model
    String jsonLd = JsonLdUtils.modelToJsonLd(data, shaclModel, jsonLdWriter);

    // Index the JSON-LD document into Elasticsearch
    IndexingUtils.simpleIndexOne(elasticsearchClient, "index-name", uri, jsonLd);
}
```

In this example:

- `uri` is the unique identifier for the document.
- `data` is the RDF `Model` containing your data.
- `shaclModel` is the SHACL `Model` used to guide the transformation during the conversion to JSON-LD.
- `jsonLdWriter` is an instance of `JsonLdWriter` configured according to your needs.
- `elasticsearchClient` is your Elasticsearch client instance.
- `"index-name"` is the name of the Elasticsearch index where the document will be stored.
- `JsonLdUtils.modelToJsonLd()` converts the RDF `Model` to a JSON-LD string using the SHACL model.
- `IndexingUtils.simpleIndexOne()` indexes the JSON-LD document into Elasticsearch.

## Running Tests

Run unit tests using Gradle:

```bash
gradle test
```

## License

This project is licensed under the Apache-2.0 license - see the `LICENSE` file for details.
