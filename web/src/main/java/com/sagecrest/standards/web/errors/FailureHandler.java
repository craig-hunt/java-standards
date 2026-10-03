package com.sagecrest.standards.web.errors;

import com.sagecrest.standards.domain.errors.DomainException;
import com.sagecrest.standards.domain.errors.ErrorConstants;
import com.sagecrest.standards.domain.errors.NotFoundException;
import com.sagecrest.standards.domain.errors.ValidationException;
import com.sagecrest.standards.web.WebConstants;
import com.sagecrest.standards.web.routing.Responses;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Turns every failure into one response shape, in one place.
 *
 * <p>No endpoint carries a try/catch, which is the point: a per-endpoint catch is a per-endpoint
 * opportunity to answer 200 for a failure, or to leak a stack trace into a body.
 *
 * <p>The switch over {@link DomainException} has no default arm. The hierarchy is sealed, so adding
 * a third category of domain failure stops the build here until somebody decides what status it
 * deserves. A default arm would instead have quietly mapped it to whatever the last author chose.
 *
 * <p>An unexpected failure is logged with its stack and answered with a generic detail. The detail
 * a client receives says nothing about the internals, because a message naming a table or a driver
 * tells an attacker more than it tells the caller.
 */
public final class FailureHandler implements HttpHandler {

  private static final Logger LOG = LoggerFactory.getLogger(FailureHandler.class);

  private final HttpHandler next;

  public FailureHandler(HttpHandler next) {
    this.next = next;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      next.handle(exchange);
    } catch (DomainException failure) {
      answer(exchange, map(failure));
    } catch (InvalidRequestBodyException malformed) {
      answer(
          exchange,
          Problem.of(
              WebConstants.STATUS_BAD_REQUEST,
              WebConstants.TITLE_BAD_REQUEST,
              ErrorConstants.CODE_INVALID_BODY,
              ErrorConstants.MSG_INVALID_BODY));
    } catch (IOException | RuntimeException unexpected) {
      LOG.error(WebConstants.MSG_REQUEST_FAILED, exchange.getRequestURI(), unexpected);
      answer(
          exchange,
          Problem.of(
              WebConstants.STATUS_INTERNAL,
              WebConstants.TITLE_INTERNAL,
              ErrorConstants.CODE_INTERNAL,
              ErrorConstants.MSG_INTERNAL));
    }
  }

  private static Problem map(DomainException failure) {
    return switch (failure) {
      case ValidationException invalid ->
          Problem.of(
              WebConstants.STATUS_UNPROCESSABLE,
              WebConstants.TITLE_UNPROCESSABLE,
              invalid.code(),
              invalid.getMessage(),
              invalid.fields());
      case NotFoundException missing ->
          Problem.of(
              WebConstants.STATUS_NOT_FOUND,
              WebConstants.TITLE_NOT_FOUND,
              missing.code(),
              missing.getMessage());
    };
  }

  private static void answer(HttpExchange exchange, Problem problem) throws IOException {
    if (Responses.alreadyStarted(exchange)) {
      return;
    }
    Responses.problem(exchange, problem);
  }
}
