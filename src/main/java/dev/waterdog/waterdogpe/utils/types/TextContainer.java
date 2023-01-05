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

package dev.waterdog.waterdogpe.utils.types;

import dev.waterdog.waterdogpe.logger.Color;
import dev.waterdog.waterdogpe.logger.MainLogger;

public class TextContainer implements Cloneable {

    private String message;

    public TextContainer(String message) {
        this(message, new String[0]);
    }

    public TextContainer(String message, String... args) {
        if (args != null && args.length >= 1) {
            message = this.translate(message, args);
        }
        this.message = message;
    }

    protected String translate(String message, String... args) {
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{%" + i + "}", args[i]);
        }
        return message;
    }

    public String getMessage() {
        return this.message;
    }

    public String clean() {
        return Color.clean(this.message);
    }

    @Override
    public String toString() {
        return this.message;
    }

    @Override
    protected TextContainer clone() {
        try {
            return (TextContainer) super.clone();
        } catch (CloneNotSupportedException e) {
            MainLogger.getLogger().throwing(e);
        }
        return null;
    }
}
