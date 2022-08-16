package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.goobi.api.mail.SendMail;
import org.goobi.beans.Institution;
import org.goobi.beans.Ldap;
import org.goobi.beans.Process;
import org.goobi.beans.Project;
import org.goobi.beans.User;
import org.goobi.beans.Usergroup;
import org.goobi.managedbeans.DatabasePaginator;
import org.goobi.managedbeans.ProcessBean;
import org.goobi.persistence.ExtendedInstitution;
import org.goobi.persistence.ExtendedInstitutionPaginator;
import org.goobi.persistence.ExtendendInstitutionManager;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.helper.FacesContextHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.XmlTools;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.InstitutionManager;
import de.sub.goobi.persistence.managers.LdapManager;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.ProjectManager;
import de.sub.goobi.persistence.managers.UserManager;
import de.sub.goobi.persistence.managers.UsergroupManager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;

@PluginImplementation
@Log4j2
public class DeliveryManagementAdministrationPlugin implements IAdministrationPlugin {

    //    - Übersichtsanzeige einer Institution (Auflistung bereits abgelieferter Publikationen (Vorgänge) mit Anzeige konfigurierter Metadaten einer Institution; Datum letzte Lieferung)

    private static final String INSTITUTION_MODE = "plugin_administration_deliveryManagement_displayMode_institution";
    private static final String USER_MODE = "plugin_administration_deliveryManagement_displayMode_user";
    private static final String PRIVACY_MODE = "plugin_administration_deliveryManagement_displayMode_privacyPolicy";
    private static final String ZDB_DATA_MODE = "plugin_administration_deliveryManagement_displayMode_zdbTitleData";

    @Getter
    private String title = "intranda_administration_deliveryManagement";

    @Getter
    private String displayMode = INSTITUTION_MODE;

    @Getter
    @Setter
    private String editionMode = "";

    @Getter
    private List<String> modes;

    @Getter
    private ExtendedInstitution institution;

    @Getter
    private List<ConfiguredField> configuredInstitutionFields = null;

    private List<String> excludeInstitutions;

    @Getter
    @Setter
    private String institutionSearchFilter;

    @Getter
    @Setter
    private String institutionSort;

    @Getter
    private ExtendedInstitutionPaginator institutionPaginator;

    @Getter
    private User user;

    @Getter
    private List<ConfiguredField> configuredUserFields = null;

    @Getter
    @Setter
    private String userSearchFilter;

    @Getter
    @Setter
    private boolean showOnlyInactiveUser;

    @Getter
    @Setter
    private String userSort;

    @Getter
    private DatabasePaginator userPaginator;

    // is set to true, when the account was disabled and gets activated
    private boolean activateAccount = false;

    @Getter
    @Setter
    private String privacyPolicyText;

    private XMLConfiguration conf;

    @Getter
    private DatabasePaginator processPaginator;

    @Getter
    private DatabasePaginator institutionProcessPaginator;

    private static final String ZDB_METADATA_TYPE = "CatalogIDPeriodicalDB"; // get this from dashboard config?

    private static final String COMBO_FIELD_NAME = "combo";

    @Getter
    @Setter
    private String sortField;
    @Getter
    @Setter
    private Process process;

    private Fileformat fileformat;

    private DocStruct logical;

    @Getter
    private List<Metadata> metadataList;

    @Getter
    @Setter
    private boolean includeFinishedZdbData;

    @Getter
    @Setter
    private String zdbSearchField;

    // metadata to display
    @Getter
    private List<String> metadataDisplayList;


    public DeliveryManagementAdministrationPlugin() {

        modes = new ArrayList<>();
        modes.add(INSTITUTION_MODE);
        modes.add(USER_MODE);
        modes.add(PRIVACY_MODE);
        modes.add(ZDB_DATA_MODE);

    }

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

        modes = new ArrayList<>();
        modes.add(INSTITUTION_MODE);
        modes.add(USER_MODE);
        Path config = Paths.get(new Helper().getGoobiConfigDirectory(), "plugin_" + title + ".xml");
        if (!StorageProvider.getInstance().isWritable(config)) {
            Helper.setFehlerMeldung("plugin_administration_delivery_configurationFileNotWritable");
        } else {
            modes.add(PRIVACY_MODE);
        }
        modes.add(ZDB_DATA_MODE);

