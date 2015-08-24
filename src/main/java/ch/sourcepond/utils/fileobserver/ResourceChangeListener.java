package ch.sourcepond.utils.fileobserver;

/**
 * @author rolandhauser
 *
 */
public interface ResourceChangeListener {

	/**
	 * @param pResource
	 */
	void resourceChange(ResourceEvent pEvent);
}
