/*
 * Copyright 2023 WaterdogTEAM
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

package dev.waterdog.waterdogpe.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionThrottle {
    private final ExpiringMap<InetAddress, Entry> map;
    private int limit;

    public ConnectionThrottle(int limit, int throttleTime) {
        this.limit = limit;
        this.map = ExpiringMap.builder()
                .expiration(throttleTime, TimeUnit.MILLISECONDS)
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .variableExpiration()
                .build();
    }

    public boolean throttle(InetAddress address) {
        Entry entry = this.map.get(address);
        if (entry == null) {
            entry = new Entry(this.limit);
            this.map.put(address, entry, ExpirationPolicy.ACCESSED);
        }

        int value = entry.value.incrementAndGet();
        return value <= entry.limit;
    }

    public void unthrottle(InetAddress address) {
        Entry entry = this.map.get(address);
        if (entry != null) {
            entry.value.decrementAndGet();
        }
    }

    public void reset(InetAddress address) {
        this.map.remove(address);
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getLimit() {
        return this.limit;
    }

    @Data
    @AllArgsConstructor
    private static class Entry {
        private final AtomicInteger value = new AtomicInteger();
        private volatile int limit;
    }
}
