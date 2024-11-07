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
import zone.cogni.asquare.rdf.ResultSetMapper;
import zone.cogni.asquare.triplestore.RdfStoreService;
import zone.cogni.sem.jena.model.ResultSetDto;
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
     * Provides a function that generates the JSON-LD document for a given URI.
     *
     * @param shaclModel   the SHACL model used for shaping the RDF data into JSON-LD
     * @param entityConfig the indexing configuration
     * @return a function that takes a URI and returns an ObjectNode representing the JSON-LD document
     */
    protected Function<String, ObjectNode> documentProvider(Model shaclModel, final IndexOrchestratorConfig.EntityConfig entityConfig) {
        String constructTemplatePath = entityConfig.getConstruct();
        String constructTemplateParamName = entityConfig.getConstructQueryParam();
        String indexName = entityConfig.getIndex();

        final RDFWriterBuilder jsonLdWriter = Rdf2JsonLd.calculateJsonldWriter(shaclModel);

        return uri -> {
            String constructQuery = TemplateUtils.processResource(templateEngine, uri, constructTemplateParamName, constructTemplatePath, extFolder);
            Model data = rdfStoreService.executeConstructQuery(constructQuery);
            ObjectNode jsonld = Rdf2JsonLd.modelToJsonLd(data, jsonLdWriter);
            jsonld.set("facets", processFacets(entityConfig.getFacets(), uri, entityConfig));
            log.info("Document with uri: {}, Index: {}, Document size: {} bytes", uri, indexName, jsonld.toString().length());
            return jsonld;
        };
    }

    /**
     * Processes the list of facets for a given entity URI and indexing configuration.
     *
     * @param facetsList   the list of facet configurations
     * @param uri          the URI of the entity being indexed
     * @param entityConfig the indexing configuration
     * @return an ObjectNode representing the processed facets
     */
    protected ObjectNode processFacets(List<IndexOrchestratorConfig.FacetConfig> facetsList, String uri, IndexOrchestratorConfig.EntityConfig entityConfig) {
        ObjectNode facets = JsonNodeFactory.instance.objectNode();
        for (IndexOrchestratorConfig.FacetConfig facet : facetsList) {
            try {
                if (facet.getBody() != null && facet.getBody().endsWith(".thymeleaf")) {
                    String template = facet.getBody();
                    String facetQuery = TemplateUtils.processResource(templateEngine, template, extFolder, Map.of("uri", uri, "entityConfig", entityConfig));
                    if (template.endsWith(".sparql.thymeleaf")) {
                        processSparqlFacet(facetQuery, facets);
                    } else if (template.endsWith(".json.thymeleaf")) {
                        processElasticsearchFacet(facet, uri, facetQuery, facets);
                    } else {
                        facets.put(facet.getName(), facetQuery);
                    }
                } else {
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
     * @param facetQuery the SPARQL query to execute
     * @param facets     the ObjectNode to which the facet results will be added
     */
    protected void processSparqlFacet(String facetQuery, ObjectNode facets) {
        ResultSetDto resultSet = rdfStoreService.executeSelectQuery(facetQuery, ResultSetMapper::resultSetToResultSetDto);

        for (String var : resultSet.getVars()) {
            String[] varParts = var.split("_");
            ObjectNode root = facets;
            for (int i = 0; i < varParts.length - 1 && varParts.length > 1; i++) {
                String part = varParts[i];
                if (root.has(part) && root.get(part).isObject()) {
                    root = (ObjectNode) root.get(part);
                } else {
                    ObjectNode child = JsonNodeFactory.instance.objectNode();
                    root.set(part, child);
                    root = child;
                }
            }

            if (varParts[0].endsWith("s")) { // when first word of var is plural then it is always array
                root.putArray(varParts[varParts.length - 1]).addAll(resultSet.collectPropertyValues(var).stream()
                                                                             .filter(Objects::nonNull).map(TextNode::new)
                                                                             .collect(Collectors.toSet()));
            } else { // single is always value
                root.put(varParts[varParts.length - 1], resultSet.collectPropertyValue(var));
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
     * Indexes all entities based on the indexing configurations.
     * Optionally resets the indices before indexing.
     *
     * @param reset if true, resets each index before indexing
     */
    public void indexAll(boolean reset) {
        Set<String> indexReset = new HashSet<>();
        for (IndexOrchestratorConfig.EntityConfig i : config.getIndexing()) {
            if (reset && !indexReset.contains(i.getIndex())) { // Reset each index only once
                ElasticsearchClientUtils.clearIndex(elasticsearchClient, i.getIndex(),
                        TemplateUtils.loadResourceStream(i.getSettings(), extFolder));
                indexReset.add(i.getIndex());
            }
            Model shaclModel = RdfUtils.loadTTL(TemplateUtils.loadResource(i.getShacl(), extFolder));
            String selectSparql = TemplateUtils.processResource(templateEngine, i.getSelect(), extFolder);
            List<String> uris = rdfStoreService.executeSelectQuery(selectSparql,
                                                       ResultSetMapper::resultSetToResultSetDto)
                                               .collectPropertyValues(i.getSelectQueryParam())
                                               .stream().collect(Collectors.toSet()) // Distinct URIs
                                               .stream().collect(Collectors.toList());
            IndexingUtils.simpleIndexAll(elasticsearchClient,
                    i.getIndex(),
                    uris,
                    documentProvider(shaclModel, i));
        }
    }

    /**
     * Indexes a single entity identified by its URI and indexing name.
     *
     * @param uri        the URI of the entity to index
     * @param entityName the name of the indexing configuration to use
     */
    public void indexOne(String uri, String entityName) {
        Optional<IndexOrchestratorConfig.EntityConfig> optionalEntityConfig = config.findIndexingByName(entityName);
        if (optionalEntityConfig.isPresent()) {
            IndexOrchestratorConfig.EntityConfig entityConfig = optionalEntityConfig.get();
            Model shaclModel = RdfUtils.loadTTL(TemplateUtils.loadResource(entityConfig.getShacl(), extFolder));
            ObjectNode jsonld = documentProvider(shaclModel, entityConfig).apply(uri);

            IndexingUtils.simpleIndexOne(elasticsearchClient,
                    entityConfig.getIndex(),
                    uri,
                    jsonld);
        } else {
            log.warn("Indexing configuration with name {} not found.", entityName);
        }
    }
}
