---
name: traffic-reporter-f2-domain-events
description: Unit-test patterns for Core-service it.water.core.service.traffic (TrafficReporter F2 domain-event pipeline) - TrafficDomainEventListener, WaterTrafficDomainEvent(Record)
metadata:
  type: project
---

## Context (as of 2026-07-27)

F2 phase of the TrafficReporter pipeline added domain-event capture: `TrafficDomainEventListener`
(implements `ApplicationEventListener<Resource>`), `WaterTrafficDomainEventRecord`,
`WaterTrafficDomainEvent`, plus two new `TrafficReporter.reportDomainEvent(...)` overloads on
`TrafficReporterImpl`. All in `it.water.core.service.traffic` (listener in the `.events` subpackage).

## Update (2026-07-27): end-to-end harness now exists

`InMemoryApplicationEventProducer` (`it.water.core.service.events`, `@FrameworkComponent(priority = 0)`)
landed and finally EMITS domain events toward `ApplicationEventListener`s, so the F2 loop can now be
closed end-to-end. It resolves the requested `Event` TYPE into an INSTANCE (a cached JDK proxy per
event `Class` when the type is an interface — the normal Water case — or a plain
`getConstructor().newInstance()` when a concrete class with an accessible default constructor is
passed), delivers synchronously to every `ApplicationEventListener`, and isolates per-listener
failures. See `InMemoryApplicationEventProducerTest` (pure Mockito) and
`DomainEventCaptureHarnessTest` (real end-to-end harness) below. The prior "no harness possible"
limitation is OBSOLETE — do not reintroduce it as a caveat in new Javadoc.

## Key pattern: no end-to-end harness WAS possible before the producer landed (historical, kept for context)

Before `InMemoryApplicationEventProducer` existed, there was no `ApplicationEventProducer`
implementation anywhere in the workspace, so nothing in production actually published an `Event`
on the Water bus — `consumerEvent`/`consumerDetailedEvent` were never invoked at runtime, and
`TrafficDomainEventListenerTest` had to be pure-unit (Mockito mocks for `ComponentRegistry`,
hand-built `Event` fixtures). That test class's Javadoc still documents this as the ORIGINAL
rationale for being unit-only; it remains valid/valuable on its own merits (deterministic branch
coverage of every gate) even though the harness gap it originally called out is now closed by
`DomainEventCaptureHarnessTest`.

## Key pattern: `InMemoryApplicationEventProducer` unit tests (Mockito)

