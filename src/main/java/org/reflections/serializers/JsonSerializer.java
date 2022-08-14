package org.reflections.serializers;

import com.google.gson.GsonBuilder;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * json serialization for {@link org.reflections.Reflections} <pre>{@code reflections.save(file, new JsonSerializer())}</pre>
 * <p></p>an example of produced json:
 * <pre>{@code
 * {
 *   "store": {
 *     "SubTypes": {
 *       "org.reflections.TestModel$C1": [
 *         "org.reflections.TestModel$C2",
 *         "org.reflections.TestModel$C3"
 *       ]
 *     },
 *     "TypesAnnotated": {
 *       "org.reflections.TestModel$AC2": [
 *         "org.reflections.TestModel$C2",
 *         "org.reflections.TestModel$C3"
 *       ]
 *     }
 *   }
 * }
 * }</pre>
 * */
public class JsonSerializer implements Serializer {

    @Override
    public Reflections read(InputStream inputStream) {
        return new GsonBuilder().setPrettyPrinting().create()
            .fromJson(new InputStreamReader(inputStream), Reflections.class);
    }

    @Override
    public File save(Reflections reflections, String filename) {
        try {
            File file = Serializer.prepareFile(filename);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(reflections);
            Files.write(file.toPath(), json.getBytes(Charset.defaultCharset()));
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
