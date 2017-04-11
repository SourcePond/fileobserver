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

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class WatchServiceWrapperTest {
    private final Path directory = mock(Path.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final WatchService watchService = mock(WatchService.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private WatchServiceWrapper wrapper;

    @Before
    public void setup() throws IOException {
        when(fs.newWatchService()).thenReturn(watchService);
        wrapper = new WatchServiceWrapper(fs);
    }

    @Test
    public void close() throws IOException {
        wrapper.close();
        verify(watchService).close();
    }

    @Test
    public void closeIoExceptionOccurred() throws IOException {
        doThrow(IOException.class).when(watchService).close();
        // This should not cause an exception
        wrapper.close();
    }

    @Test
    public void take() throws InterruptedException {
        wrapper.take();
        verify(watchService).take();
    }

    @Test
    public void register() throws IOException {
        when(directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(watchKey);
        assertSame(watchKey, wrapper.register(directory));
    }

    @Test
    public void registrationFaileBecauseWatchServiceIsClosed() throws IOException {
        final ClosedWatchServiceException expected = new ClosedWatchServiceException();
        doThrow(expected).when(directory).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        try {
            wrapper.register(directory);
            fail("Exception expected here");
        } catch (final IOException e) {
            assertSame(expected, e.getCause());
        }
    }
}
