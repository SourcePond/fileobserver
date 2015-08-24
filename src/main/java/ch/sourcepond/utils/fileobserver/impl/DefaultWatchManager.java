package ch.sourcepond.utils.fileobserver.impl;

import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import ch.sourcepond.utils.fileobserver.WatchManager;
import ch.sourcepond.utils.fileobserver.Watcher;
import ch.sourcepond.utils.fileobserver.WorkspaceLockedException;

/**
 * @author rolandhauser
 *
 */
@Named
@Singleton
public class DefaultWatchManager extends Thread implements WatchManager {
	private static final Logger LOG = getLogger(DefaultWatchManager.class);
	static final String LOCK_FILE_NAME = ".lock";
	private final Map<Path, Watcher> managers = new HashMap<>();
	private final Runtime runtime;
	private final TaskFactory taskFactory;

	/**
	 * @param pFs
	 */
	@Inject
	public DefaultWatchManager() {
		this(Runtime.getRuntime(), new TaskFactory());
	}

	/**
	 * @param pFs
	 */
	DefaultWatchManager(final Runtime pRuntime, final TaskFactory pTaskFactory) {
		runtime = pRuntime;
		taskFactory = pTaskFactory;

		// It's ok to publish this object here since it a cannot be retrieved
		// and mutated from outside through the Runtime object.
		runtime.addShutdownHook(this);
	}

	/**
	 * @param pWorkspace
	 * @return
	 * @throws WorkspaceLockedException
	 * @throws IOException
	 */
	private Path lock(final Path pWorkspace) throws WorkspaceLockedException, IOException {
		final Path lockFile = createDirectories(pWorkspace).resolve(LOCK_FILE_NAME);
		try {
			createFile(lockFile);
		} catch (final FileAlreadyExistsException e) {
			throw new WorkspaceLockedException("Workspace '" + pWorkspace + "' is locked by another process!", e);
		}
		return lockFile;
	}

	/**
	 * @param pStorageDirectory
	 * @return
	 * @throws IOException
	 */
	@Override
	public synchronized Watcher watch(final Path pWorkspace, final ExecutorService pObserverInformExecutor)
			throws WorkspaceLockedException, IOException {
		Watcher manager = managers.get(pWorkspace);
		if (manager == null) {
			final DefaultWatcher newManager = new DefaultWatcher(lock(pWorkspace), taskFactory,
					pObserverInformExecutor);
			manager = newManager;
			managers.put(pWorkspace, newManager);
		}
		return manager;
	}

	/**
	 * 
	 */
	synchronized void closeManagers() {
		for (final Watcher manager : managers.values()) {
			try {
				manager.close();
			} catch (final IOException e) {
				LOG.warn(e.getMessage(), e);
			}
		}
		managers.clear();
	}

	@Override
	public void run() {
		closeManagers();
	}
}