        conf = ConfigPlugins.getPluginConfig(title);
        conf.setExpressionEngine(new XPathExpressionEngine());

        List<HierarchicalConfiguration> configuredFields = conf.configurationsAt("/fields/field");

        configuredInstitutionFields = new ArrayList<>();
        configuredUserFields = new ArrayList<>();

        privacyPolicyText = conf.getString("/privacyStatement", ""); //NOSONAR

        excludeInstitutions = Arrays.asList(conf.getStringArray("/excludeInstitution"));

        for (HierarchicalConfiguration hc : configuredFields) {

            String label = hc.getString("@alternativeLabel", hc.getString("@label"));

            ConfiguredField field = new ConfiguredField(hc.getString("@type"), hc.getString("@name"), label, hc.getString("@fieldType", "input"),
                    hc.getBoolean("@displayInTable", false), hc.getString("@validationType", null), hc.getString("@regularExpression", null),
                    hc.getString("/validationError", null), hc.getString("@helpMessage", ""), hc.getBoolean("@required"));

            if (field.getFieldType().equals("dropdown") || field.getFieldType().equals(COMBO_FIELD_NAME)) {
                List<String> valueList = Arrays.asList(hc.getStringArray("/value"));
                field.setSelectItemList(valueList);
            }

            if ("institution".equals(field.getType())) { //NOSONAR
                configuredInstitutionFields.add(field);
            } else {
                configuredUserFields.add(field);
            }
        }

        metadataDisplayList = Arrays.asList(conf.getStringArray("/metadata"));

