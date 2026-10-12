# M15.2 Weather Subsystem Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a provider-independent, location-aware weather subsystem to Today with primary/fallback providers, persistent cache/stale behavior, per-metric units, Basic/Work/Custom presentation presets, and current/hourly/daily expandable UI.

**Architecture:** UI consumes only normalized weather state. `WeatherProvider` adapters normalize provider payloads before `WeatherRepository` applies primary/fallback and cache semantics. Workplace/current/saved locations resolve through a separate location boundary; coordinates are authoritative and display labels are metadata.

**Tech Stack:** Kotlin/Android, Compose, SharedPreferences profile stores, `HttpURLConnection`, `kotlinx.serialization`/`org.json` as already available, Android `LocationManager`, MapLibre Native Android `13.6.1`, OpenFreeMap Liberty style.

**Spec:** `docs/superpowers/specs/2026-09-16-m15-today-weather-design.md`

## Global Constraints

- Work only on `feature/m15-today-weather` in the existing isolated linked worktree; canonical `master` stays untouched.
- M15.1 behavior remains protected: fixed Hero -> Timeline -> configurable lower modules; external calendar stays read-only.
- Today UI must never branch on provider identity.
- Primary provider: Open-Meteo Forecast API. Fallback provider: MET Norway Locationforecast 2.0.
- MET Norway calls must send a non-generic app User-Agent; never use `okhttp`, `Dalvik`, `Java` or another prohibited generic identifier.
- Provider adapters may expose missing optional metrics, but must fail capability checks when a required metric/layer is unsupported.
- Coordinates are authoritative. Do not infer project weather location from platform/chat location.
- Workplace weather location is persisted separately by workplace ID; do not alter existing `Workplace(id,name)` serialization in M15.2.
- Saved locations are profile-scoped and persist stable id, name, latitude, longitude, optional address, and order.
- Current device location is explicit and permission-gated; no background location permission.
- No Weather rule engine, notifications, or periodic WorkManager scheduling in M15.2; those belong to M15.3.
- No payroll, Room, alarm, Wear, Notes, backup-payload, push, merge, release, or deploy semantics may change.
- TDD required: each implementation task must show a real RED caused by missing/incorrect behavior, then focused GREEN.

---

### Task 1: Normalized weather domain, units, presets, and forecast layers

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherModels.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherUnits.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherPresentationConfig.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherDomainTest.kt`
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- Produces `WeatherCoordinates(latitude: Double, longitude: Double)` with finite/range validation helpers.
- Produces normalized SI-ish `WeatherCurrent`, `WeatherHourlyPoint`, `WeatherDailyPoint`, and `NormalizedWeather` carrying observation/forecast timestamps and nullable optional metrics.
- Canonical values: Celsius, metres/second, hPa, millimetres, metres visibility, degrees direction.
- Produces `TemperatureUnit`, `WindSpeedUnit`, `PressureUnit`, `PrecipitationUnit`, `VisibilityUnit`, `WindDirectionFormat` and pure conversion/format helpers.
- Produces `WeatherPresetId { BASIC, WORK_WIND, CUSTOM }`, `WeatherMetric`, `WeatherMetricDisplay`, `WeatherLayerId`, `HourlyHorizon`, `DailyHorizon`, `WeatherPresentationConfig`, `basicWeatherPresentationConfig()`, `workWindWeatherPresentationConfig()`, and `toCustom()`.

- [ ] **Step 1: Write failing JVM tests** for coordinate validation, canonical-unit conversions, direction formatting, built-in preset contents/order, built-in-to-Custom copy semantics, and allowed hourly/daily horizons.
- [ ] **Step 2: Run** `:app:testDebugUnitTest --tests '*M15WeatherDomainTest*'` with `scripts/android/vps-env.sh`; require RED because weather domain/config types do not exist.
- [ ] **Step 3: Implement the minimal pure Kotlin domain/config.** No Android imports, network code, storage, provider ids, or UI.
- [ ] **Step 4: Re-run focused tests and require GREEN; run `git diff --check`.**
- [ ] **Step 5: Commit code** as `feat: add M15 weather domain` and record RED/GREEN evidence in a separate docs checkpoint commit.

### Task 2: Provider contract, repository fallback, and cache semantics

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherProvider.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherRepository.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherRepositoryTest.kt`
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- `WeatherProviderId { OPEN_METEO, MET_NORWAY }`.
- `WeatherCapability` includes CURRENT, HOURLY, DAILY plus metric capabilities.
- `WeatherFetchRequest(coordinates, hourlyHours, dailyDays, requiredMetrics, optionalMetrics)`.
- `WeatherProvider { id; capabilities; suspend fun fetch(request): WeatherProviderResult }`.
- `WeatherCache { fun read(key): WeatherCacheEntry?; fun write(key, entry) }`.
- `WeatherRepository.load(request, primaryId, fallbackId, nowMillis)` returns `WeatherLoadResult(data, sourceProvider, usedFallback, stale, fetchedAtMillis, failure?)`.
- Repository tries primary first; tries fallback on transport/server/parse failure or required-capability mismatch; writes successful normalized data to cache; if both fail, returns last cached data as stale instead of clearing it.
- Optional metrics may be absent without invalidating otherwise usable fallback data.

