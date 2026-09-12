# ShiftSalaryPlanner vNext Modernization

Статус: **канонический архитектурный план, утверждённое направление**  
Дата фиксации: **2026-09-11**  
Репозиторий: `genrudko/ShiftSalaryPlanner`  
Рабочее имя следующего поколения: **vNext Modernization**

> Этот документ описывает долговременную архитектуру, границы и roadmap. Текущую фактическую границу всегда смотреть в [`CURRENT_STATE.md`](./CURRENT_STATE.md).

## 1. Зачем существует этот проект

ShiftSalaryPlanner уже является работающим продуктом с большим количеством реальных пользовательских сценариев: календарь смен, несколько работ, расчёт зарплаты, выплаты, будильники, заметки, резервные копии, Google Drive, виджеты, Wear OS и ИИ-помощник. Проблема не в отсутствии функциональности, а в том, что приложение исторически росло быстрее своей архитектуры.

Текущая кодовая база содержит большой объём UI-state и orchestration в корневом Compose-контуре, крупные файлы экранов, слабую изоляцию feature-границ и ограниченную регрессионную защиту. UI функционально богат, но визуально перегружен и имеет слабую иерархию информации.

Цель vNext — **не переписать приложение**, а безопасно модернизировать его: сохранить поведение и данные, отделить архитектурные ответственности, затем перестроить информационную архитектуру и визуальный слой.

## 2. Источники истины

При конфликте информации использовать следующий порядок:

1. фактический Git state текущей рабочей ветки;
2. явно утверждённый bounded task / issue для текущей работы;
3. [`CURRENT_STATE.md`](./CURRENT_STATE.md);
4. этот документ;
5. README и прочая документация;
6. история чатов и человеческая память.

Task может сужать scope, но не должен молча отменять архитектурное решение из этого документа. Если нужно изменить решение — сначала обновляется каноническая документация.

## 3. Известный стартовый baseline

На момент создания плана:

- default branch: `master`;
- кандидат на последний канонический Git baseline: `3ece60f6ee694ef1baf251e7eec5beb29f146e45` от 2026-06-04;
- `app/build.gradle.kts` на этом commit содержит `versionCode = 194`, `versionName = "6.4"`;
- присланный владельцем актуальный APK имеет SHA-256 `665888705c888c73ebc809e8a75cf46f586f115af7789a0412adfc700f572ba5` и содержит AGP VCS revision `3ece60f6ee694ef1baf251e7eec5beb29f146e45`;
- номера версий APK исторически назначались вручную и **не являются доказательством Git-состояния**;
- владелец сообщил, что последняя публикация в RuStore была 2026-06-02; store build не считается source of truth;
- локальная рабочая папка проекта у владельца сохранилась и должна быть проверена в M0 до признания GitHub baseline окончательно каноническим;
- модули: `app`, `wear`;
- текущая сборка требует стабильный debug keystore на этапе Gradle configuration; debug и release используют `stableDebug` signing config;
- release minification отключена;
- Development Bridge уже существует на VPS, но `genrudko/ShiftSalaryPlanner` на момент фиксации плана ещё не является отдельным managed Bridge repo.

## 4. Главные продуктовые инварианты

Модернизация не считается успешной, если новый UI красивее, но теряет текущее поведение. Обязательные инварианты:

- существующие пользовательские данные переживают обновление;
- старые поддерживаемые backup-файлы восстанавливаются;
- payroll не меняет результаты без отдельного доказанного bugfix/task;
- несколько работ и их payroll-настройки сохраняют семантику;
- календарь остаётся быстрым для визуального чтения графика;
- шаблоны смен и системные статусы сохраняют возможности;
- будильники по сменам, snooze, отмены и reboot/reschedule не теряются;
- уведомления/full-screen alarm поведение проверяется на поддерживаемых Android-версиях;
- Wear OS и widgets не ломаются из-за внутреннего UI-refactor;
- backup/restore, Google Drive и импорт/экспорт сохраняют совместимость настолько, насколько это технически возможно;
- новая версия устанавливается поверх предыдущей без необходимости вручную пересоздавать графики и настройки.

## 5. Что намеренно НЕ делаем

- Не делаем big-bang rewrite.
- Не переписываем domain/data слой просто ради эстетики кода.
- Не смешиваем архитектурный refactor, dependency upgrade и redesign в одном большом PR.
- Не создаём десятки Gradle-модулей без доказанной выгоды.
- Не вводим Hilt/Koin/MVI/Clean Architecture только ради соответствия моде.
- Не меняем Room schema без продуктовой причины.
- Не удаляем старое поведение до миграции и проверки соответствующего сценария.
- Не добавляем крупные новые функции во время основной миграции; они идут в backlog.
- Не используем `versionName`/`versionCode` как способ определить исходный Git commit.
- Не выпускаем production APK из dirty working tree.

