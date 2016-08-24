package org.carlspring.strongbox.rest;

import org.carlspring.strongbox.client.RestClient;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ProxyConfiguration;
import org.carlspring.strongbox.resource.ConfigurationResourceResolver;
import org.carlspring.strongbox.rest.context.RestletTestContext;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import static org.junit.Assert.*;

/**
 * @author mtodorov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@RestletTestContext
@Ignore
public class ConfigurationManagementRestletTest
{

    public static final String ADD_ACCEPTED_RULE_SET_JSON = "{\n" +
                                                            "  \"rule-set\": {\n" +
                                                            "    \"group-repository\": \"group-releases-2\",\n" +
                                                            "    \"rule\": [\n" +
                                                            "      {\n" +
                                                            "        \"pattern\": \".*some.test\",\n" +
                                                            "        \"repository\": [\n" +
                                                            "          \"releases-with-trash\",\n" +
                                                            "          \"releases-with-redeployment\"\n" +
                                                            "        ]\n" +
                                                            "      }\n" +
                                                            "    ]\n" +
                                                            "  }\n" +
                                                            "}";
    public static final String ADD_ACCEPTED_REPO_JSON = "{\n" +
                                                        "  \"rule\": {\n" +
                                                        "    \"pattern\": \".*some.test\",\n" +
                                                        "    \"repository\": [\n" +
                                                        "      \"releases2\",\n" +
                                                        "      \"releases3\"\n" +
                                                        "    ]\n" +
                                                        "  }\n" +
                                                        "}";
    public static final String OVERRIDE_REPO_JSON = "{\n" +
                                                    "          \"rule\":\n" +
                                                    "            {\n" +
                                                    "              \"pattern\": \".*some.test\",\n" +
                                                    "              \"repository\": [\n" +
                                                    "                \"releases22\", \"releases32\"\n" +
                                                    "              ]\n" +
                                                    "            }\n" +
                                                    "\n" +
                                                    "}";


    private RestClient client = RestClient.getTestInstanceLoggedInAsAdmin();


    @After
    public void tearDown()
            throws Exception
    {
        if (client != null)
        {
            client.close();
        }
    }

    @Test
    public void testSetAndGetPort()
            throws Exception
    {
        int newPort = 18080;

        int status = client.setListeningPort(newPort);

        assertEquals("Failed to set port!", 200, status);
        assertEquals("Failed to get port!", newPort, client.getListeningPort());
    }

    @Test
    public void testSetAndGetBaseUrl()
            throws Exception
    {
        String baseUrl = "http://localhost:" + 40080 + "/newurl";

        int status = client.setBaseUrl(baseUrl);

        assertEquals("Failed to set baseUrl!", 200, status);

        String b = client.getBaseUrl();

        assertEquals("Failed to get baseUrl!", baseUrl, b);
    }

    @Test
    public void testSetAndGetGlobalProxyConfiguration()
            throws Exception
    {
        List<String> nonProxyHosts = new ArrayList<>();
        nonProxyHosts.add("localhost");
        nonProxyHosts.add("some-hosts.com");

        ProxyConfiguration proxyConfiguration = createProxyConfiguration();
        proxyConfiguration.setNonProxyHosts(nonProxyHosts);

        int status = client.setProxyConfiguration(proxyConfiguration);

        assertEquals("Failed to set proxy configuration!", 200, status);

        ProxyConfiguration pc = client.getProxyConfiguration(null, null);

        assertNotNull("Failed to get proxy configuration!", pc);
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getHost(), pc.getHost());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getPort(), pc.getPort());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getUsername(), pc.getUsername());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getPassword(), pc.getPassword());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getType(), pc.getType());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getNonProxyHosts(),
                     pc.getNonProxyHosts());
    }

    @Test
    public void testAddGetStorage()
            throws Exception
    {
        String storageId = "storage1";

        Storage storage1 = new Storage("storage1");

        final int response = client.addStorage(storage1);

        assertEquals("Failed to create storage!", 200, response);

        Repository r1 = new Repository("repository0");
        r1.setAllowsRedeployment(true);
        r1.setSecured(true);
        r1.setStorage(storage1);

        Repository r2 = new Repository("repository1");
        r2.setAllowsForceDeletion(true);
        r2.setTrashEnabled(true);
        r2.setStorage(storage1);
        r2.setProxyConfiguration(createProxyConfiguration());

        client.addRepository(r1);
        client.addRepository(r2);

        Storage storage = client.getStorage(storageId);

        assertNotNull("Failed to get storage (" + storageId + ")!", storage);
        assertFalse("Failed to get storage (" + storageId + ")!", storage.getRepositories().isEmpty());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository0").allowsRedeployment());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository0").isSecured());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository1").allowsForceDeletion());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository1").isTrashEnabled());

        assertNotNull("Failed to get storage (" + storageId + ")!",
                      storage.getRepositories().get("repository1").getProxyConfiguration().getHost());
        assertEquals("Failed to get storage (" + storageId + ")!",
                     "localhost",
                     storage.getRepositories().get("repository1").getProxyConfiguration().getHost());
    }

    @Test
    public void testCreateAndDeleteStorage()
            throws IOException, JAXBException
    {
        final String storageId = "storage2";
        final String repositoryId1 = "repository0";
        final String repositoryId2 = "repository1";

        Storage storage2 = new Storage(storageId);

        int response = client.addStorage(storage2);

        assertEquals("Failed to create storage!", 200, response);

        Repository r1 = new Repository(repositoryId1);
        r1.setAllowsRedeployment(true);
        r1.setSecured(true);
        r1.setStorage(storage2);
        r1.setProxyConfiguration(createProxyConfiguration());

        Repository r2 = new Repository(repositoryId2);
        r2.setAllowsRedeployment(true);
        r2.setSecured(true);
        r2.setStorage(storage2);

        client.addRepository(r1);
        client.addRepository(r2);

        final ProxyConfiguration pc = client.getProxyConfiguration(storageId, repositoryId1);

        assertNotNull("Failed to get proxy configuration!", pc);
        assertEquals("Failed to get proxy configuration!", pc.getHost(), pc.getHost());
        assertEquals("Failed to get proxy configuration!", pc.getPort(), pc.getPort());
        assertEquals("Failed to get proxy configuration!", pc.getUsername(), pc.getUsername());
        assertEquals("Failed to get proxy configuration!", pc.getPassword(), pc.getPassword());
        assertEquals("Failed to get proxy configuration!", pc.getType(), pc.getType());

        Storage storage = client.getStorage(storageId);

        assertNotNull("Failed to get storage (" + storageId + ")!", storage);
        assertFalse("Failed to get storage (" + storageId + ")!", storage.getRepositories().isEmpty());

        response = client.deleteRepository(storageId, repositoryId1, true);

        assertEquals("Failed to delete repository " + storageId + ":" + repositoryId1 + "!", 200, response);

        final Repository r = client.getRepository(storageId, repositoryId1);

        assertNull(r);

        File storageDir = new File(ConfigurationResourceResolver.getVaultDirectory() + "/storages/" + storage.getId());

        assertTrue("Storage doesn't exist!", storageDir.exists());

        response = client.deleteStorage(storageId, true);

        assertEquals("Failed to delete storage " + storageId + "!", 200, response);

        final Storage s = client.getStorage(storageId);

        assertNull("Failed to delete storage " + storageId + "!", s);

        assertFalse("Failed to delete storage!", storageDir.exists());
    }

    @Test
    public void testGetAndSetConfiguration()
            throws IOException, JAXBException
    {
        final Configuration configuration = client.getConfiguration();

        Storage storage = new Storage("storage3");

        configuration.addStorage(storage);

        final int response = client.setConfiguration(configuration);

        assertEquals("Failed to retrieve configuration!", 200, response);

        final Configuration c = client.getConfiguration();

        assertNotNull("Failed to create storage3!", c.getStorage("storage3"));
    }

    @Test
    public void addAcceptedRuleSet()
            throws Exception
    {
        Response response = acceptedRuleSet();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void removeAcceptedRuleSet()
            throws Exception
    {
        Response response = acceptedRuleSet();
        response = client
                           .prepareTarget(
                                   "/configuration/strongbox/routing/rules/set/accepted/group-releases-2")
                           .request()
                           .delete();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void addAcceptedRepository()
            throws Exception
    {
        Response response = acceptedRuleSet();
        response = acceptedRepository();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void removeAcceptedRepository()
            throws Exception
    {
        Response response = acceptedRuleSet();
        response = acceptedRepository();
        response = client
                           .prepareTarget(
                                   "/configuration/strongbox/routing/rules/accepted/group-releases-2/repositories/releases3?pattern=.*some.test")
                           .request()
                           .delete();

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void overrideAcceptedRepository()
            throws Exception
    {
        Response response = acceptedRuleSet();
        response = acceptedRepository();
        response = client
                           .prepareTarget(
                                   "/configuration/strongbox/routing/rules/accepted/group-releases-2/override/repositories")
                           .request()
                           .put(Entity.json(OVERRIDE_REPO_JSON));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private Response acceptedRuleSet()
            throws IOException
    {
        return client
                       .prepareTarget("/configuration/strongbox/routing/rules/set/accepted")
                       .request()
                       .put(Entity.json(ADD_ACCEPTED_RULE_SET_JSON));
    }

    private Response acceptedRepository()
            throws IOException
    {
        Response response;
        response = client
                           .prepareTarget(
                                   "/configuration/strongbox/routing/rules/accepted/group-releases-2/repositories")
                           .request()
                           .put(Entity.json(ADD_ACCEPTED_REPO_JSON));
        return response;
    }

    private ProxyConfiguration createProxyConfiguration()
    {
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.setHost("localhost");
        proxyConfiguration.setPort(8080);
        proxyConfiguration.setUsername("user1");
        proxyConfiguration.setPassword("pass2");
        proxyConfiguration.setType("http");

        return proxyConfiguration;
    }

}
