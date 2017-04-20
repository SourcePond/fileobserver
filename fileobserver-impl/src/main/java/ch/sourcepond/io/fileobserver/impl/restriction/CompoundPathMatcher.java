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

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;

/**
 *
 */
class CompoundPathMatcher implements PathMatcher {
    private final List<PathMatcher> matchers;

    CompoundPathMatcher(final List<PathMatcher> pMatchers) {
        matchers = pMatchers;
    }

    @Override
    public boolean matches(final Path path) {
        final List<PathMatcher> m = matchers;
        final int size = m.size();

        // To avoid creating to many objects we use a traditional
        // for-loop here
        for (int i = 0 ; i < size ; i++) {
            if (!m.get(i).matches(path)) {
                return false;
            }
        }
        return true;
    }
}
