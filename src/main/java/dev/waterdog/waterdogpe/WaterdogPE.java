/*
 * Copyright 2022 WaterdogTEAM
 * Licensed under the GNU General Public License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.waterdogpe;

import dev.waterdog.waterdogpe.logger.MainLogger;
import dev.waterdog.waterdogpe.network.protocol.ProtocolVersion;
import io.netty.util.ResourceLeakDetector;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WaterdogPE {

    public static String DATA_PATH = System.getProperty("user.dir") + "/";
    public final static String PLUGIN_PATH = DATA_PATH + "plugins";
    private static final VersionInfo versionInfo = loadVersion();

    public static void main(String[] args) {
        Thread.currentThread().setName("WaterdogPE-main");
        System.out.println("Starting WaterdogPE....");
        System.setProperty("log4j.skipJansi", "false");

        MainLogger logger = MainLogger.getLogger();
        logger.info("§bStarting WaterDogPE proxy software!");
        logger.info("§3Software Version: {}", versionInfo.baseVersion());
        logger.info("§3Build Version: {}", versionInfo.buildVersion());
        logger.info("§3Development Build: {}", versionInfo.debug());
        logger.info("§3Software Authors: {}", versionInfo.author());
        logger.info("§3Latest Supported Game Version: {}", ProtocolVersion.latest().getMinecraftVersion());


        int javaVersion = getJavaVersion();
        if (javaVersion < 17) {
            logger.error("Using unsupported Java version! Minimum supported version is Java 17, found Java " + javaVersion);
            return;
        }

        if (versionInfo.buildVersion().equals("#build") || versionInfo.branchName().equals("unknown")) {
            logger.warning("Custom build? Unofficial builds should be not run in production!");
        } else {
            logger.info("§3Discovered branch §b{}§3 commitId §b{}", versionInfo.branchName(), versionInfo.commitId());
        }

        if (versionInfo.debug()) {
            setLeakDetection(ResourceLeakDetector.Level.SIMPLE);
        } else {
            setLeakDetection(ResourceLeakDetector.Level.DISABLED);
        }

        logger.info("§eUsing memory leak detection level: {}", ResourceLeakDetector.getLevel());
        if (!versionInfo.debug() && ResourceLeakDetector.getLevel().ordinal() > ResourceLeakDetector.Level.SIMPLE.ordinal()) {
            logger.warning("§eUsing higher memory leak detection levels in production environment can affect application stability and performance!");
        }

        try {
            new ProxyServer(logger, DATA_PATH, PLUGIN_PATH);
        } catch (Exception e) {
            logger.throwing(e);
            shutdownHook();
        }
    }

    /**
     * This method is called when exception occurs or process saw shutdown
     */
    protected static void shutdownHook() {
        LogManager.shutdown();
        Runtime.getRuntime().halt(0); // force exit
    }

    private static VersionInfo loadVersion() {
        InputStream inputStream = WaterdogPE.class.getClassLoader().getResourceAsStream("git.properties");
        if (inputStream == null) {
            return VersionInfo.unknown();
        }

        Properties properties = new Properties();
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            return VersionInfo.unknown();
        }

        String branchName = properties.getProperty("git.branch", "unknown");
        String commitId = properties.getProperty("git.commit.id.abbrev", "unknown");
        boolean debug = branchName.equals("release") ? false : VersionInfo.DEFAULT_DEBUG;
        return new VersionInfo(branchName, commitId, debug);
    }

    public static void setLoggerLevel(Level level) {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration log4jConfig = context.getConfiguration();
        LoggerConfig loggerConfig = log4jConfig.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
        loggerConfig.setLevel(level);
        context.updateLoggers();
    }

    public static VersionInfo version() {
        return versionInfo;
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }

        Matcher versionMatcher = Pattern.compile("\\d+").matcher(version);
        if (versionMatcher.find()) {
            version = versionMatcher.group(0);
        }

        int index = version.indexOf(".");
        if (index != -1) {
            version = version.substring(0, index);
        }
        return Integer.parseInt(version);
    }

    private static void setLeakDetection(ResourceLeakDetector.Level level) {
        if (ResourceLeakDetector.getLevel().ordinal() < level.ordinal()) {
            ResourceLeakDetector.setLevel(level);
        }
    }
}
