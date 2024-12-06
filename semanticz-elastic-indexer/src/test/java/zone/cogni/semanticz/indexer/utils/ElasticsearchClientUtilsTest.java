package zone.cogni.semanticz.indexer.utils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.transport.endpoints.BooleanResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchClientUtilsTest {

    @Mock
    private ElasticsearchClient elasticClient;

    @Test
    public void testClearIndex() throws Exception {
        // Arrange
        String index = "test-index";
        InputStream settingsStream = new ByteArrayInputStream("{}".getBytes());

        // Mock indices client
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticClient.indices()).thenReturn(indicesClient);

        // Mock exists to return true
        BooleanResponse existsResponse = new BooleanResponse(true);
        doReturn(existsResponse).when(indicesClient).exists(any(Function.class));

        // Mock delete index response
        DeleteIndexResponse deleteResponse = mock(DeleteIndexResponse.class);
        when(deleteResponse.acknowledged()).thenReturn(true);
        doReturn(deleteResponse).when(indicesClient).delete(any(Function.class));

        // Mock create index response
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);
        when(createResponse.acknowledged()).thenReturn(true);
        doReturn(createResponse).when(indicesClient).create(any(Function.class));

        // Act
        ElasticsearchClientUtils.clearIndex(elasticClient, index, settingsStream);

        // Assert
        verify(indicesClient).exists(any(Function.class));
        verify(indicesClient).delete(any(Function.class));
        verify(indicesClient).create(any(Function.class));
    }

    @Test
    public void testEnsureIndexExists() throws Exception {
        // Arrange
        String index = "test-index";
        InputStream settingsStream = new ByteArrayInputStream("{}".getBytes());

        // Mock indices client
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticClient.indices()).thenReturn(indicesClient);

        // Mock exists to return false
        BooleanResponse existsResponse = new BooleanResponse(false);
        doReturn(existsResponse).when(indicesClient).exists(any(Function.class));

        // Mock create index response
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);
        when(createResponse.acknowledged()).thenReturn(true);
        doReturn(createResponse).when(indicesClient).create(any(Function.class));

        // Act
        ElasticsearchClientUtils.ensureIndexExists(elasticClient, index, settingsStream);

        // Assert
        verify(indicesClient).exists(any(Function.class));
        verify(indicesClient).create(any(Function.class));
    }

    @Test
    public void testCreateIndex() throws Exception {
        // Arrange
        String index = "test-index";
        InputStream settingsStream = new ByteArrayInputStream("{}".getBytes());

        // Mock indices client
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticClient.indices()).thenReturn(indicesClient);

        // Mock create index response
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);
        when(createResponse.acknowledged()).thenReturn(true);
        doReturn(createResponse).when(indicesClient).create(any(Function.class));

        // Act
        ElasticsearchClientUtils.createIndex(elasticClient, index, settingsStream);

        // Assert
        verify(indicesClient).create(any(Function.class));
    }

    @Test
    public void testExists() throws Exception {
        // Arrange
        String index = "test-index";

        // Mock indices client
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticClient.indices()).thenReturn(indicesClient);

        // Mock exists to return true
        BooleanResponse existsResponse = new BooleanResponse(true);
        doReturn(existsResponse).when(indicesClient).exists(any(Function.class));

        // Act
        boolean result = ElasticsearchClientUtils.exists(elasticClient, index);

        // Assert
        assertTrue(result);
        verify(indicesClient).exists(any(Function.class));
    }

    @Test
    public void testDeleteIndex() throws Exception {
        // Arrange
        String index = "test-index";

        // Mock indices client
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticClient.indices()).thenReturn(indicesClient);

        // Mock delete index response
        DeleteIndexResponse deleteResponse = mock(DeleteIndexResponse.class);
        when(deleteResponse.acknowledged()).thenReturn(true);
        doReturn(deleteResponse).when(indicesClient).delete(any(Function.class));

        // Act
        ElasticsearchClientUtils.deleteIndex(elasticClient, index);

        // Assert
        verify(indicesClient).delete(any(Function.class));
    }

    @Test
    public void testDeleteDocuments() throws Exception {
        // Arrange
        String index = "test-index";
        List<String> ids = Arrays.asList("1", "2", "3");

        // Mock bulk response
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.errors()).thenReturn(false);
        doReturn(bulkResponse).when(elasticClient).bulk(any(BulkRequest.class));

        // Act
        ElasticsearchClientUtils.deleteDocuments(elasticClient, index, ids);

        // Assert
        verify(elasticClient).bulk(any(BulkRequest.class));
    }

    @Test
    public void testDeleteDocument() throws Exception {
        // Arrange
        String indexName = "test-index";
        String item = "1";

        // Act
        ElasticsearchClientUtils.deleteDocument(elasticClient, indexName, item);

        // Assert
        verify(elasticClient).delete(any(Function.class));
    }

}
