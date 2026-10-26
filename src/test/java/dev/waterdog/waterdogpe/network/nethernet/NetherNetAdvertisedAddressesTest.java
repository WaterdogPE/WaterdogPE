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

package dev.waterdog.waterdogpe.network.nethernet;

import dev.waterdog.waterdogpe.utils.config.proxy.SignalingSettings;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NetherNetAdvertisedAddressesTest {
    private static final InetSocketAddress BOUND = new InetSocketAddress("192.168.1.5", 19132);

    @Test
    public void aProxyBoundToOneAddressAdvertisesIt() {
        assertEquals(List.of("192.168.1.5"), NetherNetInterface.advertisedAddresses(BOUND, new SignalingSettings(), false, false));
    }

    @Test
    public void behindRelaysTheBoundAddressIsNoStandIn() {
        // Network then refuses to bind without addresses naming the relays
        assertEquals(List.of(), NetherNetInterface.advertisedAddresses(BOUND, new SignalingSettings(), false, true));
    }

    @Test
    public void configuredAddressesWin() {
        SignalingSettings settings = new SignalingSettings();
        settings.setAdvertiseAddresses(List.of("203.0.113.9:19165"));
        assertEquals(List.of("203.0.113.9:19165"), NetherNetInterface.advertisedAddresses(BOUND, settings, false, true));
    }
}
