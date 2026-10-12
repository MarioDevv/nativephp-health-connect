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
        val write = intent.getBooleanExtra("write", false)
        val permissions = types.flatMap { listOfNotNull(HealthConnectCore.permission(it), if (write) HealthConnectCore.writePermission(it) else null) }.toSet()
        if (permissions.isEmpty()) finish() else launcher.launch(permissions)
    }
}
