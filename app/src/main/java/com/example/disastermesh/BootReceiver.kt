package com.example.disastermesh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            val svc = Intent(context, com.example.disastermesh.mesh.MeshService::class.java)
            context.startForegroundService(svc)
        }
    }
}


