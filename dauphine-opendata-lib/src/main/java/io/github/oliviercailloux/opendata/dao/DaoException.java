package io.github.oliviercailloux.opendata.dao;

/**
 * General exception thrown by most methods of {@link Dao}.<br />
 * This exception usually indicates that an error occurred during the
 * transaction.
 *
 * @author Dauphine - CLAUDEL Arnaud
 *
 */
public class DaoException extends Exception {

	private static final long serialVersionUID = 2180204496566733357L;

	public DaoException() {
		super();
	}

	public DaoException(final String message, final Throwable cause, final boolean enableSuppression,
			final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public DaoException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public DaoException(final String message) {
		super(message);
	}

	public DaoException(final Throwable cause) {
		super(cause);
	}

}