## 6. Архитектурная стратегия

Используется **controlled strangler modernization**: старый продукт остаётся рабочей спецификацией, а новые границы последовательно вытесняют старый orchestration.

Порядок зависимостей:

```text
Recover baseline
      ↓
Reproducible build
      ↓
Behavioral safety net
      ↓
App shell / navigation / state boundaries
      ↓
Domain-data hardening where needed
      ↓
UX / information architecture
      ↓
Design system + vertical slices
      ↓
Integrations / Wear / dependencies
      ↓
Release qualification
```

## 7. Целевая структура

Первоначально сохраняются Gradle-модули `app` и `wear`. Внутри `app` вводятся логические package boundaries:

```text
app/
  app/
    MainActivity
    AppRoot
    navigation/
    composition/

  feature/
    today/
    calendar/
    payroll/
    shifts/
    alarms/
    notes/
    assistant/
    settings/

  domain/
    calendar/
    payroll/
    shifts/
    ...

  data/
    room/
    datastore/
    repositories/
    migrations/

  integration/
    alarms/
    backup/
    drive/
    importexport/
    wearsync/

  design/
    theme/
    tokens/
    components/

wear/
  ...
```

Это целевая логическая форма, а не требование немедленно переместить каждый существующий файл.

### 7.1 MainActivity / AppRoot

`MainActivity` должна отвечать за Android lifecycle/window/entry point и запуск Compose. Она не должна создавать десятки Store/DAO, владеть feature-state или вручную закрывать каждый экран.

`AppRoot` отвечает за composition root приложения и подключение root navigation. Создание долгоживущих зависимостей должно быть отделено от отображения UI.

### 7.2 Экранная модель

Предпочтительная базовая модель:

```text
Composable
    ↓ actions
ViewModel / feature state owner
    ↓
application/domain service or repository
    ↓
Room / DataStore / Android service / external API
```

Для основных экранов стандарт по умолчанию:

- immutable `UiState`;
- явные `Action`/event callbacks;
- `AndroidX ViewModel` там, где состояние должно переживать recomposition/configuration;
- `StateFlow` для observable state;
- чистые функции/reducers сохраняются там, где они уже полезны;
- бизнес-расчёты не живут в Composable;
- DAO не должны становиться публичным API UI-слоя.

Новый framework состояния не вводится без доказанной причины.

## 8. Navigation target

Текущая boolean-навигация должна быть заменена типизированной моделью destinations/back stack.

Предварительная верхнеуровневая информационная архитектура:

```text
Сегодня | Календарь | Финансы | Ещё
```

В `Ещё` могут жить:

- Смены;
- Будильники;
- Заметки;
- ИИ-ассистент;
- Настройки;
- backup/import/service screens.

Часть функций должна стать контекстной:

- будильники смены доступны из смены;
- заметка даты доступна из дня;
- payroll settings рабочего места доступны рядом с этим рабочим местом/финансами;
- связанные действия показываются рядом с сущностью, а не только в глобальном разделе.

Финальная IA утверждается в M8 после отдельного UX-проектирования; этот раздел задаёт направление, а не пиксельный контракт.

## 9. UX и design principles

Редизайн должен решать иерархию информации, а не только стилизацию.

Принципы:

- меньше декоративных контейнеров и вложенных карточек;
- больше whitespace и яснее typography hierarchy;
- денежные и временные показатели получают визуальный приоритет;
- secondary actions не конкурируют с primary information;
- progressive disclosure для сложных настроек;
- power-user возможности сохраняются, но не показываются одновременно каждому пользователю;
- текущая сильная сторона — визуально читаемый цветной календарь — сохраняется;
- не вводить стекло/Expressive/анимации как цель сами по себе;
- accessibility и font scaling учитывать в design system, а не исправлять в конце.

## 10. Build и release discipline

После M2 любой чистый checkout должен собираться на VPS без локального Android Studio окружения владельца.

Обязательные правила:

- CI/debug signing отделён от release signing;
- production signing material не должен без необходимости храниться на общем executor VPS;
- `assembleDebug`, unit tests и lint должны запускаться воспроизводимо;
- production/release artifact создаётся только из `git dirty = false`;
- для production artifact фиксируются `versionName`, `versionCode`, Git SHA и build type; желательно также build timestamp;
- dependency modernization выполняется отдельными bounded tasks после стабилизации архитектуры, если конкретная зависимость не блокирует более ранний этап.

