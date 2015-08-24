package ch.sourcepond.utils.content.observer.impl;

import static java.nio.file.FileSystems.getDefault;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.ResourceChangeListener;
import ch.sourcepond.utils.fileobserver.ResourceEvent;
import ch.sourcepond.utils.fileobserver.WatchManager;
import ch.sourcepond.utils.fileobserver.Watcher;
import ch.sourcepond.utils.fileobserver.impl.DefaultWatchManager;

/**
 * @author rolandhauser
 *
 */
public class WatchManagerTest {
	/**
	 * @author rolandhauser
	 *
	 */
	private class TestListener implements ResourceChangeListener {
		private final Lock lock = new ReentrantLock();
		private final Condition actionDoneCondition = lock.newCondition();
		private final Properties props = new Properties();
		private boolean actionDone;
		private Exception unexpected;

		public void awaitAction() throws InterruptedException {
			lock.lock();
			try {
				if (!actionDone) {
					assertTrue("Action not executed!", actionDoneCondition.await(500, MILLISECONDS));
				}
			} finally {
				actionDone = false;
				lock.unlock();
			}
		}

		@Override
		public void resourceChange(final ResourceEvent pEvent) {
			lock.lock();
			try (final InputStream in = pEvent.getSource().open()) {
				props.load(in);
			} catch (final IOException e) {
				unexpected = e;
			} finally {
				actionDone = true;
				actionDoneCondition.signal();
				lock.unlock();
			}
		}
	}

	private static final String TEST_FILE_NAME = "test.properties";
	private static final String KEY = "key";
	private static final Path WORKSPACE = getDefault().getPath(SystemUtils.USER_DIR, "target");
	private final ExecutorService observerInforExecutor = Executors.newSingleThreadExecutor();
	private final TestListener observer = new TestListener();
	private final WatchManager factory = new DefaultWatchManager();
	private Watcher manager;
	private Resource resource;

	/**
	 * @throws IOException
	 * 
	 */
	@Before
	public void setup() throws Exception {
		manager = factory.watch(WORKSPACE, observerInforExecutor);
		resource = manager.watchFile(getClass().getResource("/" + TEST_FILE_NAME), TEST_FILE_NAME);
	}

	/**
	 * @throws Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (manager != null) {
			manager.close();
		}
	}

	/**
	 * @throws IOException
	 * 
	 */
	@Test
	public void verifyInformObserverAfterRegistration() throws Exception {
		final Resource resource = manager.watchFile(getClass().getResource("/" + TEST_FILE_NAME), TEST_FILE_NAME);
		resource.addObserver(observer);
		observer.awaitAction();

		// The observer should have been informed about content change. If a new
		// observer is added, it will initially informed about the observed
		// resource.
		assertEquals(1, observer.props.size());
		assertEquals("This is the initial value", observer.props.getProperty(KEY));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void verifyInformObserverAboutReCreation() throws Exception {

	}
}
