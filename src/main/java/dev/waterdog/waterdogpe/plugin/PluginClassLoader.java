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

package dev.waterdog.waterdogpe.plugin;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Simple class loader which holds classes of plugins.
 * It allows plugins to access each other.
 */
public class PluginClassLoader extends URLClassLoader {

    private final PluginManager pluginManager;
    private final Object2ObjectOpenHashMap<String, Class<?>> classes = new Object2ObjectOpenHashMap<>();

    public PluginClassLoader(PluginManager pluginManager, ClassLoader parent, File file) throws MalformedURLException {
        super(new URL[]{file.toURI().toURL()}, parent);
        this.pluginManager = pluginManager;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return this.findClass(name, true);
    }

    protected Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("dev.waterdog.waterdogpe.")) { // Proxy classes should be known
            throw new ClassNotFoundException(name);
        }

        Class<?> result = this.classes.get(name);
        if (result != null) {
            return result;
        }

        if (checkGlobal) {
            result = this.pluginManager.getClassFromCache(name);
        }

        if (result == null && (result = super.findClass(name)) != null) {
            this.pluginManager.cacheClass(name, result);
        }
        this.classes.put(name, result);
        return result;
    }

}
