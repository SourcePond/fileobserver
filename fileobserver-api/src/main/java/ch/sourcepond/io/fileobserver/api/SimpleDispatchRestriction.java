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

import java.nio.file.PathMatcher;

/**
 * An object which can be used to restrict which {@link DispatchKey} can be handled by a specific {@link PathChangeListener}.
 */
public interface SimpleDispatchRestriction {

    /**
     * <p>Adds a new {@link PathMatcher}, which matches the pattern specified to this restriction.
     * For instance, pattern "glob:&lowast;&lowast;/&lowast;.jpg" would match any path ending with
     * "jpg", see {@link java.nio.file.FileSystem#getPathMatcher(String)} for further information.</p>
     *
     * @param pSyntaxAndPattern The syntax and the pattern, must not be {@code null}
     * @return This object, never {@code null}
     */
    SimpleDispatchRestriction addPathMatcher(String pSyntaxAndPattern);

    /**
     * Adds the custom {@link PathMatcher} specified to this restriction.
     *
     * @param pCustomMatcher A custom matcher, must not be {@code null}
     * @return This object, never {@code null}
     */
    SimpleDispatchRestriction addPathMatcher(PathMatcher pCustomMatcher);
}
