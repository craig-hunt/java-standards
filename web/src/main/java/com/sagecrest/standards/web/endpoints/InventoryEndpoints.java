package com.sagecrest.standards.web.endpoints;

import com.sagecrest.standards.application.inventory.InventoryService;
import com.sagecrest.standards.domain.inventory.InventoryQuery;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.contracts.Contracts;
import com.sagecrest.standards.web.routing.Query;
import com.sagecrest.standards.web.routing.Responses;
import com.sagecrest.standards.web.routing.Route;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.List;

/** The stock route. */
public final class InventoryEndpoints {

  private final InventoryService service;

  public InventoryEndpoints(InventoryService service) {
    this.service = service;
  }

  public List<Route> routes() {
    return List.of(Route.get(WebConstants.PATH_INVENTORY, this::query));
  }

  private void query(HttpExchange exchange, List<String> captured) throws IOException {
    Query parameters = Query.of(exchange);
    InventoryQuery query =
        InventoryQuery.from(
            parameters.get(WebConstants.QUERY_SEARCH),
            parameters.get(WebConstants.QUERY_SORT),
            parameters.get(WebConstants.QUERY_DIRECTION));
    Responses.json(
        exchange, WebConstants.STATUS_OK, Contracts.InventoryResponse.from(service.query(query)));
  }
}
