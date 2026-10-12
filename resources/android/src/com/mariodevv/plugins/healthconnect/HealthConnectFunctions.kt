package com.mariodevv.plugins.healthconnect

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.nativephp.mobile.bridge.BridgeFunction
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Instant

/** Every function returns { json: "<JSON string>" }; PHP decodes it (nested data survives the bridge). */
object HealthConnectFunctions {

    private fun reply(block: suspend () -> JSONObject): Map<String, Any> = try {
        mapOf("json" to runBlocking { block() }.toString())
    } catch (e: Exception) {
        Log.e("HealthConnect", "bridge call failed", e)
        mapOf("json" to JSONObject().put("error", e.message ?: e.javaClass.simpleName).toString())
    }

    private fun range(p: Map<String, Any>): Pair<Instant, Instant> =
        Instant.parse(p["from"] as String) to Instant.parse(p["to"] as String)

    class Status(private val context: Context) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>) = reply { HealthConnectCore.status(context) }
    }

    /** Parameters: types: string[] (default all), write: bool (also ask write access where supported). Result arrives asynchronously: poll Status. */
    class RequestPermissions(private val activity: FragmentActivity) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>) = reply {
            if (!HealthConnectCore.available(activity)) return@reply JSONObject().put("error", "unavailable")
            val types = (parameters["types"] as? List<*>)?.filterIsInstance<String>()
            Handler(Looper.getMainLooper()).post {
                activity.startActivity(Intent(activity, HealthConnectPermissionActivity::class.java).apply {
                    types?.let { putExtra("types", it.toTypedArray()) }
                    putExtra("write", parameters["write"] == true)
                })
            }
            JSONObject().put("status", "permission_requested")
        }
    }

    /** Parameters: type, from, to (ISO-8601 instants). */
    class Read(private val context: Context) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>) = reply {
            val (from, to) = range(parameters)
            HealthConnectCore.read(context, parameters["type"] as String, from, to)
        }
    }

    /** Parameters: type, from, to (ISO-8601 instants). */
    class Aggregate(private val context: Context) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>) = reply {
            val (from, to) = range(parameters)
            HealthConnectCore.aggregate(context, parameters["type"] as String, from, to)
        }
    }

    /** Parameters: type, plus steps: count, from, to; weight: kg, time. */
    class Write(private val context: Context) : BridgeFunction {
        override fun execute(parameters: Map<String, Any>) = reply {
            HealthConnectCore.write(context, parameters["type"] as String, parameters)
        }
    }
}
