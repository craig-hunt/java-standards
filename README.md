# Java Standards

A working service, not a style guide. Every standard below is enforced by
something that fails the build, and the code is here so you can read the
standard rather than take its word for it.

Java 21, Maven, PostgreSQL. No application framework.

## Running it

```
docker compose up --build
```

The migrator applies the schema and exits, then the API starts on port 8080.

```
curl localhost:8080/health
curl -H 'Authorization: Bearer local-development-token' localhost:8080/api/inventory
```

Every gate, in one command:

```
./scripts/verify.sh
```

That needs JDK 21 and Maven 3.9 on PATH, plus a Docker daemon for the
integration tests. On a machine without the Java toolchain:

```
./scripts/verify-in-docker.sh
```

## The standards

**1. Dependencies point inward, and a test says so.** `domain` knows nothing of
`application`, `application` owns the ports `infrastructure` implements, and
`web` wires them. Maven's module graph forbids the obvious reversal. ArchUnit
covers what the module graph cannot say: that `domain` reaches nothing but the
language, that `java.sql` stays out of it even though it needs no dependency to
reach, and that nothing outside `web` knows an HTTP request exists.

**2. A value with rules has a type.** A `TaskId` is not a `long`, so passing a
signup identifier where a task identifier belongs does not compile. A
`TaskTitle` cannot exist untrimmed, over-long, or empty, because the record's
compact constructor is the only way in.

**3. Validation happens at construction, and there is one way in.** The C#
sibling needs a second unvalidated factory on each identifier, because Entity
Framework round-trips a key through its converter while deciding whether the key
has been set. Nothing here maps through a converter, so each type keeps one
constructor and no bypass.

**4. Every literal with a meaning has a name.** Enforced by a test that parses
every source file, main and test alike, and reports any string, number or
character outside a `static final` initializer. Zero, one and the empty string
are excused, because naming them produces `ZERO` and `EMPTY` and teaches people
to ignore the rule. `serialVersionUID` does not count as a name, so a value
cannot park there to satisfy the check.

**5. Failures are a sealed hierarchy, and one switch maps them.** `DomainException`
permits exactly two subtypes, so the mapper at the edge switches over the whole
hierarchy with no default arm. A third category of failure stops the build there
until somebody decides what status it deserves, which is the opposite of what a
default arm does.

**6. Failures are unchecked.** A checked domain failure asks every caller between
the throw and the edge to declare something it cannot act on, and the usual
answer is a catch that rethrows. A test asserts that every `Throwable` this
codebase defines extends `RuntimeException`.

**7. One place turns a failure into a response.** No endpoint carries a
try/catch. A per-endpoint catch is a per-endpoint chance to answer 200 for a
failure or to put a stack trace in a body.

**8. Every failure answers in one shape.** RFC 9457 problem details, with the
stable code and the per-field problems as extension members, on
`application/problem+json`. The Go sibling answers with a bare error object; this
is the documented divergence, and it keeps the machine-readable parts a client
already matches on.

**9. Validation reports every problem at once.** A form marks all its bad fields
in one round trip rather than one field per submission.

**10. Ports belong to the caller.** `TaskStore` lives in `application` beside the
service that calls it, not beside the JDBC class that implements it, so
`infrastructure` depends on `application` and never the reverse. A test asserts
that everything in `application.ports` is an interface.

**11. Migration is an admin process.** A separate entry point applies the schema
and exits. The API never migrates at startup, so two replicas starting together
cannot run the same DDL at the same time.

**12. State and the event announcing it commit together.** Recording a signup
writes the signup row and the outbox row in one transaction. Either both land or
neither does, so no consumer hears about a signup that failed to store and no
stored signup goes unannounced.

**13. Consumers absorb repeats.** The relay publishes after the commit, which
makes delivery at-least-once: it can deliver a message and fail before recording
that it did. The claim runs `FOR UPDATE SKIP LOCKED`, so a second replica takes a
different batch rather than manufacturing a duplicate on every pass.

**14. A message the deployment cannot resolve stays pending.** Marking it would
acknowledge something no consumer saw, which is the one outcome an outbox exists
to prevent. The wire name is a literal rather than a class name, so renaming a
class does not strand a backlog.

