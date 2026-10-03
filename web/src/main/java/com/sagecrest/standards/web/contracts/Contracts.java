package com.sagecrest.standards.web.contracts;

import com.sagecrest.standards.domain.inventory.InventoryItem;
import com.sagecrest.standards.domain.inventory.InventoryResult;
import com.sagecrest.standards.domain.signups.SignupConfirmation;
import com.sagecrest.standards.domain.tasks.TaskItem;
import com.sagecrest.standards.domain.tasks.TaskView;
import java.util.List;

/**
 * The shapes this surface sends and receives.
 *
 * <p>Every contract carries primitives rather than the domain's value types. A {@code TaskId} is a
 * record wrapping a long, and Jackson would write it as {@code {"value":1}}, so the C# sibling
 * installs JSON converters to flatten its equivalents. Converters work, but they put the wire shape
 * somewhere a reader of the contract cannot see it. Declaring the primitive here states the wire
 * shape in the type that defines it, and costs one mapping call.
 */
public final class Contracts {

  /** What a client sends to create a task. */
  public record CreateTask(String title) {}

  /**
   * What a client sends to change a task's completion.
   *
   * <p>The flag is boxed so an omitted member reads as missing rather than as false, which would
   * silently reopen a completed task.
   */
  public record UpdateTask(Boolean completed) {}

  /** One task, as a client sees it. */
  public record TaskResponse(long id, String title, boolean completed) {
    public static TaskResponse from(TaskItem task) {
      return new TaskResponse(task.id().value(), task.title().value(), task.completed());
    }
  }

  /** A filtered task list with counts over the whole set. */
  public record TaskViewResponse(List<TaskResponse> tasks, int remaining, int total) {

    public TaskViewResponse {
      tasks = List.copyOf(tasks);
    }

    public static TaskViewResponse from(TaskView view) {
      return new TaskViewResponse(
          view.tasks().stream().map(TaskResponse::from).toList(), view.remaining(), view.total());
    }
  }

  /** How many tasks a clear-completed request removed. */
  public record ClearTasksResponse(long removed) {}

  /** What a signup confirmation returns. */
  public record SignupResponse(long id, String summary) {
    public static SignupResponse from(SignupConfirmation confirmation) {
      return new SignupResponse(confirmation.id().value(), confirmation.summary());
    }
  }

  /** One stock row, as a client sees it. */
  public record InventoryItemResponse(String name, int quantity, String status) {
    public static InventoryItemResponse from(InventoryItem item) {
      return new InventoryItemResponse(item.name(), item.quantity(), item.status().value());
    }
  }

  /** Matching stock rows with counts. */
  public record InventoryResponse(List<InventoryItemResponse> items, int shown, int total) {

    public InventoryResponse {
      items = List.copyOf(items);
    }

    public static InventoryResponse from(InventoryResult result) {
      return new InventoryResponse(
          result.items().stream().map(InventoryItemResponse::from).toList(),
          result.shown(),
          result.total());
    }
  }

  private Contracts() {}
}
