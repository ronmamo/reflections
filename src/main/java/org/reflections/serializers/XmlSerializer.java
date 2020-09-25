package org.reflections.serializers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * serialization of Reflections to xml
 * <p>
 * an example of produced xml:
 *
 * <pre>
 * &#60?xml version="1.0" encoding="UTF-8"?>
 *
 * &#60Reflections>
 *  &#60SubTypesScanner>
 *      &#60entry>
 *          &#60key>com.google.inject.Module&#60/key>
 *          &#60values>
 *              &#60value>fully.qualified.name.1&#60/value>
 *              &#60value>fully.qualified.name.2&#60/value>
 * ...
 * </pre>
 */
public class XmlSerializer implements Serializer {

    private static final String ELEM_ROOT = "Reflections";
    private static final String ELEM_KEY = "key";
    private static final String ELEM_ENTRY = "entry";
    private static final String ELEM_VALUES = "values";
    private static final String ELEM_VALUE = "value";

    @Override
    public Reflections read(final InputStream inputStream) {

        try {

            final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            parserFactory.setFeature("http://xml.org/sax/features/external-general-entities",
                                     false);
            parserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities",
                                     false);
            parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            parserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                     false);

            final SAXParser parser = parserFactory.newSAXParser();

            final ReflectionsXMLHandler handler = new ReflectionsXMLHandler();

            parser.parse(inputStream, handler);

            return handler.reflections;

        } catch (final Throwable e) {
            throw new RuntimeException("Could not read. Make sure relevant dependencies exist on classpath.",
                                       e);
        }

    }

    @Override
    public File save(final Reflections reflections, final String filename) {
        final File file = Utils.prepareFile(filename);

        try (final FileWriter writer = new FileWriter(file)) {
            final Document document = createDocument(reflections);

            final Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            tf.transform(new DOMSource(document), new StreamResult(writer));

        } catch (final IOException e) {
            throw new ReflectionsException("could not save to file " + filename, e);
        } catch (final Throwable e) {
            throw new RuntimeException("Could not save to file " + filename
                    + ". Make sure relevant dependencies exist on classpath.", e);
        }

        return file;
    }

    @Override
    public String toString(final Reflections reflections) {

        try {
            final Document document = createDocument(reflections);
            final StringWriter writer = new StringWriter();

            final Transformer tf = TransformerFactory.newInstance().newTransformer();
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            tf.setOutputProperty(OutputKeys.INDENT, "yes");
            tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            tf.transform(new DOMSource(document), new StreamResult(writer));

            return writer.toString();

        } catch (final ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (final TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private Document createDocument(final Reflections reflections)
            throws ParserConfigurationException {
        final Store map = reflections.getStore();

        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = dbf.newDocumentBuilder();

        final Document document = builder.newDocument();
        document.setXmlStandalone(true);

        final Element root = document.createElement(ELEM_ROOT);
        document.appendChild(root);

        for (final String indexName : map.keySet()) {
            final Element indexElement = document.createElement(indexName);
            root.appendChild(indexElement);

            for (final String key : map.keys(indexName)) {
                final Element entryElement = document.createElement(ELEM_ENTRY);
                indexElement.appendChild(entryElement);

                final Element keyElement = document.createElement(ELEM_KEY);
                keyElement.setTextContent(key);
                entryElement.appendChild(keyElement);

                final Element valuesElement = document.createElement(ELEM_VALUES);
                entryElement.appendChild(valuesElement);

                for (final String value : map.get(indexName, key)) {
                    final Element valueElement = document.createElement(ELEM_VALUE);
                    valueElement.setTextContent(value);
                    valuesElement.appendChild(valueElement);
                }
            }
        }
        return document;
    }

    private class ReflectionsXMLHandler extends DefaultHandler {

        public Reflections reflections;

        private String indexName;

        private String key;

        private final StringBuilder buffer = new StringBuilder();

        public ReflectionsXMLHandler() {
            try {
                final Constructor<Reflections> constructor = Reflections.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                reflections = constructor.newInstance();
            } catch (@SuppressWarnings("unused") final Exception e) {
                reflections = new Reflections(new ConfigurationBuilder());
            }

        }

        @Override
        public void startElement(final String uri, final String localName, final String qName,
                                 final Attributes attributes)
                throws SAXException {

            drainBuffer();

            if (ELEM_ROOT.equals(qName)) {
                return;
            }

            if (indexName == null) {
                indexName = qName;
                return;
            }

        }

        @Override
        public void endElement(final String uri, final String localName, final String qName)
                throws SAXException {

            final String text = drainBuffer();

            if (ELEM_ROOT.equals(qName)) {
                return; // Done
            }

            if (qName.equals(indexName)) {
                indexName = null;

            } else if (ELEM_KEY.equals(qName)) {
                key = text;

            } else if (ELEM_VALUE.equals(qName)) {

                reflections.getStore().put(indexName, key, text);

            }
        }

        private String drainBuffer() {
            final String text = buffer.toString();
            buffer.setLength(0);
            return text;
        }

        @Override
        public void characters(final char[] ch, final int start, final int length)
                throws SAXException {
            buffer.append(ch, start, length);
        }

    }

}
