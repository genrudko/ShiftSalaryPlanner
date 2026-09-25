package com.vigilante.shiftsalaryplanner.ui.weather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.vigilante.shiftsalaryplanner.FixedScreenHeader
import com.vigilante.shiftsalaryplanner.appScreenPadding
import com.vigilante.shiftsalaryplanner.weather.CurrentLocationResult
import com.vigilante.shiftsalaryplanner.weather.CurrentLocationSource
import com.vigilante.shiftsalaryplanner.weather.OpenMeteoGeocodingSource
import com.vigilante.shiftsalaryplanner.weather.WeatherCoordinates
import com.vigilante.shiftsalaryplanner.weather.WeatherPlaceSearchResult
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * OpenFreeMap Liberty raster style URL (M15.2 Task 5). The map cannot render
 * offline/without tiles; attribution is provided by the MapLibre attribution
 * control and must never be disabled.
 */
const val OPEN_FREEMAP_LIBERTY_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Default camera zoom for the map pin picker. */
const val LOCATION_PICKER_MAP_ZOOM = 12.0

/** Neutral camera seed shown until the user searches/picks/GPS-seeds a place. */
val PICKER_FALLBACK_CENTER: WeatherCoordinates = WeatherCoordinates(55.7558, 37.6173)

/**
 * Confirmed picker result (M15.2 Task 5): authoritative [coordinates] taken
 * from the selected map center/pin plus editable display [label]/[address]
 * metadata. Coordinates are authoritative; label and address are display
 * metadata persisted separately by the caller.
 */
data class WeatherLocationPickerResult(
    val coordinates: WeatherCoordinates,
    val label: String,
    val address: String? = null,
)

/**
 * Pure picker session state. [coordinates] holds the last authoritative map
 * center; network (search) and GPS (current-location) failures are surfaced as
 * errors without ever mutating the existing [coordinates]/[label]/[address].
 */
data class WeatherLocationPickerState(
    val coordinates: WeatherCoordinates? = null,
    val label: String = "",
    val address: String? = null,
    val searchQuery: String = "",
    val searchResults: List<WeatherPlaceSearchResult> = emptyList(),
    val searchInProgress: Boolean = false,
    val searchError: String? = null,
    val currentLocationInProgress: Boolean = false,
    val currentLocationError: String? = null,
) {
    val canConfirm: Boolean get() = coordinates != null && label.isNotBlank()
}

/** Seeds the picker from an existing saved location without mutating it. */
fun initialStateForWeatherLocationPicker(
    initialCoordinates: WeatherCoordinates? = null,
    initialLabel: String = "",
    initialAddress: String? = null,
): WeatherLocationPickerState = WeatherLocationPickerState(
    coordinates = initialCoordinates,
    label = initialLabel,
    address = initialAddress,
)

/** Handles display metadata; `admin`/`country` map to the address line. */
fun WeatherPlaceSearchResult.toPickerAddress(): String? =
    listOfNotNull(admin, country).joinToString(", ").takeIf { it.isNotBlank() }

/** Search started for [query]: clears previous results, never touches coordinates. */
fun onWeatherLocationSearchStarted(
    state: WeatherLocationPickerState,
    query: String,
): WeatherLocationPickerState = state.copy(
    searchQuery = query,
    searchResults = emptyList(),
    searchInProgress = true,
    searchError = null,
)

/** Search returned [results]: displays them, never touches coordinates. */
fun onWeatherLocationSearchFinished(
    state: WeatherLocationPickerState,
    results: List<WeatherPlaceSearchResult>,
): WeatherLocationPickerState = state.copy(
    searchResults = results,
    searchInProgress = false,
    searchError = null,
)

/** Search/network failure: surfaces [message] and never touches the selected coordinates. */
fun onWeatherLocationSearchError(
    state: WeatherLocationPickerState,
    message: String,
): WeatherLocationPickerState = state.copy(searchInProgress = false, searchError = message)

