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

import dev.waterdog.waterdogpe.network.protocol.user.UuidFormat;
import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

import java.lang.reflect.ParameterizedType;
import java.util.Locale;

public class UuidFormatConverter implements Converter {

    public UuidFormatConverter(InternalConverter internalConverter) {
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) {
        if (object instanceof UuidFormat format) {
            return format.name();
        } else {
            throw new IllegalArgumentException("Can not serialize " + object.getClass().getSimpleName() + " as UuidFormat");
        }
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) {
        if (object == null) {
            return UuidFormat.IDENTITY;
        }
        try {
            return UuidFormat.valueOf(object.toString().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UuidFormat.IDENTITY;
        }
    }

    @Override
    public boolean supports(Class<?> type) {
        return UuidFormat.class.isAssignableFrom(type);
    }
}
