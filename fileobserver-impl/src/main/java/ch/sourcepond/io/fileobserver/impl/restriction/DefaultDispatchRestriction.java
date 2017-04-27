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
import ch.sourcepond.io.fileobserver.api.DispatchRestriction;
import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Objects.requireNonNull;

/**
 *
 */
public class DefaultDispatchRestriction implements DispatchRestriction {
    private static final Object ACCEPT_ALL = new Object();
    private final Set<Object> acceptedDirectoryKeys = new CopyOnWriteArraySet<>();
    private final List<PathMatcher> matchers = new CopyOnWriteArrayList<>();
    private final FileSystem fs;

    // Constructor for activator
    DefaultDispatchRestriction(final FileSystem pFs) {
        fs = pFs;
    }

    private void validateInitialState() {
        if (!acceptedDirectoryKeys.isEmpty()) {
            throw new IllegalStateException("Either accept or acceptAll has already been called!");
        }
    }

    @Override
    public SimpleDispatchRestriction accept(final Object... pDirectoryKeys) {
        if (requireNonNull(pDirectoryKeys, "Keys are null!").length == 0) {
            throw new IllegalArgumentException("Keys are empty!");
        }
        validateInitialState();
        for (final Object directoryKey : pDirectoryKeys) {
            acceptedDirectoryKeys.add(requireNonNull(directoryKey, "Directory-key is null"));
        }
        return this;
    }

    @Override
    public SimpleDispatchRestriction acceptAll() {
        validateInitialState();
        accept(ACCEPT_ALL);
        return this;
    }

    @Override
    public PathMatcher addPathMatcher(final String pSyntaxAndPattern) {
        return addPathMatcher(fs.getPathMatcher(pSyntaxAndPattern));
    }

    @Override
    public PathMatcher addPathMatcher(final PathMatcher pCustomMatcher) {
        matchers.add(pCustomMatcher);
        return pCustomMatcher;
    }

    public boolean isAccepted(final DispatchKey pDispatchKey) {
        final Object directoryKey = pDispatchKey.getDirectoryKey();
        final Path relativePath = pDispatchKey.getRelativePath();
        final List<PathMatcher> m = matchers;
        final int size = m.size();

        boolean accept = acceptedDirectoryKeys.contains(ACCEPT_ALL) ||
                acceptedDirectoryKeys.contains(directoryKey);

        if (accept) {
            // To avoid creating to many objects we use a traditional
            // for-loop here
            for (int i = 0; i < size; i++) {
                accept = m.get(i).matches(relativePath);
                if (accept) {
                    break;
                }
            }
        }
        return accept;
    }
}
