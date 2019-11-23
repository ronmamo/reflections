package org.reflections.serializers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.reflections.Reflections;
import org.reflections.util.Multimap;
import org.reflections.util.SetMultimap;
import org.reflections.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Map;

/** serialization of Reflections to json
 *
 * <p>an example of produced json:
 * <pre>
 * {"store":{"storeMap":
 *    {"org.reflections.scanners.TypeAnnotationsScanner":{
 *       "org.reflections.TestModel$AC1":["org.reflections.TestModel$C1"],
 *       "org.reflections.TestModel$AC2":["org.reflections.TestModel$I3",
 * ...
 * </pre>
 * */
public class JsonSerializer implements Serializer {
    private Gson gson;

    public Reflections read(final InputStream inputStream) {
        return getGson().fromJson(new InputStreamReader(inputStream), Reflections.class);
    }

    public File save(final Reflections reflections, final String filename) {
        try {
            final File file = Utils.prepareFile(filename);
            Files.write(file.toPath(), toString(reflections).getBytes(Charset.defaultCharset()));
            return file;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString(final Reflections reflections) {
        return getGson().toJson(reflections);
    }

    private Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Multimap.class, this.serializer())
                    .registerTypeAdapter(Multimap.class, this.deserializer())
                    .setPrettyPrinting()
                    .create();

        }
        return gson;
    }

    private JsonDeserializer<Multimap> deserializer() {
        return  (jsonElement, type, jsonDeserializationContext) -> {

            final SetMultimap<String,String> map = new SetMultimap<>();

            for (Map.Entry<String, JsonElement> entry : ((JsonObject) jsonElement).entrySet()) {
                for (final JsonElement element : (JsonArray) entry.getValue()) {
                    map.get(entry.getKey()).add(element.getAsString());
                }
            }
            return map;
        };
    }

    private com.google.gson.JsonSerializer<Multimap> serializer() {
        return (multimap, type, jsonSerializationContext) ->
                jsonSerializationContext.serialize(multimap.asMap());
    }
}
