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

package pe.waterdog.logger;

public interface Logger {

    void emergency(String message);

    void alert(String message);

    void critical(String message);

    void error(String message);

    void warning(String message);

    void notice(String message);

    void info(String message);

    void debug(String message);

    void emergency(String message, Throwable t);

    void alert(String message, Throwable t);

    void critical(String message, Throwable t);

    void error(String message, Throwable t);

    void warning(String message, Throwable t);

    void notice(String message, Throwable t);

    void info(String message, Throwable t);

    void debug(String message, Throwable t);
}
