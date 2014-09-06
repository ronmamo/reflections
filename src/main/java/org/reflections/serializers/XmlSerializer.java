package org.reflections.serializers;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.Utils;

import java.io.*;
import java.lang.reflect.Constructor;

/** serialization of Reflections to xml
 *
 * <p>an example of produced xml:
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
 * */
public class XmlSerializer implements Serializer {

    public Reflections read(InputStream inputStream) {
        Reflections reflections;
        try {
            Constructor<Reflections> constructor = Reflections.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            reflections = constructor.newInstance();
        } catch (Exception e) {
            reflections = new Reflections(new ConfigurationBuilder());
        }

        try {
            Document document = new SAXReader().read(inputStream);
            for (Object e1 : document.getRootElement().elements()) {
                Element index = (Element) e1;
                for (Object e2 : index.elements()) {
                    Element entry = (Element) e2;
                    Element key = entry.element("key");
                    Element values = entry.element("values");
                    for (Object o3 : values.elements()) {
                        Element value = (Element) o3;
                        reflections.getStore().getOrCreate(index.getName()).put(key.getText(), value.getText());
                    }
                }
            }
        } catch (DocumentException e) {
            throw new ReflectionsException("could not read.", e);
        } catch (Throwable e) {
            throw new RuntimeException("Could not read. Make sure relevant dependencies exist on classpath.", e);
        }

        return reflections;
    }

    public File save(final Reflections reflections, final String filename) {
        File file = Utils.prepareFile(filename);


        try {
            Document document = createDocument(reflections);
            XMLWriter xmlWriter = new XMLWriter(new FileOutputStream(file), OutputFormat.createPrettyPrint());
            xmlWriter.write(document);
            xmlWriter.close();
        } catch (IOException e) {
            throw new ReflectionsException("could not save to file " + filename, e);
        } catch (Throwable e) {
            throw new RuntimeException("Could not save to file " + filename + ". Make sure relevant dependencies exist on classpath.", e);
        }

        return file;
    }

    public String toString(final Reflections reflections) {
        Document document = createDocument(reflections);

        try {
            StringWriter writer = new StringWriter();
            XMLWriter xmlWriter = new XMLWriter(writer, OutputFormat.createPrettyPrint());
            xmlWriter.write(document);
            xmlWriter.close();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private Document createDocument(final Reflections reflections) {
        Store map = reflections.getStore();

        Document document = DocumentFactory.getInstance().createDocument();
        Element root = document.addElement("Reflections");
        for (String indexName : map.keySet()) {
            Element indexElement = root.addElement(indexName);
            for (String key : map.get(indexName).keySet()) {
                Element entryElement = indexElement.addElement("entry");
                entryElement.addElement("key").setText(key);
                Element valuesElement = entryElement.addElement("values");
                for (String value : map.get(indexName).get(key)) {
                    valuesElement.addElement("value").setText(value);
                }
            }
        }
        return document;
    }
}
