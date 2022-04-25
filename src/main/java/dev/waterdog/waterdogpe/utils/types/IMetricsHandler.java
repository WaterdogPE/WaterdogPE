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

/**
 * This interface can be used to record and display WaterdogPE-Internal metrics.
 */
public interface IMetricsHandler {

    /**
     * Called once for every batch handled by the ProxyBatchBridge which has been rewritten
     */
    public void changedBatch();

    /**
     * Called once for every batch handled by the ProxyBatchBridge which has not been rewritten
     */
    public void unchangedBatch();

    /**
     * Triggered when the packet queue of the TransferBatchBridge becomes too large and the player gets disconnected
     */
    public void packetQueueTooLarge();
}
