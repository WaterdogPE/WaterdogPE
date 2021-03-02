package dev.waterdog.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.waterdog.logger.MainLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

public class JsonConfig extends Configuration {

    protected Gson json = new Gson();

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

    @SuppressWarnings("unchecked")
    public void load(InputStream inputStream) {
        try {
            Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

            this.values = json.fromJson(reader, Map.class);
        } catch (Exception e) {
            MainLogger.getLogger().error("Unable to load Config " + this.file.toString());
        }
    }

    @Override
    public void save() {
        save(new GsonBuilder().create().toJson(this.values));
    }

    @Override
    public String getDefaultFileContent() {
        return "{}";
    }
}
