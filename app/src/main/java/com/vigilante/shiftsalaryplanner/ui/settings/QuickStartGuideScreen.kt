package com.vigilante.shiftsalaryplanner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vigilante.shiftsalaryplanner.payroll.PayMode
import com.vigilante.shiftsalaryplanner.payroll.PayrollSettings

@Composable
fun QuickStartGuideScreen(
    shiftTemplateCount: Int,
    scheduledDaysCount: Int,
    payrollSettings: PayrollSettings,
    onBack: () -> Unit,
    onDismissGuide: () -> Unit,
    onOpenShifts: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenPayrollSettings: () -> Unit,
    onOpenAlarms: () -> Unit
) {
    val payrollReady = payrollSettings.isPayrollReadyForGuide()
    val completedCount = listOf(
        shiftTemplateCount > 0,
        scheduledDaysCount > 0,
        payrollReady
    ).count { it }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(
                title = "Справка",
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = appScreenPadding()),
                verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                item("intro") {
                    GuideHeroCard(
                        completedCount = completedCount,
                        totalCount = 3,
                        onDismissGuide = onDismissGuide
                    )
                }

                item("route-title") {
                    GuideSectionTitle(
                        title = "Быстрый старт",
                        subtitle = "Минимальный маршрут, чтобы приложение начало приносить пользу"
                    )
                }

                item("step-shifts") {
                    QuickStartStepCard(
                        number = 1,
                        title = "Создай шаблоны смен",
                        subtitle = "Минимум: код смены, название, начало/конец и цвет. Код обязателен: по нему смена назначается в календарь и участвует в расчёте.",
                        meta = if (shiftTemplateCount > 0) "Шаблонов: $shiftTemplateCount" else "Пока нет шаблонов",
                        icon = Icons.Rounded.Work,
                        completed = shiftTemplateCount > 0,
                        actionText = if (shiftTemplateCount > 0) "Открыть смены" else "Создать смену",
                        onAction = onOpenShifts
                    )
                }

                item("step-calendar") {
                    QuickStartStepCard(
                        number = 2,
                        title = "Заполни календарь",
                        subtitle = "Нажми на день или открой быстрый ввод снизу. После этого расчёт, будильники и экран «Сегодня» начнут показывать реальные данные.",
                        meta = if (scheduledDaysCount > 0) "Заполнено дней: $scheduledDaysCount" else "Календарь ещё пустой",
                        icon = Icons.Rounded.CalendarMonth,
                        completed = scheduledDaysCount > 0,
                        actionText = "Перейти в календарь",
                        onAction = onOpenCalendar
                    )
                }

                item("step-payroll") {
                    QuickStartStepCard(
                        number = 3,
                        title = "Настрой зарплату",
                        subtitle = "Выбери режим оплаты: почасовая, оклад или за смену. Затем проверь НДФЛ, даты аванса/зарплаты и норму часов.",
                        meta = payrollSettings.payrollGuideMeta(),
                        icon = Icons.Rounded.Paid,
                        completed = payrollReady,
                        actionText = "Открыть расчёт",
                        onAction = onOpenPayrollSettings
                    )
                }

                item("optional") {
                    QuickStartStepCard(
                        number = 4,
                        title = "Дополнительно: будильники",
                        subtitle = "Когда шаблоны и календарь готовы, можно включить будильники по сменам. Они используют время начала из шаблона.",
                        meta = "Можно настроить позже",
                        icon = Icons.Rounded.Alarm,
                        completed = false,
                        actionText = "Открыть будильники",
                        onAction = onOpenAlarms,
                        optional = true
                    )
                }

                item("features-title") {
                    GuideSectionTitle(
                        title = "Разделы приложения",
                        subtitle = "Что где находится и в каком порядке лучше настраивать"
                    )
                }

                item("feature-calendar") {
                    HelpTopicCard(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Календарь",
                        subtitle = "Главный экран графика",
                        bullets = listOf(
                            "Тап по дню открывает выбор смены на конкретную дату.",
                            "Быстрый ввод снизу удобен для массового заполнения месяца.",
                            "Если включены несколько работ, выбирай «Все работы» или конкретную работу сверху.",
                            "Долгий тап по дню показывает смены всех работ, часы и заметки."
                        ),
                        actionText = "Открыть календарь",
                        onAction = onOpenCalendar
                    )
                }

                item("feature-shifts") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Work,
                        title = "Смены",
                        subtitle = "Шаблоны, по которым заполняется календарь",
                        bullets = listOf(
                            "Код смены обязателен: это короткая метка на календаре, например Д, Н, 8Д.",
                            "Начало и конец задают длительность смены, если общее время не выставлено вручную.",
                            "Обед вычитается из оплачиваемых часов, ночные часы используются для доплаты.",
                            "Шаблоны привязываются к работе, а системные статусы общие для всех работ."
                        ),
                        actionText = "Открыть смены",
                        onAction = onOpenShifts
                    )
                }

                item("feature-finance") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Paid,
                        title = "Финансы",
                        subtitle = "Расчёт, выплаты и расхождения",
                        bullets = listOf(
                            "В «Расчёте зарплаты» выбери режим: почасовая, оклад или за смену.",
                            "Для нескольких работ параметры зарплаты задаются отдельно для каждой работы.",
                            "В отчёте можно смотреть месяц, год или произвольный диапазон.",
                            "Фактические выплаты сравниваются с расчётом с учётом допустимого порога."
                        ),
                        actionText = "Настроить зарплату",
                        onAction = onOpenPayrollSettings
                    )
                }

                item("feature-alarms") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Alarm,
                        title = "Будильники",
                        subtitle = "Срабатывания по сменам",
                        bullets = listOf(
                            "Будильники планируются от времени начала смены.",
                            "Поведение звонка, мелодия, вибрация, отложить и длительность задаются в общих настройках будильников.",
                            "Отдельные будущие срабатывания можно отменить без отключения всего планирования.",
                            "В служебной информации проверяются уведомления, полноэкранный режим и точные будильники."
                        ),
                        actionText = "Открыть будильники",
                        onAction = onOpenAlarms
                    )
                }

                item("feature-today-notes-ai") {
                    HelpTopicGrid(
                        topics = listOf(
                            CompactHelpTopic(
                                icon = Icons.Rounded.Today,
                                title = "Сегодня",
                                text = "Сводка дня: смена, ближайший будильник, выплаты, заметки и проверка месяца. Бабблы можно настраивать."
                            ),
                            CompactHelpTopic(
                                icon = Icons.AutoMirrored.Rounded.EventNote,
                                title = "Заметки",
                                text = "Заметки привязываются к дате и смене. Можно добавлять медиа, чек-листы, аудио и фон."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "ИИ",
                                text = "Ассистент понимает команды по сменам, заметкам, будильникам и вопросам по расписанию. Провайдеры: GigaChat, Gemini, OpenAI."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.Payments,
                                title = "Выплаты",
                                text = "Аванс, зарплата и фактические суммы видны в финансах. Расхождение подсвечивается с учётом порога."
                            )
                        )
                    )
                }

                item("workflows-title") {
                    GuideSectionTitle(
                        title = "Практические сценарии",
                        subtitle = "Короткие маршруты для частых задач"
                    )
                }

                item("workflow-first-setup") {
                    HelpTopicCard(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Первая настройка с нуля",
                        subtitle = "Что сделать после установки",
                        bullets = listOf(
                            "Создай профиль, если приложением пользуются несколько человек.",
                            "Создай работы, если графики и зарплата разные.",
                            "Для каждой работы создай свои шаблоны смен.",
                            "Заполни календарь через быстрый ввод или тап по дням.",
                            "Настрой зарплату для каждой работы и проверь первый расчёт."
                        ),
                        actionText = "Начать со смен",
                        onAction = onOpenShifts
                    )
                }

                item("workflow-multiple-jobs") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Work,
                        title = "Несколько работ",
                        subtitle = "Когда в один день могут быть разные смены",
                        bullets = listOf(
                            "В календаре сверху можно выбрать конкретную работу или «Все работы».",
                            "Шаблоны смен, зарплатные настройки, надбавки и удержания ведутся отдельно по работам.",
                            "Системные статусы общие: отпуск, больничный, выходной и пользовательские статусы доступны везде.",
                            "В расчёте можно смотреть отдельную работу или общий итог."
                        ),
                        actionText = "Открыть календарь",
                        onAction = onOpenCalendar
                    )
                }

                item("workflow-month-check") {
                    HelpTopicCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "Проверка месяца",
                        subtitle = "Перед выплатой зарплаты",
                        bullets = listOf(
                            "Открой «Финансы» и выбери нужный месяц, год или диапазон.",
                            "Проверь часы, количество смен, норму и больничные/отпускные строки.",
                            "Сравни «ожидалось / пришло» и порог расхождения.",
                            "Если нужно, сохрани расчётный лист в PDF/CSV."
                        ),
                        actionText = "Открыть расчёт",
                        onAction = onOpenPayrollSettings
                    )
                }

                item("settings-title") {
                    GuideSectionTitle(
                        title = "Настройки",
                        subtitle = "Короткая карта того, за что отвечает каждый раздел"
                    )
                }

                item("settings-guide") {
                    HelpTopicGrid(
                        topics = listOf(
                            CompactHelpTopic(
                                icon = Icons.Rounded.Paid,
                                title = "Расчёт зарплаты",
                                text = "Оклад, ставка, НДФЛ, ночные, РВД, больничные, отпуск, норма часов, даты выплат."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.ColorLens,
                                title = "Внешний вид",
                                text = "Тема, палитра, шрифты по разделам, плотность интерфейса, стиль карточек и фоны."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.Settings,
                                title = "Виджет",
                                text = "Размеры, подписи, цвета смен, расширенный календарь и виджеты заметок."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.Payments,
                                title = "Допвыплаты",
                                text = "Надбавки фиксированной суммой или процентом от оклада, в том числе для ночных."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.Payments,
                                title = "Удержания",
                                text = "Удержания после НДФЛ: фиксированные, регулярные и отображаемые в расчёте."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.CalendarMonth,
                                title = "Производственный календарь",
                                text = "Федеральные праздники, ручные праздники и правило сокращённых предпраздничных дней."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.Sync,
                                title = "Импорт",
                                text = "Загрузка графика из Excel и создание недостающих шаблонов смен."
                            ),
                            CompactHelpTopic(
                                icon = Icons.Rounded.Backup,
                                title = "Резервная копия",
                                text = "Сохраняет календарь, смены, профили, зарплату, внешний вид, виджеты, будильники и заметки."
                            )
                        )
                    )
                }

                item("payroll-deep-settings") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Paid,
                        title = "Подробно: расчёт зарплаты",
                        subtitle = "Какие параметры сильнее всего влияют на итог",
                        bullets = listOf(
                            "Режим оплаты: почасовая ставка, оклад или фиксированная сумма за смену.",
                            "Ночные: база расчёта выбирается отдельно, можно учитывать ручные надбавки.",
                            "Праздники и РВД: задаются через шаблоны смен и производственный календарь.",
                            "Больничные разделяются на часть работодателя и ФСС.",
                            "Для сменного графика можно отключить сокращение предпраздничных дней."
                        ),
                        actionText = "Настроить зарплату",
                        onAction = onOpenPayrollSettings
                    )
                }

                item("appearance-deep-settings") {
                    HelpTopicCard(
                        icon = Icons.Rounded.ColorLens,
                        title = "Подробно: внешний вид",
                        subtitle = "Как привести приложение к своему стилю",
                        bullets = listOf(
                            "Можно выбрать тему, палитру, плотность, стиль карточек и цвет бабблов.",
                            "Шрифты можно задавать отдельно для календаря, финансов, заметок, ИИ и других разделов.",
                            "Live-preview показывает, как будут выглядеть карточки и нижний бар.",
                            "Если интерфейс стал слишком тяжёлым, верни классический стиль или сбрось оформление."
                        ),
                        actionText = null,
                        onAction = null
                    )
                }

                item("backup-deep-settings") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Backup,
                        title = "Подробно: бэкап и перенос",
                        subtitle = "Что сохраняется и как не потерять данные",
                        bullets = listOf(
                            "Ручной экспорт создаёт JSON-файл для переноса на другое устройство.",
                            "Google Drive сохраняет копию в скрытую папку приложения.",
                            "Автозагрузка включается отдельным тумблером и не запускается просто от входа в аккаунт.",
                            "В копию входят график, шаблоны, профили, работы, зарплатные настройки, вид, виджеты, заметки и будильники."
                        ),
                        actionText = null,
                        onAction = null
                    )
                }

                item("assistant-deep-settings") {
                    HelpTopicCard(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Подробно: ИИ-ассистент",
                        subtitle = "Локальные команды и продвинутый режим",
                        bullets = listOf(
                            "Без API ассистент выполняет базовые локальные команды: смены, заметки, будильники и переходы.",
                            "GigaChat, Gemini или OpenAI включают более гибкое понимание фраз.",
                            "Перед изменениями календаря, заметок и будильников можно включить подтверждение действий.",
                            "Голосовые ответы настраиваются отдельно: локальный TTS или сетевой провайдер."
                        ),
                        actionText = null,
                        onAction = null
                    )
                }

                item("reports-and-service") {
                    HelpTopicCard(
                        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                        title = "Отчёты и сервис",
                        subtitle = "То, что помогает проверить приложение и выгрузить данные",
                        bullets = listOf(
                            "Центр отчётов сохраняет расчётные листы и месячные отчёты в PDF/CSV.",
                            "Проверка приложения показывает проблемы с разрешениями, бэкапом, будильниками и календарём.",
                            "Журнал действий хранит последние операции ассистента и приложения.",
                            "Быстрые действия позволяют настроить кнопки в быстром вводе календаря."
                        ),
                        actionText = null,
                        onAction = null
                    )
                }

                item("permissions-after-update") {
                    HelpTopicCard(
                        icon = Icons.Rounded.Settings,
                        title = "После обновления приложения",
                        subtitle = "Что стоит проверить один раз",
                        bullets = listOf(
                            "Некоторые прошивки сбрасывают полноэкранный режим будильника после обновления.",
                            "Открой проверку приложения или служебную информацию в будильниках.",
                            "Проверь уведомления, точные будильники, фоновую работу и полноэкранный показ.",
                            "Если установлен Google Drive, проверь дату последней копии."
                        ),
                        actionText = "Открыть будильники",
                        onAction = onOpenAlarms
                    )
                }

                item("common-mistakes") {
                    HelpTopicCard(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Частые ошибки",
                        subtitle = "Что проверять, если что-то выглядит странно",
                        bullets = listOf(
                            "Смена не сохраняется: проверь код смены, он обязателен.",
                            "В расчёте нули: календарь пустой или выбрана другая работа/другой диапазон.",
                            "Будильник не показывается поверх экрана: проверь полноэкранный режим, уведомления и точные будильники.",
                            "Google Drive не входит: для debug-сборок нужен SHA-1 именно того ключа, которым подписана текущая установка.",
                            "Зарплата не сходится с 1С: настрой порог расхождения и фактические выплаты."
                        ),
                        actionText = null,
                        onAction = null
                    )
                    Spacer(modifier = Modifier.height(appScaledSpacing(112.dp)))
                }
            }
        }
    }
}

