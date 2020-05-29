package com.application.messengerforbusiness.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

val ID = "some_id"
val NAME = "FirebaseAPP"

class OreoAndAboveNotification(base: Context) : ContextWrapper(base) {
    private var notificationManager: NotificationManager? = null

    init {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            createChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        val notificationChannel =
            NotificationChannel(ID, NAME, NotificationManager.IMPORTANCE_DEFAULT)
        notificationChannel.enableLights(true)
        notificationChannel.enableVibration(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }

    public fun getManager(): NotificationManager {
        if (notificationManager == null) {
            notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificationManager as NotificationManager
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public fun getONotification(
        title: String,
        body: String,
        pIntent: PendingIntent,
        soundUri: Uri,
        icon: String
    ): Notification.Builder {
        return Notification.Builder(applicationContext, ID).setContentIntent(pIntent)
            .setContentTitle(title).setContentText(body).setSound(soundUri).setAutoCancel(true)
            .setSmallIcon(icon.toInt())
    }
}