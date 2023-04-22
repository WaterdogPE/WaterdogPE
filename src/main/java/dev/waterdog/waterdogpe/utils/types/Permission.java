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

import lombok.ToString;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Permission container for assigning permissions to players.
 */
@ToString
public class Permission {

    private final String name;
    private final AtomicBoolean value = new AtomicBoolean(false);

    public Permission(String name, boolean value) {
        this.name = name.toLowerCase();
        this.value.set(value);
    }

    public String getName() {
        return this.name;
    }

    public boolean getValue() {
        return this.value.get();
    }

    public void setValue(boolean value) {
        this.value.set(value);
    }

    public AtomicBoolean getAtomicValue() {
        return this.value;
    }
}
