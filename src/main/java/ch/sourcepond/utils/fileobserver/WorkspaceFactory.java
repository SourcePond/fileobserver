package ch.sourcepond.utils.fileobserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * @author rolandhauser
 *
 */
public interface WorkspaceFactory {

	/**
	 * @param pWorkspace
	 * @return
	 * @throws WorkspaceLockedException
	 * @throws IOException
	 */
	Workspace create(Path pWorkspace, ExecutorService pExecutor) throws WorkspaceLockedException, IOException;
}
