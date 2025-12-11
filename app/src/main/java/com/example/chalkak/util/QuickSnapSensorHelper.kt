package com.example.chalkak.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Helper class for Quick Snap sensor functionality
 * Handles accelerometer sensor listening and shake detection
 * Reduces code duplication and separates sensor logic from UI
 */
class QuickSnapSensorHelper(
    private val context: Context,
    private val onShakeDetected: () -> Unit
) : SensorEventListener {
    
    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = 
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Quick snap sensitivity settings
    private val quickSnapThresholdGravity = 2f   // How strongly shaking
    private val quickSnapSlopTimeMs = 800L       // Detection term (prevent too many actions)
    private var lastQuickSnapTime: Long = 0L
    
    private var isEnabled = false
    
    /**
     * Enable quick snap detection
     */
    fun enable() {
        if (isEnabled) return
        isEnabled = true
        accelerometer?.also { sensor ->
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
    
    /**
     * Disable quick snap detection
     */
    fun disable() {
        if (!isEnabled) return
        isEnabled = false
        sensorManager.unregisterListener(this)
    }
    
    /**
     * Check if quick snap is currently enabled
     */
    fun isQuickSnapEnabled(): Boolean = isEnabled
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used, but required by SensorEventListener
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        // If disabled, return early
        if (!isEnabled) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        // Normalize to gravity units
        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH
        
        // Calculate g-force magnitude
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
        
        // Check if shake threshold is exceeded
        if (gForce > quickSnapThresholdGravity) {
            val now = System.currentTimeMillis()
            
            // Prevent too many actions (debounce)
            if (now - lastQuickSnapTime < quickSnapSlopTimeMs) {
                return
            }
            
            lastQuickSnapTime = now
            
            // Trigger callback
            onShakeDetected()
        }
    }
    
    /**
     * Cleanup resources
     * Should be called when the helper is no longer needed (e.g., in onDestroy)
     */
    fun cleanup() {
        disable()
    }
}