@Composable
private fun GuideHeroCard(
    completedCount: Int,
    totalCount: Int,
    onDismissGuide: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(24.dp)),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.44f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(appScaledSpacing(10.dp))
                            .size(appScaledSpacing(28.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Справка и настройка",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Быстрый старт: $completedCount из $totalCount. Ниже есть подробная справка по разделам и настройкам.",
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }

            Text(
                text = "Если приложение открыто впервые, иди сверху вниз. Если уже настроено, используй этот экран как справочник по функциям.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f)
            )

            OutlinedButton(
                onClick = appHapticAction(onAction = onDismissGuide),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Больше не показывать при запуске")
            }
        }
    }
}

@Composable
private fun GuideSectionTitle(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appScaledSpacing(3.dp))
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = appListSecondaryTextColor()
        )
    }
}

@Composable
private fun QuickStartStepCard(
    number: Int,
    title: String,
    subtitle: String,
    meta: String,
    icon: ImageVector,
    completed: Boolean,
    actionText: String,
    onAction: () -> Unit,
    optional: Boolean = false
) {
    val accent = if (completed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.5f),
        border = BorderStroke(1.dp, if (completed) accent.copy(alpha = 0.34f) else appPanelBorderColor()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = if (completed) 0.16f else 0.12f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Готово",
                            tint = accent,
                            modifier = Modifier
                                .padding(appScaledSpacing(8.dp))
                                .size(appScaledSpacing(24.dp))
                        )
                    } else {
                        Text(
                            text = number.toString(),
                            modifier = Modifier.padding(
                                horizontal = appScaledSpacing(13.dp),
                                vertical = appScaledSpacing(8.dp)
                            ),
                            color = accent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(8.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(appScaledSpacing(20.dp))
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(appScaledSpacing(3.dp)))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ) {
                Text(
                    text = if (optional) "Необязательно: $meta" else meta,
                    modifier = Modifier.padding(
                        horizontal = appScaledSpacing(10.dp),
                        vertical = appScaledSpacing(5.dp)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                )
            }

            Button(
                onClick = appHapticAction(onAction = onAction),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun HelpTopicCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    bullets: List<String>,
    actionText: String?,
    onAction: (() -> Unit)?
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(appCornerRadius(22.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.5f),
        border = BorderStroke(1.dp, appPanelBorderColor()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appCardPadding()),
            verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(appScaledSpacing(12.dp)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(appScaledSpacing(9.dp))
                            .size(appScaledSpacing(24.dp))
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = appListSecondaryTextColor()
                    )
                }
            }

            bullets.forEach { bullet ->
                Text(
                    text = "• $bullet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                )
            }

            if (actionText != null && onAction != null) {
                Button(
                    onClick = appHapticAction(onAction = onAction),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionText)
                }
            }
        }
    }
}

