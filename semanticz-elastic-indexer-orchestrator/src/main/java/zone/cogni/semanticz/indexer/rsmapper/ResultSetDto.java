package zone.cogni.semanticz.indexer.rsmapper;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultSetDto {

    private final List<String> vars;
    private final List<QuerySolutionDto> querySolutions;

    public ResultSetDto(List<String> vars, List<QuerySolutionDto> querySolutions) {
        this.vars = vars;
        this.querySolutions = querySolutions;
    }

    public List<String> getVars() {
        return vars;
    }

    public List<QuerySolutionDto> getQuerySolutions() {
        return querySolutions;
    }

    public Stream<QuerySolutionDto> stream() {
        return querySolutions == null ? Stream.empty() : querySolutions.stream();
    }

    public List<String> collectPropertyValues(String propertyName) {
        return stream()
                .map(q -> q.getProperty(propertyName))
                .collect(Collectors.toList());
    }
    public String collectPropertyValue(String propertyName) {
        if (querySolutions != null && querySolutions.size() == 1) {
            return querySolutions.get(0).getProperty(propertyName);
        }
        return null;
    }

    @Override
    public String toString() {
        if (vars == null || querySolutions == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        // Header
        for (String name : vars) {
            builder.append(name).append("\t");
        }
        builder.append(StringUtils.LF);

        // Rows
        for (QuerySolutionDto qs : querySolutions) {
            for (String name : vars) {
                builder.append(qs.getProperty(name)).append("\t");
            }
            builder.append(StringUtils.LF);
        }
        return builder.toString();
    }
}
