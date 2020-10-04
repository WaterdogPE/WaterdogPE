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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigurationManager {

    public static final int JSON = 1;
    public static final int YAML = 2;

    private ProxyServer proxy;
    private ProxyConfig proxyConfig;
    private LangConfig langConfig;

    public ConfigurationManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public static Configuration newConfig(File file, int type) {
        return newConfig(file.toString(), type);
    }

    public static Configuration newConfig(String file, int type) {
        switch (type) {
            case YAML:
                return new YamlConfig(file);
            case JSON:
                //TODO: Json config
                return null;
            default:
                return null;
        }
    }

    public void loadProxyConfig() {
        File configFile = new File(this.proxy.getDataPath().toString()+"/config.yml");
        if (!configFile.exists()){
            try {
                FileUtils.saveFromResources("config.yml", configFile);
            }catch (IOException e){
                this.proxy.getLogger().error("Unable to save proxy config file!", e);
            }
        }
        this.proxyConfig = new ProxyConfig(configFile);
    }

    public void loadLanguage(){
        File langFile = new File(this.proxy.getDataPath().toString()+"/lang.ini");
        if (!langFile.exists()){
            try {
                FileUtils.saveFromResources("lang.ini", langFile);
            }catch (IOException e){
                this.proxy.getLogger().error("Can not save lang file!", e);
            }
        }

        this.langConfig = new LangConfig(langFile);
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    public ProxyConfig getProxyConfig() {
        return this.proxyConfig;
    }

    public LangConfig getLangConfig() {
        return this.langConfig;
    }
}
