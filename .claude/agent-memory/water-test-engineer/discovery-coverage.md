---
name: discovery-coverage
description: Branch-coverage campaigns on Core-service it.water.core.service.integration.discovery; LivenessListener inner class, field-resolution branches, retry/backoff edge cases
metadata:
  type: project
---

## Coverage status (as of 2026-07-02)

Starting point: 85% instructions / 70% branch → combined ~79.5% (below 80% SonarQube threshold).

## Key patterns discovered

### LivenessListener inner class testing
- `LivenessListener` is a `private final inner class` of `ServiceRegistrationLifecycleSupport`.
- To get an instance, register a support with a `CapturingLivenessClient` that stores the `ServiceLivenessListener` parameter passed to `start()`.
- Use `NullReturningCapturingLivenessClient` (returns null session but captures listener) to test the `registrationConfirmed==false` branch.
- To test `active==false` branch: register, then deregister (which sets `active=false`), then fire `onLivenessLost`.
- `registeredInstanceId` is set to null by `doDeregister` — the guard `registeredInstanceId == null` in the listener also short-circuits.

### resolveServiceNameFromField branches (via reflection)
Use `java.lang.reflect.Method` to call private `resolveServiceNameFromField(ComponentRegistry, Field)`.
Key test fixture: `FieldHolder` class with fields:
- `String plainField` — not @Inject → returns ""
- `@Inject ComponentRegistry registryField` — isAssignableFrom check → returns ""
- `@Inject CompanyApi companyApiField` — MetadataProvider (BaseAbstractService) → returns "company"
- `@Inject SampleServiceInterface sampleServiceField` — used with BlankNameMetadataProvider / ExplicitNameMetadataProvider

**Critical note**: `BaseAbstractService` implements `ServiceDiscoveryMetadataProvider`, so ANY class extending `BaseServiceImpl` or `BaseAbstractService` IS a MetadataProvider. There is no "non-MetadataProvider component" case in practice for standard Water services.

### ServiceDiscoveryGlobalOptions edge cases
- `globalOptions == null` → all `resolve*` methods use hardcoded defaults (10s timeout, 3 attempts, default backoff)
- `getHttpTimeoutSeconds() <= 0` → same defaults
- `getRegistrationRetryBackoffMs()` returning null or empty array → `DEFAULT_RETRY_BACKOFF_MS`
- Test via reflection on private `resolveHttpTimeout()`, `resolveMaxAttempts()`, `resolveBackoffMs()`
- Inject via Lombok `@Setter setGlobalOptions()`

### resolveEndpoint branches
- URL already ends with full path → returns normalized URL as-is (no double-appending)
- URL ends with `/water` → appends path directly (no extra `/water`)
- URL ends with neither → appends `/water` + path

### resolveRegistrationEndpoint returns ""
Triggered when any of: host blank OR port blank OR root blank (after normalization).
Test `resolveServicePort` fallback: `info.getServicePort()` blank + `this.port` set → uses client port.

### pickBackoff edge cases
- index beyond array length → clamped to `values.length - 1` (last element)
- negative value in array → returns 0
- empty array → returns 0

## Branch targets added (2026-07-02)
In `ServiceRegistrationLifecycleSupportTest`:
- `livenessListener_onLivenessLost_whenNotActive_doesNotScheduleRetry`
- `livenessListener_onLivenessLost_whenNotConfirmed_doesNotScheduleRetry`
- `livenessListener_onLivenessLost_withMismatchedInstanceId_doesNotScheduleRetry`
- `livenessListener_onLivenessLost_withCorrectInstanceId_schedulesRetry`

In `RestApiServiceRegistrationLifecycleManagerImplTest`:
- `activateRestApiRegistrations_nullComponentRegistry_returnsWithoutException`
- `activateRestApiRegistrations_nullClassLoader_returnsWithoutException`
- `activateRestApiRegistrations_missingApplicationProperties_logsAndReturns`
- `activateRestApiRegistrations_calledTwice_doesNotDuplicateRegistration`
- `isBusinessRoot_nullRoot_returnsFalse`
- `isBusinessRoot_blankRoot_returnsFalse`
- `resolveServiceNameFromField_*` (5 variants via reflection)
- `deriveServiceName_withNullClass_returnsEmpty`
- `resolveServiceName_componentNotInRegistry_fallsBackToDeriveServiceName`

In `ServiceDiscoveryRegistryClientImplTest`:
- `resolveEndpoint_remoteUrlAlreadyEndsWithPath_returnsNormalized`
- `resolveEndpoint_remoteUrlEndsWithWater_appendsPathDirectly`
- `pickBackoff_indexBeyondArrayLength_returnsLastElement`
- `pickBackoff_negativeValueInArray_returnsZero`
- `pickBackoff_emptyArray_returnsZero`
- `resolveHttpTimeout_nullGlobalOptions_returnsDefault`
- `resolveHttpTimeout_globalOptionsWithZeroTimeout_returnsDefault`
- `resolveMaxAttempts_nullGlobalOptions_returnsDefault`
- `resolveMaxAttempts_globalOptionsWithZeroAttempts_returnsDefault`
- `resolveBackoffMs_nullGlobalOptions_returnsDefaultArray`
- `resolveBackoffMs_globalOptionsWithNullBackoff_returnsDefault`
- `resolveBackoffMs_globalOptionsWithEmptyBackoff_returnsDefault`
- `resolveRegistrationEndpoint_infoportBlank_usesClientPort`
- `resolveRegistrationEndpoint_hostPortRootBlank_returnsEmpty`
- `resolveRegistrationEndpoint_onlyHostBlank_returnsEmpty`
- `resolveRegistrationEndpoint_onlyRootBlank_returnsEmpty`

**Why:** Combined coverage was 79.5%, below SonarQube 80% threshold. Focused on branch coverage which was only 70%.

**How to apply:** When adding tests for any `ServiceRegistrationLifecycleSupport` subclass, always capture the `LivenessListener` via `CapturingLivenessClient`. Use reflection for private methods — the pattern is established in these test files.
