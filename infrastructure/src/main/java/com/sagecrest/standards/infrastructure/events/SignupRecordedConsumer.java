package com.sagecrest.standards.infrastructure.events;

import com.sagecrest.standards.application.events.DomainEventConsumer;
import com.sagecrest.standards.domain.events.SignupRecorded;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Notes that a signup happened.
 *
 * <p>Logging stands in for whatever a real deployment would do here, and it is deliberately
 * idempotent. The outbox delivers at least once, so a consumer that charged a card or sent a mail
 * would need to recognize a repeat before acting on it.
 */
public final class SignupRecordedConsumer extends DomainEventConsumer<SignupRecorded> {

  private static final Logger LOG = LoggerFactory.getLogger(SignupRecordedConsumer.class);

  public SignupRecordedConsumer() {
    super(SignupRecorded.class);
  }

  @Override
  protected void handle(SignupRecorded event) {
    LOG.info(
        InfrastructureConstants.MSG_SIGNUP_RECORDED, event.signupId(), event.plan(), event.seats());
  }
}
