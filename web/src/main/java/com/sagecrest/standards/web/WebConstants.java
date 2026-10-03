package com.sagecrest.standards.web;

/** Every literal the HTTP surface carries, named once. */
public final class WebConstants {

  public static final String PATH_API = "/api";
  public static final String PATH_TASKS = "/api/tasks";
  public static final String PATH_SIGNUPS = "/api/signups";
  public static final String PATH_INVENTORY = "/api/inventory";
  public static final String PATH_HEALTH = "/health";
  public static final String PATH_HEALTH_READY = "/health/ready";

  public static final String QUERY_FILTER = "filter";
  public static final String QUERY_SEARCH = "search";
  public static final String QUERY_SORT = "sort";
  public static final String QUERY_DIRECTION = "direction";

  public static final String HEADER_REQUEST_ID = "X-Request-ID";
  public static final String HEADER_AUTHORIZATION = "Authorization";
  public static final String HEADER_WWW_AUTHENTICATE = "WWW-Authenticate";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_ALLOW = "Allow";

  public static final int MAX_REQUEST_ID_LENGTH = 64;
  public static final int MAX_BODY_BYTES = 1_048_576;

  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String CONTENT_TYPE_PROBLEM = "application/problem+json";

  public static final String METHOD_GET = "GET";
  public static final String METHOD_POST = "POST";
  public static final String METHOD_PATCH = "PATCH";
  public static final String METHOD_DELETE = "DELETE";

  public static final int STATUS_OK = 200;
  public static final int STATUS_CREATED = 201;
  public static final int STATUS_NO_CONTENT = 204;
  public static final int STATUS_BAD_REQUEST = 400;
  public static final int STATUS_UNAUTHORIZED = 401;
  public static final int STATUS_NOT_FOUND = 404;
  public static final int STATUS_METHOD_NOT_ALLOWED = 405;
  public static final int STATUS_UNPROCESSABLE = 422;
  public static final int STATUS_INTERNAL = 500;
  public static final int STATUS_UNAVAILABLE = 503;

  public static final String TITLE_BAD_REQUEST = "Bad Request";
  public static final String TITLE_UNAUTHORIZED = "Unauthorized";
  public static final String TITLE_NOT_FOUND = "Not Found";
  public static final String TITLE_METHOD_NOT_ALLOWED = "Method Not Allowed";
  public static final String TITLE_UNPROCESSABLE = "Unprocessable Content";
  public static final String TITLE_INTERNAL = "Internal Server Error";
  public static final String TITLE_UNAVAILABLE = "Service Unavailable";

  public static final String CODE_UNAUTHORIZED = "unauthorized";
  public static final String CODE_NOT_FOUND = "not_found";
  public static final String CODE_METHOD_NOT_ALLOWED = "method_not_allowed";

  public static final String MSG_UNAUTHORIZED = "a valid bearer token is required";
  public static final String MSG_NO_SUCH_ROUTE = "no route answers that path";
  public static final String MSG_METHOD_NOT_ALLOWED = "that path does not answer that method";
  public static final String MSG_REQUEST_FAILED = "request {} failed";
  public static final String MSG_NOT_READY = "readiness check failed";
  public static final String MSG_LISTENING = "listening on port {}";

  public static final String SCHEME_BEARER_PREFIX = "Bearer ";
  public static final String CHALLENGE_BEARER = "Bearer";

  public static final String ENV_PORT = "PORT";
  public static final String ENV_DATABASE_URL = "DATABASE_URL";
  public static final String ENV_DATABASE_USER = "DATABASE_USER";
  public static final String ENV_DATABASE_PASSWORD = "DATABASE_PASSWORD";
  public static final String ENV_API_TOKEN = "API_TOKEN";

  public static final String MSG_MISSING_SETTING = "the service needs the %s environment variable";
  public static final String MSG_TOKEN_HAS_SEPARATOR =
      "the API token must not contain a period, which would make it indistinguishable from a JSON Web Token";
  public static final String MSG_PORT_NOT_A_NUMBER =
      "the PORT environment variable must be a number";

  public static final char JWT_SEPARATOR = '.';

  private WebConstants() {}
}
