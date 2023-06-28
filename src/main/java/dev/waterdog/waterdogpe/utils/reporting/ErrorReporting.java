/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.utils.reporting;

import com.bugsnag.Bugsnag;
import com.bugsnag.Severity;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.WaterdogPE;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

public class ErrorReporting {

    private Bugsnag bugsnag = null;

    public ErrorReporting(ProxyServer server) {
        if (server.getConfiguration().isEnableAnonymousErrorReporting()) {
            bugsnag = new Bugsnag("69403750fbff896b2e37022a56e3cde4");
            bugsnag.setAppVersion(WaterdogPE.version().baseVersion());

            bugsnag.addCallback(report -> {
                report.addToTab("version", "commitId", WaterdogPE.version().commitId());
                report.addToTab("version", "buildVersion", WaterdogPE.version().buildVersion());
                report.addToTab("version", "baseVersion", WaterdogPE.version().baseVersion());
                report.addToTab("version", "branchName", WaterdogPE.version().branchName());
                report.addToTab("version", "latestProtocolVersion", WaterdogPE.version().latestProtocolVersion());

                report.addToTab("java", "JVM version", System.getProperty("java.version"));
                report.addToTab("java", "JVM runtime", System.getProperty("java.runtime.name"));
                report.addToTab("java", "JVM runtime version", System.getProperty("java.runtime.version"));

                report.addToTab("os", "Name", System.getProperty("os.name"));
                report.addToTab("os", "Version", System.getProperty("os.version"));
                report.addToTab("os", "Architecture", System.getProperty("os.arch"));
            });

            setupErrorLogger();

            server.getLogger().info("Anonymous error reporting is enabled.");
        }
    }

    private void setupErrorLogger() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        Layout<? extends Serializable> layout = PatternLayout.createDefaultLayout(config);

        Log4j2ErrorReporter reporter = new Log4j2ErrorReporter("ExceptionAppender", null, layout, false);

        reporter.start();
        config.addAppender(reporter);
        ctx.getRootLogger().addAppender(reporter);
        ctx.updateLoggers();
    }


    public boolean isEnabled() {
        return bugsnag != null;
    }

    public void reportError(Throwable t, Severity severity) {
        if(isEnabled()) {
            bugsnag.notify(t, severity);
        }
    }
}
