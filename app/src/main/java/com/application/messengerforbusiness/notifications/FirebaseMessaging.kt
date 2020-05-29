package com.application.messengerforbusiness.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.application.messengerforbusiness.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessaging : FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        val sp = getSharedPreferences("SP_USER", Context.MODE_PRIVATE)
        val savedCurrentUser = sp.getString("Current_USERID", "None")

        val sent = p0.data["sent"]
        val user = p0.data["user"]
        val fUser = FirebaseAuth.getInstance().currentUser
        if (fUser != null && sent == fUser.uid) {
            if (!savedCurrentUser.equals(user)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sendAndAboveNotification(p0)
                } else {
                    sendNormalNotification(p0)
                }
            }
        }
    }

    private fun sendNormalNotification(p0: RemoteMessage) {
        val user = p0.data["user"]
        val icon = p0.data["icon"]
        val title = p0.data["title"]
        val body = p0.data["body"]

        val i = user!!.replace("[\\D]".toRegex(), "").toInt()
        val intent = Intent(this, ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("hisUid", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT)

        val defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder =
            NotificationCompat.Builder(this).setSmallIcon(icon!!.toInt()).setContentText(body)
                .setContentTitle(title).setAutoCancel(true).setSound(defSoundUri)
                .setContentIntent(pIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var j = 0
        if (i> 0) {
            j = i
        }
        notificationManager.notify(j, builder.build())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendAndAboveNotification(p0: RemoteMessage) {
        val user = p0.data["user"]
        val icon = p0.data["icon"]
        val title = p0.data["title"]
        val body = p0.data["body"]
        val i = user!!.replace("[\\D]".toRegex(), "").toInt()
        val intent = Intent(this, ChatActivity::class.java)
        val bundle = Bundle()
        bundle.putString("hisUid", user)
        intent.putExtras(bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT)

        val defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification1 = OreoAndAboveNotification(this)
        val builder = notification1.getONotification(title!!, body!!, pIntent, defSoundUri, icon!!)

        var j = 0
        if (i> 0) {
            j = i
        }
        notification1.getManager().notify(j, builder.build())
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            updateToken(p0)
        }
    }

    private fun updateToken(p0: String) {
        val user = FirebaseAuth.getInstance().currentUser
        val ref = FirebaseDatabase.getInstance().getReference("Tokens")
        val token = Token(p0)
        ref.child(user!!.uid).setValue(token)
    }
}