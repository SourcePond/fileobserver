package ch.sourcepond.utils.fileobserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * @author rolandhauser
 *
 */
public interface WatchManager {

	/**
	 * @param pWorkspace
	 * @return
	 * @throws WorkspaceLockedException
	 * @throws IOException
	 */
	Watcher watch(Path pWorkspace, ExecutorService pExecutor) throws WorkspaceLockedException, IOException;
}
