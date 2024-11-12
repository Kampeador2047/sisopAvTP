package com.eveexite.coffeemaker.presentation.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.eveexite.coffeemaker.R;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;


public class InicioActivity extends AppCompatActivity implements ISingletonActivities, SensorEventListener{

    private MqttHandler mqttHandler;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 18.0f;
    private static final long INTERVAL_SHAKE= 1000;
    private long lastShakeTime = 0;
    private boolean shakeDetected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        initializeView();
        mqttHandler = new MqttHandler(getApplicationContext());
        connect();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void initializeView() {
        Button buttonStart = findViewById(R.id.buttonStart);

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(InicioActivity.this, MainActivity.class);
                startActivity(intent);
                System.out.println("Botón de encendido presionado");
                publishMessage(MqttHandler.TOPIC_POWER, "1");
                finish();
            }
        });
    }

    @Override
    public void connect() {
        mqttHandler.connect(MqttHandler.BROKER_URL, MqttHandler.CLIENT_ID, MqttHandler.USER, MqttHandler.PASS);

    }

    @Override
    public void subscribeToTopic(String topic) {
        Toast.makeText(this, "Subscribing to topic "+ topic, Toast.LENGTH_SHORT).show();
        mqttHandler.subscribe(topic);
    }

    @Override
    public void configBroadcastReciever() {

    }

    @Override
    public void publishMessage(String topic, String message) {
        Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic,message);
    }


    public class ConnectionLost extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(),"Conexion Perdida",Toast.LENGTH_SHORT).show();
            connect();

        }

    }

    // Métodos de SensorEventListener
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float acceleration = (float) Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            long currentTime = System.currentTimeMillis();

            if (!shakeDetected && acceleration > SHAKE_THRESHOLD && (currentTime - lastShakeTime) > INTERVAL_SHAKE) {
                shakeDetected = true;
                lastShakeTime = currentTime;
                publishMessage(MqttHandler.TOPIC_POWER, "1");
                Toast.makeText(this, "Shake detectado! Encendiendo máquina.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(InicioActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
