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
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

class FileSystemEvent implements Delayed {
    private final Instant creationTime;
    private final Instant dueTime;
    private final WatchEvent.Kind<?> kind;
    private final Path path;

    FileSystemEvent(final Instant pCreationTime,
                    final long pTimeoutInMillis,
                    final WatchEvent.Kind<?> pKind,
                    final Path pPath) {
        creationTime = pCreationTime;
        dueTime = pCreationTime.plusMillis(pTimeoutInMillis);
        kind = pKind;
        path = pPath;
    }

    public WatchEvent.Kind<?> getKind() {
        return kind;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        return unit.convert(now().minusMillis(dueTime.toEpochMilli()).toEpochMilli(), MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return dueTime.compareTo(((FileSystemEvent) o).dueTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileSystemEvent that = (FileSystemEvent) o;
        return Objects.equals(kind, that.kind) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, path);
    }
}
