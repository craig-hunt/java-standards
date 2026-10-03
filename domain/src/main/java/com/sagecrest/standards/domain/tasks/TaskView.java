package com.sagecrest.standards.domain.tasks;

import java.util.ArrayList;
import java.util.List;

/**
 * What a list request answers: the tasks the filter shows, how many remain incomplete across every
 * task, and how many tasks exist.
 *
 * <p>Remaining and total count the whole set rather than the filtered view, so the counts stay
 * steady while a reader switches filters.
 */
public record TaskView(List<TaskItem> tasks, int remaining, int total) {

  private static final int NONE = 0;

  public TaskView {
    tasks = List.copyOf(tasks);
  }

  public static TaskView summarize(List<TaskItem> all, TaskFilter filter) {
    List<TaskItem> shown = new ArrayList<>(all.size());
    int remaining = NONE;
    for (TaskItem task : all) {
      if (!task.completed()) {
        remaining++;
      }
      if (filter.includes(task)) {
        shown.add(task);
      }
    }
    return new TaskView(shown, remaining, all.size());
  }
}
