package org.reflections.serializers;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.io.Files;
import com.google.gson.*;
import org.reflections.Reflections;
import org.reflections.util.Utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * serialization of Reflections to json
 *
 * <p>an example of produced json:
 * <pre>
 * {"store":{"storeMap":
 *    {"org.reflections.scanners.TypeAnnotationsScanner":{
 *       "org.reflections.TestModel$AC1":["org.reflections.TestModel$C1"],
 *       "org.reflections.TestModel$AC2":["org.reflections.TestModel$I3",
 * ...
 * </pre>
 */
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
          .registerTypeAdapter(
              Multimap.class,
              (com.google.gson.JsonSerializer<Multimap>) (multimap, type, jsonSerializationContext) -> jsonSerializationContext.serialize(multimap.asMap()))
          .registerTypeAdapter(
              Multimap.class,
              (JsonDeserializer<Multimap>) (jsonElement, type, jsonDeserializationContext) -> {
                final Multimap<String, String> map = Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), HashSet::new);
                for (Map.Entry<String, JsonElement> entry : ((JsonObject) jsonElement).entrySet()) {
                  for (JsonElement element : (JsonArray) entry.getValue()) {
                    map.put(entry.getKey(),element.getAsString());
                  }
                }
                return map;
              })
          .setPrettyPrinting()
          .create();

    }
    return gson;
  }
}
