package com.ivarna.fluxlinux.core.services

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class FloatingKeyboardService : Service() {
    
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private var isDragging = false
    private val dragThreshold = 10f
    
    private val handler = Handler(Looper.getMainLooper())
    private var checkForegroundAppRunnable: Runnable? = null
    
    private val TERMUX_X11_PACKAGE = "com.termux.x11"
    
    companion object {
        private const val CHANNEL_ID = "floating_keyboard_channel"
        private const val NOTIFICATION_ID = 1001
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Launch Termux:X11
        launchTermuxX11()
        
        // Create floating button after a short delay (give Termux:X11 time to start)
        handler.postDelayed({
            createFloatingView()
            android.util.Log.d("FloatingKeyboard", "Created floating view after delay")
        }, 2000)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null && windowManager != null) {
            windowManager?.removeView(floatingView)
            floatingView = null
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Keyboard Button",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating keyboard toggle for Termux:X11"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Keyboard (Termux:X11)")
            .setContentText("Keyboard toggle active for Termux:X11")
            .setSmallIcon(com.ivarna.fluxlinux.R.drawable.keyboard_24)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
    
    private fun launchTermuxX11() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(TERMUX_X11_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launchIntent)
                android.util.Log.d("FloatingKeyboard", "Launched Termux:X11")
            } else {
                android.util.Log.e("FloatingKeyboard", "Termux:X11 not installed")
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingKeyboard", "Failed to launch Termux:X11", e)
        }
    }
    
    private fun startForegroundAppMonitoring() {
        checkForegroundAppRunnable = object : Runnable {
            override fun run() {
                val isTermuxX11Foreground = isTermuxX11InForeground()
                android.util.Log.d("FloatingKeyboard", "Termux:X11 in foreground: $isTermuxX11Foreground, Button exists: ${floatingView != null}")
                
                if (isTermuxX11Foreground && floatingView == null) {
                    // Termux:X11 is foreground and button not shown - create it
                    createFloatingView()
                } else if (!isTermuxX11Foreground && floatingView != null) {
                    // Termux:X11 not foreground and button is shown - remove it
                    removeFloatingView()
                }
                
                // Check again in 1 second
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(checkForegroundAppRunnable!!)
    }
    
    private fun stopForegroundAppMonitoring() {
        checkForegroundAppRunnable?.let { handler.removeCallbacks(it) }
    }
    
    private fun isTermuxX11InForeground(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val time = System.currentTimeMillis()
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    time - 1000 * 10,
                    time
                )
                
                if (stats != null && stats.isNotEmpty()) {
                    val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
                    val foregroundApp = sortedStats.firstOrNull()?.packageName
                    foregroundApp == TERMUX_X11_PACKAGE
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingKeyboard", "Error checking foreground app", e)
            false
        }
    }
    
    private fun createFloatingView() {
        if (floatingView != null) return
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // Create glassmorphic floating button
        floatingView = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(120, 120)
            
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.parseColor("#CC1A1A1A"))
                setStroke(4, android.graphics.Color.parseColor("#FF00BCD4"))
            }
            
            val iconView = android.widget.ImageView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    val padding = 24
                    setPadding(padding, padding, padding, padding)
                }
                setImageResource(com.ivarna.fluxlinux.R.drawable.keyboard_24)
                setColorFilter(android.graphics.Color.parseColor("#FF00BCD4"))
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            addView(iconView)
            elevation = 12f
        }
        
        params = WindowManager.LayoutParams(
            120, 120,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 0
            y = 200
        }
        
        floatingView?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params?.x ?: 0
                        initialY = params?.y ?: 0
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val deltaX = event.rawX - initialTouchX
                        val deltaY = event.rawY - initialTouchY
                        
                        if (Math.abs(deltaX) > dragThreshold || Math.abs(deltaY) > dragThreshold) {
                            isDragging = true
                            params?.x = initialX + deltaX.toInt()
                            params?.y = initialY + deltaY.toInt()
                            windowManager?.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            toggleKeyboard()
                        } else {
                            snapToEdge()
                        }
                        return true
                    }
                }
                return false
            }
        })
        
        try {
            windowManager?.addView(floatingView, params)
            android.util.Log.d("FloatingKeyboard", "Floating view created")
        } catch (e: Exception) {
            android.util.Log.e("FloatingKeyboard", "Failed to add floating view", e)
        }
    }
    
    private fun removeFloatingView() {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingView)
                floatingView = null
                android.util.Log.d("FloatingKeyboard", "Floating view removed")
            } catch (e: Exception) {
                android.util.Log.e("FloatingKeyboard", "Error removing floating view", e)
            }
        }
    }
    
    private fun toggleKeyboard() {
        try {
            android.util.Log.d("FloatingKeyboard", "Attempting to send BACK action via AccessibilityService")
            
            // Use AccessibilityService to perform BACK action
            if (KeyboardAccessibilityService.isServiceEnabled) {
                val success = KeyboardAccessibilityService.performBackAction()
                android.util.Log.d("FloatingKeyboard", "BACK action result: $success")
                if (!success) {
                    android.widget.Toast.makeText(this, "Failed to toggle keyboard", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.util.Log.w("FloatingKeyboard", "AccessibilityService not enabled!")
                android.widget.Toast.makeText(this, "Enable FluxLinux Accessibility Service in Settings", android.widget.Toast.LENGTH_LONG).show()
                
                // Open accessibility settings
                val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("FloatingKeyboard", "Failed to toggle keyboard", e)
        }
    }
    
    private fun snapToEdge() {
        val screenWidth = windowManager?.defaultDisplay?.width ?: 0
        val currentX = params?.x ?: 0
        
        params?.x = if (currentX < screenWidth / 2) {
            0
        } else {
            screenWidth - (floatingView?.width ?: 120)
        }
        
        windowManager?.updateViewLayout(floatingView, params)
    }
}
