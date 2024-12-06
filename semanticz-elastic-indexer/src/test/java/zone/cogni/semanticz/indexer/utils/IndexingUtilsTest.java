package zone.cogni.semanticz.indexer.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class IndexingUtilsTest {

    @Test
    public void testHandleElasticBulkResponse_noErrors() {
        // Arrange
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);

        List<BulkResponse> responses = Collections.singletonList(bulkResponse);

        // Act & Assert
        assertDoesNotThrow(() -> IndexingUtils.handleElasticBulkResponse(responses));
    }

    @Test
    public void testHandleElasticBulkResponse_withErrors() {
        // Arrange
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(true);
        when(bulkResponse.items()).thenReturn(Collections.emptyList());

        List<BulkResponse> responses = Collections.singletonList(bulkResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> IndexingUtils.handleElasticBulkResponse(responses));
        assertEquals("Elastic response got errors. Check logs.", exception.getMessage());
    }

    @Test
    public void testParseIndexRequest() {
        // Arrange
        String index = "test_index";
        String id = "test_id";
        ObjectNode document = mock(ObjectNode.class);

        // Act
        BulkOperation operation = IndexingUtils.parseIndexRequest(index, id, document);

        // Assert
        assertNotNull(operation);
        assertEquals(index, operation.index().index());
        assertEquals(id, operation.index().id());
        assertEquals(document, operation.index().document());
    }

    @Test
    public void testCreateBulkRequest_withForceRefreshTrue() {
        // Arrange
        BulkOperation operation = mock(BulkOperation.class);
        List<BulkOperation> operations = Collections.singletonList(operation);

        // Act
        BulkRequest bulkRequest = IndexingUtils.createBulkRequest(operations, true);

        // Assert
        assertNotNull(bulkRequest);
        assertEquals(operations, bulkRequest.operations());
        assertEquals(Refresh.True, bulkRequest.refresh());
    }

    @Test
    public void testCreateBulkRequest_withForceRefreshFalse() {
        // Arrange
        BulkOperation operation = mock(BulkOperation.class);
        List<BulkOperation> operations = Collections.singletonList(operation);

        // Act
        BulkRequest bulkRequest = IndexingUtils.createBulkRequest(operations, false);

        // Assert
        assertNotNull(bulkRequest);
        assertEquals(operations, bulkRequest.operations());
        assertEquals(Refresh.False, bulkRequest.refresh());
    }

    @Test
    public void testSimpleIndexAll_success() throws Exception {
        // Arrange
        ElasticsearchClient elasticClient = mock(ElasticsearchClient.class);
        String indexName = "test_index";
        List<String> uris = Arrays.asList("uri1", "uri2");
        ObjectNode document1 = mock(ObjectNode.class);
        ObjectNode document2 = mock(ObjectNode.class);

        Function<String, ObjectNode> documentProvider = uri -> {
            if (uri.equals("uri1")) return document1;
            if (uri.equals("uri2")) return document2;
            return null;
        };

        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);

        when(elasticClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        // Act
        IndexingUtils.simpleIndexAll(elasticClient, indexName, uris, documentProvider);

        // Assert
        verify(elasticClient, times(2)).bulk(any(BulkRequest.class));
    }

    @Test
    public void testSimpleIndexAll_withErrors() throws Exception {
        // Arrange
        ElasticsearchClient elasticClient = mock(ElasticsearchClient.class);
        String indexName = "test_index";
        List<String> uris = Arrays.asList("uri1", "uri2");
        ObjectNode document1 = mock(ObjectNode.class);
        ObjectNode document2 = mock(ObjectNode.class);

        Function<String, ObjectNode> documentProvider = uri -> {
            if (uri.equals("uri1")) return document1;
            if (uri.equals("uri2")) return document2;
            return null;
        };

        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(true);
        when(bulkResponse.items()).thenReturn(Collections.emptyList());

        when(elasticClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            IndexingUtils.simpleIndexAll(elasticClient, indexName, uris, documentProvider);
        });

        assertEquals("Elastic response got errors. Check logs.", exception.getMessage());
        verify(elasticClient, times(2)).bulk(any(BulkRequest.class));
    }

    @Test
    public void testSimpleIndexOne_success() throws Exception {
        // Arrange
        ElasticsearchClient elasticClient = mock(ElasticsearchClient.class);
        String indexName = "test_index";
        String uri = "uri1";
        ObjectNode document = mock(ObjectNode.class);

        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);

        when(elasticClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        // Act
        IndexingUtils.simpleIndexOne(elasticClient, indexName, uri, document);

        // Assert
        verify(elasticClient).bulk(any(BulkRequest.class));
    }

    @Test
    public void testSimpleIndexOne_withErrors() throws Exception {
        // Arrange
        ElasticsearchClient elasticClient = mock(ElasticsearchClient.class);
        String indexName = "test_index";
        String uri = "uri1";
        ObjectNode document = mock(ObjectNode.class);

        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(true);
        when(bulkResponse.items()).thenReturn(Collections.emptyList());

        when(elasticClient.bulk(any(BulkRequest.class))).thenReturn(bulkResponse);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            IndexingUtils.simpleIndexOne(elasticClient, indexName, uri, document);
        });

        assertEquals("Elastic response got errors. Check logs.", exception.getMessage());
        verify(elasticClient).bulk(any(BulkRequest.class));
    }
}
