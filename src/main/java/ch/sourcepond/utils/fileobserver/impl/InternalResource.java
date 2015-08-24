package ch.sourcepond.utils.fileobserver.impl;

import ch.sourcepond.utils.fileobserver.Resource;

/**
 * @author rolandhauser
 *
 */
interface InternalResource extends Resource {

	/**
	 * @return
	 */
	void informObservers(EventType pType);
}