**15. Requests run on virtual threads, and the ports block.** The C# sibling
threads a `CancellationToken` through every signature because a .NET request
thread is too expensive to park on a database. Parking a virtual thread costs a
heap object, so the ports return plain values and the handlers read like
straight-line code. The asynchrony moved to the scheduler, which is the only
component that needed to know about it.

**16. A connection pool is still a ceiling.** Virtual threads make ten thousand
requests cheap to queue for ten connections. They do not make it twelve
thousand. Sizing the pool stays a decision somebody makes on purpose.

**17. The clock, the executor and the environment arrive as dependencies.**
`java.time.Clock` is the JDK's own seam, so there is no `IClock` port here and a
test uses `Clock.fixed`. The readiness deadline takes an `ExecutorService`
rather than creating one per probe.

**18. Authentication wraps the context, not the route.** The bearer check sits
around the whole `/api` context, so a route added later cannot be published
without a token. The comparison runs through `MessageDigest.isEqual`, which does
not stop at the first differing byte.

**19. Liveness and readiness answer different questions.** Liveness answers as
long as the process can serve a request. Readiness answers for the dependencies.
Collapsing them turns a database outage into a restart loop.

**20. Every request carries an identifier.** `X-Request-ID` is accepted when it
is short and made of safe characters, and replaced when it is not, because an
unfiltered value travels into both the log and a response header. It is removed
from the logging context in a `finally`, so no thread stamps the next request
with the previous one's identifier.

**21. Warnings fail the build.** `-Xlint:all -Werror`, with one category off and
its reason written where the exclusion lives. This is not decorative: it caught a
generic-array warning in the conventions module and forced a better
implementation.

**22. One authority on formatting, bound to the build.** google-java-format
through Spotless, chosen for having no options worth arguing about.
`verify` fails on an unformatted file rather than rewriting it, because a build
that edits the tree produces different output on a second run. `scripts/format.sh`
does the editing, on purpose.

**23. Tests read as behavior statements, and mutation analysis checks that they
mean it.** PIT, failing the build under 70%. This is not a formality either:
mutation analysis found that none of the `TaskId.parse` tests contained a `0` or
a `9` in a non-leading position, so both ends of the digit-range check were
untested while the suite was green.

**24. Integration tests run against the real engine.** Testcontainers starts and
removes its own PostgreSQL. Everything worth testing in a store is
engine-specific: `RETURNING`, `FOR UPDATE SKIP LOCKED`, `jsonb`, a partial index.
A fake would agree with the code and disagree with production.

**25. The wiring is one readable method.** No dependency-injection container.
Every adapter is constructed once, in order, where a reader can see which
implementation satisfies which port. The first question anybody asks of a
repository like this is exactly the one a container hides.

**26. Packages are named for what they do.** A test rejects `util`, `helper`,
`common`, `misc`, `impl` and the rest. A package nobody can describe is a package
everything belongs in.

**27. A concrete class is final.** A class nobody marked final can be subclassed
to defeat the rule it was written to enforce. Make it final, or make it abstract
on purpose.

**28. Every version lives in one place.** The parent POM, the way
`Directory.Packages.props` works in the C# sibling. A module that pinned its own
version would let two modules disagree about one dependency with nothing to say
so.

**29. Comments explain why, never what.** Names carry the what. The comments here
record the reasoning a reader would otherwise have to reconstruct, and several of
them exist because the obvious alternative is wrong in a way that is not obvious.

## Layout

```
domain/          the rules, depending on nothing
application/     what the service does, and the ports it needs
infrastructure/  the adapters, and every dependency that leaves the process
web/             routing, JSON, problem details, and the wiring
conventions/     the standards above, as tests that fail
```

## API

| Method | Path                 | Answers                                  |
| ------ | -------------------- | ---------------------------------------- |
| GET    | `/health`            | liveness, no token                       |
| GET    | `/health/ready`      | readiness, no token                      |
| GET    | `/api/tasks`         | the task list, `?filter=all\|active\|completed` |
| POST   | `/api/tasks`         | creates a task                           |
| PATCH  | `/api/tasks/{id}`    | sets completion                          |
| DELETE | `/api/tasks/{id}`    | removes one task                         |
| DELETE | `/api/tasks`         | removes the completed ones               |
| POST   | `/api/signups`       | records a signup                         |
| GET    | `/api/inventory`     | stock, `?search=&sort=&direction=`       |

