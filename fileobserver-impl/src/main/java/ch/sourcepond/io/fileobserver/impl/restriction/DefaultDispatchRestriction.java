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

import ch.sourcepond.io.fileobserver.api.DispatchRestriction;
import ch.sourcepond.io.fileobserver.api.FileKey;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 *
 */
public class DefaultDispatchRestriction implements DispatchRestriction {

    private static class IndexHolder {
        protected final int startIndexInclusive;
        protected final int endIndexExclusive;

        private IndexHolder(final int pStartIndexInclusive,
                            final int pEndIndexExclusive) {
            startIndexInclusive = pStartIndexInclusive;
            endIndexExclusive = pEndIndexExclusive;
        }
    }

    private static class PatternTemplate extends IndexHolder {
        private final String syntax;
        private final String[] patternTokens;

        private PatternTemplate(final int pStartIndexInclusive,
                                final int pEndIndexExclusive,
                                final String pSyntax,
                                final String[] pPatternTokens) {
            super(pStartIndexInclusive, pEndIndexExclusive);
            syntax = pSyntax;
            patternTokens = pPatternTokens;
        }
    }

    private static class PathMatcherHolder extends IndexHolder {
        private final PathMatcher matcher;

        private PathMatcherHolder(final int pStartIndexInclusive,
                                  final int pEndIndexExclusive,
                                  final PathMatcher pMatcher) {
            super(pStartIndexInclusive, pEndIndexExclusive);
            matcher = pMatcher;
        }
    }

    private static final Object ACCEPT_ALL = new Object();
    private static final int START_INDEX = 0;
    private static final String GLOB = "glob";
    private static final String REGEX = "regex";
    private final Set<Object> acceptedDirectoryKeys = newKeySet();
    private final Collection<PatternTemplate> patternTemplates = new CopyOnWriteArrayList<>();
    private final ConcurrentMap<FileSystem, Collection<PathMatcherHolder>> matchers = new ConcurrentHashMap<>();

    DefaultDispatchRestriction() {}

    @Override
    public DispatchRestriction accept(final Object... pDirectoryKeys) {
        for (final Object directoryKey : pDirectoryKeys) {
            acceptedDirectoryKeys.add(requireNonNull(directoryKey, "Directory-key is null"));
        }
        return this;
    }

    @Override
    public DispatchRestriction acceptAll() {
        accept(ACCEPT_ALL);
        return this;
    }

    @Override
    public DispatchRestriction addGlob(final String... pPatterns) {
        doAdd(START_INDEX, MAX_VALUE, GLOB, pPatterns);
        return this;
    }

    @Override
    public DispatchRestriction addRegex(final String... pPatterns) {
        doAdd(START_INDEX, MAX_VALUE, REGEX, pPatterns);
        return this;
    }

    @Override
    public DispatchRestriction add(final String pSyntax, final String... pPatterns) {
        doAdd(START_INDEX, MAX_VALUE, pSyntax, pPatterns);
        return this;
    }

    private void validateNotNegative(final String pMessageFormat, final int pIndex) {
        if (0 > pIndex) {
            throw new IllegalArgumentException(format(pMessageFormat, pIndex));
        }
    }

    private void validateIndexes(final int pStartIndexInclusive, final int pEndIndexExlusive) {
        validateNotNegative("Start-index %d (inclusive) is negative!", pStartIndexInclusive);
        validateNotNegative("End-index %d (exclusive) is negative!", pStartIndexInclusive);
        if (pStartIndexInclusive == pEndIndexExlusive) {
            throw new IllegalArgumentException(format(
                    "Start-index %d (inclusive) is equal to the end-index %d (exclusive)",
                    pStartIndexInclusive, pEndIndexExlusive));
        }
        if (pStartIndexInclusive > pEndIndexExlusive) {
            throw new IllegalArgumentException(format(
                    "Start-index %d (inclusive) is greater than end-index %d (exclusive)",
                    pStartIndexInclusive, pEndIndexExlusive));
        }
    }

