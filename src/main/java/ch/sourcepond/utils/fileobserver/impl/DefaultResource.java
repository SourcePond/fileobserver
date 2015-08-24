package ch.sourcepond.utils.fileobserver.impl;

import static java.nio.file.Files.newInputStream;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.ResourceEvent.Type;

/**
 * @author rolandhauser
 *
 */
final class DefaultResource extends ClosableResource implements InternalResource {
	private static final Logger LOG = getLogger(DefaultResource.class);
	private final Set<ResourceChangeListener> listeners = new HashSet<>();
	private final ExecutorService executor;
	private final TaskFactory taskFactory;
	private final URL originContent;
	private final Path storagePath;

	/**
	 * @param pOrigin
	 */
	DefaultResource(final ExecutorService pExecutor, final TaskFactory pTaskFactory, final URL pOriginalContent,
			final Path pStoragePath) {
		executor = pExecutor;
		taskFactory = pTaskFactory;
		originContent = pOriginalContent;
		storagePath = pStoragePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#addObserver(ch.
	 * sourcepond.utils.content.observer.ChangeObserver)
	 */
	@Override
	public synchronized void addObserver(final ResourceChangeListener pObserver) {
		checkClosed();
		if (!listeners.add(pObserver)) {
			LOG.debug("Observer {0} already present, nothing to be added.", pObserver);
		} else {
			fireEvent(pObserver, new ResourceEvent(this, Type.LISTENER_ADDED));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#removeObserver(ch.
	 * sourcepond.utils.content.observer.ChangeObserver)
	 */
	@Override
	public synchronized void removeObserver(final ResourceChangeListener pObserver) {
		if (isClosed()) {
			LOG.warn("Watcher is closed; do nothing");
		} else if (!listeners.remove(pObserver)) {
			LOG.debug("Observer {0} not present, nothing to be removed.", pObserver);
		} else {
			fireEvent(pObserver, new ResourceEvent(this, Type.LISTENER_REMOVED));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#open()
	 */
	@Override
	public InputStream open() throws IOException {
		checkClosed();
		return newInputStream(storagePath);
	}

	/**
	 * @param pObserver
	 */
	private void fireEvent(final ResourceChangeListener pObserver, final ResourceEvent pEvent) {
		executor.execute(taskFactory.newObserverTask(pObserver, pEvent, this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.InternalResource#
	 * informObservers()
	 */
	@Override
	public void informListeners(final ResourceEvent.Type pType) {
		final ResourceEvent event = new ResourceEvent(this, pType);
		for (final ResourceChangeListener listener : listeners) {
			fireEvent(listener, event);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.Resource#getOriginContent()
	 */
	@Override
	public URL getOriginContent() {
		return originContent;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.ClosableResource#doClose()
	 */
	@Override
	protected void doClose() throws IOException {
		listeners.clear();
	}
}
