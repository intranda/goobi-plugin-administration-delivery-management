package de.intranda.goobi.plugins;

import java.util.List;

import lombok.Data;

@Data
public class ConfiguredField {

    private String name;

    private String label;

    private String fieldType;

    private boolean displayInTable;

    private String validationType;

    private String regularExpression;

    private String validationError;

    private String value;

    private List<String> selectItemList;

    public ConfiguredField(String name, String label, String fieldType, boolean displayInTable, String validationType, String regularExpression,
            String validationError) {
        this.name = name;
        this.label = label;
        this.fieldType = fieldType;
        this.displayInTable = displayInTable;
        this.validationType = validationType;
        this.regularExpression = regularExpression;
        this.validationError = validationError;
    }

}
