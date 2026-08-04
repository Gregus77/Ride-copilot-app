package com.ridecopilot.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ridecopilot.app.data.RideOfferBus
import com.ridecopilot.app.data.SettingsRepository
import com.ridecopilot.app.overlay.OverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reste lie/actif dans les reglages Android d'accessibilite en permanence : le toggle
 * "Surveillance active" de l'app ne fait que court-circuiter le traitement des evenements
 * ici, il ne desactive jamais ce service au niveau systeme.
 */
class RideAccessibilityService : AccessibilityService() {

    private val monitoredPackages = setOf("com.ubercab.driver", "ee.mtakso.driver")

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile
    private var monitoringEnabled = true

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            SettingsRepository(applicationContext).settingsFlow.collect { settings ->
                monitoringEnabled = settings.monitoringEnabled
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!monitoringEnabled) return

        val packageName = event?.packageName?.toString() ?: return
        if (packageName !in monitoredPackages) return

        val root = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        collectText(root, texts)
        root.recycle()

        if (texts.isEmpty()) return

        val offer = RideOfferParser.parse(packageName, texts) ?: return
        RideOfferBus.tryPublish(offer)

        startService(Intent(this, OverlayService::class.java))
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int = 0) {
        if (depth > 40) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out, depth + 1)
            child.recycle()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
