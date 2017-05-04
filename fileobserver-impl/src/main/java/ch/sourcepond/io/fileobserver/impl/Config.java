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
package ch.sourcepond.io.fileobserver.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 */
@ObjectClassDefinition(name = "Sourcepond fileobserver configuration", description = "PathChangeListener configuration definition")
public @interface Config {

    @AttributeDefinition(
            name="Pending duration",
            description = "Duration while no modification event is processed after a create event has been received"
    )
    long pendingDuration() default 500L;

    @AttributeDefinition(
            name="Timeout",
            description = "Duration to wait until a file is considered to be completely written"
    )
    long timeout() default 2000L;
}