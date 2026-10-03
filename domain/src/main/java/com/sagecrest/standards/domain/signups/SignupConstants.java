package com.sagecrest.standards.domain.signups;

/** Every literal the signup feature carries, named once. */
public final class SignupConstants {

  public static final String PLAN_STARTER = "Starter";
  public static final String PLAN_GROWTH = "Growth";
  public static final String PLAN_ENTERPRISE = "Enterprise";

  public static final String FIELD_FULL_NAME = "fullName";
  public static final String FIELD_EMAIL = "email";
  public static final String FIELD_PLAN = "plan";
  public static final String FIELD_SEATS = "seats";
  public static final String FIELD_NOTES = "notes";
  public static final String FIELD_ACCEPT_TERMS = "acceptTerms";

  public static final String MSG_NAME_REQUIRED = "Enter your full name.";
  public static final String MSG_EMAIL_REQUIRED = "Enter your work email.";
  public static final String MSG_EMAIL_INVALID = "Enter a valid email address.";
  public static final String MSG_PLAN_REQUIRED = "Choose a plan.";
  public static final String MSG_PLAN_UNKNOWN = "Choose the Starter, Growth, or Enterprise plan.";
  public static final String MSG_SEATS_INVALID = "Enter at least one seat.";
  public static final String MSG_NOTES_TOO_LONG = "Keep the notes within the length limit.";
  public static final String MSG_TERMS_REQUIRED = "Accept the terms to continue.";

  public static final String CODE_INVALID_ID = "invalid_id";
  public static final String MSG_INVALID_ID = "signup id must be a positive whole number";

  public static final int DEFAULT_SEATS = 1;
  public static final int MIN_SEATS = 1;
  public static final int MAX_NOTES_LENGTH = 1000;

  public static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
  public static final String SUMMARY_FORMAT = "%s on the %s plan, %d seat(s).";

  private SignupConstants() {}
}
