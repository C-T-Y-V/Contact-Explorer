package com.example.contactexplorer.feature.calling

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Enterprise Calling & Messaging Intent Dispatchers.
 * Safely dispatches standard Android telephony intents (ACTION_CALL, ACTION_DIAL, ACTION_SENDTO).
 */
object CallingDispatcher {

    fun launchDirectCall(context: Context, number: String) {
        val clean = number.trim()
        if (clean.isBlank()) return
        val uri = Uri.fromParts("tel", clean, null)
        runCatching {
            val intent = Intent(Intent.ACTION_CALL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }.onFailure {
            launchDialAction(context, clean)
        }
    }

    fun launchDialAction(context: Context, number: String) {
        val clean = number.trim()
        if (clean.isBlank()) return
        val uri = Uri.fromParts("tel", clean, null)
        runCatching {
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Could not start phone app.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchPhoneAction(context: Context, number: String) {
        launchDirectCall(context, number)
    }

    fun launchSmsAction(context: Context, number: String) {
        val clean = number.trim()
        if (clean.isBlank()) return
        val uri = Uri.parse("smsto:$clean")
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Could not start messaging app.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchEmailAction(context: Context, email: String) {
        val clean = email.trim()
        if (clean.isBlank()) return
        val uri = Uri.parse("mailto:$clean")
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }.onFailure {
            Toast.makeText(context, "Could not start email app.", Toast.LENGTH_SHORT).show()
        }
    }
}
