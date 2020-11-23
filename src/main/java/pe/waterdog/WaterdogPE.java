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

package pe.waterdog;

import io.netty.util.ResourceLeakDetector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import pe.waterdog.logger.MainLogger;

public class WaterdogPE {

    public static String DATA_PATH = System.getProperty("user.dir") + "/";
    public final static String PLUGIN_PATH = DATA_PATH + "plugins";

    public static void main(String[] args) {
        Thread.currentThread().setName("WaterdogPE-main");
        System.out.println("Starting WaterdogPE....");
        System.setProperty("log4j.skipJansi", "false");
        System.setSecurityManager(null);
        System.setProperty("java.net.preferIPv4Stack", "true");
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);

        MainLogger logger = MainLogger.getLogger();
        logger.info("§bStarting WaterDogPE proxy software!");
        logger.info("§3Software Version: " + VersionInfo.BASE_VERSION);
        logger.info("§3Build Version: " + VersionInfo.BUILD_VERSION);
        logger.info("§3Development Build: " + VersionInfo.IS_DEVELOPMENT);
        logger.info("§3Software Authors: " + VersionInfo.AUTHOR);

        if (VersionInfo.BUILD_VERSION == "#build") {
            logger.warning("Unknown build id. Custom build? Unofficial builds should be not run in production!");
        }

        if (VersionInfo.IS_DEVELOPMENT) {
            setLoggerLevel(Level.DEBUG);
        }

        try {
            ProxyServer server = new ProxyServer(logger, DATA_PATH, PLUGIN_PATH);
        } catch (Exception e) {
            logger.logException(e);
        }
    }

    public static void setLoggerLevel(Level level) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = context.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        context.updateLoggers();
    }
}
