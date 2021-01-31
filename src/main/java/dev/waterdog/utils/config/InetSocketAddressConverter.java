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

package dev.waterdog.utils.config;

import net.cubespace.Yamler.Config.Converter.Converter;
import net.cubespace.Yamler.Config.InternalConverter;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;

public class InetSocketAddressConverter implements Converter {

    private final InternalConverter internalConverter;

    public InetSocketAddressConverter(InternalConverter internalConverter) {
        this.internalConverter = internalConverter;
    }

    @Override
    public Object toConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object == null){
            return null;
        }
        InetSocketAddress address = (InetSocketAddress) object;
        return address.getHostName() + ":" + address.getPort();
    }

    @Override
    public Object fromConfig(Class<?> type, Object object, ParameterizedType parameterizedType) throws Exception {
        if (object == null) {
            return null;
        }
        String string = (String) object;
        String[] parts = string.split(":");
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    @Override
    public boolean supports(Class<?> type) {
        return InetSocketAddress.class.isAssignableFrom(type);
    }
}
