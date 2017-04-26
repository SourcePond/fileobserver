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
package ch.sourcepond.io.fileobserver.impl.restriction;

import org.junit.Test;

import java.nio.file.FileSystem;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class DefaultDispatchRestrictionAcceptTest {
    private final FileSystem fs = mock(FileSystem.class);
    private final DefaultDispatchRestriction restriction = new DefaultDispatchRestriction(fs);

    @Test(expected = NullPointerException.class)
    public void verifyNullKeysNotAllowed() {
        restriction.accept(null);
    }

    @Test(expected = NullPointerException.class)
    public void verifyNullKeyElementNotAllowed() {
        restriction.accept(new Object[] { null });
    }

    @Test(expected = IllegalArgumentException.class)
    public void verifyEmptyKeysNotAllowed() {
        restriction.accept();
    }

    @Test
    public void verifyExceptionWhenAcceptAlreadyCalled() {
        restriction.accept("any");
        try {
            restriction.accept("any");
            fail("Exception expected");
        } catch (final IllegalStateException expected) {
            // expected
        }
        try {
            restriction.acceptAll();
            fail("Exception expected");
        } catch (final IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void verifyExceptionWhenAcceptAllAlreadyCalled() {
        restriction.acceptAll();
        try {
            restriction.accept("any");
            fail("Exception expected");
        } catch (final IllegalStateException expected) {
            // expected
        }
        try {
            restriction.acceptAll();
            fail("Exception expected");
        } catch (final IllegalStateException expected) {
            // expected
        }
    }
}
