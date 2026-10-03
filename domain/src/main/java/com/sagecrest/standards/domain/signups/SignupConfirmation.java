package com.sagecrest.standards.domain.signups;

/** What a signup confirmation returns to the caller. */
public record SignupConfirmation(SignupId id, String summary) {}
