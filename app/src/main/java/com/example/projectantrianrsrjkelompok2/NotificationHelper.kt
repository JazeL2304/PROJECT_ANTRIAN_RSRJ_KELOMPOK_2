package com.example.projectantrianrsrjkelompok2.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.projectantrianrsrjkelompok2.MainActivity
import com.example.projectantrianrsrjkelompok2.R

object NotificationHelper {

    private const val CHANNEL_ID = "queue_notification_channel"
    private const val CHANNEL_NAME = "Antrian Notification"
    private const val NOTIFICATION_ID = 1001

    /**
     * Create notification channel (required for Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk antrian klinik"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show notification when queue is almost ready
     */
    fun showQueueAlmostReadyNotification(
        context: Context,
        queueNumber: Int,
        queueAhead: Int
    ) {
        // Create intent to open app when notification clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_queue_fragment", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Kita akan buat icon ini
            .setContentTitle("⏰ Antrian Anda Hampir Tiba!")
            .setContentText("Nomor antrian Anda: $queueNumber. Masih $queueAhead pasien lagi.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Nomor antrian Anda: $queueNumber\n\nMasih $queueAhead pasien di depan Anda.\n\nMohon bersiap dan jangan meninggalkan area ruang tunggu."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Show notification when queue is ready NOW
     */
    fun showQueueReadyNotification(
        context: Context,
        queueNumber: Int
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_queue_fragment", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("🔔 GILIRAN ANDA SEKARANG!")
            .setContentText("Nomor antrian $queueNumber dipanggil. Segera menuju ruang dokter!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Nomor antrian: $queueNumber\n\n✅ GILIRAN ANDA SEKARANG!\n\nSilakan segera menuju ruang dokter."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
}
