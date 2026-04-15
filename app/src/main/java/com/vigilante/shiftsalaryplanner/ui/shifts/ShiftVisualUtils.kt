package com.vigilante.shiftsalaryplanner

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Factory
import androidx.compose.material.icons.rounded.Fastfood
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Store
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.vector.ImageVector
import dalvik.system.BaseDexClassLoader
import dalvik.system.DexFile
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

data class ShiftIconOptionGroup(
    val title: String,
    val keys: List<String>
)

private data class ShiftIconDescriptor(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val aliases: List<String> = emptyList()
)

private const val MATERIAL_ROUNDED_KEY_PREFIX = "MSR:"
const val MATERIAL_ROUNDED_GROUP_TITLE = "Material (все)"

private val baseShiftIconDescriptors = listOf(
    ShiftIconDescriptor("SUN", "Солнце", Icons.Rounded.WbSunny, listOf("день", "дневная")),
    ShiftIconDescriptor("MOON", "Ночь", Icons.Rounded.Bedtime, listOf("ночная", "сон")),
    ShiftIconDescriptor("EIGHT", "8 часов", Icons.Rounded.Star, listOf("восьмичасовая", "короткая")),
    ShiftIconDescriptor("STAR", "Звезда", Icons.Rounded.Star, listOf("приоритет", "важно")),
    ShiftIconDescriptor("HOME", "Дом", Icons.Rounded.Home, listOf("выходной", "домашняя")),
    ShiftIconDescriptor("WORK", "Работа", Icons.Rounded.Work, listOf("смена", "рабочая")),
    ShiftIconDescriptor("TASK", "Задача", Icons.Rounded.TaskAlt, listOf("выполнено", "чек")),
    ShiftIconDescriptor("CHECK", "Готово", Icons.Rounded.CheckCircle, listOf("галочка", "выполнено")),
    ShiftIconDescriptor("FLAG", "Флаг", Icons.Rounded.Flag, listOf("метка", "важно")),
    ShiftIconDescriptor("OT", "Отпуск", Icons.Rounded.BeachAccess, listOf("каникулы")),
    ShiftIconDescriptor("SICK", "Больничный", Icons.Rounded.Healing, listOf("болезнь")),
    ShiftIconDescriptor("HOSPITAL", "Медпомощь", Icons.Rounded.LocalHospital, listOf("госпиталь", "больница")),
    ShiftIconDescriptor("MEDICAL", "Медсервис", Icons.Rounded.MedicalServices, listOf("медицина")),
    ShiftIconDescriptor("FIRE", "Пожарная", Icons.Rounded.LocalFireDepartment, listOf("пожар", "мчс")),
    ShiftIconDescriptor("SHIELD", "Защита", Icons.Rounded.Shield, listOf("безопасность", "охрана")),
    ShiftIconDescriptor("BUILD", "Ремонт", Icons.Rounded.Build, listOf("инструмент")),
    ShiftIconDescriptor("CONSTRUCT", "Стройка", Icons.Rounded.Construction, listOf("строительство")),
    ShiftIconDescriptor("ENGINEER", "Инженер", Icons.Rounded.Engineering, listOf("техник")),
    ShiftIconDescriptor("FACTORY", "Производство", Icons.Rounded.Factory, listOf("завод", "цех")),
    ShiftIconDescriptor("SCIENCE", "Лаборатория", Icons.Rounded.Science, listOf("наука")),
    ShiftIconDescriptor("TRUCK", "Доставка", Icons.Rounded.LocalShipping, listOf("груз", "логистика")),
    ShiftIconDescriptor("CAR", "Авто", Icons.Rounded.DirectionsCar, listOf("машина", "водитель")),
    ShiftIconDescriptor("BUS", "Автобус", Icons.Rounded.DirectionsBus, listOf("транспорт")),
    ShiftIconDescriptor("TRAIN", "Поезд", Icons.Rounded.Train, listOf("ржд")),
    ShiftIconDescriptor("FLIGHT", "Авиарейс", Icons.Rounded.Flight, listOf("самолёт", "перелёт")),
    ShiftIconDescriptor("STORE", "Магазин", Icons.Rounded.Store, listOf("торговля")),
    ShiftIconDescriptor("CART", "Покупки", Icons.Rounded.ShoppingCart, listOf("корзина")),
    ShiftIconDescriptor("INVENTORY", "Склад", Icons.Rounded.Inventory2, listOf("учёт", "товар")),
    ShiftIconDescriptor("RESTAURANT", "Ресторан", Icons.Rounded.Restaurant, listOf("еда", "питание")),
    ShiftIconDescriptor("FASTFOOD", "Фастфуд", Icons.Rounded.Fastfood, listOf("перекус")),
    ShiftIconDescriptor("COFFEE", "Кофе", Icons.Rounded.Coffee, listOf("перерыв")),
    ShiftIconDescriptor("HOTEL", "Отель", Icons.Rounded.Hotel, listOf("гостиница")),
    ShiftIconDescriptor("SCHOOL", "Обучение", Icons.Rounded.School, listOf("учёба", "курс")),
    ShiftIconDescriptor("BOOK", "Документы", Icons.Rounded.MenuBook, listOf("инструкция", "регламент")),
    ShiftIconDescriptor("PHONE", "Телефон", Icons.Rounded.PhoneIphone, listOf("моб", "связь")),
    ShiftIconDescriptor("PRINT", "Печать", Icons.Rounded.Print, listOf("принтер")),
    ShiftIconDescriptor("EDIT", "Правка", Icons.Rounded.Edit, listOf("редактирование")),
    ShiftIconDescriptor("CALENDAR", "Календарь", Icons.Rounded.CalendarMonth, listOf("дата")),
    ShiftIconDescriptor("TODAY", "Сегодня", Icons.Rounded.Today, listOf("текущая дата")),
    ShiftIconDescriptor("EVENT", "Событие", Icons.Rounded.EventNote, listOf("заметка")),
    ShiftIconDescriptor("ALARM", "Будильник", Icons.Rounded.Alarm, listOf("сигнал", "напоминание")),
    ShiftIconDescriptor("TIMER", "Таймер", Icons.Rounded.Timer, listOf("время", "отсчёт")),
    ShiftIconDescriptor("BOLT", "Срочно", Icons.Rounded.Bolt, listOf("молния", "быстро")),
    ShiftIconDescriptor("CLEAN", "Уборка", Icons.Rounded.CleaningServices, listOf("чистка")),
    ShiftIconDescriptor("CALC", "Расчёт", Icons.Rounded.Calculate, listOf("калькулятор", "зарплата")),
    ShiftIconDescriptor("MONEY", "Выплата", Icons.Rounded.Payments, listOf("деньги", "оплата")),
    ShiftIconDescriptor("BANK", "Деньги", Icons.Rounded.AttachMoney, listOf("финансы", "доход")),
    ShiftIconDescriptor("CARD", "Карта", Icons.Rounded.CreditCard, listOf("оплата картой")),
    ShiftIconDescriptor("RECEIPT", "Чек", Icons.Rounded.ReceiptLong, listOf("квитанция")),
    ShiftIconDescriptor("CYCLE", "Цикл", Icons.Rounded.Autorenew, listOf("чередование", "повтор")),
    ShiftIconDescriptor("SETTINGS", "Настройки", Icons.Rounded.Settings, listOf("параметры"))
)

