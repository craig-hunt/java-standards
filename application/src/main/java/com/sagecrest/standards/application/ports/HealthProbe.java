package com.sagecrest.standards.application.ports;

/**
 * Confirms that the backing database answers.
 *
 * <p>It returns nothing and throws on failure, so an implementation cannot report trouble by
 * answering false and leaving the reason behind.
 */
public interface HealthProbe {

  void ping();
}
