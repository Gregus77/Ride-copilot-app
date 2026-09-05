package com.ridecopilot.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.ridecopilot.app.data.LocationProvider
import com.ridecopilot.app.data.RideOfferBus
import com.ridecopilot.app.data.SettingsRepository
import com.ridecopilot.app.data.WeeklyDistanceTracker
import com.ridecopilot.app.domain.ProfitabilityCalculator
import com.ridecopilot.app.domain.ProfitabilityLevel
import com.ridecopilot.app.domain.RideOffer
import com.ridecopilot.app.network.DirectionsApi
import com.ridecopilot.app.overlay.ui.OverlayCard
import com.ridecopilot.app.overlay.ui.OverlayUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayService : LifecycleService(), ViewModelStoreOwner, SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val uiState = MutableStateFlow(OverlayUiState())

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        if (composeView == null) {
            showOverlay()
        }
        observeOffers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val channelId = "ride_copilot_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Ride Copilot actif", NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ride Copilot")
            .setContentText("Analyse des courses en cours")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        startForeground(1, notification)
    }

    private fun showOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 24
        params.y = 200

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                OverlayCard(stateFlow = uiState, onDismiss = { stopSelf() })
            }
        }

        composeView = view
        windowManager.addView(view, params)
    }

    private fun observeOffers() {
        lifecycleScope.launch {
            RideOfferBus.offers.collect { offer -> handleOffer(offer) }
        }
    }

    private suspend fun handleOffer(offer: RideOffer) {
        uiState.value = OverlayUiState(loading = true, offer = offer)

        val settings = SettingsRepository(applicationContext).settingsFlow.first()

        if (settings.googleMapsApiKey.isBlank()) {
            uiState.value = uiState.value.copy(
                loading = false,
                errorMessage = "Cle API Google Maps manquante (reglages)"
            )
            return
        }

        val origin = LocationProvider(applicationContext).getCurrentLocation()
        if (origin == null) {
            uiState.value = uiState.value.copy(loading = false, errorMessage = "Position GPS indisponible")
            return
        }

        val api = DirectionsApi(settings.googleMapsApiKey)

        val destination = offer.dropoffAddress?.let { api.geocode(it) }
        if (destination == null) {
            uiState.value = uiState.value.copy(
                loading = false,
                errorMessage = "Destination non reconnue a l'ecran"
            )
            return
        }

        val pickup = if (offer.pickupAddress != null && offer.pickupAddress != offer.dropoffAddress) {
            api.geocode(offer.pickupAddress)
        } else {
            null
        }

        val traffic = api.getRealTimeEstimate(origin.first, origin.second, pickup, destination.first, destination.second)
        if (traffic == null) {
            uiState.value = uiState.value.copy(loading = false, errorMessage = "Trafic indisponible")
            return
        }

        val weeklyTracker = WeeklyDistanceTracker(applicationContext)
        val profitability = ProfitabilityCalculator.compute(
            fareEuros = offer.announcedFareEuros,
            traffic = traffic,
            settings = settings,
            weeklyKmAlreadyDriven = weeklyTracker.getKmThisWeek()
        )
        // Approximation : on compte le km de cette course dans le quota hebdo des
        // qu'elle est jugee jouable (pas de detection fiable de l'acceptation reelle).
        if (profitability.level == ProfitabilityLevel.GOOD || profitability.level == ProfitabilityLevel.OK) {
            weeklyTracker.addKm(traffic.totalDistanceKm)
        }

        uiState.value = OverlayUiState(
            loading = false,
            offer = offer,
            traffic = traffic,
            profitability = profitability
        )
    }

    override fun onDestroy() {
        composeView?.let { windowManager.removeView(it) }
        viewModelStore.clear()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }
}
