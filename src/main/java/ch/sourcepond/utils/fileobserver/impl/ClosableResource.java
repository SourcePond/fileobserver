package ch.sourcepond.utils.fileobserver.impl;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author rolandhauser
 *
 */
abstract class ClosableResource implements Closeable {
	private volatile boolean closed;

	/**
	 * 
	 */
	protected final void checkClosed() {
		if (closed) {
			throw new IllegalStateException("This watcher has been closed!");
		}
	}

	protected final boolean isClosed() {
		return closed;
	}

	/**
	 * 
	 */
	protected abstract void doClose() throws IOException;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.io.Closeable#close()
	 */
	@Override
	public final void close() throws IOException {
		closed = true;
		doClose();
	}
}
