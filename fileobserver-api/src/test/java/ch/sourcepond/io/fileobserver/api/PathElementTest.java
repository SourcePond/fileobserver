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

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 *
 */
public class PathElementTest {
    private final Path path = mock(Path.class, withSettings().name("somefile.txt"));

    @Test
    public void any() {
        assertTrue(PathElement.any().matches(path));
    }

    @Test
    public void startsWith() {
        assertTrue(PathElement.startsWith("somefile").matches(path));
        assertFalse(PathElement.startsWith("notmatching").matches(path));
    }

    @Test
    public void endsWith() {
        assertTrue(PathElement.endsWith("txt").matches(path));
        assertFalse(PathElement.endsWith("xml").matches(path));
    }

    @Test
    public void eq() {
        assertTrue(PathElement.eq("somefile.txt").matches(path));
        assertFalse(PathElement.eq("somefile.xml").matches(path));
    }

    @Test
    public void regex() {
        assertTrue(PathElement.regex(".*").matches(path));
        assertFalse(PathElement.regex("other.*").matches(path));
    }
}
