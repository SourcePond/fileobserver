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

import static ch.sourcepond.io.fileobserver.api.PathMatcherBuilder.GLOB;
import static ch.sourcepond.io.fileobserver.api.PathMatcherBuilder.REGEX;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class SimpleDispatchRestrictionTest {
    private final String ANY_PATTERN = "anyPattern";
    private final SimpleDispatchRestriction restriction = mock(SimpleDispatchRestriction.class);

    @Test
    public void whenPathMatchesGlob() {
        doCallRealMethod().when(restriction).whenPathMatchesGlob(ANY_PATTERN);
        restriction.whenPathMatchesGlob(ANY_PATTERN);
        verify(restriction).whenPathMatchesPattern(GLOB, ANY_PATTERN);
    }

    @Test
    public void whenPathMatchesRegex() {
        doCallRealMethod().when(restriction).whenPathMatchesRegex(ANY_PATTERN);
        restriction.whenPathMatchesRegex(ANY_PATTERN);
        verify(restriction).whenPathMatchesPattern(REGEX, ANY_PATTERN);
    }
}
