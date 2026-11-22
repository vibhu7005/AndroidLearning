package com.example.ipcclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ipcclient.ui.theme.LearningTheme
import com.example.learning.ICalculator

class MainActivity : ComponentActivity() {

    // Remote interface to the server's CalculatorService. Null until connected.
    private var calculator: ICalculator? = null

    // True once bindService() succeeded; needed to know whether to unbind.
    private var isBound = false

    // Text shown on screen (Compose redraws when it changes).
    private val status = mutableStateOf("Not connected")

    // Android calls these methods when the connection to the server's service changes.
    private val connection = object : ServiceConnection {

        // Binding succeeded: `binder` is the object the server returned from onBind().
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            // Wrap the raw IBinder in the generated Proxy so we can call add() like a normal method.
            calculator = ICalculator.Stub.asInterface(binder)
            status.value = "Connected to server ✅"
            Log.d(TAG, "Connected to $name")
        }

        // Server process crashed or was killed. Android rebinds automatically when it restarts.
        // (NOT called when we unbind ourselves.)
        override fun onServiceDisconnected(name: ComponentName?) {
            calculator = null
            status.value = "Server disconnected ❌"
            Log.d(TAG, "Disconnected from $name")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindToServer()

        setContent {
            LearningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ClientScreen(
                        status = status.value,
                        onAddClick = ::callAdd,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        // Always unbind, or the connection leaks and the server service stays alive.
        if (isBound) {
            unbindService(connection)
            isBound = false
            calculator = null
        }
        super.onDestroy()
    }

    private fun bindToServer() {
        // EXPLICIT intent: exact package + class of the server's service.
        val intent = Intent().setComponent(
            ComponentName(SERVER_PACKAGE, "$SERVER_PACKAGE.CalculatorService")
        )
        try {
            // Asynchronous: returns immediately; onServiceConnected() is called later.
            isBound = bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!isBound) status.value = "Bind failed: is the Learning (server) app installed?"
        } catch (e: SecurityException) {
            // Thrown if the service requires a permission we don't hold.
            status.value = "Bind refused: ${e.message}"
        }
    }

    private fun callAdd() {
        val calc = calculator
        if (calc == null) {
            status.value = "Not connected yet"
            return
        }
        try {
            // Looks like a local call, but runs inside the SERVER app's process.
            // Note: AIDL calls are synchronous; fine here because add() is instant.
            val result = calc.add(2, 3)
            status.value = "Server says 2 + 3 = $result"
        } catch (e: RemoteException) {
            // Server died during the call.
            status.value = "Call failed: server died"
            Log.w(TAG, "add() failed", e)
        }
    }

    private companion object {
        const val TAG = "IpcClient"
        const val SERVER_PACKAGE = "com.example.learning"
    }
}

@Composable
fun ClientScreen(status: String, onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = status)
        Button(onClick = onAddClick, modifier = Modifier.padding(top = 16.dp)) {
            Text("Call add(2, 3) on server")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ClientScreenPreview() {
    LearningTheme {
        ClientScreen(status = "Connected to server ✅", onAddClick = {})
    }
}
