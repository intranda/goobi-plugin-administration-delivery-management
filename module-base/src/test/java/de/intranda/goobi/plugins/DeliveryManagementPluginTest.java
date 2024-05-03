package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Test;

import de.sub.goobi.config.ConfigurationHelper;

//@RunWith(PowerMockRunner.class)
//@PowerMockIgnore({ "javax.management.*", "javax.net.ssl.*" ,"jdk.internal.reflect.*"})
public class DeliveryManagementPluginTest {

    private static String resourcesFolder;

    @BeforeClass
    public static void setUpClass() throws Exception {
        resourcesFolder = "src/test/resources/"; // for junit tests in eclipse

        if (!Files.exists(Paths.get(resourcesFolder))) {
            resourcesFolder = "target/test-classes/"; // to run mvn test from cli or in jenkins
        }

        String log4jFile = resourcesFolder + "log4j2.xml"; // for junit tests in eclipse

        System.setProperty("log4j.configurationFile", log4jFile);
        Path goobiFolder = Paths.get(resourcesFolder, "goobi_config.properties");
        String goobiMainFolder =goobiFolder.getParent().toString();
        ConfigurationHelper.CONFIG_FILE_NAME = goobiFolder.toString();
        ConfigurationHelper.resetConfigurationFile();
        ConfigurationHelper.getInstance().setParameter("goobiFolder", goobiMainFolder + "/");

    }

    @Test
    public void testDisplayMode() throws IOException {
        DeliveryManagementAdministrationPlugin plugin = new DeliveryManagementAdministrationPlugin();
        plugin.setDisplayMode("fixture");
        assertEquals("fixture", plugin.getDisplayMode());
    }

    @Test
    public void testPossibleModes() throws IOException {
        DeliveryManagementAdministrationPlugin plugin = new DeliveryManagementAdministrationPlugin();
        assertEquals(3, plugin.getModes().size());
    }
}
