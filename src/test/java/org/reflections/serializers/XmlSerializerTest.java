package org.reflections.serializers;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.Store;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlSerializerTest {

    @Rule
    public final TemporaryFolder tempFolder = TemporaryFolder.builder().build();

    private Configuration config;
    private XmlSerializer serializer;

    @Before
    public void setUp() throws Exception {

        config = new ConfigurationBuilder().forPackages("org.reflections.scanners")
                                           .setScanners(new SubTypesScanner());

        serializer = new XmlSerializer();
    }

    /**
     * Scenario: Write out to XML file, read it back in, and compare
     *
     * @throws IOException
     */
    @Test
    public final void testRoundtrip() throws IOException {

        final Reflections reflectionsExpected = new Reflections(config);
        reflectionsExpected.expandSuperTypes();

        final File tempFile = tempFolder.newFile();

        // Serialize to temporary file
        serializer.save(reflectionsExpected, tempFile.getPath());

        try (InputStream inputStream = new FileInputStream(tempFile)) {

            // Read back XML that was just written
            final Reflections reflectionsLoaded = serializer.read(inputStream);

            final Store expectedStore = reflectionsExpected.getStore();
            final Store loadedStore = reflectionsLoaded.getStore();

            for (final String indexName : expectedStore.keySet()) {

                for (final String key : expectedStore.keys(indexName)) {

                    final Set<String> expectedValues = expectedStore.get(indexName, key);
                    final Set<String> loadedValues = loadedStore.get(indexName, key);

                    assertThat(loadedValues, equalTo(expectedValues));

                }

            }

        }

    }

    @Test
    public final void testToStringReflections() throws ParserConfigurationException, SAXException,
            IOException, XPathExpressionException {

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final XPathFactory xpathfactory = XPathFactory.newInstance();
        final XPath xpath = xpathfactory.newXPath();

        final Reflections reflections = new Reflections("org.reflections.util");

        // Method under test
        final String xmlString = serializer.toString(reflections);

        final InputSource is = new InputSource(new StringReader(xmlString));

        final Document doc = builder.parse(is);

        NodeList nodes = (NodeList) xpath.evaluate("/Reflections/SubTypesScanner", doc,
                                                   XPathConstants.NODESET);
        assertEquals(1, nodes.getLength());

        nodes = (NodeList) xpath.evaluate("//entry/key", doc, XPathConstants.NODESET);
        assertEquals(5, nodes.getLength());

        nodes = (NodeList) xpath.evaluate("//entry/values/value/text()", doc,
                                          XPathConstants.NODESET);
        assertEquals(8, nodes.getLength());

        for (int i = 0; i < nodes.getLength(); i++) {
            final Node node = nodes.item(i);
            final String value = node.getNodeValue();

            assertThat(value, containsString("org.reflections.util"));
        }

    }

}
