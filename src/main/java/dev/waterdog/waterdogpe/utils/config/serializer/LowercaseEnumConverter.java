/*
 * Copyright 2026 WaterdogTEAM
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

package dev.waterdog.waterdogpe.utils.config.serializer;

import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Writes an enum option as its lowercase name and reads it in any case. Any other value fails the
 * load, naming the values it takes.
 */
public class LowercaseEnumConverter implements Converter {

    public LowercaseEnumConverter(InternalConverter internalConverter) {
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) {
        return ((Enum<?>) object).name().toLowerCase(Locale.ROOT);
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) {
        if (object == null) {
            return null;
        }
        String value = String.valueOf(object).trim();
        for (Object constant : type.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
                return constant;
            }
        }
        String names = Arrays.stream(type.getEnumConstants())
                .map(constant -> ((Enum<?>) constant).name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(", "));
        // SignalingMode reads as "signaling mode"
        String label = type.getSimpleName().replaceAll("(?<=[a-z])(?=[A-Z])", " ").toLowerCase(Locale.ROOT);
        throw new IllegalArgumentException("\"" + value + "\" is not a valid " + label + ", expected one of " + names);
    }

    @Override
    public boolean supports(Class<?> type) {
        return type.isEnum();
    }
}
