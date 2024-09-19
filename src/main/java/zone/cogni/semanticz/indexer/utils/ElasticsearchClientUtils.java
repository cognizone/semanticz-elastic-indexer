package zone.cogni.semanticz.indexer.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.DeleteOperation;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class ElasticsearchClientUtils {

    public static void clearIndex(@Nonnull ElasticsearchClient elasticClient,
                                  @Nonnull String index,
                                  @Nonnull InputStream elasticSettingsStream) {
        if (exists(elasticClient, index))
            deleteIndex(elasticClient, index);

        createIndex(elasticClient, index, elasticSettingsStream);
    }

    public static void ensureIndexExists(@Nonnull ElasticsearchClient elasticClient,
                                         @Nonnull String index,
                                         @Nonnull InputStream elasticSettingsStream) {
        if (exists(elasticClient, index)) return;

        createIndex(elasticClient, index, elasticSettingsStream);
    }

    public static void createIndex(@Nonnull ElasticsearchClient elasticClient,
                                   @Nonnull String index,
                                   @Nonnull InputStream elasticSettingsStream) {
        try {
            CreateIndexResponse response = elasticClient.indices()
                    .create(builder -> builder.index(index)
                            .withJson(elasticSettingsStream));

            if (!response.acknowledged()) {
                throw new RuntimeException("Error while creating elastic index '" + index + "'.");
            }
        } catch (ElasticsearchException | IOException e) {
            throw new RuntimeException("Error while creating elastic index '" + index + "'.", e);
        }
    }

    public static boolean exists(@Nonnull ElasticsearchClient elasticClient,
                                 @Nonnull String index) {
        try {
            return elasticClient.indices()
                    .exists(builder -> builder.index(index))
                    .value();
        } catch (ElasticsearchException | IOException e) {
            throw new RuntimeException("Error while checking for existence of index  '" + index + "'.", e);
        }
    }

    public static void deleteIndex(@Nonnull ElasticsearchClient elasticClient,
                                   @Nonnull String index) {
        try {
            DeleteIndexResponse response = elasticClient.indices()
                    .delete(builder -> builder.index(index));

            if (!response.acknowledged()) {
                throw new RuntimeException("Error while deleting index  '" + index + "'.");
            }
        } catch (ElasticsearchException | IOException e) {
            throw new RuntimeException("Error while deleting index  '" + index + "'.", e);
        }
    }

    public static void deleteDocuments(@Nonnull ElasticsearchClient elasticClient,
                                       @Nonnull String index,
                                       @Nonnull List<String> ids) {
        if (ids.isEmpty()) return;

        BulkRequest request = getDeleteItemsBulkRequest(index, ids);

        try {
            BulkResponse response = elasticClient.bulk(request);
            handleBulkResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't send delete bulk request", e);
        }
    }

    private static BulkRequest getDeleteItemsBulkRequest(@Nonnull String index,
                                                         @Nonnull List<String> ids) {
        List<BulkOperation> bulkOperations = ids.stream().map(uri -> createDeleteBulkOperation(index, uri)).collect(Collectors.toList());
        return BulkRequest.of(b -> b.operations(bulkOperations).refresh(Refresh.True));
    }

    private static BulkOperation createDeleteBulkOperation(String index, String uri) {
        DeleteOperation deleteOperation = DeleteOperation.of(b -> b.index(index).id(uri));
        return BulkOperation.of(b -> b.delete(deleteOperation));
    }

    private static void handleBulkResponse(BulkResponse response) {
        if (!response.errors()) return;

        String errorMessage = response.items()
                .stream()
                .filter(item -> item.status() < 200 || item.status() >= 300)
                .map(BulkResponseItem::toString)
                .collect(Collectors.joining(", ", "[", "]"));

        throw new RuntimeException("Elastic delete bulk request returned errors: " + errorMessage);
    }

    public static void deleteDocument(ElasticsearchClient elasticClient, String indexName, String item) {
        try {
            elasticClient.delete(builder -> builder.index(indexName)
                    .id(item)
                    .refresh(Refresh.True));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't delete from " + indexName, e);
        }
    }

}
