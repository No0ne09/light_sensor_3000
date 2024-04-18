package com.example.step



import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

//zmienna do sledzenia wlaczenia czujnika globalnie
var isActive=false

//glowna klasa activity
@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class MainActivity : ComponentActivity() {
    // Deklaracja menedżera czujników, czujnika światła oraz LiveData dla wartości światła
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val lightValue = MutableLiveData<String>()

    // Listener dla czujnika światła
    private val lightListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            event?.let {
                val light = event.values[0]
                lightValue.postValue("$light lx")

                // Aktualizacja powiadomienia z nowym poziomem światła
                showNotification("$light lx")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            LightSensorApp(lightValue)
        }

// Inicjalizacja menedżera czujników i czujnika światła
        try {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            if (sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
                lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
            } else {
                lightValue.postValue("Brak dostępnego czujnika światła")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing sensor: ${e.message}")
            lightValue.postValue("Błąd inicjalizacji czujnika")
        }

        // Tworzenie kanału powiadomień
        createNotificationChannel()

        // Uruchomienie service w tle
        startService(Intent(this, LightSensorService::class.java))
    }
 //tworzenie kanalu powiadomien
    private fun createNotificationChannel() {
        val name = "LightSensorChannel"
        val descriptionText = "Channel for light sensor notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("LightSensorChannel", name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
//ustawienia i wyswietlanie powiadomien
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

        with(NotificationManagerCompat.from(this@MainActivity)) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            notify(1, builder.build())
        }
    }
//zmiana statusu czujnika
     private fun switch() {
        if (isActive) {
            unregisterSensor()
            lightValue.postValue(getString(R.string.Off))
        } else {
            registerSensor()
            lightValue.postValue(getString(R.string.On))
        }
        isActive=!isActive
    }

    // Metoda do rejestracji listenera czujnik
    private fun registerSensor() {
        lightSensor?.let {
            try {
                sensorManager.registerListener(lightListener, it, SensorManager.SENSOR_DELAY_NORMAL)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error registering listener: ${e.message}")
            }
        }
    }

//wyrejestrowanie czujnika
    private fun unregisterSensor() {
        try {
            sensorManager.unregisterListener(lightListener)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error unregistering listener: ${e.message}")
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LightSensorApp(lightValue: LiveData<String>) {

        var test by remember { mutableStateOf(false)}
// Launcher do żądania uprawnień
        val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            test=isGranted

        }

//uzyskanie orientacji
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
//funkcja dla klikneica zarowki
        fun ein(){
            switch()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)

        }

        //sledzenie poziomu swiatla i dostosowanie interfejsu do tego
        val currentLightValue by lightValue.observeAsState(stringResource(id = R.string.Off))
        val lightColor = when {
            currentLightValue.contains("lx") -> {
                val lightValueFloat = currentLightValue.substringBefore(" lx").toFloatOrNull() ?: 0f
                val colorValue = (lightValueFloat / 40000) * 255
                Color(255, 255, 255 - colorValue.toInt())
            }
            else -> Color.Gray
        }
        MaterialTheme(

            colorScheme = if (isSystemInDarkTheme()) {
                DarkColors

            } else {
                LightColors

            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text(text = stringResource(id = R.string.title)) })
                },
                content = { padding ->
                    if(!isLandscape)
                    {
                        Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),

                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            colorFilter = ColorFilter.tint(lightColor),
                            painter = painterResource(id = R.drawable.ligtbulb),
                            contentDescription = null,

                            modifier = Modifier

                                .size(400.dp)
                                .clickable(onClick = { ein() }),

                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = stringResource(id = R.string.Value), fontSize = 25.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(text = currentLightValue, fontSize = 30.sp)

                     }
                    }else{
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),

                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                colorFilter = ColorFilter.tint(lightColor),
                                painter = painterResource(id = R.drawable.ligtbulb),
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .size(250.dp)
                                    .clickable(onClick = { ein() }),

                                )
                            Spacer(modifier = Modifier.height(16.dp))
                            Column(modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),

                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text(text = stringResource(id = R.string.Value), fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = currentLightValue, fontSize = 24.sp)
                            }


                        }}
                }
            )
        }
    }

    override fun onResume() {
        Log.d("chuj","$isActive")
        if(isActive){registerSensor()}

        super.onResume()
    }

    override fun onPause() {
        super.onPause()
      unregisterSensor()
    }
}

