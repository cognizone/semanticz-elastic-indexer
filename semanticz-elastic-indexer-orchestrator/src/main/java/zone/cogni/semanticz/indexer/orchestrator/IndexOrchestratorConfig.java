package zone.cogni.semanticz.indexer.orchestrator;

import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Optional;

/**
 * Configuration class for IndexOrchestrator.
 * This class holds the list of indexing configurations, each defining how to index a specific entity type.
 */
public class IndexOrchestratorConfig {

    private List<EntityConfig> entityConfig;

    public List<EntityConfig> getIndexing() {
        return entityConfig;
    }

    public void setIndexing(List<EntityConfig> entityConfig) {
        this.entityConfig = entityConfig;
    }

    public void setList(List<EntityConfig> entityConfig) {
        this.entityConfig = entityConfig;
    }

    /**
     * Finds an indexing configuration by its name.
     *
     * @param name the name of the indexing configuration
     * @return an Optional containing the found indexing configuration, or empty if not found
     */
    public Optional<EntityConfig> findIndexingByName(String name) {
        return getIndexing().stream()
                            .filter(entityConfig -> entityConfig.getName().equalsIgnoreCase(name))
                            .findFirst();
    }

    /**
     * Configuration class for a facet.
     * Defines how a facet should be processed during indexing.
     */
    public static class FacetConfig {
        private String name;
        private String body;
        private String path;
        private String accept;
        private String contentType;
        private HttpMethod method;

        public HttpMethod getMethod() {
            return method;
        }

        public void setMethod(HttpMethod method) {
            this.method = method;
        }

        public String getAccept() {
            return accept;
        }

        public void setAccept(String accept) {
            this.accept = accept;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return "FacetConfig{" +
                    "name='" + name + '\'' +
                    ", body='" + body + '\'' +
                    ", path='" + path + '\'' +
                    ", accept='" + accept + '\'' +
                    ", contentType='" + contentType + '\'' +
                    ", method=" + method +
                    '}';
        }
    }

    /**
     * Configuration class for indexing a specific entity type.
     * Contains details about how to select entities, construct index documents, and configure facets.
     */
    public static class EntityConfig {
        private String name;
        private String index;
        private String shacl;
        private String construct;
        private String constructQueryParam;
        private String select;
        private String selectQueryParam;

        private String settings;

        private List<FacetConfig> facets;

        public List<FacetConfig> getFacets() {
            return facets;
        }

        public void setFacets(List<FacetConfig> facets) {
            this.facets = facets;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getSelectQueryParam() {
            return selectQueryParam;
        }

        public void setSelectQueryParam(String selectQueryParam) {
            this.selectQueryParam = selectQueryParam;
        }

        public String getSettings() {
            return settings;
        }

        public void setSettings(String settings) {
            this.settings = settings;
        }

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getShacl() {
            return shacl;
        }

        public void setShacl(String shacl) {
            this.shacl = shacl;
        }

        public String getConstruct() {
            return construct;
        }

        public void setConstruct(String construct) {
            this.construct = construct;
        }

        public String getConstructQueryParam() {
            return constructQueryParam;
        }

        public void setConstructQueryParam(String constructQueryParam) {
            this.constructQueryParam = constructQueryParam;
        }

        public String getSelect() {
            return select;
        }

        public void setSelect(String select) {
            this.select = select;
        }

        @Override
        public String toString() {
            return "Indexing{" +
                    "name='" + name + '\'' +
                    ", index='" + index + '\'' +
                    ", shacl='" + shacl + '\'' +
                    ", construct='" + construct + '\'' +
                    ", constructQueryParam='" + constructQueryParam + '\'' +
                    ", select='" + select + '\'' +
                    '}';
        }
    }
}
