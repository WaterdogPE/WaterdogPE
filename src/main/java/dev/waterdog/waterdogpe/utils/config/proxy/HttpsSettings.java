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

/**
 * TLS for the signaling endpoint, which serves HTTPS on the same port as HTTP.
 */
@Getter
public class HttpsSettings extends SettingsSection {

    @Path("certificate")
    @Comments({
            "Either a PEM chain, with its key in private_key, or a PKCS12 file, leaving that empty.",
            "Clients that reach signaling over TLS never see the first use trust prompt.",
            "Empty serves plaintext only."
    })
    private String certificate = "";

    @Path("private_key")
    @Comment("PEM private key for the certificate. Leave empty when that is a PKCS12 file")
    private String privateKey = "";

    @Path("password")
    @Comment("Password for the PKCS12, or for the PEM key if it is encrypted")
    private String password = "";

    /**
     * Whether a certificate is configured at all.
     */
    public boolean enabled() {
        return this.certificate != null && !this.certificate.isBlank();
    }
}
