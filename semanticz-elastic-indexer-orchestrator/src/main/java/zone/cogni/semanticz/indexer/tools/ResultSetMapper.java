package zone.cogni.semanticz.indexer.tools;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

public class ResultSetMapper {

    private ResultSetMapper() {
    }

    public static ResultSetDto resultSetToResultSetDto(ResultSet resultSet) {
        return resultSetToResultSetDto(resultSet, null);
    }

    public static Integer resultSetToAmount(ResultSet resultSet) {
        ResultSetDto resultSetDto = resultSetToResultSetDto(resultSet, null);
        String strAmount = resultSetDto.collectPropertyValue("amount");

        try {
            return Integer.parseInt(strAmount);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public static ResultSetDto resultSetToResultSetDto(ResultSet resultSet, PrefixCcInterface prefixCcInterface) {
        ResultSetDto resultSetDto = new ResultSetDto();

        List<String> vars = new ArrayList<>(resultSet.getResultVars());
        resultSetDto.setVars(vars);

        List<QuerySolutionDto> querySolutionsList = new ArrayList<>();
        while (resultSet.hasNext()) {
            querySolutionsList.add(querySolutionToQuerySolutionDto(resultSet.next(), prefixCcInterface));
        }
        resultSetDto.setQuerySolutions(querySolutionsList);

        return resultSetDto;
    }

    public static QuerySolutionDto querySolutionToQuerySolutionDto(QuerySolution querySolution, PrefixCcInterface prefixCcInterface) {
        QuerySolutionDto querySolutionDto = new QuerySolutionDto();
        querySolutionDto.setNodes(new HashMap<>());

        StreamSupport.stream(Spliterators.spliteratorUnknownSize(querySolution.varNames(), 0), false)
                     .forEach(varName -> {
                         RDFNode rdfNode = querySolution.get(varName);
                         querySolutionDto.getNodes().put(varName, rdfNodeToRDFNodeDto(varName, rdfNode, prefixCcInterface));
                     });

        return querySolutionDto;
    }

    private static RdfNodeDto createNodeDto(RDFNode rdfNode) {
        if (rdfNode.asLiteral().getDatatype().getJavaClass() == Boolean.class) {
            return RdfBooleanNodeDto.create(rdfNode.asLiteral().getBoolean());
        }
        return RdfStringNodeDto.create(rdfNode.asLiteral().getLexicalForm());
    }

    private static RdfNodeDto rdfLiteral(String name, RDFNode rdfNode, PrefixCcInterface prefixCcInterface) {
        RdfNodeDto rdfNodeDto = createNodeDto(rdfNode);

        rdfNodeDto.setType("literal");
        if (prefixCcInterface != null) {
            try {
                rdfNodeDto.setDatatype(prefixCcInterface.getShortenedUri(rdfNode.asLiteral().getDatatype().getURI()));
            } catch (Exception ex) {
                rdfNodeDto.setDatatype(rdfNode.asLiteral().getDatatype().getURI());
            }
        } else {
            rdfNodeDto.setDatatype(rdfNode.asLiteral().getDatatype().getURI());
        }
        rdfNodeDto.setLanguage(rdfNode.asLiteral().getLanguage());


        rdfNodeDto.setName(name);

        return rdfNodeDto;
    }

    public static RdfNodeDto rdfNodeToRDFNodeDto(String name, RDFNode rdfNode, PrefixCcInterface prefixCcInterface) {
        if (rdfNode.isLiteral()) {
            return rdfLiteral(name, rdfNode, prefixCcInterface);
        } else if (rdfNode.isURIResource()) {
            return RdfStringNodeDto.create(rdfNode.asResource().getURI(), name, "uri");
        } else if (rdfNode.isResource()) {
            return RdfStringNodeDto.create(rdfNode.toString(), name, rdfNode.asNode().isBlank() ? "blank" : "resource");
        } else if (rdfNode.isAnon()) {
            return RdfStringNodeDto.create(rdfNode.toString(), name, "anon");
        }
        return RdfStringNodeDto.create(null, name, "unsupported");
    }
}