## 11. Testing strategy

### 11.1 Characterization first

Перед опасным refactor сначала фиксируется текущее поведение тестом. Особенно это касается payroll, persistence и alarms.

### 11.2 Payroll

Минимальная матрица должна охватывать:

- hourly;
- salary;
- per-shift;
- НДФЛ;
- night hours;
- holidays;
- РВД/специальные дни;
- overtime, если поддерживается текущей моделью;
- sick leave;
- vacation;
- additions;
- deductions;
- payment schedule / аванс / зарплата;
- один workplace;
- несколько workplaces;
- диапазоны и границы периода.

Непонятный расчёт не «исправляется по красоте». Сначала определяется: это bug или существующее бизнес-правило.

### 11.3 Persistence

Нужны проверки:

```text
old Room DB -> migrations -> current DB
old supported JSON backup -> restore -> current model
old DataStore preferences -> current settings model
```

### 11.4 Alarms

Проверяются как минимум scheduling/cancellation/reschedule, смена времени, reboot/boot restoration и критические Android permission/policy сценарии. Чистую доменную часть тестировать вне устройства; platform behavior — targeted instrumentation/manual qualification.

## 12. Development workflow через Development Bridge

Целевая схема:

```text
GitHub canonical
    ↓
Development Bridge managed repo
    ↓
isolated worktree / bounded branch
    ↓
characterization or failing test when applicable
    ↓
implementation
    ↓
targeted tests
    ↓
relevant full gate
    ↓
diff/review
    ↓
GitHub PR / merge only when allowed by task
```

Правила:

- `genrudko/ShiftSalaryPlanner` — отдельный проект; не путать с `genrudko/shift_helper`/Bridge `shift-helper`;
- длинные операции выполнять durable jobs, а не сериями хрупких интерактивных shell-вызовов;
- один task должен иметь конкретную цель, allowed/forbidden scope и acceptance;
- PR должен быть ревьюируемым; «refactor architecture + redesign + dependencies» недопустим как единый changeset;
- не merge/release/deploy без явного разрешения владельца или task contract;
- после завершения milestone обновлять `CURRENT_STATE.md`.

## 13. Программа работ

### PHASE I — Recover & Stabilize

#### M0 — Recovery & Canonical Baseline

**Цель:** доказать, какое состояние исходников является последним рабочим baseline.

Работы:

- проверить локальную папку владельца: `git rev-parse HEAD`, `git status`, staged/untracked, diff относительно `3ece60f6...`;
- не модифицировать локальное дерево до снятия evidence;
- если tree clean на `3ece60f6...`, признать GitHub baseline каноническим;
- если есть изменения — сохранить полный diff/копию и определить, относятся ли они к актуальному APK;
- использовать APK только как forensic/golden artifact, а не автоматически декомпилировать весь проект;
- при необходимости точечно сравнить manifest/resources/classes/behavior с APK;
- зафиксировать окончательный baseline commit и provenance в `CURRENT_STATE.md`.

**Exit:** существует единственный документированный canonical source baseline, а возможные локальные изменения не потеряны.

**Forbidden:** refactor, redesign, dependency upgrades, destructive cleanup.

#### M1 — Development Bridge Onboarding

**Цель:** сделать проект полноценным managed repo нового рабочего контура.

Работы:

- зарегистрировать `genrudko/ShiftSalaryPlanner` как отдельный Bridge repo/project target;
- проверить remote/branch/HEAD/status;
- проверить isolated worktree creation;
- проверить Git read/write workflow в разрешённой рабочей ветке;
- проверить durable `repository_exec`/job path;
- определить безопасную стратегию GitHub PR/push без ручной передачи файлов владельцем.

**Exit:** исполнитель через MCP может сам прочитать repo, создать изолированную рабочую область, выполнить bounded изменение, тесты и подготовить GitHub changeset.

#### M2 — Reproducible VPS Build

**Цель:** убрать зависимость разработки от локального Android Studio ПК.

Работы:

- зафиксировать требуемые JDK/Android SDK/Gradle компоненты;
- обеспечить чистую VPS build environment;
- устранить обязательный stable keystore как blocker конфигурации для CI/debug;
- сохранить совместимость debug signature там, где она реально нужна Wear/Data Layer development flow, но не смешивать это с production signing;
- проверить `./gradlew ... assembleDebug`;
- запустить unit tests;
- запустить lint и классифицировать существующий baseline debt отдельно от новых ошибок;
- документировать команды/gate для последующих tasks.

