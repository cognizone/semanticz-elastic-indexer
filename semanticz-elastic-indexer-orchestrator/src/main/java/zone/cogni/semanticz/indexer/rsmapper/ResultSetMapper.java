package zone.cogni.semanticz.indexer.rsmapper;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ResultSetMapper {

    private ResultSetMapper() {
        // Utility class, no public constructor
    }

    public static ResultSetDto resultSetToResultSetDto(ResultSet resultSet) {
        List<String> vars = new ArrayList<>(resultSet.getResultVars());
        List<QuerySolutionDto> querySolutions =
                StreamSupport.stream(
                                     Spliterators.spliteratorUnknownSize(resultSet, Spliterator.ORDERED), false
                             )
                             .map(ResultSetMapper::querySolutionToQuerySolutionDto)
                             .collect(Collectors.toList());
        return new ResultSetDto(vars, querySolutions);
    }

    public static QuerySolutionDto querySolutionToQuerySolutionDto(QuerySolution querySolution) {
        Map<String, RdfNodeDto> nodes = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(querySolution.varNames(), Spliterator.ORDERED),
                false
        ).collect(Collectors.toMap(
                varName -> varName,
                varName -> rdfNodeToRDFNodeDto(querySolution.get(varName))
        ));

        return new QuerySolutionDto(nodes);
    }

    public static RdfNodeDto rdfNodeToRDFNodeDto(RDFNode rdfNode) {
        if (rdfNode == null) {
            return new RdfNodeDto(null);
        }
        if (rdfNode.isLiteral()) {
            // Handle Boolean specifically, else just return lexical form.
            if (rdfNode.asLiteral().getDatatype() != null
                    && Boolean.class.equals(rdfNode.asLiteral().getDatatype().getJavaClass())) {
                return new RdfNodeDto(String.valueOf(rdfNode.asLiteral().getBoolean()));
            }
            return new RdfNodeDto(rdfNode.asLiteral().getLexicalForm());
        }
        if (rdfNode.isURIResource()) {
            return new RdfNodeDto(rdfNode.asResource().getURI());
        }
        else if (rdfNode.isResource()) {
            return new RdfNodeDto(rdfNode.toString());
        }
        else if (rdfNode.isAnon()) {
            return new RdfNodeDto(rdfNode.toString());
        }
        return new RdfNodeDto(rdfNode.toString());
    }
}
