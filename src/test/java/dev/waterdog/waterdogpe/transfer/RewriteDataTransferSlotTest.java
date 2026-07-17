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

package dev.waterdog.waterdogpe.transfer;

import dev.waterdog.waterdogpe.network.connection.client.ClientConnection;
import dev.waterdog.waterdogpe.network.protocol.handler.TransferCallback;
import dev.waterdog.waterdogpe.network.protocol.rewrite.types.RewriteData;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RewriteDataTransferSlotTest {

    private TransferTestHarness harness;

    @BeforeEach
    void setUp() {
        this.harness = new TransferTestHarness();
    }

    @AfterEach
    void tearDown() {
        this.harness.close();
    }

    private TransferCallback newCallback() {
        ServerInfo serverInfo = this.harness.newServer("target");
        ClientConnection connection = this.harness.newDownstream(serverInfo);
        return new TransferCallback(this.harness.player, connection, this.harness.newServer("source"), 0);
    }

    @Test
    void claimsEmptySlot() {
        RewriteData rewriteData = new RewriteData();
        TransferCallback callback = newCallback();
        assertTrue(rewriteData.trySetTransferCallback(callback));
        assertSame(callback, rewriteData.getTransferCallback());
    }

    @Test
    void rejectsClaimWhileTransferActive() {
        RewriteData rewriteData = new RewriteData();
        TransferCallback active = newCallback();
        assertTrue(rewriteData.trySetTransferCallback(active));

        TransferCallback loser = newCallback();
        assertFalse(rewriteData.trySetTransferCallback(loser));
        assertSame(active, rewriteData.getTransferCallback());
    }

    @Test
    void allowsClaimOverResetCallback() {
        RewriteData rewriteData = new RewriteData();
        TransferCallback finished = newCallback();
        assertTrue(rewriteData.trySetTransferCallback(finished));
        TransferTestHarness.setField(finished, "transferPhase", TransferCallback.TransferPhase.RESET);

        TransferCallback next = newCallback();
        assertTrue(rewriteData.trySetTransferCallback(next));
        assertSame(next, rewriteData.getTransferCallback());
    }

    @Test
    void clearOnlyReleasesOwner() {
        RewriteData rewriteData = new RewriteData();
        TransferCallback owner = newCallback();
        assertTrue(rewriteData.trySetTransferCallback(owner));

        rewriteData.clearTransferCallback(newCallback());
        assertSame(owner, rewriteData.getTransferCallback());

        rewriteData.clearTransferCallback(owner);
        assertNull(rewriteData.getTransferCallback());
    }

    @Test
    void concurrentClaimsHaveSingleWinner() throws Exception {
        RewriteData rewriteData = new RewriteData();
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger winners = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            TransferCallback callback = newCallback();
            new Thread(() -> {
                try {
                    barrier.await();
                    if (rewriteData.trySetTransferCallback(callback)) {
                        winners.incrementAndGet();
                    }
                } catch (Exception ignored) {
                    // fail through the winner count
                } finally {
                    done.countDown();
                }
            }).start();
        }

        done.await();
        assertEquals(1, winners.get());
    }
}