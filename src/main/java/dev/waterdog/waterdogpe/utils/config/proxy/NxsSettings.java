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
import net.cubespace.Yamler.Config.Comment;
import net.cubespace.Yamler.Config.Comments;
import net.cubespace.Yamler.Config.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The NXS signalling provider, which finds players for this proxy rather than waiting for them to
 * arrive at an endpoint of its own.
 */
@Getter
public class NxsSettings extends SettingsSection {

    @Path("token")
    @Comment("Bearer token, or file:/path/to/token. Empty registers anonymously")
    private String token = "";

    @Path("endpoint")
    @Comment("Provider origin used for discovery and registration")
    private String endpoint = "https://agent.warden.cloud";

    @Path("advertise_addresses")
    @Comments({
            "Reachable UDP endpoints, as 198.51.100.1:19133 or [2001:db8::1]:19133.",
            "Empty derives them from the addresses this host holds, which is wrong behind a NAT or",
            "a forwarder; name the endpoint players actually reach in that case.",
            "Forwarding is not configured here, only described."
    })
    private List<String> advertiseAddresses = new ArrayList<>();

    @Path("data")
    @Comments({
            "Instance metadata. The region and pool keys place this instance,",
            "anything else is a registration tag."
    })
    private Map<String, String> data = new HashMap<>();
}
