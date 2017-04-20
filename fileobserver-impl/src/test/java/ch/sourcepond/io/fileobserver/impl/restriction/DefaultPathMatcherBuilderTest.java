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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DefaultPathMatcherBuilderTest {
    private final String ANY_SYNTAX = "anySyntax";
    private final String ANY_PATTERN = "anyPattern";
    private final DefaultDispatchRestriction restriction = mock(DefaultDispatchRestriction.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final PathMatcher matcher = mock(PathMatcher.class);
    private final PathMatcher customMatcher = mock(PathMatcher.class);
    private final Path path = mock(Path.class);
    private final CompoundPathMatcherFactory factory = mock(CompoundPathMatcherFactory.class);
    private final CompoundPathMatcher compoundMatcher = mock(CompoundPathMatcher.class);
    private final DefaultPathMatcherBuilder builder = new DefaultPathMatcherBuilder(factory, restriction, fs);

    @Before
    public void setup() {
        when(fs.getPathMatcher("anySyntax:anyPattern")).thenReturn(matcher);
        when(matcher.matches(path)).thenReturn(true);
    }

    private void verifyMatcher(final PathMatcher pMatcher) {
        when(factory.createMatcher(argThat(l -> l.size() == 1 && l.contains(pMatcher)))).thenReturn(compoundMatcher);
        builder.thenAccept();
        verify(restriction).addMatchers(compoundMatcher);
    }

    @Test
    public void andPattern() {
        assertSame(builder, builder.andPattern(ANY_SYNTAX, ANY_PATTERN));
        verifyMatcher(matcher);
    }

    @Test
    public void andWith() {
        assertSame(builder, builder.andWith(customMatcher));
        verifyMatcher(customMatcher);
    }
}
