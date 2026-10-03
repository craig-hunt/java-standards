package com.sagecrest.standards.application.tasks;

import com.sagecrest.standards.application.ports.TaskStore;
import com.sagecrest.standards.domain.tasks.TaskFilter;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.domain.tasks.TaskView;

/**
 * The task operations an endpoint calls.
 *
 * <p>Counting and filtering run here rather than in the store, so every store implementation
 * reports the same totals and a test exercises the rule without a database.
 */
public final class TaskService {

  private final TaskStore store;

  public TaskService(TaskStore store) {
    this.store = store;
  }

  public TaskView list(TaskFilter filter) {
    return TaskView.summarize(store.list(), filter);
  }

  public TaskItem create(TaskTitle title) {
    return store.create(title);
  }

  public TaskItem setCompleted(TaskId id, boolean completed) {
    return store.setCompleted(id, completed);
  }

  public void delete(TaskId id) {
    store.delete(id);
  }

  public long clearCompleted() {
    return store.deleteCompleted();
  }
}