- [ ] **Step 1: Write RED fake-provider tests** covering primary success, transport fallback, required-capability fallback, optional metric absence, successful cache write, and stale cached return when both providers fail.
- [ ] **Step 2: Verify RED** because provider/repository/cache contracts are absent.
- [ ] **Step 3: Implement pure contracts/repository.** Keep provider-specific JSON/HTTP out of this task.
- [ ] **Step 4: Re-run focused tests and `git diff --check`; require GREEN.**
- [ ] **Step 5: Commit** `feat: add M15 weather repository fallback` plus separate evidence checkpoint.

### Task 3: Open-Meteo + MET Norway adapters, provider settings, and persistent cache

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/HttpWeatherTransport.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/OpenMeteoWeatherProvider.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/MetNorwayWeatherProvider.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WeatherSettingsStore.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WeatherCacheStore.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/SettingsDataPort.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherProviderParsingTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherPersistenceTest.kt`
- Test fixtures: `app/src/test/resources/weather/open_meteo_forecast.json`, `app/src/test/resources/weather/met_norway_compact.json`
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- `HttpWeatherTransport.get(url, headers)` uses HTTPS `HttpURLConnection`, bounded connect/read timeouts, status validation, and UTF-8 body.
- Open-Meteo request asks canonical Celsius/m/s/mm output and current/hourly/daily variables including temperature, apparent temperature, humidity, precipitation, pressure, wind speed/direction/gusts, visibility and weather code.
- MET Norway fallback uses `/weatherapi/locationforecast/2.0/compact`, coordinates rounded to <=4 decimals, app-identifying User-Agent, and maps available temperature/humidity/pressure/wind/gust/precipitation/symbol fields; unsupported visibility remains optional/null.
- `WeatherSettings` persists primary/fallback provider ids, per-metric display units, direction format, preset/config, and foreground freshness default used by M15.2. M15.3 may later expose scheduling policy without changing these contracts.
- `WeatherCacheStore` persists normalized payload, provider, acquisition timestamp, coordinates key, and enough schema versioning to ignore malformed/unknown cache safely.

- [ ] **Step 1: Write offline parser/persistence RED tests** using fixed JSON fixtures and malformed-store cases. No live internet is acceptable as test evidence.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement HTTP transport, both adapters, settings, cache codec/store, and dependency wiring.** Do not expose provider JSON types to UI.
- [ ] **Step 4: Run parser/persistence tests + repository tests + production Kotlin compile; require GREEN.**
- [ ] **Step 5: Run one bounded manual provider smoke from the VPS only if network is available; treat it as supplementary evidence, never as replacement for offline tests.**
- [ ] **Step 6: Commit** `feat: add M15 weather providers and cache` plus separate evidence checkpoint.

### Task 4: Workplace, saved, and temporary weather locations

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/WeatherLocation.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/settings/WorkplaceWeatherLocationStore.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/settings/SavedWeatherLocationStore.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/ports/SettingsDataPort.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/AppDependencies.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherLocationTest.kt`
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- `WeatherLocationSource` models `Workplace(workplaceId)`, `CurrentDevice`, and `Saved(savedLocationId)`.
- `SavedWeatherLocation(id,name,coordinates,address?,order)`.
- `WorkplaceWeatherLocation(workplaceId,coordinates,label,address?)` stored separately from `WorkAssignmentsStore` workplace serialization.
- `WeatherLocationSelection(defaultSource, temporaryOverride?)`; resolver priority is temporary override -> active Workplace saved coordinates -> current device fallback when explicitly allowed by caller. A Today temporary selection must never mutate workplace default.
- Saved-location ids are stable and deletion/reorder are deterministic.

