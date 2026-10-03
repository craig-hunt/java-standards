package com.sagecrest.standards.infrastructure.persistence;

/**
 * The parameter positions this module binds.
 *
 * <p>JDBC numbers its placeholders from one, and a statement with five of them invites exactly the
 * kind of off-by-one that compiles, runs, and stores a seat count in the notes column. Naming each
 * position puts the mistake where a reader can see it.
 */
public final class Columns {

  public static final int FIRST = 1;
  public static final int SECOND = 2;
  public static final int THIRD = 3;
  public static final int FOURTH = 4;
  public static final int FIFTH = 5;
  public static final int SIXTH = 6;

  public static final int SEED_NAME = FIRST;
  public static final int SEED_QUANTITY = SECOND;
  public static final int SEED_STATUS = THIRD;

  private Columns() {}
}
