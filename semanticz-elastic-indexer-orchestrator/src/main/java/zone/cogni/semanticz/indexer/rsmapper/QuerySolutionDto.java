package zone.cogni.semanticz.indexer.rsmapper;
import java.util.Map;

public class QuerySolutionDto {

    private final Map<String, RdfNodeDto> nodes;

    public QuerySolutionDto(Map<String, RdfNodeDto> nodes) {
        this.nodes = nodes;
    }

    public Map<String, RdfNodeDto> getNodes() {
        return nodes;
    }

    public String getProperty(String key) {
        RdfNodeDto node = nodes.get(key);
        return node == null ? null : node.getValue();
    }

}
