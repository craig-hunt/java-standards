package com.sagecrest.standards.web.routing;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * An {@link HttpExchange} a test can drive without a socket.
 *
 * <p>Written by hand rather than mocked. The handlers under test read four things from an exchange
 * and write three, and a double that records those is both shorter than the mock setup would be and
 * readable as a description of what a request is.
 *
 * <p>It exists because the edge had no unit tests at all, only end-to-end ones. That gap was not
 * merely a coverage number: mutation analysis of a class reachable solely over HTTP produces
 * mutants that hang the client rather than failing it, PIT scores the hang as a kill, and the gate
 * reports a healthy figure for code nothing examined.
 *
 * <p>Everything the handlers never touch throws rather than answering a plausible default, so a
 * test that starts depending on one finds out instead of quietly passing.
 */
public final class FakeExchange extends HttpExchange {

  private static final int NOT_YET_SENT = -1;
  private static final String UNUSED = "the handlers under test do not read this";

  private final String method;
  private final URI uri;
  private final Headers requestHeaders = new Headers();
  private final Headers responseHeaders = new Headers();
  private final InputStream requestBody;
  private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();

  private int status = NOT_YET_SENT;
  private long declaredLength;

  public FakeExchange(String method, String path) {
    this(method, path, "");
  }

  public FakeExchange(String method, String path, String body) {
    this.method = method;
    this.uri = URI.create(path);
    this.requestBody = new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
  }

  public FakeExchange withRequestHeader(String name, String value) {
    requestHeaders.set(name, value);
    return this;
  }

  public int status() {
    return status;
  }

  public String responseText() {
    return responseBody.toString(StandardCharsets.UTF_8);
  }

  public String responseHeader(String name) {
    return responseHeaders.getFirst(name);
  }

  public long declaredLength() {
    return declaredLength;
  }

  @Override
  public Headers getRequestHeaders() {
    return requestHeaders;
  }

  @Override
  public Headers getResponseHeaders() {
    return responseHeaders;
  }

  @Override
  public URI getRequestURI() {
    return uri;
  }

  @Override
  public String getRequestMethod() {
    return method;
  }

  @Override
  public InputStream getRequestBody() {
    return requestBody;
  }

  @Override
  public OutputStream getResponseBody() {
    return responseBody;
  }

  @Override
  public void sendResponseHeaders(int code, long length) {
    status = code;
    declaredLength = length;
  }

  @Override
  public int getResponseCode() {
    return status;
  }

  @Override
  public void close() {
    // Nothing to release: the streams are in memory and the test reads them afterwards.
  }

  @Override
  public HttpContext getHttpContext() {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public InetSocketAddress getLocalAddress() {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public String getProtocol() {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public Object getAttribute(String name) {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public void setAttribute(String name, Object value) {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public void setStreams(InputStream in, OutputStream out) {
    throw new UnsupportedOperationException(UNUSED);
  }

  @Override
  public HttpPrincipal getPrincipal() {
    throw new UnsupportedOperationException(UNUSED);
  }
}
