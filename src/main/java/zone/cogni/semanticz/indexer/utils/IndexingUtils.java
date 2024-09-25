package zone.cogni.semanticz.indexer.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IndexingUtils {

    private static final Logger log = LoggerFactory.getLogger(IndexingUtils.class);

    public static void handleElasticBulkResponse(List<BulkResponse> elasticResponses) {
        List<BulkResponse> responsesWithError = elasticResponses.stream()
                                                                .filter(BulkResponse::errors)
                                                                .collect(Collectors.toList());
        if (!responsesWithError.isEmpty()) {
            String errorString = responsesWithError.stream()
                                                   .map(IndexingUtils::getFailedBulkRequestsString)
                                                   .collect(Collectors.joining("\n", "[", "]"));
            log.error("Elastic bulk response contained errors.\n\t " + errorString);
            throw new RuntimeException("Elastic response got errors. Check logs.");
        }
    }

    private static String getFailedBulkRequestsString(BulkResponse response) {
        return response.items()
                       .stream()
                       .filter(item -> item.status() < 200 || item.status() >= 300)
                       .map(BulkResponseItem::toString)
                       .collect(Collectors.joining(", ", "[", "]"));
    }

    private static Callable<BulkResponse> getRequestCallable(ElasticsearchClient elasticsearchClient, BulkRequest request) {
        return () -> {
            try {
                return elasticsearchClient.bulk(request);
            } catch (IOException e) {
                String documentIds = request.operations()
                                            .stream()
                                            .map(bulkOperation -> bulkOperation.index().id())
                                            .collect(Collectors.joining(",\n", "[", "]"));

                throw new RuntimeException("Something went wrong while sending bulk request with ids " + documentIds, e);
            }
        };
    }

    private static <T> Optional<T> callAndSaveException(Callable<T> callable, List<Throwable> exceptions) {
        try {
            return Optional.of(callable.call());
        } catch (Exception e) {
            exceptions.add(e);
            return Optional.empty();
        }
    }

    public static BulkOperation parseIndexRequest(String index, String id, JsonNode document) {
        return BulkOperation.of(builder -> builder.index(idx -> idx.index(index)
                                                                   .id(id)
                                                                   .document(document)));
    }

    public static BulkRequest createBulkRequest(List<BulkOperation> operations, boolean forceRefresh) {
        Refresh refreshParam = forceRefresh ? Refresh.True : Refresh.False;
        return BulkRequest.of(builder -> builder.operations(operations)
                                                .refresh(refreshParam));
    }

    public static void simpleIndexAll(ElasticsearchClient elasticClient, String indexName, List<String> uris, Function<String, ObjectNode> documentProvider) {
        List<Throwable> exceptions = new ArrayList<>();
        List<BulkResponse> elasticResponses = new ArrayList<>();
        for (String uri : uris) {
            Optional<BulkResponse> responseOptional = simpleIndexOne(elasticClient, indexName, uri, documentProvider.apply(uri), exceptions);
            if (responseOptional.isPresent()) {
                elasticResponses.add(responseOptional.get());
            }
        }
        IndexingUtils.handleElasticBulkResponse(elasticResponses);
    }

    public static void simpleIndexOne(ElasticsearchClient elasticClient, String indexName, String uri, ObjectNode document) {
        List<Throwable> exceptions = new ArrayList<>();
        Optional<BulkResponse> responseOptional = simpleIndexOne(elasticClient, indexName, uri, document, exceptions);
        if (responseOptional.isPresent()) {
            IndexingUtils.handleElasticBulkResponse(List.of(responseOptional.get()));
        }
    }

    public static Optional<BulkResponse> simpleIndexOne(ElasticsearchClient elasticClient, String indexName, String uri, ObjectNode document, List<Throwable> exceptions) {
        BulkOperation op = IndexingUtils.parseIndexRequest(indexName, uri, document);
        BulkRequest request = IndexingUtils.createBulkRequest(List.of(op), true);
        Callable<BulkResponse> callableRequest = IndexingUtils.getRequestCallable(elasticClient, request);
        return IndexingUtils.callAndSaveException(callableRequest, exceptions);
    }

}
