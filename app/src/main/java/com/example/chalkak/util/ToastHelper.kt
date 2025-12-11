package com.example.chalkak.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.example.chalkak.R

/**
 * Helper class for displaying custom toast messages
 * Centralizes custom toast creation and display logic
 * Reduces code duplication across fragments and activities
 */
object ToastHelper {
    
    /**
     * Show a custom centered toast message
     * @param context Context for creating toast and inflating layout
     * @param message Message to display
     * @param duration Toast duration (default: Toast.LENGTH_SHORT)
     */
    fun showCenterToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.custom_toast, null)
        
        val txt = view.findViewById<TextView>(R.id.txtToastMessage)
        txt.text = message
        
        val toast = Toast(context)
        toast.duration = duration
        toast.view = view
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }
}
