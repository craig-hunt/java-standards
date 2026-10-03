package com.sagecrest.standards.domain.signups;

/**
 * What the form submitted, before validation.
 *
 * <p>Seats stays boxed so an omitted count takes the default while an explicit zero still fails.
 * Collapsing both to zero would reject a form that never mentioned seats.
 */
public record SignupRequest(
    String fullName, String email, String plan, Integer seats, String notes, boolean acceptTerms) {}
