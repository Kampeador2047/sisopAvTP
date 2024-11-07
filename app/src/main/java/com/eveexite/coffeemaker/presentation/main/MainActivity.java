package com.eveexite.coffeemaker.presentation.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.eveexite.coffeemaker.R;

public class MainActivity extends Activity {
    private MqttHandler mqttHandler;
    private TextView vTitle;
    private TextView txtTemp;
    private Switch switchTea;
    private Switch switchCoffee;
    private Switch switchSugar;
    private Switch switchPower;

    public IntentFilter filterReceive;
    private final DataReceiver dataReceiver = new DataReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtTemp = findViewById(R.id.vWaterTemperature);
        switchTea = findViewById(R.id.switch_tea);
        switchCoffee = findViewById(R.id.switch_coffee);
        switchSugar = findViewById(R.id.switch_sugar);
        switchPower = findViewById(R.id.fabPower);
        vTitle = findViewById(R.id.vTitle);

        switchTea.setEnabled(false);
        switchCoffee.setEnabled(false);
        switchSugar.setEnabled(false);
        switchPower.setEnabled(false);


        mqttHandler = new MqttHandler(getApplicationContext());

        connect();

        configurarBroadcastReciever();
    }

    private void connect() {
        mqttHandler.connect(mqttHandler.BROKER_URL,mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);
    }


    //Metodo que crea y configurar un broadcast receiver para comunicar el servicio que recibe los mensaje del servidor
    //con la activity principal
    private void configurarBroadcastReciever()
    {
        filterReceive = new IntentFilter(MqttHandler.ACTION_DATA_RECEIVE);
        filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(dataReceiver, filterReceive);

    }

    @Override
    protected void onDestroy() {
        mqttHandler.disconnect();
        super.onDestroy();
        unregisterReceiver(dataReceiver);
    }

    private void publishMessage(String topic, String message){
        Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic,message);
    }

    private class DataReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("agua")) {
                String temp = intent.getStringExtra("agua");
                txtTemp.setText(temp + "°");
                if (Float.parseFloat(temp) < 30) {
                    Toast.makeText(context, "El agua está fría", Toast.LENGTH_SHORT).show();
                }
            }
            if (intent.hasExtra("te")) {
                switchTea.setChecked(intent.getBooleanExtra("te", false));
            }
            if (intent.hasExtra("azucar")) {
                switchSugar.setChecked(intent.getBooleanExtra("azucar", false));
            }
            if (intent.hasExtra("cafe")) {
                switchCoffee.setChecked(intent.getBooleanExtra("cafe", false));
            }
            if (intent.hasExtra("encendido")) {
                switchPower.setChecked(intent.getBooleanExtra("encendido", false));
            }

            if (intent.hasExtra("mensaje")) {
                String mensaje = intent.getStringExtra("mensaje");
                vTitle.setText(mensaje);  // Actualizar el mensaje en el TitleView
            }
        }
    }

    //Metodo que actua como Listener de los eventos que ocurren en los componentes graficos de la activty
    private View.OnClickListener botonesListeners = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.fabPower) {
                publishMessage(MqttHandler.TOPIC_POWER, "0");
                Toast.makeText(getApplicationContext(), "Apagando máquina", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Acción no permitida", Toast.LENGTH_LONG).show();
            }
        }
    };


    public class ConnectionLost extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(),"Conexion Perdida",Toast.LENGTH_SHORT).show();
            connect();

        }

    }
}
