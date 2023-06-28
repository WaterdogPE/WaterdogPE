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

import com.bugsnag.Severity;
import dev.waterdog.waterdogpe.ProxyServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.io.Serializable;

public class Log4j2ErrorReporter extends AbstractAppender {


    public Log4j2ErrorReporter(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        if (event.getThrown() == null) {
            return;
        }

        ProxyServer.getInstance().getErrorReporting().reportError(event.getThrown(), event.getLevel().intLevel() > Level.WARN.intLevel() ? Severity.WARNING : Severity.ERROR);
    }

    public static void init() {
        // no-op
    }
}
