package zone.cogni.semanticz.indexer.orchestrator;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFWriterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.thymeleaf.TemplateEngine;
import zone.cogni.semanticz.connectors.general.RdfStoreService;
import zone.cogni.semanticz.indexer.tools.ResultSetDto;
import zone.cogni.semanticz.indexer.tools.ResultSetMapper;
import zone.cogni.semanticz.indexer.utils.ElasticsearchClientUtils;
import zone.cogni.semanticz.indexer.utils.IndexingUtils;
import zone.cogni.semanticz.jsonldshaper.Rdf2JsonLd;
import zone.cogni.semanticz.jsonldshaper.utils.RdfUtils;
import zone.cogni.semanticz.webflux.TemplateUtils;
import zone.cogni.semanticz.webflux.WebProxy;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * IndexOrchestrator is responsible for orchestrating the indexing process of entities into Elasticsearch.
 * It supports both SPARQL and Elasticsearch facets and allows for configuration-driven indexing.
 * This class provides methods to index all entities or a single entity based on the provided configuration.
 */
public class IndexOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IndexOrchestrator.class);

    protected final RdfStoreService rdfStoreService;
    protected final ElasticsearchClient elasticsearchClient;
    protected final String extFolder;
    protected final IndexOrchestratorConfig config;
    protected final WebProxy webProxy;
    protected final TemplateEngine templateEngine;

    public IndexOrchestrator(RdfStoreService rdfStoreService,
                             ElasticsearchClient elasticsearchClient,
                             IndexOrchestratorConfig config,
                             WebProxy webProxy,
                             TemplateEngine templateEngine,
                             String extFolder) {
        this.rdfStoreService = rdfStoreService;
        this.elasticsearchClient = elasticsearchClient;
        this.templateEngine = templateEngine;
        this.extFolder = extFolder;
        this.webProxy = webProxy;
        this.config = config;
    }

    /**
     * Allows subclasses to provide a custom RDFWriterBuilder for JSON-LD writing.
     * By default, it uses Rdf2JsonLd.calculateJsonldWriter(shaclModel) to create the writer.
     *
     * @param shaclModel the SHACL model used for shaping the RDF data into JSON-LD
     * @return an RDFWriterBuilder configured for JSON-LD writing
     */
    protected RDFWriterBuilder getJsonLdWriter(Model shaclModel) {
        return Rdf2JsonLd.calculateJsonldWriter(shaclModel);
    }

    /**
     * Provides a function that generates the JSON-LD document for a given URI.
     *
     * @param shaclModel   the SHACL model used for shaping the RDF data into JSON-LD
     * @param entityConfig the indexing configuration
     * @return a function that takes a URI and returns an ObjectNode representing the JSON-LD document
     */
    protected Function<String, ObjectNode> documentProvider(Model shaclModel,
                                                            IndexOrchestratorConfig.EntityConfig entityConfig) {
        String constructTemplatePath = entityConfig.getConstruct();
        String constructTemplateParamName = entityConfig.getConstructQueryParam();
        String indexName = entityConfig.getIndex();

        final RDFWriterBuilder jsonLdWriter = getJsonLdWriter(shaclModel);

        return uri -> {
            String constructQuery = TemplateUtils.processResource(
                    templateEngine,
                    uri,
                    constructTemplateParamName,
                    constructTemplatePath,
                    extFolder
            );

            Model data = rdfStoreService.executeConstructQuery(constructQuery);

            ObjectNode jsonld = Rdf2JsonLd.modelToJsonLd(data, jsonLdWriter);

            jsonld.set("facets", processFacets(entityConfig.getFacets(), uri, entityConfig));

            log.info("Document with uri: {}, Index: {}, Document size: {} bytes",
                    uri, indexName, jsonld.toString().length());

            return jsonld;
        };
    }

    /**
     * Introduces a hook that allows subclasses to provide custom facet logic.
     * If this method returns true, the facet is considered handled and the default logic won't execute.
     *
     * @param facet        the facet configuration
     * @param uri          the URI of the entity being indexed
     * @param entityConfig the indexing configuration
     * @param facets       the ObjectNode representing the processed facets
     * @return true if facet is handled by the custom logic, false otherwise
     */
    protected boolean handleCustomFacet(IndexOrchestratorConfig.FacetConfig facet, String uri, IndexOrchestratorConfig.EntityConfig entityConfig, ObjectNode facets) {
        // By default, do nothing and return false
        // Subclasses can override this method to add custom facet processing logic
        return false;
    }

    /**
     * Processes the list of facets for a given entity URI and indexing configuration.
     *
     * @param facetsList   the list of facet configurations
     * @param uri          the URI of the entity being indexed
     * @param entityConfig the indexing configuration
     * @return an ObjectNode representing the processed facets
     */
    protected ObjectNode processFacets(List<IndexOrchestratorConfig.FacetConfig> facetsList,
                                       String uri,
                                       IndexOrchestratorConfig.EntityConfig entityConfig) {
        ObjectNode facets = JsonNodeFactory.instance.objectNode();

        for (IndexOrchestratorConfig.FacetConfig facet : facetsList) {
            try {
                // Check if the facet can be handled by a subclass's custom logic.
                boolean handled = handleCustomFacet(facet, uri, entityConfig, facets);
                if (handled) {
                    continue;
                }

                // Default facet handling
                String body = facet.getBody();
                if (body != null && body.endsWith(".thymeleaf")) {
                    String facetQuery = TemplateUtils.processResource(
                            templateEngine,
                            body,
                            extFolder,
                            Map.of("uri", uri, "entityConfig", entityConfig)
                    );

                    if (body.endsWith(".sparql.thymeleaf")) {
                        // SPARQL query-based facet
                        processSparqlFacet(facetQuery, facets);
                    } else if (body.endsWith(".json.thymeleaf")) {
                        // JSON-based facet (ElasticSearch)
                        processElasticsearchFacet(facet, uri, facetQuery, facets);
                    } else {
                        // Any other Thymeleaf template (just store the processed result)
                        facets.put(facet.getName(), facetQuery);
                    }
                } else {
                    // Non-Thymeleaf facet, assume Elasticsearch-based
                    processElasticsearchFacet(facet, uri, null, facets);
                }
            } catch (Exception ex) {
                log.error("Error processing facet {}: {}", facet, ex.getMessage(), ex);
            }
        }

        return facets;
    }

    /**
     * Processes a SPARQL facet query and adds the results to the facets object.
     *
     * <p>This method executes a SPARQL SELECT query and iterates over the returned ResultSet.
     * Each variable in the query result corresponds to a certain piece of data that we want
     * to place into our JSON structure. Variables are named with an underscore "_" to represent
     * hierarchical nesting within the resulting JSON. For example, a variable named "authors_name"
     * would be split into ["authors", "name"], indicating that the value should be nested like:
     *
     * <pre>
     * {
     *   "authors": {
     *     "name": "someValue"
     *   }
     * }
     * </pre>
     * <p>
     * If the first segment of the variable name is plural (e.g., "authors"), it implies that
     * multiple values are expected for that branch. In this case, the method creates an array
     * rather than a single value. For example, a variable called "authors_names" (with an 's'
     * at the end of the first segment) would produce:
     *
     * <pre>
     * {
     *   "authors": {
     *     "names": ["value1", "value2", ...]
     *   }
     * }
     * </pre>
     * <p>
     * The logic is as follows:
     * 1. Split the variable name by underscores.
     * 2. Use the parts before the last part to build a hierarchy of JSON objects.
     * 3. If the first part of the variable ends with 's', treat the final part as an array key.
     * Otherwise, treat it as a single value.
     * 4. Insert the collected values at the final leaf key, either as an array or a single value.
     * <p>
     * This approach allows for flexible hierarchical JSON construction directly from SPARQL results
     * by simply naming variables with underscore delimiters to indicate structure.
     *
     * @param facetQuery the SPARQL query to execute
     * @param facets     the ObjectNode to which the facet results will be added
     */

    protected void processSparqlFacet(String facetQuery, ObjectNode facets) {
        ResultSetDto resultSet = rdfStoreService.executeSelectQuery(facetQuery, ResultSetMapper::resultSetToResultSetDto);

        // For each variable, we split its name by underscore to determine the JSON nesting.
        // For example, if var = "authors_name", varParts = ["authors", "name"].
        // We'll create a structure like: facets.authors.name = value.
        //
        // If the first element ("authors") ends with 's', it means it's an array.
        // That tells us to put multiple values under "name" as an array.
        //
        // If we had more levels, e.g., "books_authors_name",
        // we'd create: facets.books.authors.name
        // The logic is generic for arbitrary nesting levels.
        for (String var : resultSet.getVars()) {
            String[] varParts = var.split("_");
            ObjectNode currentNode = facets;

            // Traverse through all but the last part to build or reuse nested objects
            for (int i = 0; i < varParts.length - 1 && varParts.length > 1; i++) {
                String part = varParts[i];
                if (currentNode.has(part) && currentNode.get(part).isObject()) {
                    currentNode = (ObjectNode) currentNode.get(part);
                } else {
                    ObjectNode child = JsonNodeFactory.instance.objectNode();
                    currentNode.set(part, child);
                    currentNode = child;
                }
            }

            String lastPart = varParts[varParts.length - 1];

            // If the first part is plural, interpret the last part as an array of values
            if (varParts[0].endsWith("s")) {
                currentNode.putArray(lastPart).addAll(
                        resultSet.collectPropertyValues(var).stream()
                                 .filter(Objects::nonNull)
                                 .map(TextNode::new)
                                 .collect(Collectors.toSet())
                );
            } else {
                // Otherwise, it's a single value
                currentNode.put(lastPart, resultSet.collectPropertyValue(var));
            }
        }
    }

    /**
     * Processes an Elasticsearch facet and adds the results to the facets object.
     *
     * @param facet      the facet configuration
     * @param uri        the URI of the entity being indexed
     * @param facetQuery the facet query, if applicable
     * @param facets     the ObjectNode to which the facet results will be added
     */
    protected void processElasticsearchFacet(IndexOrchestratorConfig.FacetConfig facet, String uri, String facetQuery, ObjectNode facets) {
        String esPath = facet.getPath().replace("<DOCUMENT_ID>", URLEncoder.encode(uri, StandardCharsets.UTF_8));
        try {
            ResponseEntity<String> response = webProxy.proxyResponse(esPath, facet.getMethod(), facet.getAccept(), facet.getContentType(), facetQuery);
            int statusCode = response.getStatusCodeValue();

            if (statusCode >= 200 && statusCode < 300) {
                facets.set(facet.getName(), new ObjectMapper().readTree(response.getBody()));
            } else {
                log.warn("Elastic facet query status {}. Path: {}. Facet: {}", statusCode, esPath, facet);
            }
        } catch (Exception e) {
            log.error("Error executing Elasticsearch POST for facet {}. Path {}. Query: {}", facet.getName(), esPath, facetQuery, e);
        }
    }

    /**
     * Performs indexing for all configured entities.
     * If requested, the associated Elasticsearch indices are reset before indexing.
     *
     * @param reset if true, each index is cleared before indexing begins
     */
    public void indexAll(boolean reset) {
        Set<String> resetIndices = new HashSet<>();

        for (IndexOrchestratorConfig.EntityConfig entityConfig : config.getIndexing()) {
            // Reset the index once if requested
            if (reset && !resetIndices.contains(entityConfig.getIndex())) {
                ElasticsearchClientUtils.clearIndex(
                        elasticsearchClient,
                        entityConfig.getIndex(),
                        TemplateUtils.loadResourceStream(entityConfig.getSettings(), extFolder)
                );
                resetIndices.add(entityConfig.getIndex());
            }

            // Load SHACL model and prepare the SPARQL select query
            Model shaclModel = RdfUtils.loadTTL(
                    TemplateUtils.loadResource(entityConfig.getShacl(), extFolder)
            );
            String selectSparql = TemplateUtils.processResource(
                    templateEngine,
                    entityConfig.getSelect(),
                    extFolder
            );

            // Execute the SPARQL query and gather distinct URIs to index
            List<String> uris = rdfStoreService.executeSelectQuery(selectSparql, ResultSetMapper::resultSetToResultSetDto)
                                               .collectPropertyValues(entityConfig.getSelectQueryParam())
                                               .stream()
                                               .distinct()
                                               .collect(Collectors.toList());

            // Perform the indexing of retrieved URIs
            IndexingUtils.simpleIndexAll(
                    elasticsearchClient,
                    entityConfig.getIndex(),
                    uris,
                    documentProvider(shaclModel, entityConfig)
            );
        }
    }

    /**
     * Indexes a single entity identified by its URI using the specified indexing configuration.
     *
     * @param uri        the URI of the entity to index
     * @param entityName the name of the indexing configuration to use
     */
    public void indexOne(String uri, String entityName) {
        Optional<IndexOrchestratorConfig.EntityConfig> optionalConfig = config.findIndexingByName(entityName);

        if (!optionalConfig.isPresent()) {
            log.warn("Indexing configuration with name {} not found.", entityName);
            return;
        }

        IndexOrchestratorConfig.EntityConfig entityConfig = optionalConfig.get();
        Model shaclModel = RdfUtils.loadTTL(
                TemplateUtils.loadResource(entityConfig.getShacl(), extFolder)
        );
        ObjectNode jsonld = documentProvider(shaclModel, entityConfig).apply(uri);

        IndexingUtils.simpleIndexOne(
                elasticsearchClient,
                entityConfig.getIndex(),
                uri,
                jsonld
        );
    }
}
