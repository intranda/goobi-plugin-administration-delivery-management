package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;

import lombok.Data;

@Data
public class ConfiguredField {

    private String type;

    private String name;

    private String label;

    private String fieldType;

    private String regularExpression;

    private String validationError;

    private String value;

    private String subValue;

    private List<SelectItem> selectItemList = new ArrayList<>();

    private String helpMessage;

    private boolean required;

    private String placeholderText;

    private boolean fieldValid = true;

    public ConfiguredField(String type, String name, String label, String fieldType, String regularExpression, String validationError, //NOSONAR
            String helpMessage, boolean required, String placeholderText) {
        this.type = type;
        this.name = name;
        this.label = label;
        this.fieldType = fieldType;
        this.regularExpression = regularExpression;
        this.validationError = validationError;
        this.helpMessage = helpMessage;
        this.required = required;
        this.placeholderText = placeholderText;
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

    public void validateField(FacesContext context, UIComponent comp, Object obj) { //NOSONAR
        fieldValid = true;
        if (obj instanceof Boolean) {
            boolean val = (boolean) obj;
            if (val && StringUtils.isBlank(value)) {
                fieldValid = false;
            }
            return;
        }

        String testValue = (String) obj;

        if ("combo".equals(fieldType)) {
            if (comp.getClientId().endsWith("combo")) {
                if ("null".equals(testValue)) {
                    testValue = null;
                }

            } else if (comp.getClientId().endsWith("combo2") && StringUtils.isBlank(testValue) && getBooleanValue()) {
                fieldValid = false;
            }
        }

        //  simple field validation
        if (StringUtils.isBlank(testValue) && required) {
            fieldValid = false;
        }
        if (StringUtils.isNotBlank(testValue) && StringUtils.isNotBlank(regularExpression) && !testValue.matches(regularExpression)) {
            fieldValid = false;
        }
    }

}
