package dev.waterdog.utils;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.waterdog.logger.MainLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class JsonConfig extends Configuration {

  protected Gson json = new Gson();

  public JsonConfig() {
    super();
  }

  public JsonConfig(File file) {
    super(file);
  }

  public JsonConfig(Path path) {
    super(path);
  }

  public JsonConfig(String file) {
    super(file);
  }

  @Override
  public void load() {
    load(null);
  }

  @SuppressWarnings("unchecked")
  public void load(InputStream inputStream) {
    if (this.file == null && inputStream == null) return;

    try {
      Reader reader;

      if (inputStream == null) {
        reader = new BufferedReader(new FileReader(this.file));
      } else {
        reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
      }

      this.values = json.fromJson(reader, Map.class);
    } catch (Exception e) {
      MainLogger
        .getLogger()
        .error("Unable to load Config " + this.file.toString());
    }
  }

  @Override
  public void save() {
    if (this.file == null) return;

    String json = new GsonBuilder().create().toJson(this.values);

    try {
      Files.write(this.file.toPath(), json.getBytes(Charsets.UTF_8));
    } catch (IOException e) {
      MainLogger
        .getLogger()
        .error("Unable to save Config " + this.file.toString());
    }
  }

  @Override
  public String getDefaultFileContent() {
    return "{}";
  }
}
