package com.sagecrest.standards.web;

import com.sagecrest.standards.application.events.FanOutEventDispatcher;
import com.sagecrest.standards.application.health.HealthService;
import com.sagecrest.standards.application.inventory.InventoryService;
import com.sagecrest.standards.application.ports.EventConsumer;
import com.sagecrest.standards.application.signups.SignupService;
import com.sagecrest.standards.application.tasks.TaskService;
import com.sagecrest.standards.infrastructure.events.OutboxPublisher;
import com.sagecrest.standards.infrastructure.events.OutboxRelay;
import com.sagecrest.standards.infrastructure.events.SignupRecordedConsumer;
import com.sagecrest.standards.infrastructure.events.TaskCompletedConsumer;
import com.sagecrest.standards.infrastructure.health.JdbcHealthProbe;
import com.sagecrest.standards.infrastructure.persistence.ConnectionPool;
import com.sagecrest.standards.infrastructure.persistence.Database;
import com.sagecrest.standards.infrastructure.stores.JdbcInventoryStore;
import com.sagecrest.standards.infrastructure.stores.JdbcSignupStore;
import com.sagecrest.standards.infrastructure.stores.JdbcTaskStore;
import com.sagecrest.standards.web.auth.BearerTokenHandler;
import com.sagecrest.standards.web.endpoints.HealthEndpoints;
import com.sagecrest.standards.web.endpoints.InventoryEndpoints;
import com.sagecrest.standards.web.endpoints.SignupEndpoints;
import com.sagecrest.standards.web.endpoints.TaskEndpoints;
import com.sagecrest.standards.web.errors.FailureHandler;
import com.sagecrest.standards.web.routing.RequestIdHandler;
import com.sagecrest.standards.web.routing.Route;
import com.sagecrest.standards.web.routing.Router;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The whole wiring, in one readable method.
 *
 * <p>No dependency-injection container. Every adapter is constructed once, in order, where a reader
 * can see which implementation satisfies which port. A container would move those decisions into
 * annotations and a scan, and the first question anybody asks of this repository is exactly the one
 * a container hides.
 *
 * <p>Authentication wraps the {@code /api} context rather than each route, so a route added later
 * cannot be published without a token.
 */
public final class Service implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(Service.class);

  private static final String PATH_ROOT = "/";
  private static final int BACKLOG_DEFAULT = 0;
  private static final int SHUTDOWN_SECONDS = 2;
  private static final Duration RELAY_INTERVAL = Duration.ofSeconds(5);

  private final HttpServer server;
  private final HikariDataSource pool;
  private final OutboxRelay relay;
  private final ExecutorService probes;
  private final ExecutorService requests;

  public static void main(String[] args) throws IOException {
    Service service = new Service(Settings.fromEnvironment());
    Runtime.getRuntime().addShutdownHook(new Thread(service::close));
    service.start();
  }

  public Service(Settings settings) throws IOException {
    // No schema work here. Migrator applies it and exits before any replica starts,
    // so two replicas rolling out together cannot run the same DDL at once.
    pool =
        ConnectionPool.open(
            settings.database().url(), settings.database().user(), settings.database().password());
    Database database = new Database(pool);

    Clock clock = Clock.systemUTC();
    TaskService tasks = new TaskService(new JdbcTaskStore(database, clock));
    SignupService signups = new SignupService(new JdbcSignupStore(database, clock));
    InventoryService inventory = new InventoryService(new JdbcInventoryStore(database));

    probes = Executors.newVirtualThreadPerTaskExecutor();
    HealthService health = new HealthService(new JdbcHealthProbe(database), probes);

    List<EventConsumer> consumers =
        List.of(new SignupRecordedConsumer(), new TaskCompletedConsumer());
    relay =
        new OutboxRelay(
            new OutboxPublisher(database, new FanOutEventDispatcher(consumers), clock),
            Executors.newSingleThreadScheduledExecutor(),
            RELAY_INTERVAL);

    List<Route> api = new ArrayList<>();
    api.addAll(new TaskEndpoints(tasks).routes());
    api.addAll(new SignupEndpoints(signups).routes());
    api.addAll(new InventoryEndpoints(inventory).routes());

    // Virtual threads, one per request. Every port below blocks, which is what makes the
    // handlers readable; parking a virtual thread on a query costs a heap object rather
    // than an OS thread, so the readable shape is also the cheap one.
    requests = Executors.newVirtualThreadPerTaskExecutor();

    server = HttpServer.create(new InetSocketAddress(settings.port()), BACKLOG_DEFAULT);
    server.setExecutor(requests);
    server.createContext(
        WebConstants.PATH_API,
        guarded(new BearerTokenHandler(new Router(api), settings.apiToken())));
    server.createContext(
        WebConstants.PATH_HEALTH, guarded(new Router(new HealthEndpoints(health).routes())));
    // Anything outside the two contexts above answers with the same problem shape as a bad
    // path inside them. Without this the server's own handler answers an empty 404, and a
    // client parsing problem JSON gets nothing to parse.
    server.createContext(PATH_ROOT, guarded(new Router(List.of())));
  }

  public void start() {
    relay.start();
    server.start();
    LOG.info(WebConstants.MSG_LISTENING, server.getAddress().getPort());
  }

  public int port() {
    return server.getAddress().getPort();
  }

  @Override
  public void close() {
    server.stop(SHUTDOWN_SECONDS);
    relay.close();
    probes.shutdownNow();
    requests.shutdownNow();
    pool.close();
  }

  /** Request identity first, so a rejected request still carries one, then failure mapping. */
  private static HttpHandler guarded(HttpHandler handler) {
    return new RequestIdHandler(new FailureHandler(handler));
  }
}
