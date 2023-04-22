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

package dev.waterdog.waterdogpe.utils.bstats;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import dev.waterdog.waterdogpe.utils.config.proxy.ProxyConfig;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bstats.MetricsBase;
import org.bstats.charts.CustomChart;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bstats.config.MetricsConfig;
import org.bstats.json.JsonObjectBuilder;

@Log4j2
public class Metrics {
    private static Metrics instance;
    private MetricsBase metricsBase;

    private Metrics(int serviceId, boolean defaultEnabled) {
        File configFile = Path.of("plugins", "bStats", "config.txt").toFile();
        MetricsConfig config;
        try {
            config = new MetricsConfig(configFile, defaultEnabled);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create bStats config", e);
        }

        this.metricsBase = new MetricsBase(
                "server-implementation",
                config.getServerUUID(),
                serviceId,
                config.isEnabled(),
                this::appendPlatformData,
                jsonObjectBuilder -> { /* NOP */ },
                null,
                () -> ProxyServer.getInstance().isRunning(),
                log::warn,
                log::info,
                config.isLogErrorsEnabled(),
                config.isLogSentDataEnabled(),
                config.isLogResponseStatusTextEnabled()
        );

        if (!config.didExistBefore()) {
            // Send an info message when the bStats config file gets created for the first time
            log.info("WaterdogPE and some of its plugins collect metrics"
                    + " and send them to bStats (https://bStats.org).");
            log.info("bStats collects some basic information for plugin"
                    + " authors, like how many people use");
            log.info("their plugin and their total player count."
                    + " It's recommended to keep bStats enabled, but");
            log.info("if you're not comfortable with this, you can opt-out"
                    + " by editing the config.txt file in");
            log.info("the '/bStats/' folder and setting enabled to false.");
        }
    }

    /**
     * Adds a custom chart.
     *
     * @param chart The chart to add.
     */
    public void addCustomChart(CustomChart chart) {
        metricsBase.addCustomChart(chart);
    }

    private void appendPlatformData(JsonObjectBuilder builder) {
        builder.appendField("osName", System.getProperty("os.name"));
        builder.appendField("osArch", System.getProperty("os.arch"));
        builder.appendField("osVersion", System.getProperty("os.version"));
        builder.appendField("coreCount", Runtime.getRuntime().availableProcessors());
    }

    public void shutdown() {
        this.metricsBase.shutdown();
    }

    public static void startMetrics(ProxyServer server, ProxyConfig metricsConfig) {
        if (instance != null) {
            throw new IllegalStateException("Metrics were already initialised");
        }

        Metrics metrics = new Metrics(WaterdogPE.version().metricsId(), metricsConfig.isEnableAnonymousStatistics());

        metrics.addCustomChart(
                new SingleLineChart("players", () -> server.getPlayers().size())
        );
        metrics.addCustomChart(
                new SingleLineChart("managed_servers", () -> server.getServers().size())
        );
        metrics.addCustomChart(
                new SimplePie("online_mode",
                        () -> server.getConfiguration().isOnlineMode() ? "online" : "offline")
        );
        metrics.addCustomChart(new SimplePie("waterdog_version",
                () -> WaterdogPE.version().baseVersion()));

        metrics.addCustomChart(new DrilldownPie("java_version", () -> {
            Map<String, Map<String, Integer>> map = new HashMap<>();
            String javaVersion = System.getProperty("java.version");
            Map<String, Integer> entry = new HashMap<>();
            entry.put(javaVersion, 1);

            // http://openjdk.java.net/jeps/223
            // Java decided to change their versioning scheme and in doing so modified the
            // java.version system property to return $major[.$minor][.$security][-ea], as opposed to
            // 1.$major.0_$identifier we can handle pre-9 by checking if the "major" is equal to "1",
            // otherwise, 9+
            String majorVersion = javaVersion.split("\\.")[0];
            String release;

            int indexOf = javaVersion.lastIndexOf('.');

            if (majorVersion.equals("1")) {
                release = "Java " + javaVersion.substring(0, indexOf);
            } else {
                // of course, it really wouldn't be all that simple if they didn't add a quirk, now
                // would it valid strings for the major may potentially include values such as -ea to
                // denote a pre release
                Matcher versionMatcher = Pattern.compile("\\d+").matcher(majorVersion);
                if (versionMatcher.find()) {
                    majorVersion = versionMatcher.group(0);
                }
                release = "Java " + majorVersion;
            }
            map.put(release, entry);

            return map;
        }));
        instance = metrics;
    }

    public static Metrics get() {
        return instance;
    }

}