/*
 * Copyright 2021 WaterdogTEAM
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.waterdog.utils.types;

import dev.waterdog.ProxyServer;

public class TranslationContainer extends TextContainer {

    private String[] params;

    public TranslationContainer(String message) {
        super(message);
    }

    public TranslationContainer(String message, String... args) {
        super(message, args);
        this.params = args;
    }

    @Override
    protected String translate(String message, String... args) {
        return message;
    }

    public String getTranslated() {
        return ProxyServer.getInstance().translate(this);
    }

    public String[] getParams() {
        return this.params;
    }

    @Override
    public String toString() {
        return this.getTranslated();
    }
}
