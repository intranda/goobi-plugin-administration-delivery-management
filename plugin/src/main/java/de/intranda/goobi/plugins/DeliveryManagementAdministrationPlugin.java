package de.intranda.goobi.plugins;

import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.sub.goobi.config.ConfigPlugins;
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
    @Setter
    private String displayMode;

    @Getter
    private String[] modes = {"displayMode_institution", "displayMode_user"};


    @Getter
    private String value = "test2334";

    @Override
    public PluginType getType() {
        return PluginType.Administration;
    }

    @Override
    public String getGui() {
        return "/uii/plugin_administration_deliveryManagement.xhtml";
    }


    private void loadConfiguration() {
        XMLConfiguration conf =  ConfigPlugins.getPluginConfig(title);

    }

}
