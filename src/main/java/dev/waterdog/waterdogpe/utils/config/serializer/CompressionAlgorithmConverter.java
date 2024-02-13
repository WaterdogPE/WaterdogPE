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

package dev.waterdog.waterdogpe.utils.config.serializer;

import dev.waterdog.waterdogpe.network.connection.codec.compression.CompressionType;
import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

import java.lang.reflect.ParameterizedType;

public class CompressionAlgorithmConverter implements Converter {

    private final InternalConverter internalConverter;

    public CompressionAlgorithmConverter(InternalConverter internalConverter) {
        this.internalConverter = internalConverter;
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object instanceof CompressionType algorithm) {
            return algorithm.getIdentifier();
        } else {
            throw new IllegalArgumentException("Can not serialize " + object.getClass().getSimpleName() + " as CompressionType");
        }
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object == null) {
            return null;
        }
        return CompressionType.fromString((String) object);
    }

    @Override
    public boolean supports(Class<?> type) {
        return CompressionType.class.isAssignableFrom(type);
    }
}
