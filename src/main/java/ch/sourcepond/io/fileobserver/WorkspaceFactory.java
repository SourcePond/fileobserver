/*Copyright (C) 2015 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.util.concurrent.Executor;

/**
 * Factory service to create new {@link Workspace} instances.
 *
 */
public interface WorkspaceFactory {

	/**
	 * Creates a new {@link Workspace} for the directory specified. The
	 * workspace will use the executor specified to inform registered listeners
	 * (see {@link ResourceChangeListener}) about changes (see
	 * {@link ResourceEvent}) on files/directories within the watched directory.
	 * 
	 * @param pListenerNotifier
	 *            Executor to be used for informing listeners about changes;
	 *            must not be {@code null}.
	 * @param pDirectory
	 *            Directory to watched; must not be {@code null} and must be a
	 *            directory.
	 * @return New workspace instance, never {@code null}.
	 * @throws IOException
	 *             Thrown, if no {@link WatchService} could be created for the
	 *             path specified.
	 */
	Workspace create(Executor pListenerNotifier, Path pDirectory) throws IOException;
}
