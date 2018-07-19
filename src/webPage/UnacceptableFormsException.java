/**
 * 
 */
package webPage;

/**
 * @author Ian Shef
 *
 */
public class UnacceptableFormsException extends IllegalArgumentException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public UnacceptableFormsException() {
		// Currently empty.
	}

	/**
	 * @param s
	 */
	public UnacceptableFormsException(String s) {
		super(s);
	}

	/**
	 * @param cause
	 */
	public UnacceptableFormsException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public UnacceptableFormsException(String message, Throwable cause) {
		super(message, cause);
	}
}
