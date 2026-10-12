package com.mariodevv.plugins.healthconnect

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.health.connect.client.PermissionController

class HealthConnectPermissionActivity : FragmentActivity() {
    private val launcher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { finish() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val types = intent.getStringArrayExtra("types")?.toList() ?: HealthConnectCore.types
        val permissions = types.mapNotNull { HealthConnectCore.permission(it) }.toSet()
        if (permissions.isEmpty()) finish() else launcher.launch(permissions)
    }
}
