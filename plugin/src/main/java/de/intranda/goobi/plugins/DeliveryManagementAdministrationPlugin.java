package de.intranda.goobi.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringUtils;
import org.goobi.api.mail.SendMail;
import org.goobi.beans.Institution;
import org.goobi.beans.User;
import org.goobi.managedbeans.InstitutionBean;
import org.goobi.managedbeans.UserBean;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.InstitutionManager;
import de.sub.goobi.persistence.managers.LdapManager;
import de.sub.goobi.persistence.managers.UserManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class DeliveryManagementAdministrationPlugin implements IAdministrationPlugin {

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
    private String[] modes = { "displayMode_institution", "displayMode_user" };

    @Getter
    private Institution institution;
    @Getter
    private InstitutionBean institutionBean = Helper.getBeanByClass(InstitutionBean.class);
    @Getter
    private List<ConfiguredField> configuredInstitutionFields = null;

    @Getter
    private User user;
    @Getter
    private UserBean userBean = Helper.getBeanByClass(UserBean.class);
    @Getter
    private List<ConfiguredField> configuredUserFields = null;

    // is set to true, when the account was disabled and gets activated
    private boolean activateAccount = false;

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
        List<HierarchicalConfiguration> userFields = conf.configurationsAt("/user/field");
        configuredInstitutionFields = new ArrayList<>();
        configuredUserFields = new ArrayList<>();

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

        for (HierarchicalConfiguration hc : userFields) {
            ConfiguredField field = new ConfiguredField(hc.getString("@name"), hc.getString("@label"), hc.getString("@fieldType", "input"),
                    hc.getBoolean("@displayInTable", false), hc.getString("@validationType", null), hc.getString("@regularExpression", null),
                    hc.getString("/validationError", null));

            if (field.getFieldType().equals("dropdown") || field.getFieldType().equals("multiselect")) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                field.setSelectItemList(valueList);
            }
            configuredUserFields.add(field);
        }

    }

    public void setDisplayMode(String displayMode) {
        if (this.displayMode == null || !this.displayMode.equals(displayMode)) {
            this.displayMode = displayMode;
            if (displayMode.equals("displayMode_institution")) {
                institutionBean.FilterKein();
            }
            if (displayMode.equals("displayMode_user")) {
                userBean.FilterKein();
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

    public void createNewInstitution() {
        Institution institution = new Institution();
        setInstitution(institution);
    }

    public void setUserIsActive(boolean active) {
        if (!user.isActive() && active) {
            // account gets activated, send mail
            activateAccount = true;
        } else if (!active) {
            // account gets deactivated
            activateAccount = false;
        }
        user.setActive(active);
    }

    public boolean isUserIsActive() {
        return user.isActive();
    }

    public void setUser(User user) {
        if (this.user == null || !this.user.equals(user)) {
            this.user = user;
            for (ConfiguredField field : configuredUserFields) {
                field.setValue(user.getAdditionalData().get(field.getName()));
            }
        }
    }

    public void saveUser() {
        for (ConfiguredField field : configuredUserFields) {
            user.getAdditionalData().put(field.getName(), field.getValue());
        }
        try {
            UserManager.saveUser(user);
        } catch (DAOException e) {
            log.error(e);
        }
        if (activateAccount && StringUtils.isNotBlank(user.getEmail())) {
            // TODO send mail when account was activated
            String messageSubject = SendMail.getInstance().getConfig().getUserActivationMailSubject();
            String messageBody =
                    SendMail.getInstance().getConfig().getUserActivationMailBody().replace("{login}", user.getLogin());
            SendMail.getInstance().sendMailToUser(messageSubject, messageBody, user.getEmail());
        }
        userBean.FilterKein();
    }

    public void deleteUser() {
        try {
            UserManager.deleteUser(user);
        } catch (DAOException e) {
            log.error(e);
        }
        userBean.FilterKein();
    }

    public void createNewUser() {
        User user = new User();
        setUser(user);
    }

    public Integer getAuthenticationType() {
        if (user.getLdapGruppe() != null) {
            return user.getLdapGruppe().getId();
        } else {
            return null;
        }
    }

    public void setAuthenticationType(Integer inAuswahl) {
        if (inAuswahl.intValue() != 0) {
            try {
                user.setLdapGruppe(LdapManager.getLdapById(inAuswahl));
            } catch (DAOException e) {
                Helper.setFehlerMeldung("Error on writing to database", e);
            }
        }
    }

    public Integer getCurrentInstitutionID() {
        if (user.getInstitution() != null) {
            return user.getInstitution().getId();
        } else {
            return Integer.valueOf(0);
        }
    }

    public void setCurrentInstitutionID(Integer id) {
        if (id != null && id.intValue() != 0) {
            Institution institution = InstitutionManager.getInstitutionById(id);
            user.setInstitution(institution);
        }
    }
}
