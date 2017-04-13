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
package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DefaultDeliveryRestrictionTest extends CopyResourcesTest {
    private static final Object ANY_ACCEPTED_DIRECTORY_KEY = new Object();
    private static final Object ANY_IGNORED_DIRECTORY_KEY = new Object();
    protected FileKey testfile_1111_txt_key = mock(FileKey.class);
    protected FileKey testfile_111_txt_key = mock(FileKey.class);
    protected FileKey testfile_121_txt_key = mock(FileKey.class);
    protected FileKey testfile_11_xml_key = mock(FileKey.class);
    protected FileKey testfile_2111_txt_key = mock(FileKey.class);
    protected FileKey testfile_211_txt_key = mock(FileKey.class);
    protected FileKey testfile_221_txt_key = mock(FileKey.class);
    protected FileKey testfile_21_xml_key = mock(FileKey.class);
    protected FileKey testfile_txt_key = mock(FileKey.class);
    private final DefaultDeliveryRestriction restriction = new DefaultDeliveryRestriction();

    private void setupKey(final FileKey pKey, final Path pPath, final Object pDirectoryKey) {
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

    private void verifySubPathMatches() {
        assertFalse(restriction.isAccepted(testfile_11_xml_key));
        assertTrue(restriction.isAccepted(testfile_1111_txt_key));
        assertFalse(restriction.isAccepted(testfile_111_txt_key));
        assertFalse(restriction.isAccepted(testfile_121_txt_key));
        assertTrue(restriction.isAccepted(testfile_2111_txt_key));
        assertFalse(restriction.isAccepted(testfile_211_txt_key));
        assertFalse(restriction.isAccepted(testfile_221_txt_key));
        assertFalse(restriction.isAccepted(testfile_21_xml_key));
        assertFalse(restriction.isAccepted(testfile_txt_key));
    }

    @Test
    public void add() {
        restriction.add("glob", "subdir_1", "*.xml");
        restriction.add("glob", "subdir_2", "*.xml");
        verifyMatches();
    }

    @Test
    public void addGlob() {
        restriction.addGlob("subdir_1", "*.xml");
        restriction.addGlob("subdir_2", "*.xml");
        verifyMatches();
    }

    @Test
    public void addRegex() {
        restriction.addRegex("subdir_1", ".*\\.xml");
        restriction.addRegex("subdir_2", ".*\\.xml");
        verifyMatches();
    }

    @Test
    public void addMatchSubPath() {
        restriction.add(1, 4,"glob", "**", "*.txt");
        verifySubPathMatches();
    }

    @Test
    public void addGlobMatchSubPath() {
        restriction.addGlob(1, 4, "**", "*.txt");
        restriction.addGlob(1, 4, "**", "*.txt");
        verifySubPathMatches();
    }

    @Test
    public void addRegexMatchSubPath() {
        restriction.addRegex(1, 4, ".*\\.txt");
        restriction.addRegex(1, 4, ".*\\.txt");
        verifySubPathMatches();
    }
}