private val baseShiftIconLabelMap = baseShiftIconDescriptors.associate { it.key to it.label }
private val baseShiftIconVectorMap = baseShiftIconDescriptors.associate { it.key to it.icon }
private val baseShiftIconAliasMap = baseShiftIconDescriptors.associate { it.key to it.aliases }

private val baseShiftIconOptionGroups = listOf(
    ShiftIconOptionGroup(
        title = "Смена и статус",
        keys = listOf("SUN", "MOON", "EIGHT", "STAR", "HOME", "WORK", "TASK", "CHECK", "FLAG", "OT", "SICK")
    ),
    ShiftIconOptionGroup(
        title = "Медицина и безопасность",
        keys = listOf("HOSPITAL", "MEDICAL", "FIRE", "SHIELD", "BUILD", "CONSTRUCT", "ENGINEER", "FACTORY", "SCIENCE")
    ),
    ShiftIconOptionGroup(
        title = "Транспорт и логистика",
        keys = listOf("TRUCK", "CAR", "BUS", "TRAIN", "FLIGHT")
    ),
    ShiftIconOptionGroup(
        title = "Сервисы и бытовое",
        keys = listOf("STORE", "CART", "INVENTORY", "RESTAURANT", "FASTFOOD", "COFFEE", "HOTEL", "SCHOOL", "BOOK", "PHONE", "PRINT")
    ),
    ShiftIconOptionGroup(
        title = "Планирование и финансы",
        keys = listOf("EDIT", "CALENDAR", "TODAY", "EVENT", "ALARM", "TIMER", "BOLT", "CLEAN", "CALC", "MONEY", "BANK", "CARD", "RECEIPT")
    ),
    ShiftIconOptionGroup(
        title = "Служебные",
        keys = listOf("CYCLE", "SETTINGS", "TEXT")
    )
)