**Exit:** clean checkout на VPS воспроизводимо собирается и тестируется без файлов с домашнего ПК, кроме явно управляемых non-production development credentials при необходимости.

#### M3 — Behavioral Safety Net

**Цель:** защитить наиболее дорогие для регрессии функции до архитектурной операции.

Работы:

- инвентаризировать существующие payroll tests;
- добавить characterization fixtures для ключевых payroll сценариев;
- защитить multi-workplace calculations;
- добавить persistence/migration fixtures для критических данных;
- зафиксировать backup compatibility examples;
- выделить testable alarm scheduling rules и добавить unit tests;
- составить небольшой manual/device qualification checklist только для того, что невозможно доказать JVM-тестами.

**Exit:** критический refactor может дать красный сигнал до выпуска неверной зарплаты/потери данных/явной поломки alarm rules.

### PHASE II — Re-architect

#### M4 — App Shell Extraction

Вынести creation/composition dependencies и root orchestration из `MainActivity`; оставить Activity маленьким Android entry point. UI на этом этапе должен функционально оставаться прежним.

**Exit:** `MainActivity` больше не является главным владельцем прикладного состояния и service construction.

#### M5 — Navigation Rewrite

Создать типизированные destinations/root navigation; постепенно заменить boolean screen flags и giant back-handler. Сначала сохранить текущую пользовательскую структуру, не смешивая с redesign.

**Exit:** root navigation централизована, back stack предсказуем и покрыт targeted tests.

#### M6 — State & Feature Boundaries

Последовательно выделить `Today`, `Calendar`, `Payroll`, `Shifts`, `Alarms`, `Notes`, `Assistant`, `Settings`. Ввести `UiState`/actions/ViewModel там, где это оправдано. Мигрировать по одному feature.

**Exit:** крупные feature можно читать, тестировать и изменять независимо; root не владеет их внутренним state.

#### M7 — Domain/Data Hardening

Разорвать прямые UI->DAO/Store связи там, где они мешают feature boundaries; ввести узкие repository/service interfaces. Не переписывать рабочий payroll engine без необходимости.

**Exit:** UI можно менять без массового изменения persistence и domain logic.

### PHASE III — Redesign & Migrate

#### M8 — UX / Information Architecture

На основе работающей версии и пользовательских сценариев утвердить целевую IA, primary navigation, context actions, hierarchy screens и settings taxonomy.

**Exit:** утверждён UX-spec до массового UI implementation.

#### M9 — Design System

Зафиксировать typography, spacing, surfaces, shapes, colors, buttons, fields, chips, monetary values, calendar cells, dialogs/sheets, accessibility rules.

**Exit:** новые экраны собираются из общего языка компонентов, а не локальных стилей.

#### M10 — Calendar Vertical Slice

Мигрировать Calendar month, day detail, quick assignment/brush, workplace/status interactions и representative multi-workplace states на новый design system. Сохранить скорость визуального чтения графика и полный семантический объём ячейки.

#### M11 — Finance Vertical Slice

Мигрировать Finance Summary, Payments/fact-vs-plan, Calculation, payslip и контекстные payroll settings. Это самый чувствительный бизнес-срез и должен опираться на M3; арифметика payroll не меняется ради UI.

#### M12 — More / Workplaces / Contextual Settings Shell

Собрать grouped More (`Work / Tools / App / Data / Advanced`), сделать Workplace сущностью первого класса и перенести discoverability entity-owned settings ближе к их контексту без удаления старых возможностей.

#### M13 — Shifts & Alarms

Мигрировать shift templates/editor и alarm flows; сохранить Android scheduling semantics и привязку шаблонов/рабочих мест.

#### M14 — Notes & Assistant

Мигрировать notes/media и AI assistant. Assistant остаётся вторичным/экспериментальным до доказанной ценности конкретных сценариев и должен работать через явные application interfaces.

#### M15 — Today + Remaining Settings Rationalization

Перевести на новый design system конфигурируемую сводку Today и завершить рационализацию глобальных Settings. Today дополняет Calendar-first workflow, а не заменяет его; настройки конкретной сущности по возможности остаются рядом с сущностью.

#### M16 — Integration Hardening

Проверить Google Drive, backup, import/export, widgets, notifications, services и platform integrations после основной миграции.

#### M17 — Wear OS

Стабилизировать явный phone->Wear data contract/DTO, локальный cache и companion screens. Wear не должен зависеть от внутренних UI-state phone-приложения.

