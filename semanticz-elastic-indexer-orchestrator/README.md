# Index Orchestrator

## Introduction

The **Index Orchestrator** is a configurable, out-of-the-box indexing service that supports both SPARQL and Elasticsearch facets. It facilitates the indexing of entities from an RDF store into Elasticsearch.

## Key Features

- **Configurable per entity**: Define how each entity type is selected and indexed, allowing for different configurations per entity.
- **Supports SPARQL and Elasticsearch facets**: Use SPARQL queries or Elasticsearch queries to compute facets for your indexed documents.
- **Template-based**: Utilize Thymeleaf templates for dynamic query generation and string evaluation.
- **SHACL-based shaping**: Use SHACL shapes to define how RDF data is transformed into JSON-LD for indexing.

## Configuration Structure

The configuration is divided per entity, and each entity can reside in a different Elasticsearch index. Each entity configuration includes:

- **Selection of entities**: Define how to select entities from the RDF store.
- **Construction of index documents**: Define how to construct the index document for each entity.
- **Facets**: Configure SPARQL and Elasticsearch facets.

### Example Configuration

Here is an example of how the configuration might look in a project:

```yaml
indexing:
  orchestrator:
    list:
      - name: "dataset"
        index: "datacat.data"
        shacl: model/dcat-ep-ed.shapes.ttl
        construct: "index/dataset/construct-data-assets.sparql.thymeleaf"
        construct_query_param: "uri"
        select: "index/dataset/select-data-assets.sparql.thymeleaf"
        select_query_param: "uri"
        settings: "index/settings.datacat.data.json"
        facets:
          - body: "index/types.sparql.thymeleaf"
          - name: rootUri
            body: "index/rootUri.thymeleaf"
          - name: config
            body: "index/config.thymeleaf"
          - body: "index/dataset/created.sparql.thymeleaf"
          - body: "index/dataset/modified.sparql.thymeleaf"
          - body: "index/dataset/texts.sparql.thymeleaf"
          - body: "index/dataset/title.sparql.thymeleaf"
          - body: "index/dataset/description.sparql.thymeleaf"
          - name: views
            method: GET
            path: "/datacat.views/_doc/<DOCUMENT_ID>?filter_path=_source.views"
            accept: "*/*"
          - name: popularity
            method: POST
            path: "/datacat.views/_search?filter_path=aggregations.popularity.value"
            accept: "*/*"
            content-type: "application/json"
            body: "index/dataset/popularity.json.thymeleaf"
```

## Facet Configuration

Facets can be of various types:

- **SPARQL facets**: Execute SPARQL queries against the RDF store to compute facet values.
- **Elasticsearch facets**: Execute Elasticsearch queries to compute facet values.
    - GET with no body
    - POST with no body
    - POST with query in JSON body
- **Thymeleaf facets**: Use Thymeleaf templates for calculating single string values.

### SPARQL Facet Example

```yaml
- body: "index/dataset/title.sparql.thymeleaf"
```

Template (`title.sparql.thymeleaf`):

```sparql
PREFIX dcterms: <http://purl.org/dc/terms/>

SELECT
?title_en
?title_fr
{
  {
    SELECT ?title_en {
      <[[${uri}]]> dcterms:title ?title_en
      FILTER(LANG(?title_en) = "en")
    }
  }
  UNION
  {
    SELECT ?title_fr {
      <[[${uri}]]> dcterms:title ?title_fr
      FILTER(LANG(?title_fr) = "fr")
    }
  }
}
```

### Elasticsearch Facet Example

```yaml
- name: popularity
  method: POST
  path: "/datacat.views/_search?filter_path=aggregations.popularity.value"
  accept: "*/*"
  content-type: "application/json"
  body: "index/dataset/popularity.json.thymeleaf"
```

Template (`popularity.json.thymeleaf`):

```json
{
  "size": 0,
  "query": {
    "ids": {
      "values": ["[[${uri}]]"]
    }
  },
  "aggs": {
    "popularity": {
      "sum": {
        "script": {
          "source": "doc['popularity'].value"
        }
      }
    }
  }
}
```

### Thymeleaf Facet Example

```yaml
- name: rootUri
  body: "index/rootUri.thymeleaf"
```

Template (`rootUri.thymeleaf`):

```text
[[${uri}]]
```

## Usage Instructions

### Setting Up IndexOrchestrator

1. **Include IndexOrchestrator in your project**: Add the `IndexOrchestrator` and `IndexOrchestratorConfig` classes to your project.

2. **Configure your application**: Define the indexing configurations in your application's configuration files (e.g., YAML or properties files).

3. **Instantiate IndexOrchestrator**: In your service or component, instantiate `IndexOrchestrator` with the required dependencies.

```java
@Service
public class ElasticProxy extends WebProxy {

    public ElasticProxy(@Value("${elasticsearch.url}") String url,
                        @Value("${elasticsearch.user:}") String username,
                        @Value("${elasticsearch.password:}") String password,
                        @Value("${elasticsearch.proxyPath:}") String indexName,
                        @Value("${elasticsearch.readtimeout:#{null}}") Integer readTimeout,
                        @Value("${elasticsearch.connectTimeout:#{null}}") Integer connectTimeout) throws SSLException {
        super(url, username, password, indexName, readTimeout, connectTimeout);
    }
}

@Service
public class IndexService {

    private final SpringTemplateEngine templateEngine;
    private final IndexOrchestrator indexOrchestrator;

    public IndexService(RdfStoreService rdfStoreService,
                        ElasticsearchClient elasticsearchClient,
                        IndexConfig config,
                        ElasticProxy elasticProxy,
                        @Value("${datacat.ext.folder:defaultFolder}") String extFolder) {
        this.templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(new StringTemplateResolver());
        this.indexOrchestrator = new IndexOrchestrator(
                rdfStoreService,
                elasticsearchClient,
                config.getOrchestrator(),
                elasticProxy,
                templateEngine,
                extFolder
        );
    }

    public void indexAll(boolean reset) {
        indexOrchestrator.indexAll(reset);
    }

    public void indexOne(String uri, String entityName) {
        indexOrchestrator.indexOne(uri, entityName);
    }
}
```

