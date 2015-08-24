package ch.sourcepond.utils.fileobserver.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ch.sourcepond.utils.fileobserver.WatchManager;

/**
 * @author rolandhauser
 *
 */
public final class WatchManagerActivator implements BundleActivator {
	private DefaultWatchManager factory;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.
	 * BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		factory = new DefaultWatchManager();
		context.registerService(WatchManager.class, factory, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		if (factory != null) {
			factory.closeManagers();
		}
	}
}
