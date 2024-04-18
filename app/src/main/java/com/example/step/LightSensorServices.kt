package com.example.step


import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.MutableLiveData


class LightSensorService : Service() {

    // Deklaracja menedżera czujników, czujnika światła oraz LiveData dla wartości światła
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val lightValue = MutableLiveData<String>()

    //listener dla czujnika swiatla
    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {


            // Sprawdzenie czy czujnik wlaczony i aktualizacja wartości światła
            if (isActive) {
                event?.let {
                    val light = event.values[0]
                    lightValue.postValue("$light lx")

                    // Aktualizacja powiadomienia z nowym poziomem światła
                    showNotification("$light lx")
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Inicjalizacja menedżera czujników i czujnika światła
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // Rejestracja listenera dla czujnika światła
        lightSensor?.let {
            sensorManager.registerListener(lightListener, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(lightListener)
    }


    //wyswietlanie i ustawienia powiadmoien
    private fun showNotification(message: String) {
        val builder = NotificationCompat.Builder(this, "LightSensorChannel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.Value))
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(null)
            .setSound(android.net.Uri.EMPTY)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        with(NotificationManagerCompat.from(this@LightSensorService)) {
            if (ActivityCompat.checkSelfPermission(
                    this@LightSensorService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            // Wyświetlenie powiadomienia
            notify(1, builder.build())
        }
    }
}