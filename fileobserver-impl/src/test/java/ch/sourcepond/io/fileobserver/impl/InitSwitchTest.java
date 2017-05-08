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
package ch.sourcepond.io.fileobserver.impl;

import org.junit.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 *
 */
public class InitSwitchTest {
    private final Consumer<Runnable> consumer = r -> r.run();
    private final InitSwitch<Runnable> initSwitch = new InitSwitch<>(consumer);
    private final Runnable target = mock(Runnable.class);

    @Test
    public void addNotInitializedYet() {
        initSwitch.add(target);
        verifyZeroInteractions(target);
    }

    @Test
    public void addAfterInitialization() {
        initSwitch.init();
        initSwitch.add(target);
        verify(target).run();
    }

    @Test
    public void addAndInitialized() {
        initSwitch.add(target);
        initSwitch.init();
        verify(target).run();
    }

    @Test(expected = IllegalStateException.class)
    public void exceptionWhenInitMoreThanOnce() {
        initSwitch.init();

        // This should cause an exception
        initSwitch.init();
    }
}