## Configuration

| Variable            | Required | Meaning                         |
| ------------------- | -------- | ------------------------------- |
| `DATABASE_URL`      | yes      | JDBC URL                        |
| `DATABASE_USER`     | yes      | database user                   |
| `DATABASE_PASSWORD` | yes      | database password               |
| `API_TOKEN`         | yes      | the bearer token `/api` expects |
| `PORT`              | no       | defaults to 8080                |

All of them are checked at startup. A missing password found on the first
request is an outage; found at startup it is a deployment that never went live.

## Divergences from the Go and C# siblings

Each one is a place where Java's answer is genuinely different, rather than a
place where this repository disagrees about the standard.

- **No framework at the edge.** The C# sibling uses Minimal APIs because
  ASP.NET Core *is* the platform there. Java's platform HTTP server
  (`jdk.httpserver`) is deliberately minimal, so this adds a small router of its
  own rather than a framework, the way the Go sibling uses `net/http`. A
  framework would supply its own opinions about routing, binding and error
  mapping, and those opinions are what this repository exists to state for
  itself. The conventions carry over to Spring Boot unchanged; what would not
  carry over is a reader's ability to see them.

- **Blocking ports instead of async signatures.** Standard 15. The C# ports
  return `Task` and take a `CancellationToken`; these return values and take
  neither.

- **No clock port.** The JDK ships `java.time.Clock`. Inventing an `IClock`
  equivalent would add a port whose only implementation delegates to the
  platform.

- **Two concrete failure types instead of one per failure.** The C# sibling gives
  each failure its own exception class, which its edge needs because it switches
  on the type to pick a status. The sealed base here already splits the two
  categories a status depends on, so the named factories beside each feature
  supply the readable construction site, and tests assert on the code a client
  matches rather than on a class name no client can see.

- **A class token on the typed consumer.** The C# base writes `domainEvent is
  TEvent` because .NET generics survive to runtime. Java erases them, so a
  subclass states its event type once as a constructor argument. Erasure also
  decides a method name: a protected `consume(E)` beside the public
  `consume(DomainEvent)` does not compile, because both erase to the same
  signature.

- **Contracts carry primitives.** The C# sibling installs JSON converters so a
  `TaskId` writes as a number. Converters work, but they put the wire shape
  somewhere a reader of the contract cannot see it. Declaring `long id` states it
  in the type that defines it.

- **The signup request binds to a domain type.** The one place a domain type is
  allowed on the wire. A validation failure answers with problems keyed by field
  name, and those keys have to be the names the client sent; a separate contract
  would let the two drift, and the drift would surface as a form that cannot find
  the input a message belongs to.

- **No JWT.** The siblings also accept a signed token. Doing that in Java means
  taking on a JWT library, and verifying a signature badly is worse than not
  offering the option, so this surface accepts the static token only.

- **The literal rule is a test, not a compiler plugin.** The C# sibling ships a
  Roslyn analyzer, which runs at compile time because Roslyn is already in the
  build. The Java equivalent, an Error Prone check, costs the build a set of
  `--add-exports` flags into javac's internals and moves the rule somewhere a
  reader cannot run on its own. This walks the AST from a test, as the Go sibling
  does.

- **No row types.** The C# stores map through mutable row classes because Entity
  Framework tracks changes on them. JDBC hands back a result set and `RETURNING`
  hands back the assigned key in the same round trip, so the mapping reads
  straight into the domain type. The outbox message is the exception: a stored
  event really is a wire format, holding a type name and a JSON string that no
  domain type should know about.

- **Testcontainers 1.21.4, not 2.x.** The 2.x major renames the BOM modules this
  build imports, so moving to it is a migration with its own verification rather
  than a version bump. Not 1.19.x either: Docker Desktop 29 answers the older
  client with a 400 and Testcontainers reports no valid Docker environment, which
  reads as a broken machine rather than a stale dependency.

## Adopting this

Take the standards and the conventions module. The three domains here
(tasks, signups, stock) exist so the structure has something to hold, and they
match the C# sibling so the two can be compared line for line. They are not the
point.
