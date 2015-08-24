package ch.sourcepond.utils.fileobserver.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.ChangeObserver;
import ch.sourcepond.utils.fileobserver.Resource;

/**
 * @author rolandhauser
 *
 */
final class InformObserverTask implements Runnable {
	private static final Logger LOG = getLogger(InformObserverTask.class);
	private final ChangeObserver observer;
	private final EventType type;
	private final Resource resource;

	/**
	 * @param pObserver
	 */
	InformObserverTask(final ChangeObserver pObserver, final EventType pType, final Resource pResource) {
		observer = pObserver;
		type = pType;
		resource = pResource;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			type.dispatch(observer, resource);
		} catch (final Exception e) {
			LOG.warn("Caught unexpected exception", e);
		}
	}
}
