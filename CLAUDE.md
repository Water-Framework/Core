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
