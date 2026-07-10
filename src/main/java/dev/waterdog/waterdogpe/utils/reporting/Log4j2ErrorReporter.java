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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Log4j2ErrorReporter extends AbstractAppender {

    // Cap how many events we forward to Bugsnag per window so a burst of exceptions
    // (e.g. a flood of malformed-packet decode errors) can't outrun the async delivery
    // and pile up in memory until the heap is exhausted.
    private static final long RATE_WINDOW_MS = 1000L;
    private static final int MAX_REPORTS_PER_WINDOW = 30;

    private final AtomicLong windowStart = new AtomicLong(0L);
    private final AtomicInteger windowCount = new AtomicInteger(0);
    private final AtomicLong droppedReports = new AtomicLong(0L);

    public Log4j2ErrorReporter(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
    }

    @Override
    public void append(LogEvent event) {
        if (event.getThrown() == null) {
            return;
        }

        // Only report warnings and errors; lower-severity events aren't actionable reports.
        if (event.getLevel().intLevel() > Level.WARN.intLevel()) {
            return;
        }

        if (!this.allowReport(System.currentTimeMillis())) {
            this.droppedReports.incrementAndGet();
            return;
        }

        Severity severity = event.getLevel().intLevel() <= Level.ERROR.intLevel() ? Severity.ERROR : Severity.WARNING;
        ProxyServer.getInstance().getErrorReporting().reportError(event.getThrown(), severity);
    }

    private boolean allowReport(long now) {
        long start = this.windowStart.get();
        if (now - start >= RATE_WINDOW_MS && this.windowStart.compareAndSet(start, now)) {
            this.windowCount.set(0);
        }
        return this.windowCount.incrementAndGet() <= MAX_REPORTS_PER_WINDOW;
    }

    public long getDroppedReports() {
        return this.droppedReports.get();
    }

    public static void init() {
        // no-op
    }
}
