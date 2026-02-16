package com.example.composetutorial

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SensorService : Service(), SensorEventListener {private lateinit var SM: SensorManager
    private var proxSensor: Sensor? = null
    private var flag = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    // Proximity sensor
    override fun onCreate() {super.onCreate()
        SM = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proxSensor = SM.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel("my_channel", "My Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, "my_channel_01")
            .setContentTitle("prox alert")
            .setContentText("in use")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(101, notification)
        }
        //listener
        if (proxSensor != null) {SM.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return START_STICKY
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                val v = event.values[0]

                if (v < event.sensor.maximumRange) {
                    if (!flag) {
                        flag = true

                        //Alert
                        val i = Intent(this, MainActivity::class.java)
                        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        val pi = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_IMMUTABLE)

                        val notif = NotificationCompat.Builder(this, "my_channel")
                            .setContentTitle("Prox alert")
                            .setContentText("something is infront, $v cm!")
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pi)
                            .setAutoCancel(true)
                            .build()

                        val nm =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(102, notif)
                    }
                } else {
                    flag = false
                }
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onDestroy() {
        super.onDestroy()
        SM.unregisterListener(this)
    }
}