package com.sagecrest.standards.application.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.sagecrest.standards.application.ports.EventConsumer;
import com.sagecrest.standards.domain.events.DomainEvent;
import com.sagecrest.standards.domain.events.SignupRecorded;
import com.sagecrest.standards.domain.events.TaskCompleted;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FanOutEventDispatcherTest {

  private static final String EMAIL = "ada@example.com";
  private static final String PLAN = "Growth";
  private static final String TITLE = "Read the ADR";
  private static final int SEATS = 3;
  private static final long ID = 7L;

  private static final class Collecting<E extends DomainEvent> extends DomainEventConsumer<E> {
    private final List<E> received = new ArrayList<>();

    Collecting(Class<E> accepted) {
      super(accepted);
    }

    @Override
    protected void handle(E event) {
      received.add(event);
    }
  }

  private static SignupRecorded signupRecorded() {
    return new SignupRecorded(UUID.randomUUID(), Instant.now(), ID, EMAIL, PLAN, SEATS);
  }

  private static TaskCompleted taskCompleted() {
    return new TaskCompleted(UUID.randomUUID(), Instant.now(), ID, TITLE);
  }

  @Test
  @DisplayName("delivers an event to the consumer that wants it and to no other")
  void deliversOnlyToTheConsumerThatWantsIt() {
    Collecting<SignupRecorded> signups = new Collecting<>(SignupRecorded.class);
    Collecting<TaskCompleted> tasks = new Collecting<>(TaskCompleted.class);
    FanOutEventDispatcher dispatcher = new FanOutEventDispatcher(List.of(signups, tasks));

    SignupRecorded recorded = signupRecorded();
    dispatcher.dispatch(recorded);

    assertThat(signups.received).containsExactly(recorded);
    assertThat(tasks.received).isEmpty();
  }

  @Test
  void routesTheOtherEventToTheOtherConsumer() {
    Collecting<SignupRecorded> signups = new Collecting<>(SignupRecorded.class);
    Collecting<TaskCompleted> tasks = new Collecting<>(TaskCompleted.class);
    FanOutEventDispatcher dispatcher = new FanOutEventDispatcher(List.of(signups, tasks));

    TaskCompleted completed = taskCompleted();
    dispatcher.dispatch(completed);

    assertThat(tasks.received).containsExactly(completed);
    assertThat(signups.received).isEmpty();
  }

  @Test
  @DisplayName("delivers to every consumer that accepts, not merely the first")
  void deliversToEveryConsumerThatAccepts() {
    Collecting<SignupRecorded> first = new Collecting<>(SignupRecorded.class);
    Collecting<SignupRecorded> second = new Collecting<>(SignupRecorded.class);
    FanOutEventDispatcher dispatcher = new FanOutEventDispatcher(List.of(first, second));

    SignupRecorded recorded = signupRecorded();
    dispatcher.dispatch(recorded);

    assertThat(first.received).containsExactly(recorded);
    assertThat(second.received).containsExactly(recorded);
  }

  @Test
  void deliversNothingWhenNobodyIsListening() {
    List<EventConsumer> none = List.of();

    new FanOutEventDispatcher(none).dispatch(signupRecorded());
  }

  @Test
  @DisplayName("copies the consumer list, so a later change cannot reroute dispatch")
  void copiesTheConsumerList() {
    Collecting<SignupRecorded> signups = new Collecting<>(SignupRecorded.class);
    List<EventConsumer> registered = new ArrayList<>(List.of(signups));
    FanOutEventDispatcher dispatcher = new FanOutEventDispatcher(registered);

    registered.clear();
    SignupRecorded recorded = signupRecorded();
    dispatcher.dispatch(recorded);

    assertThat(signups.received).containsExactly(recorded);
  }

  @Test
  @DisplayName("a typed consumer refuses an event of another type")
  void aTypedConsumerRefusesAnotherType() {
    Collecting<SignupRecorded> signups = new Collecting<>(SignupRecorded.class);

    assertThat(signups.accepts(signupRecorded())).isTrue();
    assertThat(signups.accepts(taskCompleted())).isFalse();
  }
}
