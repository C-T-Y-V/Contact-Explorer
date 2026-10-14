package com.example.contactexplorer.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class ContactPermissionState(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canCall: Boolean = true,
)

class ContactPermissions(context: Context) {
    private val appContext = context.applicationContext

    fun currentState() = ContactPermissionState(
        canRead = granted(Manifest.permission.READ_CONTACTS),
        canWrite = granted(Manifest.permission.WRITE_CONTACTS),
        canCall = granted(Manifest.permission.CALL_PHONE),
    )

    fun canCall(): Boolean = granted(Manifest.permission.CALL_PHONE)

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}
