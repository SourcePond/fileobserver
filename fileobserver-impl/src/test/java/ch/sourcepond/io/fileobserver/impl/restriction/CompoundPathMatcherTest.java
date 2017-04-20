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

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class CompoundPathMatcherTest {
    private final PathMatcher first = mock(PathMatcher.class);
    private final PathMatcher second = mock(PathMatcher.class);
    private final Path path = mock(Path.class);
    private final CompoundPathMatcher matcher = new CompoundPathMatcher(asList(first, second));

    @Before
    public void setup() {
        when(first.matches(path)).thenReturn(true);
        when(second.matches(path)).thenReturn(true);
    }

    @Test
    public void verifyBothMatches() {
        assertTrue(matcher.matches(path));
        verify(first).matches(path);
        verify(second).matches(path);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void verifySecondDoesNotMatch() {
        when(second.matches(path)).thenReturn(false);
        assertFalse(matcher.matches(path));
        verify(first).matches(path);
        verify(second).matches(path);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    public void verifyFirstDoesNotMatch() {
        when(first.matches(path)).thenReturn(false);
        assertFalse(matcher.matches(path));
        verify(first).matches(path);
        verify(second, never()).matches(path);
        verifyNoMoreInteractions(first, second);
    }
}
