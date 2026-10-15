package com.acme.salary.persistence;

import org.hibernate.community.dialect.SQLiteDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

/**
 * The community SQLite dialect reports constraint failures (unique, check, not-null) as generic
 * JDBC errors, so Spring surfaces them as {@code JpaSystemException}. Mapping SQLite's
 * {@code SQLITE_CONSTRAINT} result code to Hibernate's {@link ConstraintViolationException} lets
 * Spring translate them to {@code DataIntegrityViolationException}, like on any other database.
 */
public class ConstraintAwareSQLiteDialect extends SQLiteDialect {

	/** Primary result code; extended codes (e.g. 2067 = unique) keep it in the low byte. */
	private static final int SQLITE_CONSTRAINT = 19;

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		SQLExceptionConversionDelegate fallback = super.buildSQLExceptionConversionDelegate();
		return (sqlException, message, sql) -> {
			if ((sqlException.getErrorCode() & 0xFF) == SQLITE_CONSTRAINT) {
				return new ConstraintViolationException(message, sqlException, sql, null);
			}
			return fallback == null ? null : fallback.convert(sqlException, message, sql);
		};
	}

}
