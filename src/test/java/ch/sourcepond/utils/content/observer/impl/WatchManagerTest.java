package ch.sourcepond.utils.content.observer.impl;

import static java.nio.file.FileSystems.getDefault;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ch.sourcepond.utils.fileobserver.ChangeObserver;
import ch.sourcepond.utils.fileobserver.Resource;
import ch.sourcepond.utils.fileobserver.WatchManager;
import ch.sourcepond.utils.fileobserver.Watcher;
import ch.sourcepond.utils.fileobserver.impl.DefaultWatchManager;

/**
 * @author rolandhauser
 *
 */
@Ignore
public class WatchManagerTest {
	/**
	 * @author rolandhauser
	 *
	 */
	private class TestObserver implements ChangeObserver {
		private final Properties props = new Properties();
		private Exception unexpected;

		@Override
		public synchronized void created(final Resource pResource) {
			try (final InputStream in = pResource.open()) {
				props.load(in);
			} catch (final IOException e) {
				unexpected = e;
			}
		}

		@Override
		public synchronized void modified(final Resource pResource) {
			// TODO Auto-generated method stub

		}

		@Override
		public synchronized void deleted(final URL pContent) {
			// TODO Auto-generated method stub

		}
	}

	private static final String TEST_FILE_NAME = "test.properties";
	private static final String KEY = "key";
	private static final Path WORKSPACE = getDefault().getPath(SystemUtils.USER_DIR, "target");
	private final ExecutorService observerInforExecutor = Executors.newSingleThreadExecutor();
	private final TestObserver observer = new TestObserver();
	private final WatchManager factory = new DefaultWatchManager();
	private Watcher manager;

	/**
	 * @throws IOException
	 * 
	 */
	@Before
	public void setup() throws Exception {
		manager = factory.watch(WORKSPACE, observerInforExecutor);
	}

	/**
	 * @throws IOException
	 * 
	 */
	@Test
	public void verifyObservation() throws IOException {
		final Resource resource = manager.watchFile(getClass().getResource("/" + TEST_FILE_NAME), TEST_FILE_NAME);
		resource.addObserver(observer);

		// The observer should have been informed about content change. If a new
		// observer is added, it will initially informed about the observed
		// resource.
		assertEquals(1, observer.props.size());
		assertEquals("This is the initial value", observer.props.getProperty(KEY));
	}
}
