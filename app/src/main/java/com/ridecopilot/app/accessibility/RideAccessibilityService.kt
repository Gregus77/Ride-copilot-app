package com.ridecopilot.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ridecopilot.app.data.RideOfferBus
import com.ridecopilot.app.overlay.OverlayService

class RideAccessibilityService : AccessibilityService() {

    private val monitoredPackages = setOf("com.ubercab.driver", "ee.mtakso.driver")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
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
}
