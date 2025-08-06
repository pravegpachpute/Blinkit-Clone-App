package com.userBlinkit.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.userBlinkit.R
import com.userBlinkit.activity.UsersMainActivity
import kotlin.random.Random

class NotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        //1. Initialize channel
        val channelId = "UserBlinkit"
        val channel = NotificationChannel(
            channelId,
            "Blinkit",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Blinkit messages"
            enableLights(true)
        }

        //2. manager create our channel
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        //When we click notification open our app
        val pendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, UsersMainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT)

        //3. which notification show our channel
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(message.data["title"]) //same name as class NotificationData
            .setContentText(message.data["body"])
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        //4. manager notify to send our notification
        manager.notify(Random.Default.nextInt(), notification)
    }
}