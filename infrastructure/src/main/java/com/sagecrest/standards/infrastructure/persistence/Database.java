package com.sagecrest.standards.infrastructure.persistence;

import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * The one place that handles JDBC's bookkeeping, so a store reads as SQL and mapping.
 *
 * <p>Every store goes through {@link #inTransaction}. A store that opened its own connection and
 * forgot to commit would leave work uncommitted in a way no test notices until two statements had
 * to agree, and a store that forgot to close one would exhaust the pool under load.
 *
 * <p>A pool is not optional. Virtual threads make a request cheap to park, which is exactly why the
 * pool matters more on Java 21 rather than less: ten thousand virtual threads will queue politely
 * for ten connections, and the queueing is cheap, but the ceiling is still ten. Sizing the pool
 * remains a decision somebody has to make on purpose.
 */
public final class Database {

  private final DataSource source;

  public Database(DataSource source) {
    this.source = source;
  }

  public <T> T inTransaction(TransactionalWork<T> work) {
    try (Connection connection = source.getConnection()) {
      connection.setAutoCommit(false);
      try {
        T answer = work.apply(connection);
        connection.commit();
        return answer;
      } catch (SQLException | RuntimeException failure) {
        connection.rollback();
        throw failure;
      }
    } catch (SQLException failure) {
      throw new InfrastructureException(InfrastructureConstants.MSG_TRANSACTION_FAILED, failure);
    }
  }

  /**
   * Runs read-only work on a connection outside any transaction of its own.
   *
   * <p>A single statement needs no transaction, and wrapping one in a transaction would hold a
   * connection open for the time it takes to map the rows.
   */
  public <T> T query(TransactionalWork<T> work) {
    try (Connection connection = source.getConnection()) {
      return work.apply(connection);
    } catch (SQLException failure) {
      throw new InfrastructureException(InfrastructureConstants.MSG_QUERY_FAILED, failure);
    }
  }
}
