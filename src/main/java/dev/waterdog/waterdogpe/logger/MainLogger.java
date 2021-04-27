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

package dev.waterdog.waterdogpe.logger;

import lombok.extern.log4j.Log4j2;

/**
 * Main Server logger
 */
@Log4j2
public class MainLogger implements Logger {

    private static MainLogger instance = new MainLogger();

    public static MainLogger getInstance() {
        return instance;
    }

    public static MainLogger getLogger() {
        return instance;
    }

    @Override
    public void emergency(String message) {
        log.fatal(message);
    }

    @Override
    public void alert(String message) {
        log.warn(message);
    }

    @Override
    public void critical(String message) {
        log.fatal(message);
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void warning(String message) {
        log.warn(message);
    }

    @Override
    public void notice(String message) {
        log.warn(message);
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void debug(String message) {
        log.debug(message);
    }

    public void logException(Throwable t) {
        log.throwing(t);
    }

    @Override
    public void emergency(String message, Throwable t) {
        log.fatal(message, t);
    }

    @Override
    public void alert(String message, Throwable t) {
        log.warn(message, t);
    }

    @Override
    public void critical(String message, Throwable t) {
        log.fatal(message, t);
    }

    @Override
    public void error(String message, Throwable t) {
        log.error(message, t);
    }

    @Override
    public void warning(String message, Throwable t) {
        log.warn(message, t);
    }

    @Override
    public void notice(String message, Throwable t) {
        log.warn(message, t);
    }

    @Override
    public void info(String message, Throwable t) {
        log.info(message, t);
    }

    @Override
    public void debug(String message, Throwable t) {
        log.debug(message, t);
    }
}
