package com.example.learning

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class CalculatorService : Service() {

    // Implementation of the AIDL interface. Runs in THIS app's process, on a Binder thread.
    private val binder = object : ICalculator.Stub() {
        override fun add(a: Int, b: Int): Int {
            Log.d(TAG, "add($a, $b) callerUid=${getCallingUid()} thread=${Thread.currentThread().name}")
            return a + b
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "onBind: client connected")
        return binder
    }

    private companion object {
        const val TAG = "CalculatorService"
    }
}
