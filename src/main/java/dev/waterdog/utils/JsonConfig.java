package dev.waterdog.utils;

import com.google.gson.Gson;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class JsonConfig extends Configuration {

    protected static Gson json = new Gson();

    public JsonConfig(File file) {
        super(file);
    }

    public JsonConfig(Path path) {
        super(path);
    }

    public JsonConfig(String file) {
        super(file);
    }

    public JsonConfig(File saveFile, InputStream inputStream) {
        super(saveFile, inputStream);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Map<String, Object> deserialize(InputStream inputStream) {
        return json.fromJson(new InputStreamReader(inputStream, StandardCharsets.UTF_8), Map.class);
    }

    @Override
    protected String serialize(Map<String, Object> values) {
        return json.toJson(this.values);
    }
}