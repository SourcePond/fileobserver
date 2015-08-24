package ch.sourcepond.utils.fileobserver.impl;

import java.io.Closeable;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
interface InternalResource extends Resource, Closeable {

	/**
	 * @return
	 */
	void informListeners(ResourceEvent.Type pType);
}