fun shiftIconOptionGroups(context: Context? = null): List<ShiftIconOptionGroup> {
    if (context == null) return baseShiftIconOptionGroups
    val materialKeys = materialRoundedIconKeys(context)
    if (materialKeys.isEmpty()) return baseShiftIconOptionGroups
    return baseShiftIconOptionGroups + ShiftIconOptionGroup(
        title = MATERIAL_ROUNDED_GROUP_TITLE,
        keys = materialKeys
    )
}

fun availableShiftIconKeys(context: Context? = null): List<String> = shiftIconOptionGroups(context).flatMap { it.keys }

fun materialRoundedIconKeys(context: Context): List<String> {
    return MaterialRoundedIconCatalog.allSymbols(context).map { materialRoundedIconKey(it) }
}

fun materialRoundedIconKey(symbol: String): String = "$MATERIAL_ROUNDED_KEY_PREFIX$symbol"

fun isMaterialRoundedIconKey(iconKey: String): Boolean = iconKey.startsWith(MATERIAL_ROUNDED_KEY_PREFIX)

private fun materialRoundedSymbolFromKey(iconKey: String): String = iconKey.removePrefix(MATERIAL_ROUNDED_KEY_PREFIX)

fun shiftIconSearchTokens(iconKey: String): List<String> {
    val key = iconKey.trim()
    if (isMaterialRoundedIconKey(key)) {
        val symbol = materialRoundedSymbolFromKey(key)
        val title = humanizeMaterialSymbol(symbol)
        return buildList {
            add(symbol)
            add(title)
            add(title.replace(" ", ""))
            add(symbol.lowercase())
        }.map { it.lowercase() }.distinct()
    }

    val normalizedKey = key.uppercase()
    val label = shiftIconLabel(normalizedKey)
    val aliases = baseShiftIconAliasMap[normalizedKey].orEmpty()
    return buildList {
        add(normalizedKey)
        add(label)
        addAll(aliases)
    }.map { it.trim().lowercase() }.distinct()
}

fun matchesShiftIconQuery(iconKey: String, query: String): Boolean {
    val normalizedQuery = query.trim().lowercase()
    if (normalizedQuery.isBlank()) return true
    return shiftIconSearchTokens(iconKey).any { token -> token.contains(normalizedQuery) }
}

fun shiftIconLabel(iconKey: String): String {
    return when {
        iconKey.startsWith("EMOJI:") -> "Эмодзи"
        iconKey == "TEXT" -> "Текст (код смены)"
        isMaterialRoundedIconKey(iconKey) -> "Material • ${humanizeMaterialSymbol(materialRoundedSymbolFromKey(iconKey))}"
        else -> baseShiftIconLabelMap[iconKey] ?: iconKey
    }
}

fun materialShiftIcon(iconKey: String): ImageVector? {
    return when {
        iconKey.startsWith("EMOJI:") -> null
        iconKey == "TEXT" -> null
        isMaterialRoundedIconKey(iconKey) -> MaterialRoundedIconCatalog.resolve(materialRoundedSymbolFromKey(iconKey))
        else -> baseShiftIconVectorMap[iconKey]
    }
}

