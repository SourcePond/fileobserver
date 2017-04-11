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

import java.nio.file.Path;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;

/**
 *
 */
@FunctionalInterface
public interface PathElement {

    boolean matches(Path pName);

    static PathElement any() {
        return p -> true;
    }

    static PathElement startsWith(final String pPrefix) {
        requireNonNull(pPrefix, "Prefix is null");
        return p -> p.toString().startsWith(pPrefix);
    }

    static PathElement endsWith(final String pPostfix) {
        requireNonNull(pPostfix, "Postfix is null");
        return p -> p.toString().endsWith(pPostfix);
    }

    static PathElement eq(final String pName) {
        return p -> p.toString().equals(pName);
    }

    static PathElement regex(final String pRegex) {
        final Pattern pattern = compile(requireNonNull(pRegex, "Regex is null"));
        return p -> pattern.matcher(p.toString()).matches();
    }
}
