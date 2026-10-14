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

package dev.waterdog.waterdogpe.utils.config.proxy;

import lombok.Getter;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;

import java.util.HashMap;
import java.util.Map;

/**
 * NXS options most proxies leave unchanged. Named like Geyser's advanced NXS options.
 */
@Getter
public class NxsAdvancedSettings extends SettingsSection {

    @Path("assisted_joins")
    @Comments({
            "Lets the signaling service help players connect when they cannot reach this proxy directly,",
            "for example behind a NAT without port forwarding"
    })
    private boolean assistedJoins = false;

    @Path("connection_checks")
    @Comments({
            "Lets the signaling service check from several regions that players can reach this proxy,",
            "and stop offering addresses that fail"
    })
    private boolean connectionChecks = true;

    @Path("stun_discovery")
    @Comments({
            "Finds this proxy's public address through the signaling service's STUN servers when it has",
            "none of its own. Not used when advertise_addresses is set"
    })
    private boolean stunDiscovery = true;

    @Path("location")
    @Comments({
            "Where this proxy is: country (a two-letter code), city, latitude and longitude. Leave empty",
            "to let the signaling service estimate it, which it may also let its operator override"
    })
    private Map<String, String> location = new HashMap<>();

    @Path("deregister_on_shutdown")
    @Comments({
            "Removes this proxy from the signaling service on shutdown. Enable this when the data folder",
            "does not survive a restart, such as in a container without a volume, or each restart adds a new proxy"
    })
    private boolean deregisterOnShutdown = false;
}
