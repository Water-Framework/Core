# Core Module — Water Framework Core Abstractions

## Purpose
Provides all foundational abstractions, interfaces, and utilities for the Water Framework. Every other Water module depends on Core. Defines the component lifecycle (`@FrameworkComponent`, `@Inject`, `@OnActivate/@OnDeactivate`), the permission system (`@AccessControl`, `PermissionManager`), the base entity hierarchy, the query builder, and the test extension. Does NOT provide any runtime implementations — those live in the `Implementation` module (OSGi or Spring).

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `Core-api` | All | `BaseEntity`, `BaseEntityApi`, `BaseEntitySystemApi`, `BaseRepository`, `RestApi`, `ComponentRegistry`, `PermissionManager`, `QueryBuilder` |
| `Core-model` | All | `AbstractEntity`, `WaterException`, `WaterRuntimeException`, `ValidationException`, `UnauthorizedException` |
| `Core-bundle` | All | `WaterRuntime`, `ApplicationInitializer`, `ApplicationProperties` |
| `Core-interceptors` | All | `@AllowPermissions`, `@AllowRoles`, `@AllowPermissionsOnReturn`, `MethodInterceptor`, `InterceptorExecutor` |
| `Core-permission` | All | `@AccessControl`, `@DefaultRoleAccess`, `CrudActions`, `Action`, `PermissionUtil` |
| `Core-registry` | All | `ComponentRegistration`, `ComponentFilter`, `ComponentConfiguration`, `ComponentFilterBuilder` |
| `Core-security` | All | `WaterAbstractSecurityContext`, `SecurityContext`, `EncryptionUtil` |
| `Core-service` | All | `BaseEntityServiceImpl`, `BaseEntitySystemServiceImpl` |
| `Core-testing-utils` | Test | `WaterTestExtension`, `TestComponentRegistry`, `TestRuntimeInitializer`, `TestRuntimeUtils` |
| `Core-validation` | All | `@NoMalitiusCode`, `@NotNullOnPersist`, `@ValidPassword` |

## Entity Hierarchy

```java
Resource                                   // Marker: all managed objects
  └─ BaseEntity extends Resource           // Adds: id, createDate, modifyDate, version
       └─ AbstractEntity implements BaseEntity
            └─ AbstractJpaEntity (in JpaRepository module)
                 └─ AbstractJpaExpandableEntity  // Supports dynamic field extensions
```

### BaseEntity Interface
```java
public interface BaseEntity extends Resource {
    long getId();
    Date getEntityCreateDate();
    Date getEntityModifyDate();
    Integer getEntityVersion();
    boolean isExpandableEntity();
    long[] getCategoryIds();
    void setCategoryIds(long[] categoryIds);
    long[] getTagIds();
    void setTagIds(long[] tagIds);
}
```

## ComponentRegistry

Central service locator and lifecycle manager. Both OSGi and Spring implementations are in the `Implementation` module.

```java
public interface ComponentRegistry {
    <T> List<T> findComponents(Class<T> componentClass, ComponentFilter filter);
    <T> T findComponent(Class<T> componentClass, ComponentFilter filter);
    <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass,
                                                          T component,
                                                          ComponentConfiguration configuration);
    <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration);
    ComponentFilterBuilder getComponentFilterBuilder();
    <T extends BaseEntitySystemApi> T findEntitySystemApi(String entityClassName);
    <T extends BaseRepository> T findEntityRepository(String entityClassName);
    <T extends BaseEntity> BaseRepository<T> findEntityExtensionRepository(Class<T> entityClass);
}
```

## BaseRepository Interface

```java
public interface BaseRepository<T extends BaseEntity> {
    T persist(T entity);
    T persist(T entity, Runnable postPersistAction);
    T update(T entity);
    T update(T entity, Runnable postUpdateAction);
    void remove(long id);
    void remove(T entity);
    void removeAllByIds(Iterable<Long> ids);
    void removeAll();
    T find(long id);
    T find(Query filter);
    T find(String hqlFilter);
    PaginableResult<T> findAll(int delta, int page, Query filter, QueryOrder order);
    long countAll(Query filter);
    QueryBuilder getQueryBuilderInstance();
}
```

## Permission System

