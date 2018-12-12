package com.tvd12.reflections.serializers;

import com.google.gson.*;
import com.tvd12.reflections.Reflections;
import com.tvd12.reflections.io.Files;
import com.tvd12.reflections.util.Multimap;
import com.tvd12.reflections.util.Multimaps;
import com.tvd12.reflections.util.SetMultimap;
import com.tvd12.reflections.util.Sets;
import com.tvd12.reflections.util.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** serialization of Reflections to json
 *
 * <p>an example of produced json:
 * <pre>
 * {"store":{"storeMap":
 *    {"com.tvd12.reflections.scanners.TypeAnnotationsScanner":{
 *       "com.tvd12.reflections.TestModel$AC1":["com.tvd12.reflections.TestModel$C1"],
 *       "com.tvd12.reflections.TestModel$AC2":["com.tvd12.reflections.TestModel$I3",
 * ...
 * </pre>
 * */
@SuppressWarnings("rawtypes")
public class JsonSerializer implements Serializer {
    private Gson gson;

    public Reflections read(InputStream inputStream) {
        return getGson().fromJson(new InputStreamReader(inputStream), Reflections.class);
    }

    public File save(Reflections reflections, String filename) {
        try {
            File file = Utils.prepareFile(filename);
            Files.write(toString(reflections), file, Charset.defaultCharset());
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString(Reflections reflections) {
        return getGson().toJson(reflections);
    }

    private Gson getGson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    .registerTypeAdapter(Multimap.class, new com.google.gson.JsonSerializer<Multimap>() {
						public JsonElement serialize(Multimap multimap, Type type, JsonSerializationContext jsonSerializationContext) {
                            return jsonSerializationContext.serialize(multimap.asMap());
                        }
                    })
                    .registerTypeAdapter(Multimap.class, new JsonDeserializer<Multimap>() {
                        public Multimap deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                            final SetMultimap<String,String> map = Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
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

        }
        return gson;
    }
}