/** A search [result] was chosen: it seeds the map center and label metadata. */
fun onWeatherLocationResultPicked(
    state: WeatherLocationPickerState,
    result: WeatherPlaceSearchResult,
): WeatherLocationPickerState = state.copy(
    coordinates = result.coordinates,
    label = result.name,
    address = result.toPickerAddress(),
    searchQuery = result.name,
    searchResults = emptyList(),
    searchInProgress = false,
    searchError = null,
)

/** Current-location request started. */
fun onCurrentLocationStarted(
    state: WeatherLocationPickerState,
): WeatherLocationPickerState = state.copy(currentLocationInProgress = true, currentLocationError = null)

/** Current GPS succeeded: seeds the map center; label only fills when still blank. */
fun onCurrentLocationFinished(
    state: WeatherLocationPickerState,
    coordinates: WeatherCoordinates,
    providerLabel: String?,
): WeatherLocationPickerState = state.copy(
    coordinates = coordinates,
    label = providerLabel ?: "Current device",
    address = null,
    currentLocationInProgress = false,
    currentLocationError = null,
)

/** Current GPS failure: surfaces [message] and never touches the selected coordinates. */
fun onCurrentLocationError(
    state: WeatherLocationPickerState,
    message: String,
): WeatherLocationPickerState = state.copy(currentLocationInProgress = false, currentLocationError = message)

/** Map center changed (camera idle): the pin center becomes authoritative. */
fun onPickerCoordinatesChanged(
    state: WeatherLocationPickerState,
    coordinates: WeatherCoordinates,
): WeatherLocationPickerState = state.copy(coordinates = coordinates)
/**
 * M15.2 Task 5 weather-location picker.
 *
 * - search results and current GPS seed the map center;
 * - the map is MapLibre Native Android with the OpenFreeMap Liberty style and
 *   the attribution control stays visible;
 * - a centered pin tracks the camera; the camera-center becomes the
 *   authoritative coordinates once the map is idle;
 * - the user edits the display label/address metadata and confirms with
 *   [onConfirm]; search/GPS failures only surface an error and never mutate
 *   the already selected coordinates.
 */
