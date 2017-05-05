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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class PendingEventRegistryTest {
    private final Path path = mock(Path.class);
    private final PendingEventRegistry registry = new PendingEventRegistry();

    @Before
    public void setup() {
        registry.setTimoutInMilliseconds(1000L);
        registry.start();
    }

    @After
    public void tearDown() {
        registry.stop();
    }

    @Test
    public void verifyRegister() throws Exception {
        registry.registerCreateEvent(path);
        assertFalse(registry.isModificationAllowed(path));
        sleep(1500);
        assertTrue(registry.isModificationAllowed(path));
    }

    @Test(expected = IllegalStateException.class)
    public void registerNotAllowedWhenThreadNotRunning() throws Exception {
        registry.stop();
        sleep(100);
        registry.registerCreateEvent(null);
    }
}
