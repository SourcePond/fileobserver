package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.impl.EventType.CREATED;
import static java.nio.file.Files.newInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.sourcepond.utils.fileobserver.ChangeObserver;

/**
 * @author rolandhauser
 *
 */
final class DefaultResource implements InternalResource {
	private static final Logger LOG = LoggerFactory.getLogger(DefaultResource.class);
	private final Set<ChangeObserver> observers = new HashSet<>();
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
	 * @see
	 * ch.sourcepond.utils.fileobserver.impl.WatchedFile#addObserver(ch.
	 * sourcepond.utils.content.observer.ChangeObserver)
	 */
	@Override
	public synchronized void addObserver(final ChangeObserver pObserver) {
		if (!observers.add(pObserver)) {
			LOG.debug("Observer {0} already present, nothing to be added.", pObserver);
		} else {
			informObserver(pObserver, CREATED);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.impl.WatchedFile#removeObserver(ch.
	 * sourcepond.utils.content.observer.ChangeObserver)
	 */
	@Override
	public synchronized void removeObserver(final ChangeObserver pObserver) {
		if (!observers.remove(pObserver)) {
			LOG.debug("Observer {0} not present, nothing to be removed.", pObserver);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.WatchedFile#open()
	 */
	@Override
	public InputStream open() throws IOException {
		return newInputStream(storagePath);
	}

	/**
	 * @param pObserver
	 */
	private void informObserver(final ChangeObserver pObserver, final EventType pType) {
		executor.execute(taskFactory.newObserverTask(pObserver, pType, this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.sourcepond.utils.fileobserver.impl.InternalResource#
	 * informObservers()
	 */
	@Override
	public void informObservers(final EventType pType) {
		for (final ChangeObserver observer : observers) {
			informObserver(observer, pType);
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
}
