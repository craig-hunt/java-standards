package com.sagecrest.standards.domain.signups;

/**
 * How many seats a signup takes.
 *
 * <p>{@link #of} accepts a boxed value so an omitted count takes the default while an explicit zero
 * still fails. Collapsing both to zero would reject a form that never mentioned seats, and
 * defaulting both would accept one that asked for none.
 */
public record Seats(int value) {

  public Seats {
    if (value < SignupConstants.MIN_SEATS) {
      throw SignupErrors.seatsInvalid();
    }
  }

  public static Seats of(Integer raw) {
    return raw == null ? new Seats(SignupConstants.DEFAULT_SEATS) : new Seats(raw);
  }

  @Override
  public String toString() {
    return Integer.toString(value);
  }
}
