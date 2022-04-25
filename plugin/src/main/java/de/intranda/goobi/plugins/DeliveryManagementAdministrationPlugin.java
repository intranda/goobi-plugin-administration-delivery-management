package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.goobi.beans.Institution;
import org.goobi.managedbeans.InstitutionBean;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.InstitutionManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class DeliveryManagementAdministrationPlugin implements IAdministrationPlugin {

    //    - Anlegen und Bearbeiten von Institutionen mit verschiedenen zugehörigen Metadaten (Adressen, Kontaktdaten, Ansprechpartner etc.)
    //    - Anlegen und Bearbeiten von Benutzeraccounts mit verschiedenen zugehörigen Metadaten und Zuweisung der Benutzer zu den vorhandenen Institutionen
    //    - Hinterlegen einer zentralen Datenschutzerklärung
    //    - Suche und Sortierung von Nutzern
    //    - Zuweisung von Nutzern zu bestehenden Institutionen
    //    - Übersichtsanzeige einer Institution (Auflistung bereits abgelieferter Publikationen (Vorgänge) mit Anzeige konfigurierter Metadaten einer Institution; Datum letzte Lieferung)
    //    - Import und Export von Stammdaten einer Institution als json oder xml
    //    - Übersicht über freizuschaltende Nutzer und Möglichkeit, eine Freischaltung zu ermöglichen oder zu verhindern
    //    - Mailversand über erfolgte oder verweigerte Freischaltung
    //    - weitreichende Konfigurierbarkeit des Plugins und der Mail-Texte über eine Konfigurationsdatei

    @Getter
    private String title = "intranda_administration_deliveryManagement";

    @Getter
    private String displayMode;

    @Getter
    @Setter
    private String editionMode = "";

    @Getter

    private Institution institution;

    @Getter
    private String[] modes = { "displayMode_institution", "displayMode_user" };

    @Getter
    private InstitutionBean institutionBean = Helper.getBeanByClass(InstitutionBean.class);

    @Getter
    private List<ConfiguredField> configuredInstitutionFields = null;

    @Override
    public PluginType getType() {
        return PluginType.Administration;
    }

    @Override
    public String getGui() {
        // load configuration on first requests
        if (configuredInstitutionFields == null) {
            loadConfiguration();
        }

        return "/uii/plugin_administration_deliveryManagement.xhtml";
    }

    private void loadConfiguration() {
        XMLConfiguration conf = ConfigPlugins.getPluginConfig(title);
        conf.setExpressionEngine(new XPathExpressionEngine());
        List<HierarchicalConfiguration> institutionFields = conf.configurationsAt("/institution/field");

        configuredInstitutionFields = new ArrayList<>();

        for (HierarchicalConfiguration hc : institutionFields) {

            ConfiguredField field = new ConfiguredField(hc.getString("@name"), hc.getString("@label"), hc.getString("@fieldType", "input"),
                    hc.getBoolean("@displayInTable", false), hc.getString("@validationType", null), hc.getString("@regularExpression", null),
                    hc.getString("/validationError", null));

            if (field.getFieldType().equals("dropdown") || field.getFieldType().equals("multiselect")) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                field.setSelectItemList(valueList);
            }
            configuredInstitutionFields.add(field);
        }
    }

    public void setDisplayMode(String displayMode) {
        if (this.displayMode == null || !this.displayMode.equals(displayMode)) {
            this.displayMode = displayMode;
            if (displayMode.equals("displayMode_institution")) {
                institutionBean.FilterKein();
            }
        }
    }

    public void setInstitution(Institution institution) {
        if (this.institution == null || !this.institution.equals(institution)) {
            this.institution = institution;
            for (ConfiguredField field : configuredInstitutionFields) {
                field.setValue(institution.getAdditionalData().get(field.getName()));
            }
        }
    }

    public void saveInstitution() {
        for (ConfiguredField field : configuredInstitutionFields) {
            institution.getAdditionalData().put(field.getName(), field.getValue());
        }

        InstitutionManager.saveInstitution(institution);
        institutionBean.FilterKein();
    }

    public void deleteInstitution() {
        InstitutionManager.deleteInstitution(institution);
        institutionBean.FilterKein();
    }
}
