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

public class InicioActivity extends AppCompatActivity implements ISingletonActivities {

    private MqttHandler mqttHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inicio);
        initializeView();
        mqttHandler = new MqttHandler(getApplicationContext());
        connect();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
}
