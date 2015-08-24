package ch.sourcepond.utils.fileobserver.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;

/**
 * @author rolandhauser
 *
 */
final class InformObserverTask implements Runnable {
	private static final Logger LOG = getLogger(InformObserverTask.class);
	private final ResourceChangeListener listener;
	private final ResourceEvent event;
	private final Resource resource;

	/**
	 * @param pListener
	 */
	InformObserverTask(final ResourceChangeListener pListener, final ResourceEvent pEvent, final Resource pResource) {
		listener = pListener;
		event = pEvent;
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
			listener.resourceChange(event);
		} catch (final Exception e) {
			LOG.warn("Caught unexpected exception", e);
		}
	}
}