        filterInstitution(); // temporary fix to load the data for the first page
    }

    public void setDisplayMode(String displayMode) {
        if (this.displayMode == null || !this.displayMode.equals(displayMode)) {
            this.displayMode = displayMode;
            if (displayMode.equals(INSTITUTION_MODE)) {
                filterInstitution();
            }
            if (displayMode.equals(USER_MODE)) {
                filterUser();
            }

            if (displayMode.equals(ZDB_DATA_MODE)) {
                generateZdbTitleList();
            }
        }
    }

    public void setInstitution(ExtendedInstitution institution) {
        if (this.institution == null || !this.institution.equals(institution)) {
            this.institution = institution;
            for (ConfiguredField field : configuredInstitutionFields) {
                String value = institution.getAdditionalData().get(field.getName());
                if (field.getFieldType().equals(COMBO_FIELD_NAME) && StringUtils.isNotBlank(value) && !"false".equals(value)) {
                    field.setBooleanValue(true);
                    field.setSubValue(value);
                } else {
                    field.setValue(value);
                }

            }
        }
    }

    public void saveInstitution() {
        for (ConfiguredField field : configuredInstitutionFields) {
            if (field.getFieldType().equals(COMBO_FIELD_NAME) && field.getBooleanValue()) {
                institution.getAdditionalData().put(field.getName(), field.getSubValue());
            } else {
                institution.getAdditionalData().put(field.getName(), field.getValue());
            }
        }

        ExtendendInstitutionManager.saveInstitution(institution);
        filterInstitution();
    }

    public void deleteInstitution() {
        ExtendendInstitutionManager.deleteInstitution(institution);
        filterInstitution();
    }

    public void filterInstitution() {
        ExtendendInstitutionManager manager = new ExtendendInstitutionManager();
        institutionPaginator = new ExtendedInstitutionPaginator(getInsitutionSqlSortString(), institutionSearchFilter, manager);
    }

    private String getInsitutionSqlSortString() {
        String sort = "";
        if (StringUtils.isNotBlank(institutionSort)) {
            switch (institutionSort) {
                case "benutzer.Nachname, benutzer.Vorname":
                case "benutzer.Nachname Desc, benutzer.Vorname Desc":
                case "benutzer.login":
                case "benutzer.login Desc":
                case "benutzer.email":
                case "benutzer.email Desc":
                case "institution.shortName":
                case "institution.shortName Desc":
                case "institution.longName":
                case "institution.longName Desc":
                case "lastDate":
                case "lastDate Desc":
                    sort = institutionSort;
                    break;
                case "items":
                    sort="CAST(items as SIGNED INTEGER)";
                    break;
                case "items Desc":
                    sort="CAST(items as SIGNED INTEGER) Desc";
                    break;
                default:
                    // free configured field
                    if (institutionSort.endsWith("Desc")) {
                        sort = "ExtractValue(institution.additional_data, '/root/" + institutionSort.replace(" Desc", "") + "') desc";
                    } else {
                        sort = "ExtractValue(institution.additional_data, '/root/" + institutionSort + "')";
                    }
                    break;
            }
        }
        return sort;
    }

    public void filterUser() {
        UserManager m = new UserManager();
        StringBuilder sqlQuery = new StringBuilder();
        sqlQuery.append("userstatus!='deleted'");
        if (StringUtils.isNotBlank(userSearchFilter)) {
            String like = " like '%" + StringEscapeUtils.escapeSql(userSearchFilter) + "%'";
            sqlQuery.append("and (login").append(like);
            sqlQuery.append(" or Vorname").append(like);
            sqlQuery.append(" or Nachname").append(like);
            sqlQuery.append(" or email").append(like);
            for (ConfiguredField cf : configuredUserFields) {
                sqlQuery.append(" or ExtractValue(benutzer.additional_data, '/root/").append(cf.getName()).append("')").append(like);
            }
            sqlQuery.append(" or BenutzerID IN (SELECT DISTINCT BenutzerID FROM benutzer, institution WHERE ");
            sqlQuery.append("benutzer.institution_id = institution.id AND (institution.shortName ");
            sqlQuery.append(like);
            sqlQuery.append(" OR institution.longName ");
            sqlQuery.append(like);
            sqlQuery.append("))");

            sqlQuery.append(")");
        }
        if (!excludeInstitutions.isEmpty()) {
            sqlQuery.append(" AND benutzer.institution_id NOT IN (SELECT institution.id from institution where ");
            sqlQuery.append("institution.shortName in (");
            StringBuilder inst = new StringBuilder();
            for (String s : excludeInstitutions) {
                if (inst.length() > 0) {
                    inst.append(", ");
                }
                inst.append("'");
                inst.append(s);
                inst.append("'");
            }
            sqlQuery.append(inst.toString());
            sqlQuery.append("))");
        }

        if (showOnlyInactiveUser) {
            sqlQuery.append(" AND userstatus!='active'");
        }

        userPaginator = new DatabasePaginator(getUserSqlSortString(), sqlQuery.toString(), m, "");
    }

    private String getUserSqlSortString() {
        String sort = "";
        if (StringUtils.isNotBlank(userSort)) {
            switch (userSort) {
                case "benutzer.Nachname, benutzer.Vorname":
                case "benutzer.Nachname Desc, benutzer.Vorname Desc":
                case "benutzer.login":
                case "benutzer.login Desc":
                case "benutzer.email":
                case "benutzer.email Desc":
                case "institution.shortName":
                case "institution.shortName Desc":
                    sort = userSort;
                    break;
                default:
                    // free configured field
                    if (userSort.endsWith("Desc")) {
                        sort = "ExtractValue(benutzer.additional_data, '/root/" + userSort.replace(" Desc", "") + "') desc";
                    } else {
                        sort = "ExtractValue(benutzer.additional_data, '/root/" + userSort + "')";
                    }
                    break;
            }
        }
        return sort;
    }

    public void createNewInstitution() {
        setInstitution(new ExtendedInstitution(new Institution()));
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
                String value = user.getAdditionalData().get(field.getName());
                if (field.getFieldType().equals(COMBO_FIELD_NAME) && StringUtils.isNotBlank(value) && !"false".equals(value)) {
                    field.setBooleanValue(true);
                    field.setSubValue(value);
                } else {
                    field.setValue(user.getAdditionalData().get(field.getName()));
                }
            }
        }
    }

    public void saveUser() {
        for (ConfiguredField field : configuredUserFields) {
            if (field.getFieldType().equals(COMBO_FIELD_NAME) && field.getBooleanValue()) {
                user.getAdditionalData().put(field.getName(), field.getSubValue());
            } else {
                user.getAdditionalData().put(field.getName(), field.getValue());
            }
        }
        try {
            UserManager.saveUser(user);
        } catch (DAOException e) {
            log.error(e);
        }
        if (activateAccount && StringUtils.isNotBlank(user.getEmail())) {
            // send mail when account gets activated
            String messageSubject = SendMail.getInstance().getConfig().getUserActivationMailSubject();
            String messageBody = SendMail.getInstance().getConfig().getUserActivationMailBody().replace("{login}", user.getLogin());
            SendMail.getInstance().sendMailToUser(messageSubject, messageBody, user.getEmail());
        }
        filterUser();
    }

    public void deleteUser() {
        try {
            UserManager.deleteUser(user);
        } catch (DAOException e) {
            log.error(e);
        }
        filterUser();
    }

    public void createNewUser() {
        setUser(new User());
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
            Institution i = ExtendendInstitutionManager.getInstitutionById(id);
            user.setInstitution(i);
        }
    }

    public void savePrivacyPolicy() {
        // check if element exists
        if (conf.getString("/privacyStatement") != null) {
            conf.clearProperty("/privacyStatement");
        }
        conf.addProperty("/privacyStatement", privacyPolicyText);
        String file = "plugin_" + title + ".xml";

        try {

            conf.save(new File(new Helper().getGoobiConfigDirectory() + file));
        } catch (ConfigurationException e) {
            log.error(e);
        }

    }

    public void generateZdbTitleList() {

        StringBuilder sb = new StringBuilder();
        sb.append(
                "(prozesse.ProzesseID IN (SELECT DISTINCT processid FROM metadata WHERE metadata.name = 'DocStruct' AND metadata.value = 'ZdbTitle')) ");
        sb.append("AND prozesse.istTemplate = false ");
        if (!includeFinishedZdbData) {
            sb.append("and not exists (select * from metadata m2 where m2.name='CatalogIDPeriodicalDB' and m2.processid = prozesse.ProzesseID) ");
        }
        if (StringUtils.isNotBlank(zdbSearchField)) {
            sb.append("AND (prozesse.ProzesseID IN (SELECT DISTINCT processid FROM metadata WHERE metadata.value LIKE '%"
                    + StringEscapeUtils.escapeSql(zdbSearchField) + "%'))");
        }

        ProcessManager m = new ProcessManager();
        processPaginator = new DatabasePaginator("prozesse.titel", sb.toString(), m, "process_all");
    }

    public void generateInstitutionProcessTitleList() {
        StringBuilder sb = new StringBuilder();
        sb.append("(prozesse.ProzesseID in (select prozesseID from prozesseeigenschaften where ");
        sb.append("prozesseeigenschaften.Titel = 'Institution' AND prozesseeigenschaften.Wert = '");
        sb.append(institution.getShortName());
        sb.append("')) AND prozesse.istTemplate = false ");
        ProcessManager m = new ProcessManager();
        institutionProcessPaginator = new DatabasePaginator("prozesse.titel", sb.toString(), m, "process_all");
    }

    public void openProcess(boolean includeZdbID) {
        metadataList = new ArrayList<>();
        Prefs prefs = process.getRegelsatz().getPreferences();
        try {
            fileformat = process.readMetadataFile();
            DigitalDocument digitalDocument = fileformat.getDigitalDocument();
            logical = digitalDocument.getLogicalDocStruct();
            List<Metadata> mdl = logical.getAllMetadata();
            boolean identifierAvailable = false;

            for (Metadata md : mdl) {
                metadataList.add(md);
                if (md.getType().getName().equals(ZDB_METADATA_TYPE)) {
                    identifierAvailable = true;
                }
            }

            if (!identifierAvailable && includeZdbID) {
                Metadata md = new Metadata(prefs.getMetadataTypeByName(ZDB_METADATA_TYPE));
                metadataList.add(md);
            }

        } catch (IOException | SwapException | UGHException e1) {
            log.error(e1);
        }
    }

    public void saveZdbTitleData() {

        for (Metadata md : metadataList) {
            if (md.getParent() == null) {
                try {
                    logical.addMetadata(md);
                } catch (MetadataTypeNotAllowedException | DocStructHasNoTypeException e) {
                    log.error(e);
                }
            }
        }

        try {
            process.writeMetadataFile(fileformat);
        } catch (WriteException | PreferencesException | IOException | SwapException e) {
            log.error(e);
        }

        // update metadata list
        generateZdbTitleList();
    }

    public String showImports() {
        String filter = "processproperty:Institution:" + institution.getShortName();
        ProcessBean bean = Helper.getBeanByClass(ProcessBean.class);
        bean.setFilter(filter);
        bean.setModusAnzeige("aktuell");
        return bean.FilterAlleStart();
    }

    public void exportInstitutionCoreData() {
        FacesContext facesContext = FacesContextHelper.getCurrentFacesContext();
        if (!facesContext.getResponseComplete()) {
            Map<String, String> data = institution.getAdditionalData();

            Document doc = new Document();
            Element root = new Element("data");
            doc.setRootElement(root);
            Element inst = new Element("institution");
            root.addContent(inst);

            createElement(inst, "institutionLongName", institution.getLongName());//NOSONAR
            createElement(inst, "institutionShortName", institution.getShortName());

            for (Entry<String, String> entry : data.entrySet()) {
                createElement(inst, entry.getKey(), entry.getValue());
            }

            List<User> allUser = UserManager.getAllUsers();
            for (User usr : allUser) {
                if (usr.getInstitution().getId().equals(institution.getId())) {
                    Element u = new Element("user");
                    root.addContent(u);

                    createElement(u, "login", usr.getLogin()); //NOSONAR
                    createElement(u, "ldaploginName", usr.getLdaplogin());
                    // encryptedPassword
                    // passwordSalt
                    if (usr.getLdapGruppe() != null) {
                        createElement(u, "ldapName", usr.getLdapGruppe().getTitel());
                    }

                    createElement(u, "firstname", usr.getVorname());
                    createElement(u, "lastname", usr.getNachname());

                    createElement(u, "email", usr.getEmail());
                    createElement(u, "location", usr.getStandort());

                    createElement(u, "tablesize", "" + usr.getTabellengroesse());
                    createElement(u, "sessiontimeout", "" + usr.getSessiontimeout());

                    createElement(u, "lang", usr.getMetadatenSprache());
                    createElement(u, "dashboard", usr.getDashboardPlugin());

                    for (Entry<String, String> entry : data.entrySet()) {
                        createElement(u, entry.getKey(), entry.getValue());
                    }

                    Element grp = new Element("usergroups");
                    u.addContent(grp);
                    for (Usergroup ug : usr.getBenutzergruppen()) {
                        createElement(grp, "group", ug.getTitel());
                    }

                    Element projects = new Element("projects");
                    u.addContent(projects);
                    for (Project p : usr.getProjekte()) {
                        createElement(projects, "project", p.getTitel());
                    }
                }
            }
            // write xml to output stream

            XMLOutputter outp = new XMLOutputter();
            outp.setFormat(Format.getPrettyFormat());
            String fileName = "export.xml";
            /*
             * Vorbereiten der Header-Informationen
             */
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();

            ServletContext servletContext = (ServletContext) facesContext.getExternalContext().getContext();
            String contentType = servletContext.getMimeType(fileName);
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");

            response.setContentType(contentType);

            try {
                ServletOutputStream out = response.getOutputStream();
                outp.output(doc, out);
                out.flush();
            } catch (IOException e) {
                log.error(e);
            }
            facesContext.responseComplete();
        }
    }

    private void createElement(Element parent, String elementName, String elementValue) {
        Element element = new Element(elementName);
        element.setText(elementValue);
        parent.addContent(element);
    }

    private void importInstitutionCoreData(Path path) {
        SAXBuilder saxBuilder = XmlTools.getSAXBuilder();
        Document doc = null;
        try {
            doc = saxBuilder.build(path.toFile());
        } catch (JDOMException | IOException e) {
            return;
        }

        Element root = doc.getRootElement();
        Element institutionElement = root.getChild("institution");
        String longName = institutionElement.getChildText("institutionLongName");
        List<Institution> existingInstitutions = InstitutionManager.getAllInstitutionsAsList();
        Institution currentInstitution = null;
        for (Institution inst : existingInstitutions) {
            if (inst.getLongName().equals(longName)) {
                // merge data
                importInstitutionData(inst, institutionElement);
                currentInstitution = inst;
                break;
            }
        }

        // if inst not found, create new one, save
        if (currentInstitution == null) {
            currentInstitution = new Institution();
            importInstitutionData(currentInstitution, institutionElement);
        }
        // save institution data
        InstitutionManager.saveInstitution(currentInstitution);

        List<User> existingUser = UserManager.getAllUsers();
        List<Element> userList = root.getChildren("user");

        for (Element userElement : userList) {
            String login = userElement.getChildText("login");
            User currentUser = null;

            // check if user exists, merge
            for (User u : existingUser) {
                if (u.getLogin().equals(login)) {
                    // merge data
                    currentUser = u;
                    currentUser.setInstitution(currentInstitution);
                    importUserData(currentUser, userElement);
                    break;
                }
            }
            // or create new one
            if (currentUser == null) {
                currentUser = new User();
                currentUser.setInstitution(currentInstitution);
                try {
                    UserManager.saveUser(currentUser);
                } catch (DAOException e) {
                    log.error(e);
                }
                importUserData(currentUser, userElement);
            }
        }
    }

    private void importUserData(User currentUser, Element userElement) {
        List<Usergroup> userGroups = UsergroupManager.getAllUsergroups();
        List<Project> projects = ProjectManager.getAllProjects();

        for (Element element : userElement.getChildren()) {
            switch (element.getName()) {
                case "login":
                    currentUser.setLogin(element.getValue());
                    break;
                case "ldaploginName":
                    currentUser.setLdaplogin(element.getValue());
                    break;
                case "ldapName":
                    for (Ldap ldap : LdapManager.getAllLdapsAsList()) {
                        if (ldap.getTitel().equals(element.getText())) {
                            currentUser.setLdapGruppe(ldap);
                        }
                    }
                    break;
                case "firstname":
                    currentUser.setVorname(element.getValue());
                    break;
                case "lastname":
                    currentUser.setNachname(element.getValue());
                    break;
                case "email":
                    currentUser.setEmail(element.getValue());
                    break;
                case "location":
                    currentUser.setStandort(element.getValue());
                    break;
                case "tablesize":
                    currentUser.setTabellengroesse(Integer.valueOf(element.getValue()));
                    break;
                case "sessiontimeout":
                    currentUser.setSessiontimeout(Integer.valueOf(element.getValue()));
                    break;
                case "lang":
                    currentUser.setMetadatenSprache(element.getValue());
                    break;
                case "dashboard":
                    currentUser.setDashboardPlugin(element.getValue());
                    break;
                case "usergroups":
                    for (Element child : element.getChildren()) {
                        String userGroupName = child.getValue();
                        for (Usergroup grp : userGroups) {
                            if (grp.getTitel().equals(userGroupName) && grp.getBenutzer().contains(currentUser)) {
                                grp.getBenutzer().add(currentUser);
                                try {
                                    UsergroupManager.saveUsergroup(grp);
                                } catch (DAOException e) {
                                    log.error(e);
                                }
                            }
                        }
                    }

                    break;
                case "projects":
                    for (Element child : element.getChildren()) {
                        String projectName = child.getValue();
                        for (Project p : projects) {
                            if (p.getTitel().equals(projectName) && !p.getBenutzer().contains(currentUser)) {
                                p.getBenutzer().add(currentUser);
                                try {
                                    ProjectManager.saveProject(p);
                                } catch (DAOException e) {
                                    log.error(e);
                                }
                            }
                        }
                    }

                    break;
                default:
                    currentUser.getAdditionalData().put(element.getName(), element.getValue());
            }
        }

        // save user
        try {
            UserManager.saveUser(currentUser);
        } catch (DAOException e) {
            log.error(e);
        }
    }

    private void importInstitutionData(Institution inst, Element institutionElement) {
        for (Element element : institutionElement.getChildren()) {
            if (element.getName().equals("institutionLongName")) {
                inst.setLongName(element.getText());
            } else if (element.getName().equals("institutionShortName")) {
                inst.setShortName(element.getText());
            } else {
                inst.getAdditionalData().put(element.getName(), element.getValue());
            }
        }
    }

    public Path copyFile(String fileName, InputStream in) {
        try {
            String extension = fileName.substring(fileName.indexOf("."));
            Path importFile = Files.createTempFile(fileName, extension); // NOSONAR, temp file is save to use
            try (OutputStream out = new FileOutputStream(importFile.toFile())) {
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = in.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
                out.flush();
            }
            return importFile;
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
        return null;
    }

    public void handleFileUpload(FileUploadEvent event) {
        try {
            UploadedFile upload = event.getFile();
            Path importFile = copyFile(upload.getFileName(), upload.getInputStream());
            importInstitutionCoreData(importFile);
            editionMode = "";
            filterInstitution();
            // delete file after import
            StorageProvider.getInstance().deleteFile(importFile);
        } catch (IOException e) {
            log.error("Error while uploading files", e);
        }
    }
}