private data class CompactHelpTopic(
    val icon: ImageVector,
    val title: String,
    val text: String
)

@Composable
private fun HelpTopicGrid(
    topics: List<CompactHelpTopic>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(appBlockSpacing())
    ) {
        topics.chunked(2).forEach { rowTopics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(appBlockSpacing())
            ) {
                rowTopics.forEach { topic ->
                    CompactHelpTopicCard(
                        topic = topic,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowTopics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CompactHelpTopicCard(
    topic: CompactHelpTopic,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(appCornerRadius(18.dp)),
        color = appBubbleBackgroundColor(defaultAlpha = 0.46f),
        border = BorderStroke(1.dp, appPanelBorderColor()),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(appScaledSpacing(12.dp)),
            verticalArrangement = Arrangement.spacedBy(appScaledSpacing(6.dp))
        ) {
            Icon(
                imageVector = topic.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(appScaledSpacing(22.dp))
            )
            Text(
                text = topic.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = topic.text,
                style = MaterialTheme.typography.bodySmall,
                color = appListSecondaryTextColor()
            )
        }
    }
}

private fun PayrollSettings.isPayrollReadyForGuide(): Boolean {
    return when (runCatching { PayMode.valueOf(payMode) }.getOrElse { PayMode.HOURLY }) {
        PayMode.PER_SHIFT -> true
        PayMode.HOURLY -> baseSalary > 0.0 || extraSalary > 0.0 || monthlyNormHours > 0.0
        PayMode.MONTHLY_SALARY -> baseSalary > 0.0
    }
}

private fun PayrollSettings.payrollGuideMeta(): String {
    val mode = when (runCatching { PayMode.valueOf(payMode) }.getOrElse { PayMode.HOURLY }) {
        PayMode.HOURLY -> "Почасовая"
        PayMode.MONTHLY_SALARY -> "Оклад"
        PayMode.PER_SHIFT -> "За смену"
    }
    return "$mode · база ${formatMoney(baseSalary)} · НДФЛ ${(ndflPercent * 100).toInt()}%"
}
