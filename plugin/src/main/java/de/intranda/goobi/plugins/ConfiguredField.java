package de.intranda.goobi.plugins;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
public class ConfiguredField {

    private String type;

    private String name;

    private String label;

    private String fieldType;

    private boolean displayInTable;

    private String validationType;

    private String regularExpression;

    private String validationError;

    private String value;

    private String subValue;

    private List<String> selectItemList;

    private String helpMessage;

    private boolean required;

    public ConfiguredField(String type, String name, String label, String fieldType, boolean displayInTable, String validationType,
            String regularExpression, String validationError, String helpMessage, boolean required) {
        this.type = type;
        this.name = name;
        this.label = label;
        this.fieldType = fieldType;
        this.displayInTable = displayInTable;
        this.validationType = validationType;
        this.regularExpression = regularExpression;
        this.validationError = validationError;
        this.helpMessage = helpMessage;
        this.required = required;
    }

    public void setBooleanValue(boolean val) {
        if (val) {
            value = "true";
        } else {
            value = "false";
        }
    }

    public boolean getBooleanValue() {
        return StringUtils.isNotBlank(value) && "true".equals(value);
    }

}
