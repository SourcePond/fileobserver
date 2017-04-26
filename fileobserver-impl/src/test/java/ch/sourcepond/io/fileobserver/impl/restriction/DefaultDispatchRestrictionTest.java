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
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import com.google.common.jimfs.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static com.google.common.jimfs.Jimfs.newFileSystem;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public abstract class DefaultDispatchRestrictionTest extends CopyResourcesTest {
    private static final Object ANY_ACCEPTED_DIRECTORY_KEY = new Object();
    private static final Object ANY_IGNORED_DIRECTORY_KEY = new Object();
    protected DispatchKey testfile_1111_txt_key = mock(DispatchKey.class);
    protected DispatchKey testfile_111_txt_key = mock(DispatchKey.class);
    protected DispatchKey testfile_121_txt_key = mock(DispatchKey.class);
    protected DispatchKey testfile_11_xml_key = mock(DispatchKey.class);
    protected DispatchKey testfile_2111_txt_key = mock(DispatchKey.class);
    protected DispatchKey testfile_211_txt_key = mock(DispatchKey.class);
    protected DispatchKey testfile_221_txt_key = mock(DispatchKey.class);
    protected DispatchKey testfile_21_xml_key = mock(DispatchKey.class);
    protected DispatchKey testfile_txt_key = mock(DispatchKey.class);
    private final DefaultDispatchRestriction restriction = new DefaultDispatchRestrictionFactory().createRestriction(root_dir_path.getFileSystem());

    @Override
    protected Path createRootPath() {
        return newFileSystem(configuration()).getPath(randomUUID().toString());
    }

    protected abstract Configuration configuration();

    private void setupKey(final DispatchKey pKey, final Path pPath, final Object pDirectoryKey) {
        when(pKey.getDirectoryKey()).thenReturn(pDirectoryKey);
        when(pKey.getRelativePath()).thenReturn(pPath);
    }

    @Before
    public void setup() {
        setupKey(testfile_1111_txt_key, root_dir_path.relativize(testfile_1111_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_111_txt_key, root_dir_path.relativize(testfile_111_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_121_txt_key, root_dir_path.relativize(testfile_121_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_11_xml_key, root_dir_path.relativize(testfile_11_xml_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_2111_txt_key, root_dir_path.relativize(testfile_2111_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_211_txt_key, root_dir_path.relativize(testfile_211_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_221_txt_key, root_dir_path.relativize(testfile_221_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_21_xml_key, root_dir_path.relativize(testfile_21_xml_path), ANY_ACCEPTED_DIRECTORY_KEY);
        setupKey(testfile_txt_key, root_dir_path.relativize(testfile_txt_path), ANY_ACCEPTED_DIRECTORY_KEY);
    }

    @Test
    public void verifyDefaultConstructor() {
        // Should not cause an exception
        new DefaultDispatchRestriction(root_dir_path.getFileSystem());
    }

    private void verifyMatches() {
        assertTrue(restriction.isAccepted(testfile_11_xml_key));
        assertFalse(restriction.isAccepted(testfile_1111_txt_key));
        assertFalse(restriction.isAccepted(testfile_111_txt_key));
        assertFalse(restriction.isAccepted(testfile_121_txt_key));
        assertFalse(restriction.isAccepted(testfile_2111_txt_key));
        assertFalse(restriction.isAccepted(testfile_211_txt_key));
        assertFalse(restriction.isAccepted(testfile_221_txt_key));
        assertTrue(restriction.isAccepted(testfile_21_xml_key));
        assertFalse(restriction.isAccepted(testfile_txt_key));
    }

    @Test
    public void add() {
        assertSame(restriction, restriction.whenPathMatches("glob:subdir_1/*.xml").thenAccept());
        assertSame(restriction, restriction.whenPathMatches("glob:subdir_2/*.xml").thenAccept());
        assertSame(restriction, restriction.acceptAll());
        verifyMatches();
    }

    @Test
    public void addCustomPattern() {
        final PathMatcher customMatcher = mock(PathMatcher.class);
        when(customMatcher.matches(root_dir_path.relativize(testfile_11_xml_path))).thenReturn(true);
        when(customMatcher.matches(root_dir_path.relativize(testfile_21_xml_path))).thenReturn(true);
        assertSame(restriction, restriction.acceptAll());
        assertSame(restriction, restriction.whenPathMatches(customMatcher).thenAccept());
        verifyMatches();
    }

    @Test
    public void directoryKeyNotAccepted() {
        assertSame(restriction, restriction.whenPathMatches("glob:**/*.*").thenAccept());
        when(testfile_11_xml_key.getDirectoryKey()).thenReturn(ANY_IGNORED_DIRECTORY_KEY);
        assertFalse(restriction.isAccepted(testfile_11_xml_key));
    }

    @Test
    public void allDirectoryKeysAccepted() {
        assertSame(restriction, restriction.whenPathMatches("glob:**/*.*").thenAccept());
        when(testfile_11_xml_key.getDirectoryKey()).thenReturn(new Object());
        when(testfile_21_xml_key.getDirectoryKey()).thenReturn(new Object());
        assertSame(restriction, restriction.acceptAll());
        assertTrue(restriction.isAccepted(testfile_11_xml_key));
        assertTrue(restriction.isAccepted(testfile_21_xml_key));
    }

    @Test
    public void acceptSpecifiedDirectoryKeyOnly() {
        assertSame(restriction, restriction.whenPathMatches("glob:**/*.*").thenAccept());
        when(testfile_11_xml_key.getDirectoryKey()).thenReturn(ANY_ACCEPTED_DIRECTORY_KEY);
        when(testfile_21_xml_key.getDirectoryKey()).thenReturn(ANY_IGNORED_DIRECTORY_KEY);
        assertSame(restriction, restriction.accept(ANY_ACCEPTED_DIRECTORY_KEY));
        assertTrue(restriction.isAccepted(testfile_11_xml_key));
        assertFalse(restriction.isAccepted(testfile_21_xml_key));
    }
}
