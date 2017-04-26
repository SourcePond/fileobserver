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
import java.nio.file.PathMatcher;

/**
 * <p>Object to build and add a compound {@link PathMatcher} to the associated restriction. An instance
 * of this interface can be retrieved through any {@code whenPathMatches*} method on
 * {@link SimpleDispatchRestriction}.</p>
 * <p>{@link PathMatcher#matches(Path)} of the matcher being constructed will only return {@code true}, if and only if
 * all chained matchers return {@code true}. {@link #thenAccept()} builds the final compound-matcher.
 * </p>
 */
public interface PathMatcherBuilder {

    /**
     * Adds a new {@link PathMatcher}, which matches the pattern specified, to the compound matcher
     * being constructed. For instance, pattern "glob:&lowast;&lowast;/&lowast;.jpg" would match any path ending with
     * "jpg", see {@link java.nio.file.FileSystem#getPathMatcher(String)} for further information.
     *
     * @param pSyntaxAndpPattern The syntax and the pattern, must not be {@code null}
     * @return This object, never {@code null}
     */
    PathMatcherBuilder and(String pSyntaxAndpPattern);

    /**
     * Adds the custom {@link PathMatcher} specified to the compound matcher being constructed.
     *
     * @param pMatcher A custom matcher, must not be {@code null}
     * @return This object, never {@code null}
     */
    PathMatcherBuilder and(PathMatcher pMatcher);

    /**
     * Builds the final compound {@link PathMatcher} which chains all matcher instances chained with
     * {@link #and(String)} and {@link #and(PathMatcher)} and adds it to the associated
     * restriction.
     *
     * @return The associated restriction, never {@code null}
     */
    SimpleDispatchRestriction thenAccept();
}