`InMemoryApplicationEventProducerTest` (`it.water.core.service.events`) mocks `ComponentRegistry`
+ `ApplicationEventListener`s directly (same rationale as the listener test: registry-null /
findComponents-throws / empty-or-null-listener-list / per-listener-isolation / reflective-failure
branches aren't practical to force through a real registry). Key fixtures:
- Real Water event interfaces (`PostSaveEvent.class`, `PreSaveEvent.class`,
  `PostUpdateDetailedEvent.class`) are used directly as the `Class<K extends Event>` argument —
  no need for custom event-interface fixtures, since the producer builds the proxy itself.
- `Resource` has **zero abstract methods** (only a default `getResourceName()`), so
  `new Resource() {}` is a valid, trivial anonymous fixture — no custom Resource/BaseEntity fixture
  class needed for most tests (only one nested `BaseEntity` fixture was needed, to call the
  delivered proxy's no-op `execute(...)`).
- Concrete (non-interface) `Event` fixtures: `public static class ConcreteEvent implements Event {}`
  (has an implicit public no-arg constructor → instantiated via `newInstance()`) vs.
  `public static final class ConcreteEventNoDefaultCtor implements Event { private ConcreteEventNoDefaultCtor() {} }`
  (private ctor → `getConstructor()` throws → `eventInstance(...)` swallows and returns `null` →
  `produceEvent` returns BEFORE ever calling `dispatch()`/touching the registry — assert
  `verifyNoInteractions(componentRegistry)`, don't stub `findComponents` in that test at all, or
  strict stubbing flags it as unused).
- `eventClassNull` also short-circuits in `eventInstance(null)` before `dispatch()` — same
  `verifyNoInteractions(componentRegistry)` pattern.
- Cache identity: same `Class` → `assertSame` on the captured event across two `produceEvent`
  calls; different `Class` → `assertNotSame`.
- Proxy `equals`/`hashCode`/`toString`: `toString()` format is `"WaterEvent[" + eventClass.getName() + "]"`.

## Key pattern: top-level (not nested) event/entity fixtures for whitelist "simple name" tests

`TrafficDomainEventListener.simpleName(fqn)` is just `fqn.substring(fqn.lastIndexOf('.') + 1)`.
A **nested static test fixture class** (e.g. `TrafficDomainEventListenerTest.TestEntity`) has
`getName()` = `...OuterClass$TestEntity` — the `$` is NOT a dot, so `simpleName()` returns
`"OuterClass$TestEntity"`, not `"TestEntity"`. This silently breaks any "whitelist matches by
simple name" test if fixtures are nested.

Fix: define all event/entity/resource test fixtures used for whitelist-matching scenarios as
**top-level classes**, package-private, all packed into ONE shared file (Java allows multiple
non-public top-level types per `.java` file — no filename/class-name matching required when none
are `public`). See `Core-service/src/test/java/it/water/core/service/traffic/events/
TrafficDomainEventFixtures.java`: `TestDomainEntity` (BaseEntity), `TestPlainResource` (Resource
only, no id), and one fixture class per CRUD event interface (`TestPreSaveEvent`,
`TestPostSaveEvent`, `TestPreUpdateEvent`, `TestPostUpdateEvent`, `TestPreUpdateDetailedEvent`,
`TestPostUpdateDetailedEvent`, `TestPreRemoveEvent`, `TestPostRemoveEvent`, `TestGenericEvent`
implements plain `Event` for the non-CRUD/GENERIC branch). Existing convention precedent:
`TestTrafficEventListener.java` in the same test tree is already a standalone top-level file.

## Key pattern: mocking `ApplicationProperties.getPropertyOrDefault(...)` — it is a DEFAULT method

`ApplicationProperties.getPropertyOrDefault(String, String)` is a **default interface method**
that internally calls `getProperty(...)`. When you `Mockito.mock(ApplicationProperties.class)`,
Mockito intercepts ALL methods including defaults — an **unstubbed** call to
`getPropertyOrDefault(key, default)` returns Mockito's default (`null`), it does NOT fall through
to the real default-method body. This means `Boolean.parseBoolean(null)` → `false`, which silently
flips "enabled" branches to disabled if you forget to stub. Always stub
`getPropertyOrDefault(...)` directly with the exact `(key, default)` args the production code
uses — never rely on the interface's real default logic executing on a mock. Already the
established pattern in `TrafficReporterImplTest.stubEnabled(...)`.

## Key pattern: strict-stubbing (`MockitoExtension`) traps for multi-branch gate methods

`TrafficDomainEventListener.capture(...)` reads properties in a fixed, unconditional order once
its short-circuits are passed:
1. `eventCaptureEnabled(props)` — ALWAYS calls `getPropertyOrDefault(PROP_EVENTS_ENABLED, "true")`
   whenever `props != null`, regardless of the value.
2. `isCaptured(props, resourceType)` — ALWAYS calls `getPropertyOrDefault(PROP_EVENTS_MODE,
   "opt-in")` whenever `props != null` (even if mode ends up being `opt-out`, i.e. even when the
   whitelist itself is never consulted).
3. `whitelist(props)` — calls `getPropertyOrDefault(PROP_EVENTS_WHITELIST, "")` ONLY when mode is
   NOT opt-out AND `resourceType != null`.

Consequence: a shared `wireEnabledReporter(mode)` helper that stubs ENABLED+MODE (but not
WHITELIST) is safe to reuse across every test that reaches `isCaptured` — but a test that
short-circuits EARLIER (e.g. `eventCaptureEnabled` returns false because ENABLED="false") must
NOT also stub MODE, or Mockito's strict stubbs (`UnnecessaryStubbingException`) will fail the
test even though the assertions themselves would have passed. Write that one test with a fully
manual, inline stub set (see `capture_eventsGloballyDisabledViaProperty_noCapture` in
`TrafficDomainEventListenerTest`) instead of reusing the shared helper.

## Bug found (not fixed, production out of scope): `EventInvocationHandler.defaultValueFor` mishandles `byte`/`short`

`InMemoryApplicationEventProducer.EventInvocationHandler#defaultValueFor(Class<?> returnType)`
explicitly special-cases `boolean`/`char`/`long`/`float`/`double` but falls through to a bare
`return 0;` (autoboxes to `Integer`) for every OTHER primitive — including `byte` and `short`. Per
the `InvocationHandler.invoke` contract, a primitive-returning proxy method requires the handler to
return an instance of the EXACT corresponding wrapper class; `Integer` is not a `Byte` nor a
`Short`, so the JDK proxy machinery throws `ClassCastException` at the call site for any event
interface method declared to return `byte` or `short`. This is a REAL, distinct gap from the
NPE-on-null-unboxing the method was written to guard against (that part works correctly for the
five explicitly-handled types).

Discovered while adding `InMemoryApplicationEventProducerTest#proxyDefaultValueFor_coversEveryReturnTypeBranch`
(fixture: `PrimitiveReturningEvent`, top-level public interface in `it.water.core.service.events`
with one method per JDK primitive return type + void + Object). The coordinator's original request
expected `(byte) 0`/`(short) 0` defaults with no throw — written faithfully to spec, the test would
have failed. Since production code was out of scope for that task, the test instead asserts the
CURRENT behavior honestly via `assertThrows(ClassCastException.class, event::doByte/doShort)`,
with the discrepancy documented in the test's Javadoc. **A proper fix (not yet applied)**: add
explicit `byte.class`/`short.class` branches to `defaultValueFor` returning `(byte) 0`/`(short) 0`.
Flag this to a human/coordinator before closing out F2 as fully done — it's a pre-existing,
shippable bug (any real event interface declaring a `byte`/`short`-returning method would crash at
dispatch time), not just a coverage gap.

**Why:** branch-count analysis showed byte/short add ZERO coverage value beyond what `int` already
exercises (both fall through to the identical `return 0` line/branch) — so don't feel compelled to
invoke them just for coverage; only do so if specifically documenting this bug.

**How to apply:** if asked to raise coverage on this class again, do NOT assume every primitive
return type in a "test every branch" request is safe to assert as "no throw" — verify JDK Proxy's
primitive-wrapper-exactness contract first for any newly-added primitive-returning method.

## Key pattern: shared/accumulating `ComponentRegistry` across harness test classes — never assert exact capture COUNTS

`TrafficReporterEgressHarnessTest`'s Javadoc already documents that the module's test
`ComponentRegistry` is effectively shared/accumulating across the whole test run: each harness
class's `@BeforeAll` re-triggers framework component scanning (per `TestRuntimeInitializer`), and
`findComponent` (singular) deterministically returns the first-ever-registered instance for a
given priority — but `findComponents` (PLURAL, used by both `InMemoryTrafficPublisher.dispatch()`
and `InMemoryApplicationEventProducer.dispatch()`) returns EVERY accumulated registration. Since
`TestTrafficEventListener` is manually registered fresh (`new TestTrafficEventListener()`) in each
harness class's own `@BeforeAll` and NEVER unregistered, and all instances share the same
`static CAPTURED` list, a single dispatch can legitimately be observed more than once if multiple
harness classes have run earlier in the same JVM. This is why every existing harness assertion
(`awaitCapture`, `awaitCaptureByOperation`) only checks EXISTENCE (`.findFirst()`), never an exact
count.

`DomainEventCaptureHarnessTest`'s anti-loop scenario needed a genuine "no unbounded growth" proof
without tripping over this. Fix: assert `count >= 1` after the first poll, then assert the count
is STABLE (unchanged) after an additional sleep — never assert `count == 1`. Stability across time
is what actually proves the loop guard holds; the absolute value is contaminated by cross-class
listener accumulation and is not a meaningful signal either way. Field-CONTENT assertions
(recordType/changePhase/changeOperation/resourceType/resourceId/eventClass/beforeRef/afterRef) via
`.findFirst()` remain valid regardless of duplication, since every duplicate instance computes
identical field values from the same input (only `recordId`/`timestamp` differ, which are never
asserted against exact values).

## F4 — policy: sampling, redaction, enrichment, correlation (2026-07-27)

`TrafficCorrelationContext` (static ThreadLocal, `open()`/`open(id)`/`current()`/`close()`) +
`TrafficRecordPolicy` (`it.water.core.service.traffic.policy`, constructor
`(ApplicationProperties, Runtime, ClusterNodeOptions)`, methods `normalize(T)` and
`shouldReport(TrafficRecord)`) sit behind `TrafficReporterImpl`, which now funnels EVERY
`report*` call through `applyPolicies`: master switch → `normalize` → `shouldReport`, publishing
the **normalized COPY**, never the caller's instance. The three Water record classes gained
`@SuperBuilder(toBuilder = true)` to support this rebuild. `RestTrafficCaptureImpl` gained
`requestStarted()` (opens the correlation scope) and lost its identity-resolution and
error-truncation code (moved to the policy); `captureRestCall` now closes the scope in a `finally`
unconditionally.

### Key pattern: F4 invalidated 7 pre-existing tests on purpose — fix, don't route around

- **3× `assertSame(record, captor.getValue().record())` in `TrafficReporterImplTest`** — the
  reporter now rebuilds the record via `toBuilder()`, so identity legitimately changes but
  `@EqualsAndHashCode`-based value equality still holds. Fix: `assertEquals` (+ an explicit
  `assertNotSame` alongside it, so a future reader can't mistake the fix for "identity happens to
  still match" and silently regress it back). This equality holds specifically because `Runtime`/
  `ClusterNodeOptions` mocks are left **unstubbed** in these tests — an unstubbed mock behaves like a
  real, unconfigured collaborator (every enrichment field it could fill stays null), which is what
  makes `assertEquals(original, normalized)` valid without a dedicated "prove nothing changed" test.
- **4× identity tests in `RestTrafficCaptureImplTest`** (`identityResolvedFromRuntimeSecurityContext`,
  `runtimeNull_identityNull`, `securityContextNull_identityNull`, `runtimeLookupThrows_identityNullNoException`)
  — deleted entirely, along with the now-unused `Runtime`/`SecurityContext` mocks/imports. Identity
  resolution isn't just moved, it's GONE from this class (no `resolveIdentity()` method exists
  anymore), so a straggler test would either fail to compile or pass vacuously/misleadingly. The
  coordinator's message named only 3 of the 4 — when a stated removal list clearly omits a sibling
  test testing the exact same removed capability, remove that one too and say so, rather than
  leaving dead/misleading coverage behind.
- **1× truncation test** (`errorMessageLongerThan200Chars_truncatedTo200`) — truncation moved to the
  policy. Rewritten to assert the OPPOSITE: the full, untouched message reaches `reportCall` (a
  250-char message stays 250 chars at this layer); the 200-char-truncation assertion moved to
  `TrafficRecordPolicyTest`.

### Key pattern: correlation-scope test hygiene (ThreadLocal leak risk)

`TrafficCorrelationContext` is a static ThreadLocal shared by the WHOLE test run on whatever thread
JUnit uses (no parallel execution configured) — a scope opened and not closed in one test class can
silently corrupt an unrelated assertion in a LATER test class (e.g. break the `assertEquals(original,
normalized)` equality above, since `enrich()` would suddenly fill a non-null `correlationId`).
Discipline applied everywhere in this module: every test that calls `TrafficCorrelationContext.open()`
wraps the body in `try { ... } finally { TrafficCorrelationContext.close(); }`;
`TrafficCorrelationContextTest` additionally has a class-wide `@AfterEach` calling `close()`, and
`RestTrafficCaptureImplTest`'s `@BeforeEach` proactively calls `close()` too as a belt-and-braces
guard. `TrafficReporterImplTest`/`TrafficRecordPolicyTest` (which don't primarily test correlation,
but DO depend on it being clean for their equality assertions) also carry a defensive `@AfterEach
clearCorrelationScope()`.

### Key pattern: sampling-bucket tests must COMPUTE keys, never hardcode hashes

`TrafficRecordPolicy.shouldReport`'s intermediate-rate branch buckets on
`(key.hashCode() & Integer.MAX_VALUE) % SAMPLING_BUCKETS` (package-private constant, 10000).
`String#hashCode()` is specified/stable across JVM runs, so a test needing a key deterministically
inside/outside a rate threshold computes the SAME formula in a small helper
(`bucketOf(String)`/`findKeyWithBucket(Predicate<String>, String prefix)` in
`TrafficRecordPolicyTest`) and brute-force searches `prefix + i` for `i` up to 100_000 - never
hardcode a literal key known (by prior manual computation) to hash a certain way. For pure
DETERMINISM/coherence tests (ADR-7: same `correlationId` → same verdict across different records),
no computed key is needed at all - just assert repeated/cross-record stability at an intermediate
rate (e.g. 0.5), since determinism holds regardless of which side of the threshold the shared key
falls on.

### Key pattern: `TrafficRecordPolicy`'s package-private constants are directly testable

`SCOPE_PER_TYPE`, `ERROR_MESSAGE_MAX_LENGTH`, `SAMPLING_BUCKETS` are package-private (no modifier);
`PROP_SAMPLING_RATE`, `PROP_SAMPLING_SCOPE`, `PROP_PAYLOAD_CAPTURE`, `PROP_SERVICE_NAME` are
`public`. Since `TrafficRecordPolicyTest` lives in the SAME package
(`it.water.core.service.traffic.policy`), reference `TrafficRecordPolicy.CONSTANT` directly instead
of re-declaring/hardcoding the literal string/number in the test - avoids drift if production ever
renames a property key. `TrafficReporterImplTest` (different package) imports `PROP_SAMPLING_RATE`
specifically via `import static ...TrafficRecordPolicy.PROP_SAMPLING_RATE` since it's `public`.

### Key pattern: `doubleProperty`'s internal default is ALWAYS `null`, not the caller's numeric default

`TrafficRecordPolicy.doubleProperty(name, defaultValue)` calls `stringProperty(name, null)` -
literally `null`, regardless of what numeric `defaultValue` was passed in - then falls back to
`defaultValue` only if the string comes back null/blank. So to stub a sampling-rate property in a
test, stub `applicationProperties.getPropertyOrDefault(PROP_SAMPLING_RATE, null)` (exact `null`
second arg), not `getPropertyOrDefault(PROP_SAMPLING_RATE, "1.0")` or any other literal - the latter
will never match and the stub will be silently ignored (falling through to Mockito's own unstubbed
`null`, which happens to coincidentally often produce the same numeric default anyway, masking the
mistake). Same gotcha applies to `stringProperty(PROP_SAMPLING_SCOPE, SCOPE_PER_TYPE)` inside
`samplingRate()` EXCEPT that one genuinely passes `SCOPE_PER_TYPE` as its default (not null) - stub
it with that exact second argument.

No production bugs found in F4 (`TrafficCorrelationContext`, `TrafficRecordPolicy`,
`TrafficReporterImpl`, `RestTrafficCaptureImpl` all traced line-by-line against every test scenario).

### F4 test file map
- `Core-service/src/test/java/it/water/core/service/traffic/TrafficCorrelationContextTest.java` (new)
  — 9 tests: non-null/distinct ids, `open(id)` adoption, `open(null/blank)` fresh-id fallback,
  `current()` outside scope, `close()` idempotent/safe, `open()` overwrites an already-open scope.
- `Core-service/src/test/java/it/water/core/service/traffic/policy/TrafficRecordPolicyTest.java` (new,
  the big one, ~30 tests) — enrichment (5 fields incl. never-overwrite-existing-value proof, all 4
  null-collaborator combos, concrete-type preservation for call/domain-event records, non-`WaterTrafficRecord`
  passthrough), redaction (truncation at/under/over 200, payload-capture default-false/explicit-true,
  non-call-record no-op), sampling (default 1.0, 0.0, non-numeric fallback, ADR-7 cross-record
  determinism at rate 0.5, recordId fallback with computed in/out keys, per-type override, global-scope
  ignoring per-type via `verify(..., never())`, null recordType, null record).
- `Core-service/src/test/java/it/water/core/service/traffic/TrafficReporterImplTest.java` (modified)
  — 3 `assertSame`→`assertEquals`+`assertNotSame` fixes, renamed accordingly; added sampling-rate
  0.0/1.0 tests and null-record tests (all three `report*` overloads); added `Runtime`/
  `ClusterNodeOptions` `@Mock` fields (left unstubbed by design) and a defensive `@AfterEach`.
- `Core-service/src/test/java/it/water/core/service/traffic/rest/RestTrafficCaptureImplTest.java`
  (modified) — removed 4 identity tests + `Runtime`/`SecurityContext` mocks/imports; rewrote the
  truncation test to assert the message reaches the reporter INTACT; added `requestStarted()` scope-open
  test and 4 "always closes the scope" tests (success/reporter-absent/path-excluded/reportCall-throws),
  each wrapped in `try/finally`; added a defensive `TrafficCorrelationContext.close()` to `@BeforeEach`.

## F3 — incoming REST capture (2026-07-27)

`RestTrafficCapture` (Core-api contract) / `RestTrafficCaptureImpl` (Core-service,
`it.water.core.service.traffic.rest`) centralizes enablement, path normalization/exclusion,
identity enrichment and record building for incoming REST calls; two thin per-runtime adapters
call into it: `CxfTrafficCaptureFilter` (Rest-api-manager-apache-cxf, JAX-RS
`ContainerRequestFilter`+`ContainerResponseFilter`, javax) and `SpringTrafficCaptureInterceptor`
(Rest-spring-api, `HandlerInterceptor`, jakarta). Same F2 gate-method stubbing discipline applies
(`isEnabled()`/`restCaptureEnabled()` always reads `water.traffic.rest.enabled` once `props!=null`;
`isExcluded()` only reads `water.traffic.rest.exclude` when the normalized path is non-null) — use
two wiring helpers (`wireEnabledCaptureNoExclusions()` for null/blank-path tests,
`wireEnabledCaptureWithExclude(value)` for everything else) exactly like the F2 listener's
`wireEnabledReporter(mode)` split.

**REST filters ARE unit-testable directly with Mockito** (NOT a `RestControllerImpl`, so the
Rest module's Karate-only rule from its `CLAUDE.md` does not apply): precedent is
`CxfSecurityHeadersFilterTest` (same module, `security/filters` package) — mock
`ContainerRequestContext`/`ContainerResponseContext`/`UriInfo` directly, instantiate the filter via
its plain constructor, no CXF bus needed. `@Context`-injected fields (e.g. `servletRequest`) simply
stay `null` when built this way — assert the resulting `null` client IP explicitly rather than
trying to inject it.

Boxed-`Integer`/int-literal `assertEquals` ambiguity (already known from the multitenancy work):
`record.statusCode()`/`response.getStatus()`-derived `Integer` captor values must be compared via
`assertEquals(Integer.valueOf(200), actual)`, never `assertEquals(200, actual)` — the latter is a
genuinely ambiguous overload between `assertEquals(int,int)` and `assertEquals(Object,Object)` in
this exact mixed-literal-vs-boxed scenario.

No production bugs found in F3 (`RestTrafficCaptureImpl`, `CxfTrafficCaptureFilter`,
`SpringTrafficCaptureInterceptor` all traced line-by-line against every test scenario and matched
expected behavior exactly) — contrast with the F2 byte/short `ClassCastException` bug found earlier.

### F3 test file map
- `Core-service/src/test/java/it/water/core/service/traffic/rest/RestTrafficCaptureImplTest.java`
  — ~30 unit tests: isEnabled() short-circuits/defaults, captureRestCall() short-circuits,
  exclusions (prefix match incl. spaces/multi-entry/no-match), path normalization (7 shapes, verified
  via captured record path, not by calling the package-private `normalizePath` directly), full
  record-mapping + outcome-derivation table, identity resolution (Runtime→SecurityContext, incl.
  null/throws), fail-safe on `reportCall` throwing.
- `Rest-api-manager-apache-cxf/src/test/java/it/water/service/rest/manager/cxf/traffic/CxfTrafficCaptureFilterTest.java`
  — 12 tests: request-filter enabled/disabled/absent/registry-null/findComponent-throws; response-filter
  no-marker/non-Long-marker/capture-absent/registry-null/happy-path (method+path+status+duration≥0+null
  clientIp+null error)/null-UriInfo→null-path/captureRestCall-throws-not-propagated.
- `Rest-spring-api/src/test/java/it/water/service/rest/spring/traffic/SpringTrafficCaptureInterceptorTest.java`
  — 14 tests using real `MockHttpServletRequest`/`MockHttpServletResponse` (from `spring-test`, already
  a transitive test dep via `spring-boot-starter-test`) instead of mocks for the servlet objects, since
  attributes/method/URI/remoteAddr/status are plain mutable state on them: preHandle ALWAYS returns
  true (even when the capture component itself throws); afterCompletion passes the aborting exception
  through to `captureRestCall` (key behavioral difference from the S2S interceptors, which cannot
  observe the error path at all — see R1 in `S2STrafficCaptureHarnessTest`).

## Test file map (F2)

- `Core-service/src/test/java/it/water/core/service/traffic/events/TrafficDomainEventFixtures.java`
  — shared top-level fixtures (see above).
- `Core-service/src/test/java/it/water/core/service/traffic/events/TrafficDomainEventListenerTest.java`
  — 27 pure-Mockito unit tests: loop guard (subject-is-TrafficRecord / event-is-TrafficEvent),
  registry-null, findComponents-throws, empty-reporter-list, reporter-disabled,
  ApplicationProperties-unresolvable-defaults, events-globally-disabled, opt-in whitelist
  (empty/FQN/simple-name/mismatch/spaces+multi-entry), opt-out (always captures, including null
  resourceType), full CRUD phase/operation/recordType mapping table (Pre/PostSave,
  Pre/PostUpdate, Pre/PostUpdateDetailed, Pre/PostRemove, generic non-CRUD), before/after subject
  resolution, resourceId-null-for-non-BaseEntity, fail-safe swallow-on-reporter-throw.
- `Core-service/src/test/java/it/water/core/service/traffic/WaterTrafficDomainEventRecordTest.java`
  — builder/POJO tests (inherited fields, domain-specific fields, null beforeRef/afterRef,
  null changePhase/resourceType for generic events).
- `Core-service/src/test/java/it/water/core/service/traffic/WaterTrafficDomainEventTest.java`
  — POJO tests incl. covariant `record()` override returning `TrafficDomainEventRecord`.
- `Core-service/src/test/java/it/water/core/service/traffic/TrafficReporterImplTest.java`
  — extended (not replaced) with 5 new tests for the two `reportDomainEvent(...)` overloads,
  following the exact same `stubEnabled(...)`/`ArgumentCaptor<TrafficEvent>` pattern already used
  for `report`/`reportCall`.
- `Core-service/src/test/java/it/water/core/service/events/InMemoryApplicationEventProducerTest.java`
  — pure Mockito unit tests for `InMemoryApplicationEventProducer` (see dedicated section above):
  multi-listener delivery, detailed-event delivery, instanceof-requested-interface, per-type
  caching (same/different Class), proxy equals/hashCode/toString, proxy `execute(...)` no-op,
  registry-null, findComponents-throws, empty/null listener list, per-listener isolation,
  event-class-null, concrete-class-with/without-default-constructor.
- `Core-service/src/test/java/it/water/core/service/traffic/DomainEventCaptureTestEntity.java`
  — public, top-level `BaseEntity` fixture for the harness test below (kept public/top-level per
  the same rationale as the `.events` package fixtures, even though nothing here actually goes
  through `TestServiceProxy`).
- `Core-service/src/test/java/it/water/core/service/traffic/DomainEventCaptureHarnessTest.java`
  — real end-to-end harness (`@ExtendWith(WaterTestExtension.class)`, `PER_CLASS`, ordered):
  resolves the producer via `componentRegistry.findComponent(ApplicationEventProducer.class, null)`,
  drives `produceEvent`/`produceDetailedEvent` with `PostSaveEvent`/`PostUpdateDetailedEvent`,
  and polls `TestTrafficEventListener.CAPTURED` (reused, not re-created) for the resulting
  `TrafficDomainEventRecord`. Scenarios: whitelisted PostSaveEvent capture (with field-by-field
  mapping assertions incl. `eventClass` being the INTERFACE name, not the proxy class name),
  detailed-event before/after refs, non-whitelisted entity (opt-in) → no capture, traffic globally
  disabled → no capture, and the anti-loop stability scenario (see dedicated section above for why
  it asserts stability, not an exact count).

**Why:** F2 introduced a new listener + record + event + two reporter overloads with zero
existing test coverage; SonarQube requires ≥80% instruction coverage on new code. The later
`InMemoryApplicationEventProducer` finally closed the end-to-end loop.

**How to apply:** When F3 (or any later phase) adds another `ApplicationEventListener` or another
`TrafficReporter`/`ApplicationEventProducer` overload, reuse this exact file layout and the gotchas
above (top-level fixtures for simple-name matching; default-method mocking; strict-stubbing
call-order awareness for gate methods; never assert exact capture counts in a shared-registry
harness — assert stability/existence instead).