### @AccessControl Annotation
```java
@AccessControl(
    availableActions = {CrudActions.class},         // or mix: {CrudActions.FIND, MyActions.class}
    rolesPermissions = {
        @DefaultRoleAccess(roleName = "myManager",  actions = {CrudActions.class}),
        @DefaultRoleAccess(roleName = "myViewer",   actions = {CrudActions.FIND, CrudActions.FIND_ALL}),
        @DefaultRoleAccess(roleName = "myEditor",   actions = {CrudActions.SAVE, CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL})
    }
)
```

### CrudActions (standard bitmask values)
| Action | Bitmask |
|---|---|
| SAVE | 1 |
| UPDATE | 2 |
| REMOVE | 4 |
| FIND | 8 |
| FIND_ALL | 16 |

### PermissionManager
```java
public interface PermissionManager extends Service {
    boolean userHasRoles(String username, String[] rolesNames);
    void addPermissionIfNotExists(Role role, Class<? extends Resource> resource, Action action);
    boolean checkPermission(String username, Resource entity, Action action);
    boolean checkPermission(String username, Class<? extends Resource> resource, Action action);
    boolean checkPermissionAndOwnership(String username, String resourceName, Action action, Resource... entities);
    boolean checkUserOwnsResource(User user, Object resource);
    static boolean isProtectedEntity(Object entity);
    Map<String, Map<String, Map<String, Boolean>>> entityPermissionMap(String username, Map<String, List<Long>> entityPks);
}
```

## Traffic Capture (telemetry)

Off by default. Two switches, both in `Core-service/src/main/resources/it.water.application.properties`:

| Property | Default | Effect |
|---|---|---|
| `water.traffic.enabled` | `false` | master switch of the reporter: with this off nothing is ever reported |
| `water.traffic.s2s.enabled` | `false` | Service-to-Service capture |
| `water.traffic.s2s.exclude` | *(empty)* | comma-separated `Class` or `Class#method` prefixes to exclude |
| `water.traffic.events.*` | see file | CRUD/domain-event capture (opt-in whitelist, captures nothing by default) |
| `water.traffic.rest.*` | enabled | inbound REST capture |

### The S2S capture is opt-out, within a fixed scope

Once enabled it reports **every method declared by one of the four architectural layers** —
`RestApi`, `BaseApi`, `BaseSystemApi`, `BaseRepository` — because those are exactly the boundaries a
request crosses. The output is therefore the FLOW of a request (REST → Api → SystemApi → repository),
not a log of every call in the process. Anything else is out of scope by construction: a component's
own helper interfaces, any plain `Service`, the framework's plumbing, and the traffic pipeline itself.

The scope is per **method**, not per component: a class that sits on a layer and also exposes methods
of its own does not get those reported.

```java
@NoTraffic                              // on a method, or on a type for all of its methods
public String hotAccessor() { ... }

@ReportTraffic(operation = "lend")      // does NOT enable capture: only renames the operation
public void lendBook(long id) { ... }
```

Records carry `parentId` + `depth`, so nested calls form a call tree instead of a flat set of records
sharing a `correlationId`. A failed call is captured too, with `outcome=ERROR` plus `errorType` /
`errorMessage`, and the exception reaches the caller unchanged.

### Recommended configuration when you turn it on

The defaults ship everything OFF on purpose — the pipeline is additive and must not change anyone's
performance profile without an explicit choice. A sensible starting point for a deployment that wants
request flows:

```properties
water.traffic.enabled=true            # master switch
water.traffic.s2s.enabled=true        # flow across the four layers
water.traffic.events.enabled=false    # CRUD events: redundant here, the repository layer is
                                      # already captured by the S2S hop
water.traffic.sampling.rate=0.1       # thin out per correlationId: a whole request tree is kept
                                      # or dropped together, never half of it
water.events.listeners.cache.ttl.ms=1000   # only when components do not change after startup
```

`water.traffic.payload.capture` stays `false`: payloads are never captured unless explicitly opted in.

### Is it working? Ask the counters

`TrafficReporterStats` (ingress) and `TrafficPublisherStats` (egress) are resolvable from the registry
and answer the question a silent pipeline always raises:

| Symptom | Look at |
|---|---|
| `recordsReceived` is 0 | the capture: switch off, method outside the four layers, or excluded |
| received but not `recordsReported` | the sampling rate |
| reported but nothing arrives downstream | the publisher (`droppedOverflow`, `queueSize`) |
| `recordsFailed` > 0 | a defect: failures are swallowed to protect the caller, this is where they surface |

