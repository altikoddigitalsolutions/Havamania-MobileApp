package com.havamania

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.havamania.ui.theme.HavamaniaTheme

internal fun deviceLocationEnabled(context: Context): Boolean = runCatching {
    (context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager)
        ?.let(LocationManagerCompat::isLocationEnabled) == true
}.getOrDefault(false)

@Serializable
internal enum class LocationAccessDialog { NONE, RATIONALE, DENIED, GPS_DISABLED, ERROR }
@Serializable
internal data class WeatherLocationAccessState(
    val offered: Boolean = false,
    val dialog: LocationAccessDialog = LocationAccessDialog.NONE,
    val requesting: Boolean = false,
    val awaitingSettings: Boolean = false
)

internal interface WeatherLocationGateway {
    fun hasPermission(): Boolean
    fun locationEnabled(): Boolean
    fun requestPermission()
    fun openAppSettings()
    fun openLocationSettings()
}

internal class WeatherLocationAccessController(
    private val gateway: WeatherLocationGateway,
    private val locate: () -> Unit,
    initial: WeatherLocationAccessState = WeatherLocationAccessState()
) {
    private val mutable = MutableStateFlow(initial)
    val state = mutable.asStateFlow()
    private var active = true
    fun close() { active = false }
    fun dismiss() { if (active) mutable.value = mutable.value.copy(dialog = LocationAccessDialog.NONE, requesting = false, awaitingSettings = false) }
    fun offer(autoLocate: Boolean) {
        if (active && !state.value.offered) start(autoLocate)
    }
    fun start(autoLocate: Boolean = true) {
        if (!active) return
        try {
            val dialog = when {
                !gateway.hasPermission() -> LocationAccessDialog.RATIONALE
                !gateway.locationEnabled() -> LocationAccessDialog.GPS_DISABLED
                else -> LocationAccessDialog.NONE
            }
            mutable.value = WeatherLocationAccessState(offered = true, dialog = dialog)
            if (dialog == LocationAccessDialog.NONE && autoLocate) locate()
        } catch (_: Exception) { fail() }
    }
    fun requestPermission() {
        if (!active || state.value.requesting) return
        mutable.value = state.value.copy(dialog = LocationAccessDialog.NONE, requesting = true)
        try { gateway.requestPermission() } catch (_: Exception) { fail() }
    }
    fun permissionResult(granted: Boolean) {
        if (!active || !state.value.requesting) return
        mutable.value = state.value.copy(requesting = false)
        if (granted) start() else mutable.value = state.value.copy(dialog = LocationAccessDialog.DENIED)
    }
    fun settings(location: Boolean) {
        if (!active) return
        mutable.value = state.value.copy(dialog = LocationAccessDialog.NONE, awaitingSettings = true)
        try {
            if (location) gateway.openLocationSettings() else gateway.openAppSettings()
        } catch (_: Exception) { fail() }
    }
    fun resumed() {
        if (!active) return
        try {
            if (state.value.awaitingSettings ||
                (state.value.dialog == LocationAccessDialog.DENIED && gateway.hasPermission())) {
                if (gateway.hasPermission()) start()
                else mutable.value = state.value.copy(awaitingSettings = false, dialog = LocationAccessDialog.DENIED)
            }
        } catch (_: Exception) { fail() }
    }
    private fun fail() { mutable.value = state.value.copy(dialog = LocationAccessDialog.ERROR, requesting = false, awaitingSettings = false) }
}

@Composable
internal fun WeatherLocationAccess(
    session: Long, noCity: Boolean, autoLocate: Boolean, onLocate: () -> Unit,
    onManual: () -> Unit, content: @Composable (() -> Unit) -> Unit
) = key(session) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val latestLocate by rememberUpdatedState(onLocate)
    val target = remember { mutableStateOf<WeatherLocationAccessController?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        target.value?.permissionResult(result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
    }
    val gateway = remember(context, launcher) { object : WeatherLocationGateway {
        override fun hasPermission() = PermissionHelper.hasLocationPermission(context)
        override fun locationEnabled() = deviceLocationEnabled(context)
        override fun requestPermission() { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
        override fun openAppSettings() { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"))) }
        override fun openLocationSettings() { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
    } }
    val saver = remember(gateway) { Saver<WeatherLocationAccessController, String>(
        save = { Json.encodeToString(WeatherLocationAccessState.serializer(), it.state.value) },
        restore = { value -> WeatherLocationAccessController(gateway, { latestLocate() },
            runCatching { Json.decodeFromString(WeatherLocationAccessState.serializer(), value) }.getOrDefault(WeatherLocationAccessState())) }
    ) }
    val controller = rememberSaveable(saver = saver) { WeatherLocationAccessController(gateway, { latestLocate() }) }
    var wasNoCity by rememberSaveable { mutableStateOf(noCity) }
    SideEffect { target.value = controller }
    DisposableEffect(controller) { onDispose { controller.close(); target.value = null } }
    LaunchedEffect(noCity, autoLocate, controller) {
        if (noCity) controller.offer(autoLocate) else if (wasNoCity) controller.dismiss()
        wasNoCity = noCity
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, controller) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) controller.resumed() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    content { controller.start() }
    WeatherLocationAccessDialog(controller, onManual)
}

@Composable
internal fun WeatherLocationAccessDialog(controller: WeatherLocationAccessController, onManual: () -> Unit) {
    val state by controller.state.collectAsState()
    if (state.dialog == LocationAccessDialog.NONE) return
    val colors = HavamaniaTheme.colors
    AlertDialog(onDismissRequest = controller::dismiss, containerColor = colors.surface,
        title = { Text(when (state.dialog) {
            LocationAccessDialog.RATIONALE -> "Mevcut konumunu kullan"
            LocationAccessDialog.DENIED -> "Konum izni verilmedi"
            LocationAccessDialog.GPS_DISABLED -> "Cihaz konumu kapalı"
            else -> "Konum ayarları açılamadı"
        }, color = colors.textPrimary) },
        text = { Text(when (state.dialog) {
            LocationAccessDialog.RATIONALE -> "Bulunduğun şehir için hava durumunu gösterebilmemiz için konum izni gerekir. Yaklaşık konum izni de yeterlidir. Konumu elle de seçebilirsin."
            LocationAccessDialog.DENIED -> "Konumu elle seçerek devam edebilirsin. İzin penceresi yeniden açılmıyorsa uygulama ayarlarından konum iznini etkinleştir."
            LocationAccessDialog.GPS_DISABLED -> "Mevcut şehri bulmak için cihazın konum hizmetini aç. İstersen konumu elle seçerek de devam edebilirsin."
            else -> "Ayarlar açılamadı. Konumu elle seçebilir veya daha sonra yeniden deneyebilirsin."
        }, color = colors.textSecondary) },
        confirmButton = {
            when (state.dialog) {
                LocationAccessDialog.RATIONALE -> TextButton(onClick = controller::requestPermission) { Text("İzin ver") }
                LocationAccessDialog.DENIED -> TextButton(onClick = { controller.settings(false) }) { Text("Uygulama ayarlarını aç") }
                LocationAccessDialog.GPS_DISABLED -> TextButton(onClick = { controller.settings(true) }) { Text("Konum ayarlarını aç") }
                else -> TextButton(onClick = controller::dismiss) { Text("Kapat") }
            }
        },
        dismissButton = { TextButton(onClick = { controller.dismiss(); onManual() }) { Text("Konumu elle seç") } })
}