fun iconGlyph(iconKey: String, fallbackCode: String): String {
    return when {
        iconKey.startsWith("EMOJI:") -> iconKey.removePrefix("EMOJI:").ifBlank { fallbackCode }
        iconKey == "SUN" -> "☀"
        iconKey == "MOON" -> "☾"
        iconKey == "EIGHT" -> "8"
        iconKey == "HOME" -> "⌂"
        iconKey == "OT" -> "ОТ"
        iconKey == "SICK" -> "✚"
        iconKey == "STAR" -> "★"
        iconKey == "CHECK" -> "✓"
        iconKey == "TASK" -> "✓"
        iconKey == "TEXT" -> fallbackCode
        materialShiftIcon(iconKey) != null -> shiftIconLabel(iconKey).take(1)
        else -> fallbackCode
    }
}

fun shiftGlyphFontSize(glyph: String) = when {
    glyph.length <= 2 -> 18
    glyph.length == 3 -> 14
    else -> 12
}

private fun humanizeMaterialSymbol(symbol: String): String {
    val cleaned = symbol.trim().trimStart('_').replace('_', ' ')
    return cleaned
        .replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .ifBlank { symbol }
}

private object MaterialRoundedIconCatalog {
    private const val ROUNDED_PACKAGE = "androidx.compose.material.icons.rounded."
    private const val KOTLIN_FILE_SUFFIX = "Kt"

    @Volatile
    private var cachedSymbols: List<String>? = null
    private val methodCache = ConcurrentHashMap<String, Method?>()

    fun allSymbols(context: Context): List<String> {
        cachedSymbols?.let { return it }
        synchronized(this) {
            cachedSymbols?.let { return it }
            val loaded = loadSymbols(context)
            cachedSymbols = loaded
            return loaded
        }
    }

    fun resolve(symbol: String): ImageVector? {
        val method = methodCache.getOrPut(symbol) { resolveGetter(symbol) } ?: return null
        return runCatching { method.invoke(null, Icons.Rounded) as? ImageVector }.getOrNull()
    }

    private fun resolveGetter(symbol: String): Method? {
        val className = "$ROUNDED_PACKAGE$symbol$KOTLIN_FILE_SUFFIX"
        val cls = runCatching { Class.forName(className) }.getOrNull() ?: return null
        val getter = cls.declaredMethods.firstOrNull { method ->
            Modifier.isStatic(method.modifiers) &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes[0].name == "androidx.compose.material.icons.Icons\$Rounded" &&
                method.returnType.name == "androidx.compose.ui.graphics.vector.ImageVector"
        } ?: return null
        getter.isAccessible = true
        return getter
    }

    private fun loadSymbols(context: Context): List<String> {
        val symbols = linkedSetOf<String>()
        collectDexFiles(context).forEach { dex ->
            runCatching {
                val entries = dex.entries()
                while (entries.hasMoreElements()) {
                    val className = entries.nextElement()
                    if (!className.startsWith(ROUNDED_PACKAGE) || !className.endsWith(KOTLIN_FILE_SUFFIX)) continue
                    val symbol = className
                        .removePrefix(ROUNDED_PACKAGE)
                        .removeSuffix(KOTLIN_FILE_SUFFIX)
                    if (symbol.isNotBlank() && symbol != "Icons") {
                        symbols += symbol
                    }
                }
            }
        }
        return symbols.sorted()
    }

    private fun collectDexFiles(context: Context): List<DexFile> {
        val dexFiles = mutableListOf<DexFile>()
        val sourceDex = runCatching { DexFile(context.applicationInfo.sourceDir) }.getOrNull()
        if (sourceDex != null) {
            dexFiles += sourceDex
        }

        val classLoader = context.classLoader
        if (classLoader is BaseDexClassLoader) {
            val pathList = runCatching {
                val pathListField = BaseDexClassLoader::class.java.getDeclaredField("pathList")
                pathListField.isAccessible = true
                pathListField.get(classLoader)
            }.getOrNull()

            if (pathList != null) {
                val dexElements = runCatching {
                    val dexElementsField = pathList.javaClass.getDeclaredField("dexElements")
                    dexElementsField.isAccessible = true
                    dexElementsField.get(pathList) as? Array<*>
                }.getOrNull().orEmpty()

                dexElements.forEach { element ->
                    val dex = runCatching {
                        val dexField = element?.javaClass?.getDeclaredField("dexFile")
                        dexField?.isAccessible = true
                        dexField?.get(element) as? DexFile
                    }.getOrNull()
                    if (dex != null) dexFiles += dex
                }
            }
        }

        return dexFiles.distinctBy { it.name ?: it.toString() }
    }
}
