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

import java.util.List;

/**
 * SDP text handling for HTTP signaling.
 */
public final class SdpUtil {

    private static final String IDENTITY = "a=identity:";

    private SdpUtil() {
    }

    /**
     * Inserts an identity assertion as a session level attribute, ahead of the first media line
     * where both the guide and real clients put it.
     */
    public static String withIdentity(String sdp, String value) {
        String eol = eol(sdp);
        StringBuilder out = new StringBuilder(sdp.length() + value.length() + 16);
        boolean inserted = false;
        for (String line : lines(sdp)) {
            if (line.isEmpty()) {
                continue;
            }
            if (!inserted && line.startsWith("m=")) {
                out.append(IDENTITY).append(value).append(eol);
                inserted = true;
            }
            out.append(line).append(eol);
        }
        return out.toString();
    }

    /**
     * Folds trickled candidates into the answer. HTTP signaling is a single round trip, so every
     * candidate has to travel in the answer body. Fingerprint lines are untouched, which is why
     * this does not invalidate the identity assertion.
     */
    public static String withCandidates(String answer, List<String> candidates) {
        String eol = eol(answer);
        StringBuilder out = new StringBuilder(answer.length() + candidates.size() * 96);
        for (String line : lines(answer)) {
            if (!line.isEmpty()) {
                out.append(line).append(eol);
            }
        }
        for (String candidate : candidates) {
            out.append("a=").append(candidate.trim()).append(eol);
        }
        return out.append("a=end-of-candidates").append(eol).toString();
    }

    private static String[] lines(String sdp) {
        return sdp.split("\r\n|\n", -1);
    }

    private static String eol(String sdp) {
        return sdp.contains("\r\n") ? "\r\n" : "\n";
    }
}
