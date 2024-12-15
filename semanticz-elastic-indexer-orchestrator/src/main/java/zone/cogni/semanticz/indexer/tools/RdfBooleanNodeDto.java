package zone.cogni.semanticz.indexer.tools;


public class RdfBooleanNodeDto extends RdfNodeDto<Boolean> {

    public static RdfBooleanNodeDto create(Boolean value) {
        RdfBooleanNodeDto node = new RdfBooleanNodeDto();
        node.setValue(value);
        return node;
    }

    public String getStringValue() {
        return getValue() ? "true" : "false";
    }
}