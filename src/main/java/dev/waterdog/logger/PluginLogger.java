/*
 * Copyright 2021 WaterdogTEAM
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

package dev.waterdog.logger;

import dev.waterdog.plugin.Plugin;
import lombok.extern.log4j.Log4j2;

/**
 * Plugin-specific loggers.
 * Every plugin has its own logger, to identify log outputs from different plugins
 * without the need to manually prepend a prefix every time
 */
@Log4j2
public class PluginLogger implements Logger {

    private final String prefix;

    public PluginLogger(Plugin plugin) {
        this.prefix = "[" + plugin.getName() + "] ";
    }

    @Override
    public void emergency(String message) {
        log.fatal(this.prefix + message);
    }

    @Override
    public void alert(String message) {
        log.warn(this.prefix + message);
    }

    @Override
    public void critical(String message) {
        log.fatal(this.prefix + message);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void warning(String message) {
        log.warn(this.prefix + message);
    }

    @Override
    public void notice(String message) {
        log.warn(this.prefix + message);
    }

    @Override
    public void info(String message) {
        log.info(this.prefix + message);
    }

    @Override
    public void debug(String message) {
        log.debug(this.prefix + message);
    }

    public void logException(Throwable t) {
        log.throwing(t);
    }

    @Override
    public void emergency(String message, Throwable t) {
        log.fatal(this.prefix + message, t);
    }

    @Override
    public void alert(String message, Throwable t) {
        log.warn(this.prefix + message, t);
    }

    @Override
    public void critical(String message, Throwable t) {
        log.fatal(this.prefix + message, t);
    }

    @Override
    public void error(String message, Throwable t) {
        log.error(this.prefix + message, t);
    }

    @Override
    public void warning(String message, Throwable t) {
        log.warn(this.prefix + message, t);
    }

    @Override
    public void notice(String message, Throwable t) {
        log.warn(this.prefix + message, t);
    }

    @Override
    public void info(String message, Throwable t) {
        log.info(this.prefix + message, t);
    }

    @Override
    public void debug(String message, Throwable t) {
        log.debug(this.prefix + message, t);
    }
}
