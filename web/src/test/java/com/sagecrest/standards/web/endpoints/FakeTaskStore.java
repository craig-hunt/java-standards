package com.sagecrest.standards.web.endpoints;

import com.sagecrest.standards.application.ports.TaskStore;
import com.sagecrest.standards.domain.tasks.TaskErrors;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import java.util.ArrayList;
import java.util.List;

/**
 * A task store that holds its rows in a list.
 *
 * <p>The endpoint tests need a real {@code TaskService}, and the service takes the port rather than
 * a concrete store, so a small fake is enough and a mocking framework would only obscure what the
 * test arranged.
 */
final class FakeTaskStore implements TaskStore {

  private static final long FIRST_ID = 1L;

  private final List<TaskItem> rows = new ArrayList<>();
  private long nextId = FIRST_ID;

  void given(String title, boolean completed) {
    rows.add(new TaskItem(new TaskId(nextId++), new TaskTitle(title), completed));
  }

  @Override
  public List<TaskItem> list() {
    return List.copyOf(rows);
  }

  @Override
  public TaskItem create(TaskTitle title) {
    TaskItem created = new TaskItem(new TaskId(nextId++), title, false);
    rows.add(created);
    return created;
  }

  @Override
  public TaskItem setCompleted(TaskId id, boolean completed) {
    for (int index = 0; index < rows.size(); index++) {
      if (rows.get(index).id().equals(id)) {
        TaskItem updated = new TaskItem(id, rows.get(index).title(), completed);
        rows.set(index, updated);
        return updated;
      }
    }
    throw TaskErrors.notFound();
  }

  @Override
  public void delete(TaskId id) {
    if (!rows.removeIf(row -> row.id().equals(id))) {
      throw TaskErrors.notFound();
    }
  }

  @Override
  public long deleteCompleted() {
    long completed = rows.stream().filter(TaskItem::completed).count();
    rows.removeIf(TaskItem::completed);
    return completed;
  }
}
