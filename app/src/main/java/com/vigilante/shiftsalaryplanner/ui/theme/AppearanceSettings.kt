package com.vigilante.shiftsalaryplanner.ui.theme

import java.time.LocalTime

enum class ThemeMode {
    LIGHT,
    DARK,
    AUTO,
    SCHEDULE
}

enum class AppColorSchemeMode {
    MINT,
    OCEAN,
    SUNSET,
    GRAPHITE,
    CUSTOM,
    DYNAMIC
}

enum class AppFontMode {
    SYSTEM,
    SANS,
    SERIF,
    MONO,
    EXTERNAL_MANROPE,
    EXTERNAL_CUSTOM
}

enum class AppearanceFontSection {
    CALENDAR,
    TODAY,
    ASSISTANT,
    NOTES,
    FINANCE,
    ALARMS,
    SHIFTS,
    SETTINGS
}

enum class UiDensityMode {
    COMFORTABLE,
    COMPACT
}

enum class UiContrastMode {
    STANDARD,
    HIGH
}

enum class AnimationSpeedMode {
    NORMAL,
    SLOW,
    OFF
}

enum class CornerStyleMode {
    SOFT,
    STANDARD,
    SHARP
}

enum class AppVisualStyleMode {
    CLASSIC,
    EXPRESSIVE,
    EXPRESSIVE_GLASS
}

enum class CalendarDefaultWorkplaceMode {
    ALL_WORKPLACES,
    ACTIVE_WORKPLACE
}

enum class CurrencySymbolMode(
    val symbol: String,
    val code: String,
    val displayName: String,
    val region: String
) {
    RUB("₽", "RUB", "Российский рубль", "СНГ и соседние страны"),
    BYN("Br", "BYN", "Белорусский рубль", "СНГ и соседние страны"),
    KZT("₸", "KZT", "Казахстанский тенге", "СНГ и соседние страны"),
    UAH("₴", "UAH", "Украинская гривна", "СНГ и соседние страны"),
    GEL("₾", "GEL", "Грузинский лари", "СНГ и соседние страны"),
    AMD("֏", "AMD", "Армянский драм", "СНГ и соседние страны"),
    AZN("₼", "AZN", "Азербайджанский манат", "СНГ и соседние страны"),
    KGS("с", "KGS", "Киргизский сом", "СНГ и соседние страны"),
    UZS("so'm", "UZS", "Узбекский сум", "СНГ и соседние страны"),
    TJS("ЅМ", "TJS", "Таджикский сомони", "СНГ и соседние страны"),
    TMT("m", "TMT", "Туркменский манат", "СНГ и соседние страны"),
    MDL("L", "MDL", "Молдавский лей", "СНГ и соседние страны"),

    EUR("€", "EUR", "Евро", "Европа"),
    GBP("£", "GBP", "Британский фунт", "Европа"),
    CHF("Fr", "CHF", "Швейцарский франк", "Европа"),
    PLN("zł", "PLN", "Польский злотый", "Европа"),
    CZK("Kč", "CZK", "Чешская крона", "Европа"),
    HUF("Ft", "HUF", "Венгерский форинт", "Европа"),
    RON("lei", "RON", "Румынский лей", "Европа"),
    BGN("лв", "BGN", "Болгарский лев", "Европа"),
    SEK("kr", "SEK", "Шведская крона", "Европа"),
    NOK("kr", "NOK", "Норвежская крона", "Европа"),
    DKK("kr", "DKK", "Датская крона", "Европа"),
    RSD("дин", "RSD", "Сербский динар", "Европа"),
    TRY("₺", "TRY", "Турецкая лира", "Европа"),

    USD("$", "USD", "Доллар США", "Америка"),
    CAD("C$", "CAD", "Канадский доллар", "Америка"),
    MXN("$", "MXN", "Мексиканское песо", "Америка"),
    BRL("R$", "BRL", "Бразильский реал", "Америка"),
    ARS("$", "ARS", "Аргентинское песо", "Америка"),
    CLP("$", "CLP", "Чилийское песо", "Америка"),
    COP("$", "COP", "Колумбийское песо", "Америка"),
    PEN("S/", "PEN", "Перуанский соль", "Америка"),
    UYU("\$U", "UYU", "Уругвайское песо", "Америка"),

    CNY("¥", "CNY", "Китайский юань", "Азия"),
    JPY("¥", "JPY", "Японская иена", "Азия"),
    KRW("₩", "KRW", "Южнокорейская вона", "Азия"),
    INR("₹", "INR", "Индийская рупия", "Азия"),
    THB("฿", "THB", "Тайский бат", "Азия"),
    VND("₫", "VND", "Вьетнамский донг", "Азия"),
    IDR("Rp", "IDR", "Индонезийская рупия", "Азия"),
    MYR("RM", "MYR", "Малайзийский ринггит", "Азия"),
    PHP("₱", "PHP", "Филиппинское песо", "Азия"),
    SGD("S$", "SGD", "Сингапурский доллар", "Азия"),
    HKD("HK$", "HKD", "Гонконгский доллар", "Азия"),
    TWD("NT$", "TWD", "Новый тайваньский доллар", "Азия"),
    PKR("₨", "PKR", "Пакистанская рупия", "Азия"),
    BDT("৳", "BDT", "Бангладешская така", "Азия"),

    AED("د.إ", "AED", "Дирхам ОАЭ", "Ближний Восток"),
    SAR("ر.س", "SAR", "Саудовский риял", "Ближний Восток"),
    QAR("ر.ق", "QAR", "Катарский риял", "Ближний Восток"),
    KWD("د.ك", "KWD", "Кувейтский динар", "Ближний Восток"),
    BHD(".د.ب", "BHD", "Бахрейнский динар", "Ближний Восток"),
    OMR("ر.ع", "OMR", "Оманский риал", "Ближний Восток"),
    ILS("₪", "ILS", "Израильский шекель", "Ближний Восток"),
    JOD("د.ا", "JOD", "Иорданский динар", "Ближний Восток");

    val shortLabel: String
        get() = "$symbol $code"

    val fullLabel: String
        get() = "$symbol $code • $displayName"
}

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val colorSchemeMode: AppColorSchemeMode = AppColorSchemeMode.MINT,
    val fontMode: AppFontMode = AppFontMode.SYSTEM,
    val calendarFontMode: AppFontMode? = null,
    val todayFontMode: AppFontMode? = null,
    val assistantFontMode: AppFontMode? = null,
    val notesFontMode: AppFontMode? = null,
    val financeFontMode: AppFontMode? = null,
    val alarmsFontMode: AppFontMode? = null,
    val shiftsFontMode: AppFontMode? = null,
    val settingsFontMode: AppFontMode? = null,
    val currencySymbolMode: CurrencySymbolMode = CurrencySymbolMode.RUB,
    val fontScale: Float = 1.0f,
    val uiDensityMode: UiDensityMode = UiDensityMode.COMFORTABLE,
    val uiContrastMode: UiContrastMode = UiContrastMode.STANDARD,
    val animationSpeedMode: AnimationSpeedMode = AnimationSpeedMode.NORMAL,
    val cornerStyleMode: CornerStyleMode = CornerStyleMode.STANDARD,
    val visualStyleMode: AppVisualStyleMode = AppVisualStyleMode.CLASSIC,
    val calendarDefaultWorkplaceMode: CalendarDefaultWorkplaceMode = CalendarDefaultWorkplaceMode.ALL_WORKPLACES,
    val customPrimaryHex: String = "#0D665A",
    val customSecondaryHex: String = "#3F6371",
    val customTertiaryHex: String = "#5A5C7E",
    val customBackgroundHex: String = "#F4F8F7",
    val customBubbleHex: String = "",
    val customFontUri: String = "",
    val customFontDisplayName: String = "",
    val scheduledDarkStartHour: Int = 22,
    val scheduledDarkStartMinute: Int = 0,
    val scheduledDarkEndHour: Int = 7,
    val scheduledDarkEndMinute: Int = 0
)