### Indexing All Entities

To index all entities based on the configuration:

```java
// Index all entities, resetting indices first
indexService.indexAll(true);
```

### Indexing a Single Entity

To index a single entity:

```java
String uri = "http://example.com/resource/123";
String indexingName = "dataset";

indexService.indexOne(uri, indexingName);
```

### Facet Type Detection and Configuration Examples

The **Index Orchestrator** identifies the type of each facet based on the file extension of the facet's `body` attribute. This classification ensures that each facet is processed using the appropriate method. Below are the possible facet types along with example configurations for each case:

#### 1. SPARQL Facets (`.sparql.thymeleaf`)

**Description**:  
Facets that execute SPARQL queries against the RDF store to retrieve and compute facet values based on RDF data.

**Configuration Example**:
```yaml
facets:
  - body: "index/dataset/title.sparql.thymeleaf"
```

**Explanation**:  
This facet uses a SPARQL Thymeleaf template to query titles in different languages for the dataset entity.

#### 2. Elasticsearch Facets with JSON Body (`.json.thymeleaf`)

**Description**:  
Facets that send Elasticsearch queries with a JSON body, typically used for complex aggregations or scripted computations within Elasticsearch.

**Configuration Example**:
```yaml
facets:
  - name: popularity
    method: POST
    path: "/datacat.views/_search?filter_path=aggregations.popularity.value"
    accept: "*/*"
    content-type: "application/json"
    body: "index/dataset/popularity.json.thymeleaf"
```

**Explanation**:  
This facet calculates the popularity of a dataset by sending a POST request with a JSON body generated from a Thymeleaf template.

#### 3. Simple Thymeleaf Facets (`.thymeleaf`)

**Description**:  
Facets that use Thymeleaf templates to generate single string values or straightforward JSON structures based on the entity's URI or other contextual information.

**Configuration Example**:
```yaml
facets:
  - name: rootUri
    body: "index/rootUri.thymeleaf"
```

**Explanation**:  
This facet generates the root URI for the entity using a simple Thymeleaf template.

#### 4. Elasticsearch Facets without Body (No `.thymeleaf` Extension)

**Description**:  
Facets that perform Elasticsearch operations using GET or POST requests without a request body, suitable for simple data retrieval or aggregations that do not require additional query parameters.

**Configuration Example**:
```yaml
facets:
  - name: views
    method: GET
    path: "/datacat.views/_doc/<DOCUMENT_ID>?filter_path=_source.views"
    accept: "*/*"
```

**Explanation**:  
This facet retrieves the number of views for a document by sending a GET request to the specified Elasticsearch path without a request body.

### Facet Value Naming Mechanisms

The **Index Orchestrator** determines the JSON field names for facet values based on the facet type and configuration. These naming mechanisms are grouped into two main categories:

#### 1. **Explicit Naming via `name` Attribute**

**Applicable Facet Types:**
- **Elasticsearch Facets with JSON Body (`.json.thymeleaf`)**
- **Simple Thymeleaf Facets (`.thymeleaf`)**
- **Elasticsearch Facets without Body (No `.thymeleaf` Extension)**

**Description:**
For these facet types, the JSON field name is explicitly defined using the `name` attribute in the facet configuration. This approach provides clear and direct control over the naming of each facet value in the resulting JSON.

**Example Configurations:**

- **Elasticsearch Facet with JSON Body:**
  ```yaml
  facets:
    - name: popularity
      method: POST
      path: "/datacat.views/_search?filter_path=aggregations.popularity.value"
      accept: "*/*"
      content-type: "application/json"
      body: "index/dataset/popularity.json.thymeleaf"
  ```
  **Resulting JSON:**
  ```json
  {
    "popularity": 75.5
  }
  ```

- **Simple Thymeleaf Facet:**
  ```yaml
  facets:
    - name: rootUri
      body: "index/rootUri.thymeleaf"
  ```
  **Resulting JSON:**
  ```json
  {
    "rootUri": "http://example.com/resource/123"
  }
  ```

- **Elasticsearch Facet without Body:**
  ```yaml
  facets:
    - name: views
      method: GET
      path: "/datacat.views/_doc/<DOCUMENT_ID>?filter_path=_source.views"
      accept: "*/*"
  ```
  **Resulting JSON:**
  ```json
  {
    "views": 150
  }
  ```

#### 2. **Dynamic Naming Based on SPARQL Query Variables**

**Applicable Facet Type:**
- **SPARQL Facets (`.sparql.thymeleaf`)**

**Description:**
For SPARQL facets, the JSON field names are dynamically derived from the variable names specified in the SPARQL queries. If a variable name contains underscores (`_`), it signifies a nested JSON structure, where each segment separated by an underscore represents a deeper level in the JSON hierarchy.

**Example Configuration:**

- **SPARQL Facet:**
  ```yaml
  facets:
    - body: "index/dataset/s"
  ```

  **SPARQL Query Variables:**
  ```sparql
  SELECT ?title_en ?title_fr
  ```

  **Resulting JSON:**
  ```json
  {
    "title": {
      "en": "English Title",
      "fr": "French Title"
    }
  }
  ```

## License

This project is licensed under the Apache-2.0 License. You may obtain a copy of the License at:

- **[LICENSE](./../LICENSE)**