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

package dev.waterdog.waterdogpe.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this Annotation to when event is excepted to support complete future.
 * Completable futures are used when event should be completed later than its handlers.
 * When calling event with this Annotation, CompletableFuture<Event> will be returned and will be completed once
 * all assigned completable futures are completed.
 * Therefore, eventFuture.whenComplete(futureEvent, error) should be used processing completed event.
 * It is NOT recommended using this Annotation if event is called really often or is excepted to return result immediately.
 * Events with @AsyncEvent Annotation implements this behaviour by default.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CompletableEvent {
}
