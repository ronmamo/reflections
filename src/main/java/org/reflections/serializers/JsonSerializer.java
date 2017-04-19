package org.reflections.serializers;

import com.google.common.base.Supplier;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.gson.*;
import org.reflections.Reflections;
import org.reflections.util.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Serialization of Reflections to json.
 *
 * <p>
 * an example of produced json:
 * <pre>
 * {"store":{"storeMap":
 *    {"org.reflections.scanners.TypeAnnotationsScanner":{
 *       "org.reflections.TestModel$AC1":["org.reflections.TestModel$C1"],
 *       "org.reflections.TestModel$AC2":["org.reflections.TestModel$I3",
 * ...
 * </pre>
 *
 */
public class JsonSerializer implements Serializer {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Multimap.class, new com.google.gson.JsonSerializer<Multimap>() {
                @Override
                public JsonElement serialize(Multimap multimap, Type type, JsonSerializationContext jsonSerializationContext) {
                    return jsonSerializationContext.serialize(multimap.asMap());
                }
            })
            .registerTypeAdapter(Multimap.class, new JsonDeserializer<Multimap>() {
                @Override
                public Multimap deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                    final SetMultimap<String, String> map = Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
                        @Override
                        public Set<String> get() {
                            return Sets.newHashSet();
                        }
                    });
                    for (Map.Entry<String, JsonElement> entry : ((JsonObject) jsonElement).entrySet()) {
                        for (JsonElement element : (JsonArray) entry.getValue()) {
                            map.get(entry.getKey()).add(element.getAsString());
                        }
                    }
                    return map;
                }
            })
            .setPrettyPrinting()
            .create();

    @Override
    public Reflections read(InputStream inputStream) {
        return gson.fromJson(new InputStreamReader(inputStream), Reflections.class);
    }

    @Override
    public File save(Reflections reflections, String filename) {
        try {
            File file = Utils.prepareFile(filename);
            Files.write(toString(reflections), file, Charset.defaultCharset());
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString(Reflections reflections) {
        return gson.toJson(reflections);
    }
}

