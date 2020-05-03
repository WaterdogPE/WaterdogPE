/**
 * Copyright 2020 WaterdogTEAM
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pe.waterdog.utils;
import pe.waterdog.ProxyServer;
import pe.waterdog.logger.Logger;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationManager {

    public static final int JSON = 1;
    public static final int YAML = 2;

    private ProxyServer server;
    private ProxyConfig proxyConfig;

    public ConfigurationManager(ProxyServer server){
        this.server = server;
    }

    public static Configuration newConfig(File file, int type){
        return newConfig(file.toString(), type);
    }

    public static Configuration newConfig(String file, int type){
        switch (type){
            case YAML:
                return new YamlConfig(file);
            case JSON:
                //TODO: Json config
                return null;
            default:
                return null;
        }
    }

    public void loadProxyConfig(){
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.yml");
        Path path = Paths.get(this.server.getDataPath().toString(), "config.yml");

        try {
            if (!path.toFile().exists()){
                Files.copy(inputStream, path);
            }

        }catch (Exception e){
            Logger.getLogger().error("Unable to save proxy config file!", e);
        }

        this.proxyConfig = new ProxyConfig(path);
    }

    public ProxyServer getServer() {
        return server;
    }

    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }
}