@Composable
fun WeatherLocationPickerScreen(
    initialCoordinates: WeatherCoordinates? = null,
    initialLabel: String = "",
    initialAddress: String? = null,
    geocodingSource: OpenMeteoGeocodingSource,
    currentLocationSource: CurrentLocationSource? = null,
    onConfirm: (WeatherLocationPickerResult) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember {
        mutableStateOf(initialStateForWeatherLocationPicker(initialCoordinates, initialLabel, initialAddress))
    }
    val latestState by rememberUpdatedState(state)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val requestCurrentLocation: () -> Unit = {
        val source = currentLocationSource
        if (source != null) {
            state = onCurrentLocationStarted(state)
            scope.launch {
                when (val outcome = source.currentLocation()) {
                    is CurrentLocationResult.Available -> {
                        state = onCurrentLocationFinished(
                            state = state,
                            coordinates = outcome.coordinates,
                            providerLabel = "Current device",
                        )
                    }
                    is CurrentLocationResult.PermissionDenied ->
                        state = onCurrentLocationError(state, "Нет разрешения на геолокацию")
                    is CurrentLocationResult.ProviderDisabled ->
                        state = onCurrentLocationError(state, "Геолокация выключена на устройстве")
                    is CurrentLocationResult.NoLocationFix ->
                        state = onCurrentLocationError(state, "Не удалось получить координаты")
                    is CurrentLocationResult.Failed ->
                        state = onCurrentLocationError(state, outcome.message)
                }
            }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted =
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestCurrentLocation()
        } else {
            state = onCurrentLocationError(state, "Нет разрешения на геолокацию")
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            FixedScreenHeader(title = "Выбор локации", onBack = onDismiss)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = appScreenPadding()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchField(
                    query = state.searchQuery,
                    searchInProgress = state.searchInProgress,
                    searchError = state.searchError,
                    results = state.searchResults,
                    onQueryChange = { query ->
                        state = onWeatherLocationSearchStarted(state, query)
                        scope.launch {
                            runCatching { geocodingSource.search(query) }
                                .onSuccess { results ->
                                    if (latestState.searchQuery == query) {
                                        state = onWeatherLocationSearchFinished(state, results)
                                    }
                                }
                                .onFailure { error ->
                                    if (latestState.searchQuery == query) {
                                        state = onWeatherLocationSearchError(
                                            state,
                                            error.message ?: "Поиск недоступен",
                                        )
                                    }
                                }
                        }
                    },
                    onResultPicked = { result -> state = onWeatherLocationResultPicked(state, result) },
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    LibertyCenterPickerMap(
                        center = state.coordinates ?: PICKER_FALLBACK_CENTER,
                        onCenterChanged = { coordinates ->
                            state = onPickerCoordinatesChanged(state, coordinates)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Выбранная точка",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(42.dp),
                    )
                }
                CurrentLocationRow(
                    currentLocationSource = currentLocationSource,
                    currentLocationInProgress = state.currentLocationInProgress,
                    currentLocationError = state.currentLocationError,
                    onUseCurrentLocation = {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            requestCurrentLocation()
                        } else {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                )
                            )
                        }
                    },
                )

                OutlinedTextField(
                    value = state.label,
                    onValueChange = { state = state.copy(label = it) },
                    label = { Text("Название (метка)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.address.orEmpty(),
                    onValueChange = { state = state.copy(address = it) },
                    label = { Text("Адрес") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) { Text("Отмена") }
                    Button(
                        onClick = {
                            val coordinates = state.coordinates ?: return@Button
                            onConfirm(
                                WeatherLocationPickerResult(
                                    coordinates = coordinates,
                                    label = state.label.trim(),
                                    address = state.address?.trim()?.takeIf { it.isNotEmpty() },
                                )
                            )
                        },
                        enabled = state.canConfirm,
                        modifier = Modifier.weight(1f),
                    ) { Text("Подтвердить") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    searchInProgress: Boolean,
    searchError: String?,
    results: List<WeatherPlaceSearchResult>,
    onQueryChange: (String) -> Unit,
    onResultPicked: (WeatherPlaceSearchResult) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Поиск по названию") },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (searchInProgress) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp))
            Text("Поиск…", style = MaterialTheme.typography.bodySmall)
        }
    }
    searchError?.let { error ->
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (results.isNotEmpty()) {
        LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
            items(
                items = results,
                key = { result ->
                    "${result.name}|${result.coordinates.latitude}|${result.coordinates.longitude}"
                },
            ) { result ->
                val address = result.toPickerAddress()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultPicked(result) }
                        .padding(vertical = 6.dp)
                ) {
                    Text(result.name, fontWeight = FontWeight.SemiBold)
                    if (address != null) {
                        Text(
                            text = address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentLocationRow(
    currentLocationSource: CurrentLocationSource?,
    currentLocationInProgress: Boolean,
    currentLocationError: String?,
    onUseCurrentLocation: () -> Unit,
) {
    OutlinedButton(
        onClick = onUseCurrentLocation,
        enabled = currentLocationSource != null && !currentLocationInProgress,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (currentLocationInProgress) CircularProgressIndicator(modifier = Modifier.size(16.dp))
        else Icon(Icons.Rounded.MyLocation, contentDescription = null)
        Spacer(modifier = Modifier.size(8.dp))
        Text("Использовать текущее местоположение")
    }
    currentLocationError?.let { error ->
        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LibertyCenterPickerMap(
    center: WeatherCoordinates,
    onCenterChanged: (WeatherCoordinates) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    var maplibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val latestCenter by rememberUpdatedState(center)

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            maplibreMap = map
            map.setStyle(OPEN_FREEMAP_LIBERTY_STYLE_URL)
            map.addOnCameraIdleListener {
                val target = map.cameraPosition.target
                val coordinates = target?.let { WeatherCoordinates.of(it.latitude, it.longitude) }
                if (coordinates != null) onCenterChanged(coordinates)
            }
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(latestCenter.latitude, latestCenter.longitude),
                    LOCATION_PICKER_MAP_ZOOM,
                )
            )
        }
    }

    LaunchedEffect(center) {
        val map = maplibreMap
        if (map != null) {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(center.latitude, center.longitude),
                    LOCATION_PICKER_MAP_ZOOM,
                )
            )
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
