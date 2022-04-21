package de.intranda.goobi.plugins;

import org.apache.commons.configuration.XMLConfiguration;
import org.goobi.production.enums.PluginType;
import org.goobi.production.plugin.interfaces.IAdministrationPlugin;

import de.sub.goobi.config.ConfigPlugins;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class DeliveryManagementAdministrationPlugin implements IAdministrationPlugin {

    @Getter
    private String title = "intranda_administration_deliveryManagement";

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
