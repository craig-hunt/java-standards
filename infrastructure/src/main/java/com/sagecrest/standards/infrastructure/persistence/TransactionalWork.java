package com.sagecrest.standards.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Work that runs inside one transaction.
 *
 * <p>It is allowed to throw {@link SQLException} so a caller writes plain JDBC without wrapping
 * every statement. {@link Database} converts it once, at the boundary.
 *
 * @param <T> what the work answers
 */
@FunctionalInterface
public interface TransactionalWork<T> {

  T apply(Connection connection) throws SQLException;
}
