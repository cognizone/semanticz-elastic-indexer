# SemantiCZ Elastic Indexer

## Overview

SemantiCZ Elastic Indexer is a suite of Java-based tools designed to facilitate the indexing and searching of RDF (Resource Description Framework) data within Elasticsearch. 

- **[semanticz-elastic-indexer](./semanticz-elastic-indexer/README.md)**
- **[semanticz-elastic-indexer-orchestrator](./semanticz-elastic-indexer-orchestrator/README.md)**
- **[semanticz-webflux-tools](./semanticz-webflux-tools/README.md)**

---

## Subprojects

### 1. semanticz-elastic-indexer

**Description**: This core library simplifies the process of indexing and searching documents in Elasticsearch. It allows for the conversion of RDF data into JSON-LD using SHACL (Shapes Constraint Language) shapes and provides utilities for both simple and bulk indexing methods.

**Key Features**:

- **RDF to JSON-LD Conversion**: Transforms RDF models into JSON-LD format using SHACL models, enabling seamless integration with Elasticsearch.
- **Indexing Utilities**: Offers straightforward methods for indexing documents into Elasticsearch clusters, supporting both individual and bulk operations.
- **Integration with Other Libraries**: Works in conjunction with `semanticz-rdf2jsonld` to enhance data transformation capabilities.

---

### 2. semanticz-elastic-indexer-orchestrator

**Description**: A configurable indexing service that orchestrates the indexing of entities from an RDF store into Elasticsearch. It supports both SPARQL and Elasticsearch facets, providing flexibility in defining how each entity type is selected and indexed.

**Key Features**:

- **Per-Entity Configuration**: Allows distinct configurations for different entity types, specifying selection criteria and indexing strategies.
- **Facet Support**: Enables the use of SPARQL queries and Elasticsearch queries to compute facets, enhancing search and aggregation functionalities.
- **Template-Based Configuration**: Utilizes Thymeleaf templates for dynamic query generation and string evaluation, offering flexibility and reusability in configurations.
- **SHACL-Based Shaping**: Employs SHACL shapes to define how RDF data is transformed into JSON-LD, ensuring data consistency and integrity.

---

### 3. semanticz-webflux-tools

**Description**: A utility library providing tools for working with proxy calls using Spring WebFlux and processing templates with Thymeleaf.

**Key Features**:

- **Proxy Endpoint Simplification**: Simplifies the creation and management of proxy endpoints within WebFlux applications.
- **Template Processing**: Facilitates the use of Thymeleaf for processing templates.
- **Utility Methods**: Provides helpful methods for resource loading and processing.

---

## Getting Started

### Prerequisites

- **Java**: Version 11 or higher.
- **Gradle**: Version 8.5 or higher (if building from source).
- **Elasticsearch Cluster**: Required for indexing and searching operations.
- **RDF Store**: Necessary for data sourcing, especially when using the Index Orchestrator.

### Installation

The libraries are available on Maven Central and can be included in your project by adding the appropriate dependencies to your build configuration. Ensure that you include only the modules relevant to your project's needs.

### Building from Source

To build the project from source:

1. **Clone the Repository**: Obtain the source code by cloning the repository to your local machine.

2. **Navigate to the Project Directory**: Change your working directory to the cloned repository.

3. **Build with Gradle**: Use Gradle to build the project. The build artifacts for each subproject will be located in their respective `build/libs` directories after a successful build.

---

## License

This project is licensed under the Apache-2.0 License. You may obtain a copy of the License at:

- **[LICENSE](./LICENSE)**

