/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver.impl.fs;

import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 */
final class PendingEvent implements Delayed {
    private final Instant creationTime = now();
    private final Instant threshold;
    private final Path path;

    PendingEvent(final Path pPath, final long pTimeoutInMilliseconds) {
        path = pPath;
        threshold = creationTime.plusMillis(pTimeoutInMilliseconds);
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return unit.convert(threshold.minusMillis(now().toEpochMilli()).toEpochMilli(), MILLISECONDS);
    }

    @Override
    public int compareTo(final Delayed o) {
        return threshold.compareTo(((PendingEvent)o).threshold);
    }

    Path getPath() {
        return path;
    }
}
