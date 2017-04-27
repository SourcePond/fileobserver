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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultDispatchRestrictionAcceptTest {
    private static final String ANY_PATTERN = "anyPattern";
    private static final String ACCEPTED_KEY = "accepted";
    private static final String IGNORED_KEY = "ignored";
    private final FileSystem fs = mock(FileSystem.class);
    private final Path path = mock(Path.class);
    private final PathMatcher matcher = mock(PathMatcher.class);
    private final DispatchKey key = mock(DispatchKey.class);
    private final DefaultDispatchRestriction restriction = new DefaultDispatchRestrictionFactory().createRestriction(fs);

    @Before
    public void setup() {
        when(key.getRelativePath()).thenReturn(path);
        when(fs.getPathMatcher(ANY_PATTERN)).thenReturn(matcher);
    }

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

    @Test
    public void directoryKeyIsIgnored() {
        when(key.getDirectoryKey()).thenReturn(IGNORED_KEY);
        restriction.accept(ACCEPTED_KEY);
        assertFalse(restriction.isAccepted(key));
    }

    @Test
    public void anyDirectoryKeyIsAccepted() {
        when(key.getDirectoryKey()).thenReturn(new Object());
        restriction.acceptAll();
        assertTrue(restriction.isAccepted(key));
    }

    @Test
    public void isAcceptedWithoutMatchers() {
        when(key.getDirectoryKey()).thenReturn(ACCEPTED_KEY);
        restriction.accept(ACCEPTED_KEY);
        assertTrue(restriction.isAccepted(key));
    }

    @Test
    public void isAcceptedWithMatcher() {
        when(matcher.matches(path)).thenReturn(true);
        when(key.getDirectoryKey()).thenReturn(ACCEPTED_KEY);
        restriction.accept(ACCEPTED_KEY);
        restriction.addPathMatcher(ANY_PATTERN);
        assertTrue(restriction.isAccepted(key));
        verify(matcher).matches(path);
    }

    @Test
    public void isAcceptedWithCustomMatcher() {
        when(matcher.matches(path)).thenReturn(true);
        when(key.getDirectoryKey()).thenReturn(ACCEPTED_KEY);
        restriction.accept(ACCEPTED_KEY);
        restriction.addPathMatcher(matcher);
        assertTrue(restriction.isAccepted(key));
        verify(matcher).matches(path);
    }

    @Test
    public void isAcceptedMatcherDoesNotMatch() {
        when(key.getDirectoryKey()).thenReturn(ACCEPTED_KEY);
        restriction.accept(ACCEPTED_KEY);
        restriction.addPathMatcher(ANY_PATTERN);
        assertFalse(restriction.isAccepted(key));
        verify(matcher).matches(path);
    }
}
