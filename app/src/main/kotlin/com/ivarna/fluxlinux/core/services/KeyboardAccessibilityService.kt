package com.ivarna.fluxlinux.core.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class KeyboardAccessibilityService : AccessibilityService() {
    
    companion object {
        var instance: KeyboardAccessibilityService? = null
        var isServiceEnabled = false
        
        fun performBackAction(): Boolean {
            return instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        }
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        isServiceEnabled = true
        android.util.Log.d("KeyboardAccessibility", "Accessibility Service connected")
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to process events, just need the service to be able to perform actions
    }
    
    override fun onInterrupt() {
        // Handle interruption
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isServiceEnabled = false
        android.util.Log.d("KeyboardAccessibility", "Accessibility Service destroyed")
    }
}
