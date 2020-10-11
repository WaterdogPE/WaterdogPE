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

package pe.waterdog.utils.types;

import lombok.ToString;

import java.util.concurrent.atomic.AtomicBoolean;

@ToString
public class Permission {

    private final String name;
    private final AtomicBoolean value = new AtomicBoolean(false);

    public Permission(String name, boolean value){
        this.name = name.toLowerCase();
        this.value.set(value);
    }

    public String getName() {
        return this.name;
    }

    public void setValue(boolean value) {
        this.value.set(value);
    }

    public boolean getValue() {
        return this.value.get();
    }

    public AtomicBoolean getAtomicValue() {
        return this.value;
    }
}