### Writing a global interceptor

`GlobalBeforeMethodInterceptor` / `GlobalAfterMethodInterceptor` (in `Core-api`) are invoked on
**every** intercepted method, with no annotation opting in. Three rules, learned the hard way:

- **Scope yourself explicitly.** The framework offers every method and does not filter. OSGi proxies
  interfaces only while the Spring pointcut also matches the target's own methods, so an unscoped
  implementation behaves differently per runtime — and since Water's own interceptors are `Service`s
  that the chain invokes while dispatching, it will also end up observing the machinery observing it.
- **Be cheap.** You are on the hottest path in the framework. Short-circuit on your own configuration
  before doing anything; prefer a property read to a registry lookup.
- **Never throw.** Not even indirectly: the hook runs on the calls a container makes while still
  wiring itself, where a registry lookup can fail with a raw NPE. Absorb everything.

Note that a global interceptor declaring both services is instantiated once per declared service, so
the registry returns several distinct instances of it; `WaterAbstractInterceptor` deduplicates them
**by class** before dispatching. Two deliberately different instances of the same interceptor class
would collapse into one — use two classes if you need two behaviours.

## Interceptor Annotations

```java
@AllowPermissions(actions = {"save"}, systemApiRef = MySystemApi.class)  // Permission check
@AllowRoles(rolesNames = {"adminRole", "managerRole"})                    // Role check
@AllowPermissionsOnReturn(actions = {"find"}, systemApiRef = MySystemApi.class)  // Post-method filter
```

## Service Layer Pattern

```java
// Api layer (permission-checked)
@FrameworkComponent
public class MyEntityServiceImpl extends BaseEntityServiceImpl<MyEntity>
    implements MyEntityApi {
    @Inject ComponentRegistry componentRegistry;
    @Inject MyEntitySystemApi systemApi;
}

// SystemApi layer (no auth, business logic here)
@FrameworkComponent
public class MyEntitySystemServiceImpl extends BaseEntitySystemServiceImpl<MyEntity>
    implements MyEntitySystemApi {
    @Inject MyEntityRepository repository;
}
```

## Testing Utilities

```java
// Standard test setup
@ExtendWith(WaterTestExtension.class)
class MyTest {
    @Inject static ComponentRegistry componentRegistry;
    @Inject static MyEntityApi myApi;

    @BeforeAll
    static void setup() {
        TestRuntimeUtils.impersonateAdmin(componentRegistry);
    }

    // After permission tests, restore admin:
    TestRuntimeUtils.impersonateAdmin(componentRegistry);

    // To test as a specific user:
    TestRuntimeInitializer.getInstance().impersonate(user, runtime);
}
```

## ApplicationProperties

```java
public class ApplicationProperties {
    String getProperty(String propertyName);
    String getProperty(String propertyName, String defaultValue);
    void setProperty(String propertyName, String value);
    void loadProperties(Properties properties);
}
```

## Dependencies
- `jakarta.validation:jakarta.validation-api` — JSR-303 annotations
- `jakarta.ws.rs:jakarta.ws.rs-api` — JAX-RS REST annotations
- `org.atteo.classindex:classindex` — compile-time classpath indexing
- `org.projectlombok:lombok` — boilerplate reduction
- `org.slf4j:slf4j-api` — logging abstraction
- `org.bouncycastle:bcprov-jdk18on` — cryptographic operations in `Core-security`

## Testing
- Always use `@ExtendWith(WaterTestExtension.class)` — never instantiate services manually
- `TestComponentRegistry` works without OSGi/Spring runtime
- REST tests: **Karate only** for any RestApi implementations
- After each permission test that changes the current user: restore admin with `TestRuntimeUtils.impersonateAdmin(componentRegistry)`

## Code Generation Rules
- NEVER depend on a specific runtime (OSGi/Spring) from within a module — depend only on Core interfaces
- `@FrameworkComponent` registers a class with the framework runtime — always required for services
- `@Inject` is Water's DI annotation — use it instead of Spring's `@Autowired` or CDI's `@Inject` in module code
- Custom actions beyond CRUD: extend `Action` enum in a `*Actions` class, add to `@AccessControl(availableActions=...)`
- `@NoMalitiusCode` — apply to all user-supplied String fields to prevent XSS/injection
- `@NotNullOnPersist` — use instead of `@NotNull` for fields that are nullable in DTOs but required at persistence time
