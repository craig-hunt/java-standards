package com.sagecrest.standards.web.endpoints;

import com.sagecrest.standards.application.tasks.TaskService;
import com.sagecrest.standards.domain.tasks.TaskErrors;
import com.sagecrest.standards.domain.tasks.TaskFilter;
import com.sagecrest.standards.domain.tasks.TaskId;
import com.sagecrest.standards.domain.tasks.TaskTitle;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.contracts.Contracts;
import com.sagecrest.standards.web.json.Json;
import com.sagecrest.standards.web.routing.Query;
import com.sagecrest.standards.web.routing.Responses;
import com.sagecrest.standards.web.routing.Route;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;

/**
 * The task routes.
 *
 * <p>No handler here carries a try/catch. A domain type that rejects its input throws, and the
 * failure mapper at the edge turns that into a status and a body. An endpoint that caught would be
 * deciding the wire shape of a failure in a place nobody looks for it.
 */
public final class TaskEndpoints {

  private static final int ONE_PATH_SEGMENT = 0;

  private final TaskService service;

  public TaskEndpoints(TaskService service) {
    this.service = service;
  }

  public List<Route> routes() {
    // The literal path precedes the parameterized one, so DELETE /api/tasks clears the
    // completed set and DELETE /api/tasks/4 removes one task.
    return List.of(
        Route.get(WebConstants.PATH_TASKS, this::list),
        Route.post(WebConstants.PATH_TASKS, this::create),
        Route.delete(WebConstants.PATH_TASKS, this::clearCompleted),
        Route.patchUnder(WebConstants.PATH_TASKS, this::update),
        Route.deleteUnder(WebConstants.PATH_TASKS, this::delete));
  }

  private void list(HttpExchange exchange, List<String> captured) throws IOException {
    TaskFilter filter = TaskFilter.parse(Query.of(exchange).get(WebConstants.QUERY_FILTER));
    Responses.json(
        exchange, WebConstants.STATUS_OK, Contracts.TaskViewResponse.from(service.list(filter)));
  }

  private void create(HttpExchange exchange, List<String> captured) throws IOException {
    Contracts.CreateTask request = Json.read(exchange.getRequestBody(), Contracts.CreateTask.class);
    Responses.json(
        exchange,
        WebConstants.STATUS_CREATED,
        Contracts.TaskResponse.from(service.create(new TaskTitle(request.title()))));
  }

  private void update(HttpExchange exchange, List<String> captured) throws IOException {
    TaskId id = identify(captured);
    Contracts.UpdateTask request = Json.read(exchange.getRequestBody(), Contracts.UpdateTask.class);
    if (request.completed() == null) {
      throw TaskErrors.completedRequired();
    }
    Responses.json(
        exchange,
        WebConstants.STATUS_OK,
        Contracts.TaskResponse.from(service.setCompleted(id, request.completed())));
  }

  private void delete(HttpExchange exchange, List<String> captured) throws IOException {
    service.delete(identify(captured));
    Responses.empty(exchange, WebConstants.STATUS_NO_CONTENT);
  }

  private void clearCompleted(HttpExchange exchange, List<String> captured) throws IOException {
    Responses.json(
        exchange,
        WebConstants.STATUS_OK,
        new Contracts.ClearTasksResponse(service.clearCompleted()));
  }

  /**
   * Reads the identifier out of the path, treating an unreadable one as naming no task.
   *
   * <p>404 rather than 422: a path segment that is not a number addresses nothing, and telling a
   * client its value failed validation would imply the path was otherwise right. The siblings
   * answer the same way, by refusing the route rather than the value.
   */
  private static TaskId identify(List<String> captured) {
    return TaskId.parse(captured.get(ONE_PATH_SEGMENT)).orElseThrow(TaskErrors::notFound);
  }
}
