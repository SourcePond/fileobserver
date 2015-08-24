package ch.sourcepond.utils.fileobserver.impl;

import static ch.sourcepond.utils.fileobserver.impl.EventType.CREATED;
import static ch.sourcepond.utils.fileobserver.impl.EventType.DELETED;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.Watcher;

/**
 * @author rolandhauser
 *
 */
final class DefaultWatcher implements Watcher, Runnable {
	private static final Logger LOG = getLogger(DefaultWatcher.class);
	private final Map<URL, InternalResource> watchedFiles = new HashMap<>();
	private final Map<Path, InternalResource> resources = new ConcurrentHashMap<>();
	private final Path lockFile;
	private final TaskFactory taskFactory;
	private final ExecutorService informObserverExector;
	private final WatchService watchService;
	private volatile boolean open = true;

	/**
	 * @param pLockFile
	 * @param pWatchService
	 * @throws IOException
	 */
	DefaultWatcher(final Path pLockFile, final TaskFactory pTaskFactory, final ExecutorService pInformObserverExector)
			throws IOException {
		lockFile = pLockFile;
		taskFactory = pTaskFactory;
		informObserverExector = pInformObserverExector;
		watchService = pLockFile.getFileSystem().newWatchService();
	}

	/**
	 * @param pUrl
	 * @param pPath
	 * @return
	 */
	private Path determinePath(final URL pUrl, final String[] pPath) {
		Path currentPath = lockFile.getParent();
		for (final String path : pPath) {
			currentPath = currentPath.resolve(path);
		}
		return currentPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ch.sourcepond.utils.fileobserver.impl.WatchManager#watchFile(java.net
	 * .URL, java.lang.String[])
	 */
	@Override
	public synchronized Resource watchFile(final URL pOriginContent, final String... pPath) throws IOException {
		notNull(pOriginContent, "URL cannot be null!");
		notEmpty(pPath, "At least one path element must be specified!");
		InternalResource file = watchedFiles.get(pOriginContent);

		if (file == null) {
			final Path path = determinePath(pOriginContent, pPath);
			createDirectories(path.getParent());

			try (final InputStream in = pOriginContent.openStream()) {
				copy(in, path);
			}

			path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
			file = new DefaultResource(informObserverExector, taskFactory, pOriginContent, path);

			watchedFiles.put(pOriginContent, file);
			resources.put(path, file);
		}
		return file;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		open = false;
		try {
			watchService.close();
		} catch (final IOException e) {
			LOG.debug(e.getMessage(), e);
		} finally {
			deleteIfExists(lockFile);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		try {
			while (open) {
				final WatchKey key = watchService.take();

				for (final WatchEvent<?> event : key.pollEvents()) {
					final Kind<?> kind = event.kind();

					// Overflow event.
					if (OVERFLOW.equals(kind)) {
						continue; // loop
					}

					if (ENTRY_CREATE.equals(kind)) {
						informObservers(event, CREATED);
					} else if (ENTRY_MODIFY.equals(kind)) {
						informObservers(event, EventType.MODIFIED);
					} else if (ENTRY_DELETE.equals(kind)) {
						informObservers(event, DELETED);
					}

					// Inside the loop
					if (key.reset()) {
						break;
					}
				}
			}
		} catch (final InterruptedException e) {
			LOG.error(e.getMessage(), e);
		} catch (final ClosedWatchServiceException e) {
			LOG.warn(e.getMessage(), e);
		} finally {
			try {
				close();
			} catch (final IOException e) {
				LOG.warn(e.getMessage(), e);
			}
		}
	}

	/**
	 * @param pPath
	 * @param pType
	 */
	private void informObservers(final WatchEvent<?> pEvent, final EventType pType) {
		final InternalResource resource = resources.get(pEvent.context());
		if (resource != null) {
			resource.informObservers(pType);
		}
	}
}
