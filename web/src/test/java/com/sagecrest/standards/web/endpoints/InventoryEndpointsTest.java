package com.sagecrest.standards.web.endpoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sagecrest.standards.application.inventory.InventoryService;
import com.sagecrest.standards.application.ports.InventoryStore;
import com.sagecrest.standards.domain.errors.ValidationException;
import com.sagecrest.standards.domain.inventory.InventoryConstants;
import com.sagecrest.standards.domain.inventory.InventoryItem;
import com.sagecrest.standards.domain.inventory.Status;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.FakeExchange;
import com.sagecrest.standards.web.routing.Router;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InventoryEndpointsTest {

  private static final String ALPHA = "Alpha";
  private static final String BRAVO = "Bravo";
  private static final int FEW = 2;
  private static final int MANY = 9;

  private static final String NO_QUERY = "/api/inventory";
  private static final String DESCENDING = "/api/inventory?sort=name&direction=descending";
  private static final String SEARCH_ALPHA = "/api/inventory?search=alpha";
  private static final String BAD_SORT = "/api/inventory?sort=colour";
  private static final String BAD_DIRECTION = "/api/inventory?direction=sideways";

  private static final String ONE_SHOWN = "\"shown\":1";
  private static final String TWO_IN_TOTAL = "\"total\":2";

  private static final InventoryStore STORE =
      () ->
          List.of(
              new InventoryItem(BRAVO, MANY, new Status(InventoryConstants.STATUS_IN_STOCK)),
              new InventoryItem(ALPHA, FEW, new Status(InventoryConstants.STATUS_LOW)));

  private static FakeExchange queried(String path) throws IOException {
    FakeExchange exchange = new FakeExchange(WebConstants.METHOD_GET, path);
    new Router(new InventoryEndpoints(new InventoryService(STORE)).routes()).handle(exchange);
    return exchange;
  }

  @Test
  @DisplayName("orders by name ascending when the request names neither")
  void ordersByNameAscendingByDefault() throws IOException {
    FakeExchange exchange = queried(NO_QUERY);

    assertThat(exchange.status()).isEqualTo(WebConstants.STATUS_OK);
    assertThat(exchange.responseText().indexOf(ALPHA))
        .as("Alpha precedes Bravo in the body, whatever order the store returned")
        .isLessThan(exchange.responseText().indexOf(BRAVO));
  }

  @Test
  void reversesTheOrderWhenAsked() throws IOException {
    String body = queried(DESCENDING).responseText();

    assertThat(body.indexOf(BRAVO)).isLessThan(body.indexOf(ALPHA));
  }

  @Test
  @DisplayName("counts what it showed against what exists")
  void countsWhatItShowedAgainstWhatExists() throws IOException {
    assertThat(queried(SEARCH_ALPHA).responseText()).contains(ONE_SHOWN, TWO_IN_TOTAL);
  }

  @Test
  void refusesAColumnAndAnOrderItDoesNotPublish() {
    assertThatThrownBy(() -> queried(BAD_SORT))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).getMessage())
        .isEqualTo(InventoryConstants.MSG_INVALID_SORT);
    assertThatThrownBy(() -> queried(BAD_DIRECTION))
        .isInstanceOf(ValidationException.class)
        .extracting(failure -> ((ValidationException) failure).getMessage())
        .isEqualTo(InventoryConstants.MSG_INVALID_DIRECTION);
  }
}
