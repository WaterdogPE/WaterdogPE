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

import org.fusesource.jansi.Ansi;

import java.util.regex.Pattern;

/**
 * Base color class usable for sending colors in the console
 */
public enum Color {

    BLACK('0', 0x0, Ansi.Color.BLACK),
    DARK_BLUE('1', 0x1, Ansi.Color.BLUE),
    DARK_GREEN('2', 0x2, Ansi.Color.GREEN),
    DARK_AQUA('3', 0x3, Ansi.Color.CYAN),
    DARK_RED('4', 0x4, Ansi.Color.RED),
    DARK_PURPLE('5', 0x5, Ansi.Color.MAGENTA),
    GOLD('6', 0x6, Ansi.Color.YELLOW),
    GRAY('7', 0x7, Ansi.Color.WHITE),
    DARK_GRAY('8', 0x8, Ansi.Color.BLACK, true),
    BLUE('9', 0x9, Ansi.Color.BLUE, true),
    GREEN('a', 0xA, Ansi.Color.GREEN, true),
    AQUA('b', 0xB, Ansi.Color.CYAN, true),
    RED('c', 0xC, Ansi.Color.RED, true),
    LIGHT_PURPLE('d', 0xD, Ansi.Color.MAGENTA, true),
    YELLOW('e', 0xE, Ansi.Color.YELLOW, true),
    WHITE('f', 0xF, Ansi.Color.WHITE, true),
    BOLD('l', 0x11, null, false, Ansi.Attribute.UNDERLINE_DOUBLE),
    STRIKETHROUGH('m', 0x12, null, false, Ansi.Attribute.STRIKETHROUGH_ON),
    UNDERLINE('n', 0x13, null, false, Ansi.Attribute.UNDERLINE),
    ITALIC('o', 0x14, null, false, Ansi.Attribute.ITALIC),
    RESET('r', 0x15, null, false, Ansi.Attribute.RESET);

    public static final char ESCAPE = '\u00A7';
    private static final Pattern CLEAN_PATTERN = Pattern.compile("(?i)" + ESCAPE + "[0-9A-FK-OR]");

    private final char code;
    private final int intCode;
    private final Ansi.Color color;
    private final boolean bold;
    private final Ansi.Attribute attribute;
    private final String toString;

    Color(char code, int intCode, Ansi.Color color) {
        this(code, intCode, color, false, null);
    }

    Color(char code, int intCode, Ansi.Color color, boolean bold) {
        this(code, intCode, color, bold, null);
    }

    Color(char code, int intCode, Ansi.Color color, boolean bold, Ansi.Attribute attribute) {
        this.code = code;
        this.intCode = intCode;
        this.color = color;
        this.bold = bold;
        this.attribute = attribute;
        this.toString = new String(new char[]{ESCAPE, code});
    }

    public static String clean(final String input) {
        if (input == null) {
            return null;
        }
        return CLEAN_PATTERN.matcher(input).replaceAll("");
    }

    @Override
    public String toString() {
        return this.toString;
    }

    public Ansi getAnsi() {
        Ansi ansi = (attribute == null) ? Ansi.ansi().a(Ansi.Attribute.RESET) : Ansi.ansi().a(this.attribute);
        if (this.color != null) {
            ansi = ansi.fg(this.color);
        }
        return this.bold ? ansi.bold() : ansi;
    }
}
