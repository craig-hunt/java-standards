package com.sagecrest.standards.infrastructure.events;

import com.sagecrest.standards.application.events.DomainEventConsumer;
import com.sagecrest.standards.domain.events.TaskCompleted;
import com.sagecrest.standards.infrastructure.InfrastructureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Notes that a task was completed. */
public final class TaskCompletedConsumer extends DomainEventConsumer<TaskCompleted> {

  private static final Logger LOG = LoggerFactory.getLogger(TaskCompletedConsumer.class);

  public TaskCompletedConsumer() {
    super(TaskCompleted.class);
  }

  @Override
  protected void handle(TaskCompleted event) {
    LOG.info(InfrastructureConstants.MSG_TASK_COMPLETED, event.taskId(), event.title());
  }
}