- [ ] **Step 1: Write RED tests** for workplace-keyed persistence, saved-location round trip/reorder/delete, malformed coordinate rejection, resolver priority, and temporary override non-mutation.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement pure model/stores/resolver and dependency wiring.** Do not modify existing `Workplace(id,name)` JSON.
- [ ] **Step 4: Re-run focused tests plus M15.1/M7 settings compatibility tests; require GREEN.**
- [ ] **Step 5: Commit** `feat: add M15 weather locations` plus separate evidence checkpoint.

### Task 5: Current-location permission, geocoding search, and map pin picker

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/AndroidCurrentLocationSource.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/weather/OpenMeteoGeocodingSource.kt`
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherLocationPickerScreen.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherLocationPlatformStructureTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherGeocodingTest.kt`
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- Manifest declares only `ACCESS_COARSE_LOCATION` and `ACCESS_FINE_LOCATION`; no background location permission.
- `AndroidCurrentLocationSource` uses Android `LocationManager`, permission checks, last-known/current single-shot behavior, and returns normalized coordinates or an explicit unavailable/error result.
- `WeatherPlaceSearchResult(name, admin?, country?, coordinates)` parsed from Open-Meteo geocoding results.
- Map pin uses MapLibre Native Android `13.6.1` with OpenFreeMap Liberty style `https://tiles.openfreemap.org/styles/liberty`; selected map center/pin produces authoritative coordinates. Attribution must remain visible.
- Search result or current GPS can seed the map; confirming the picker returns coordinates + editable display label/address metadata.

- [ ] **Step 1: Write RED parser/structure tests** for geocoding, permission declarations, absence of background permission, exact pinned MapLibre dependency, and picker contract.
- [ ] **Step 2: Verify RED.**
- [ ] **Step 3: Implement location source, geocoder and picker.** Network errors must leave existing saved coordinates intact.
- [ ] **Step 4: Run focused tests + `assembleDebug`; require GREEN.**
- [ ] **Step 5: Commit** `feat: add M15 weather location picker` plus separate evidence checkpoint.

### Task 6: Weather settings and location-management UI

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/WeatherSettingsScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/settings/WorkplacesScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Modify related More/settings navigation only as required by existing M12 taxonomy.
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15WeatherSettingsStructureTest.kt`
- Screenshot: add deterministic Weather settings/location states.
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- Weather settings exposes primary/fallback provider, per-measurement units, Basic/Work/Custom preset, metric/layer visibility/order, hourly 6/12/24/48h and daily 3/5/7/10d horizons.
- Editing Basic/Work copies to Custom before mutation.
- Workplaces exposes weather-location setup without altering payroll settings semantics.
- Saved locations support add/edit/delete/reorder using search/map/current-location entry points.

- [x] **Step 1: Add RED structure/state tests** for defaults, preset->Custom transition, provider selection, per-unit settings, workplace entry point, and saved-location actions.
- [x] **Step 2: Verify RED.**
- [x] **Step 3: Implement minimal Evolution-style UI and navigation using existing settings patterns.**
- [x] **Step 4: Add deterministic Light/Dark/fontScale 1.3 screenshots; run RED/update/validate and inspect refs when artifact transport permits.**
- [x] **Step 5: Run production compile + M12/M15 settings compatibility tests; require GREEN.**
- [x] **Step 6: Commit** `feat: add M15 weather settings` plus separate evidence checkpoint.

**Task 6 recovery acceptance (2026-10-12, new VPS):** The migrated Task-6 implementation was recovered as Git snapshot `ad1d55f`, rather than a historical standalone `feat:` commit; no Git history was rewritten to imitate a prior commit. Regression test-only repair `fd3928a`, localized unit labels `e27bc83`, visual references `a903a96` and ledger evidence constitute the traceable implementation/verification lineage. RED was recreated retrospectively (not passed off as a contemporaneous historical run) on the isolated pre-Task-6 `28e98b2` baseline: 2/2 expected structural test failures in job `e4afa2f8f001ad98c4a4e6610e829c47`. Final phone/Wear gate `539bae7f6e85a2a635efe64282ecd582` is GREEN: 406/406 app JVM tests, 52 screenshot cases green (validated independently, then UP-TO-DATE), both debug APKs produced, app/Wear lint 0 errors. No Task 7 work included. Detailed hashes and qualifications: `docs/project/WORK_PROGRESS.md`.

### Task 7: Today compact + expandable current/hourly/daily weather

**Files:**
- Create: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/weather/TodayWeatherModule.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/ui/today/TodayScreen.kt`
- Modify: `app/src/main/java/com/vigilante/shiftsalaryplanner/app/MainActivity.kt`
- Modify: `app/src/screenshotTest/kotlin/com/vigilante/shiftsalaryplanner/M15TodayVisualScreenshotTest.kt`
- Test: `app/src/test/java/com/vigilante/shiftsalaryplanner/M15TodayWeatherStructureTest.kt`
- Update evidence: `docs/project/WORK_PROGRESS.md`

