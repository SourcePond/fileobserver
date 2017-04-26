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

import ch.sourcepond.io.fileobserver.api.PathMatcherBuilder;
import ch.sourcepond.io.fileobserver.api.SimpleDispatchRestriction;

import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
class DefaultPathMatcherBuilder implements PathMatcherBuilder {
    private final List<PathMatcher> matchers = new ArrayList<>();
    private final CompoundPathMatcherFactory factory;
    private final DefaultDispatchRestriction restriction;
    private final FileSystem fs;

    DefaultPathMatcherBuilder(final CompoundPathMatcherFactory pFactory,
                                     final DefaultDispatchRestriction pRestriction,
                                     final FileSystem pFs) {
        factory = pFactory;
        restriction = pRestriction;
        fs = pFs;
    }

    @Override
    public PathMatcherBuilder and(final String pSyntaxAndPattern) {
        matchers.add(fs.getPathMatcher(pSyntaxAndPattern));
        return this;
    }

    @Override
    public PathMatcherBuilder and(final PathMatcher pMatcher) {
        matchers.add(pMatcher);
        return this;
    }

    @Override
    public SimpleDispatchRestriction thenAccept() {
        restriction.addMatchers(factory.createMatcher(matchers));
        return restriction;
    }
}