fun AppearanceSettings.fontModeForSection(section: AppearanceFontSection): AppFontMode? {
    return when (section) {
        AppearanceFontSection.CALENDAR -> calendarFontMode
        AppearanceFontSection.TODAY -> todayFontMode
        AppearanceFontSection.ASSISTANT -> assistantFontMode
        AppearanceFontSection.NOTES -> notesFontMode
        AppearanceFontSection.FINANCE -> financeFontMode
        AppearanceFontSection.ALARMS -> alarmsFontMode
        AppearanceFontSection.SHIFTS -> shiftsFontMode
        AppearanceFontSection.SETTINGS -> settingsFontMode
    }
}

fun AppearanceSettings.scheduledDarkStartTime(): LocalTime {
    return LocalTime.of(
        scheduledDarkStartHour.coerceIn(0, 23),
        scheduledDarkStartMinute.coerceIn(0, 59)
    )
}

fun AppearanceSettings.scheduledDarkEndTime(): LocalTime {
    return LocalTime.of(
        scheduledDarkEndHour.coerceIn(0, 23),
        scheduledDarkEndMinute.coerceIn(0, 59)
    )
}

fun isDarkTimeNow(
    now: LocalTime,
    start: LocalTime,
    end: LocalTime
): Boolean {
    if (start == end) return false

    return if (start < end) {
        now >= start && now < end
    } else {
        now >= start || now < end
    }
}

fun sanitizeHexColor(input: String, fallback: String): String {
    val clean = input.trim().removePrefix("#").uppercase()
    val normalized = when (clean.length) {
        3 -> clean.map { "$it$it" }.joinToString(separator = "")
        6 -> clean
        8 -> clean.substring(2)
        else -> ""
    }

    if (normalized.length != 6 || normalized.any { it !in "0123456789ABCDEF" }) {
        return fallback
    }
    return "#$normalized"
}