**Interfaces:**
- Hero compact weather shows location label, temperature/condition, wind direction/speed and gusts without provider-specific branches.
- Expandable module shows source/fallback marker, last update/stale state, current metric rows according to active presentation config, an hourly strip for configured horizon, and daily cards for configured horizon.
- Missing optional metrics render as unavailable/omitted according to presentation config; they do not crash or clear otherwise valid fallback data.
- Manual refresh is available. Background scheduling and weather-rule timeline events are deferred to M15.3.
- Today temporary location override is in-memory/session state unless the user explicitly saves a Workplace/SavedLocation choice.

- [x] **Step 1: Write RED structure/state tests** for Hero injection, fallback/stale metadata, horizons, missing optional metric handling, and no provider-specific symbols in Today UI.
- [x] **Step 2: Verify RED.**
- [x] **Step 3: Wire repository/location/settings state into Today and implement compact + expanded module.**
- [x] **Step 4: Add deterministic screenshots for Basic, Work/Wind, fallback+stale, dark, and fontScale 1.3; run screenshot RED/update/validate loop.**
- [x] **Step 5: Run focused M15 tests + production compile; require GREEN.**
- [x] **Step 6: Commit** `feat: add M15 Today weather` plus separate evidence checkpoint.

**Task 7 acceptance (2026-10-12, new VPS):** RED-first Task-7 test compilation failed on absent weather module/forecast helpers (`8fcf62ea4bbdb07f34fc13a28140b8f1`), then focused compile/unit GREEN. Phone app modules and provider-independent repository wiring committed as `3819cfe`; deterministic five-state Today screenshots as `d4cc70f`. Historical Task-6 anti-Task-7 tests became obsolete and were replaced by positive interface/store-isolation regressions (`3bc5d9d`). Full app gate `dbe31fb186c9c8b3f38b348e75649f94`: **410/410 JVM tests, 57 screenshot references, app debug APK, all GREEN**. More evidence in `docs/project/WORK_PROGRESS.md`. Task 8 remains a separate unstarted closeout.

### Task 8: M15.2 regression, review, and closeout

**Files:**
- Update: `docs/project/WORK_PROGRESS.md`
- Update: `docs/project/CURRENT_STATE.md`
- Update spec only if implementation exposed an explicit accepted ruling.

**Interfaces:** qualification boundary only.

- [x] **Step 1: Run focused M15.2 tests plus M15.1 calendar/Today, M12 settings/navigation, Notes/settings-port, alarms and backup compatibility tests.**
- [x] **Step 2: Run full app JVM tests and independent screenshot validation.**
- [x] **Step 3: Run app `assembleDebug + lintDebug`; run Wear compile/assemble/lint regression.**
- [x] **Step 4: Run `git diff --check`; verify no forbidden payroll/Room/alarm/Wear/Notes/backup semantic changes; record APK hashes.**
- [x] **Step 5: Review provider attribution/User-Agent, location permissions, map attribution, cache/stale behavior and provider-independence against the spec. Repair only proven findings via RED -> GREEN.**
- [x] **Step 6: Record final evidence and commit docs-only closeout. Do not start M15.3 in the same closeout.**
