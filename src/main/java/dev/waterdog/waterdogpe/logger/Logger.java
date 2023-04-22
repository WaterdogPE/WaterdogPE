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

package dev.waterdog.waterdogpe.logger;

/**
 * Base Logger class
 */
public interface Logger {

    void debug(String message);

    void debug(String message, Object... params);

    void debug(String message, Throwable t);

    void info(String message);

    void info(String message, Object... params);

    void info(String message, Throwable t);

    void warning(String message);

    void warning(String message, Object... params);

    void warning(String message, Throwable t);

    void error(String message);

    void error(String message, Object... params);

    void error(String message, Throwable t);

    void critical(String message);

    void critical(String message, Object... params);

    void critical(String message, Throwable t);

    void throwing(Throwable throwable);

    @Deprecated
    default void alert(String message) {
        this.warning(message);
    }

    @Deprecated
    default void alert(String message, Throwable t) {
        this.warning(message, t);
    }

    @Deprecated
    default void notice(String message) {
        this.info(message);
    }

    @Deprecated
    default void notice(String message, Throwable t) {
        this.info(message, t);
    }

    @Deprecated
    default void emergency(String message)  {
        this.warning(message);
    }

    @Deprecated
    default void emergency(String message, Throwable t) {
        this.warning(message, t);
    }
}