#### M18 — Dependency Modernization

Отдельно провести dependency audit и обновления Compose BOM/прочих библиотек, удаляя deprecated API. Не использовать dependency update как скрытый redesign/refactor.

#### M19 — Performance & Accessibility

Проверить startup, recomposition hotspots, calendar rendering, Room queries, battery/alarm behavior, TalkBack/content descriptions, font scaling и contrast.

#### M20 — Release Qualification

Проверить upgrade с поддерживаемой старой установки, migrations, backup restore, alarms/reboot, Wear sync, widgets, signing и production artifact provenance.

#### M21 — Release & Cleanup

Определить финальные versionCode/versionName, выпустить production artifact/RuStore release, tag source commit; только после квалификации удалить доказанно мёртвые compatibility paths.

## 14. Пять настоящих контрольных границ

Проект не должен превращаться в бюрократию из двадцати искусственных gate. Основные доказуемые границы:

**A — Canonical baseline proven**  
Известно, из каких исходников продолжается разработка.

**B — VPS builds independently**  
Bridge/VPS способен без ручного копипаста собирать и тестировать проект.

**C — Architecture disentangled, behavior preserved**  
Главные feature boundaries, state и navigation отделены при сохранённом старом поведении.

**D — Core UX migrated**  
Today, Calendar и Finance работают в новой IA/design system; остальные основные flows мигрированы.

**E — Upgrade-safe production release**  
Старая установка безопасно обновляется, данные/расчёты/alarms/integrations квалифицированы, artifact имеет однозначный Git provenance.

## 15. Размерность implementation tasks

Milestone — не один prompt и не один PR. Пример M5:

```text
M5.1 navigation inventory
M5.2 destination model
M5.3 root navigation host
M5.4 migrate Today destination
M5.5 migrate Calendar destination
M5.6 migrate Payroll destination
M5.7 remove obsolete boolean navigation
M5.8 back-stack regression gate
```

Каждый bounded task обязан содержать:

- objective;
- baseline branch/commit;
- allowed scope;
- forbidden scope;
- expected files/areas;
- tests/gate;
- acceptance criteria;
- stop condition.

## 16. Risk register

### Payroll silent regression — CRITICAL

Митигируется characterization tests и запретом semantic cleanup без доказательства.

### Data/backup incompatibility — CRITICAL

Митигируется migration fixtures, real backup restore tests и non-destructive schema policy.

### Alarm/platform behavior — HIGH

Митигируется разделением pure scheduling rules и Android qualification.

### Navigation scenario loss — HIGH

Митигируется inventory старых destinations/actions до удаления boolean flows.

### Scope creep — HIGH

Новые крупные features не входят в migration milestones.

### Over-architecture — MEDIUM

Package boundaries раньше Gradle modularization; framework добавляется только при измеримой выгоде.

### Dependency churn — MEDIUM

Dependency upgrades отделяются от architecture/UI changes.

## 17. Decision register

На 2026-09-11 приняты следующие решения:

1. **No rewrite.** Используется controlled modernization существующего продукта.
2. **Behavior before aesthetics.** Сначала baseline/build/tests/architecture, потом redesign.
3. **GitHub canonical after M0 proof.** Локальная папка и APK используются только для восстановления/verification.
4. **APK is golden/forensic artifact, not source tree.** Декомпиляция — резервный и точечный инструмент.
5. **Keep `app` + `wear` initially.** Не начинать с массовой Gradle modularization.
6. **Typed navigation.** Boolean-based root navigation должна исчезнуть.
7. **ViewModel + StateFlow by default, no mandatory new UI framework.**
8. **Payroll semantics protected.** Не менять без отдельной причины и тестов.
9. **New IA is allowed.** vNext сохраняет возможности, но не обязана копировать UX-модель 6.x один в один.
10. **Target primary navigation direction:** `Сегодня / Календарь / Финансы / Ещё`, финально утверждается M8.
11. **Bridge/VPS is the intended development execution path.** Владелец не должен вручную копировать Kotlin-фрагменты между чатом и IDE.
12. **Production artifacts must be traceable to clean Git state.**

## 18. Definition of project success

vNext считается завершённым, когда приложение сохраняет зрелые функции текущего ShiftSalaryPlanner, безопасно обновляет существующие пользовательские данные, имеет новую понятную IA/UI, воспроизводимо собирается из clean Git commit и может дальше развиваться небольшими изолированными задачами без возврата к god-composable и ручному copy/paste workflow.
