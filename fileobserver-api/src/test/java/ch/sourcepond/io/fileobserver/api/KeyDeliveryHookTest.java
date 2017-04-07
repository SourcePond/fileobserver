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
package ch.sourcepond.io.fileobserver.api;

import org.junit.Test;

import java.nio.file.Path;

import static org.mockito.Mockito.*;

/**
 *
 */
public class KeyDeliveryHookTest {
    private final FileKey key = mock(FileKey.class);
    private final Path file = mock(Path.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);

    @Test
    public void beforeDiscard() {
        doCallRealMethod().when(hook).beforeDiscard(key);
        hook.beforeDiscard(key);
        verify(hook).before(key);
    }

    @Test
    public void beforeModify() {
        doCallRealMethod().when(hook).beforeModify(key, file);
        hook.beforeModify(key, file);
        verify(hook).before(key);
    }

    @Test
    public void afterDiscard() {
        doCallRealMethod().when(hook).afterDiscard(key);
        hook.afterDiscard(key);
        verify(hook).after(key);
    }

    @Test
    public void afterModify() {
        doCallRealMethod().when(hook).afterModify(key, file);
        hook.afterModify(key, file);
        verify(hook).after(key);
    }

    @Test
    public void before() {
        doCallRealMethod().when(hook).before(key);
        verifyNoMoreInteractions(hook);
    }

    @Test
    public void after() {
        doCallRealMethod().when(hook).after(key);
        verifyNoMoreInteractions(hook);
    }
}