    @Override
    public DispatchRestriction addGlob(final int pStartIndexInclusive, final int pEndIndexExlusive, final String... pPatterns) {
        add(pStartIndexInclusive, pEndIndexExlusive, GLOB, pPatterns);
        return this;
    }

    @Override
    public DispatchRestriction addRegex(final int pStartIndexInclusive, final int pEndIndexExlusive, final String... pPatterns) {
        add(pStartIndexInclusive, pEndIndexExlusive, REGEX, pPatterns);
        return this;
    }

    @Override
    public DispatchRestriction add(final int pStartIndexInclusive, final int pEndIndexExlusive, final String pSyntax, final String... pPatterns) {
        validateIndexes(pStartIndexInclusive, pEndIndexExlusive);
        doAdd(pStartIndexInclusive, pEndIndexExlusive, pSyntax, pPatterns);
        return this;
    }

    private void doAdd(final int pStartIndexInclusive, final int pEndIndexExlusive, final String pSyntax, final String... pPatterns) {
        patternTemplates.add(new PatternTemplate(pStartIndexInclusive, pEndIndexExlusive, pSyntax, pPatterns));
    }

    private PathMatcherHolder createMatcher(final FileSystem pFs, final PatternTemplate pTemplate) {
        final StringBuilder builder = new StringBuilder(pTemplate.syntax).append(':');
        final String[] patternTokens = pTemplate.patternTokens;
        for (int i = 0, end = patternTokens.length - 1; i < patternTokens.length; i++) {
            builder.append(patternTokens[i]);
            if (end > i) {
                builder.append(pFs.getSeparator());
            }
        }
        return new PathMatcherHolder(pTemplate.startIndexInclusive,
                pTemplate.endIndexExclusive,
                pFs.getPathMatcher(builder.toString()));
    }

    private Collection<PathMatcherHolder> createMatchers(final FileSystem pFs) {
        final List<PathMatcherHolder> matcherHolders = new ArrayList<>(patternTemplates.size());
        patternTemplates.forEach(patternTemplate -> matcherHolders.add(createMatcher(pFs, patternTemplate)));
        return matcherHolders;
    }

    private Collection<PathMatcherHolder> getMatcher(final FileSystem pFs) {
        return matchers.computeIfAbsent(pFs, fs -> createMatchers(pFs));
    }

    private boolean match(final FileKey pFileKey, final PathMatcherHolder pHolder) {
        boolean match;
        Path adjusted = pFileKey.getRelativePath();

        if (MAX_VALUE > pHolder.endIndexExclusive) {
            match = adjusted.getNameCount() >= pHolder.endIndexExclusive;
            if (match) {
                adjusted = adjusted.subpath(pHolder.startIndexInclusive, pHolder.endIndexExclusive);
                match = pHolder.matcher.matches(adjusted);
            }
        } else {
            match = pHolder.matcher.matches(adjusted);
        }

        return match;
    }

    private boolean matches(final FileKey pFileKey) {
        final Collection<PathMatcherHolder> matcherHolders = getMatcher(pFileKey.getRelativePath().getFileSystem());
        boolean matches = matcherHolders.isEmpty();
        if (!matches) {
            for (final PathMatcherHolder holder : matcherHolders) {
                matches = match(pFileKey, holder);
                if (matches) {
                    break;
                }
            }
        }
        return matches;
    }

    void removeFileSystem(final FileSystem pFs) {
        matchers.remove(pFs);
    }

    public boolean isAccepted(final FileKey pFileKey) {
        final Object directoryKey = pFileKey.getDirectoryKey();
        boolean accept = acceptedDirectoryKeys.contains(ACCEPT_ALL) ||
                acceptedDirectoryKeys.contains(directoryKey);

        if (accept) {
            accept = matches(pFileKey);
        }

        return accept;
    }
}
