package com.eveexite.coffeemaker.presentation.main;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.eveexite.coffeemaker.R;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private MqttHandler mqttHandler;
    private TextView vTitle;
    private TextView txtJson;
    private ImageView imgTemp;
    private Switch switchTea;
    private Switch switchCoffee;
    private Switch switchSugar;
    private Button buttonOFF;

    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;
    private final ReceptorOperacion receiver =new ReceptorOperacion();
    private final ConnectionLost connectionLost =new ConnectionLost();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeView();
        mqttHandler = new MqttHandler(getApplicationContext());
        connect();
        configBroadcastReciever();
    }

    private void initializeView() {
        txtJson = findViewById(R.id.txtJson);
        imgTemp = findViewById(R.id.imgTemp);
        switchTea = findViewById(R.id.switch_tea);
        switchCoffee = findViewById(R.id.switch_coffee);
        switchSugar = findViewById(R.id.switch_sugar);
        buttonOFF = findViewById(R.id.fabPower);
        vTitle = findViewById(R.id.vTitle);

        switchTea.setEnabled(false);
        switchCoffee.setEnabled(false);
        switchSugar.setEnabled(false);

        buttonOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Acción al hacer clic en el botón de apagado
                Intent intent = new Intent(MainActivity.this, InicioActivity.class);
                startActivity(intent);
                System.out.println("Botón de apagado presionado");
            }
        });
    }

    private void connect() {
        mqttHandler.connect(mqttHandler.BROKER_URL, mqttHandler.CLIENT_ID, mqttHandler.USER, mqttHandler.PASS);
        try {

            Thread.sleep(1000);

            subscribeToTopic(MqttHandler.TOPIC_WATER_TEMP);
            subscribeToTopic(MqttHandler.TOPIC_WATER_TEMP);
            subscribeToTopic(MqttHandler.TOPIC_COFFEE);
            subscribeToTopic(MqttHandler.TOPIC_SUGAR);
            subscribeToTopic(MqttHandler.TOPIC_TEA);
            subscribeToTopic(MqttHandler.TOPIC_POWER);
            subscribeToTopic(MqttHandler.TOPIC_READY);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    //Metodo que crea y configurar un broadcast receiver para comunicar el servicio que recibe los mensaje del servidor
    //con la activity principal
    private void configBroadcastReciever()
    {
        //se asocia(registra) la  accion RESPUESTA_OPERACION, para que cuando el Servicio de recepcion la ejecute
        //se invoque automaticamente el OnRecive del objeto receiver
        filterReceive = new IntentFilter(MqttHandler.ACTION_DATA_RECEIVE);
        filterConncetionLost = new IntentFilter(MqttHandler.ACTION_CONNECTION_LOST);

        filterReceive.addCategory(Intent.CATEGORY_DEFAULT);
        filterConncetionLost.addCategory(Intent.CATEGORY_DEFAULT);

        registerReceiver(receiver, filterReceive);
        registerReceiver(connectionLost,filterConncetionLost);

    }

    @Override
    protected void onDestroy() {
        mqttHandler.disconnect();
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    private void publishMessage(String topic, String message){
        Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic,message);
    }

    private void subscribeToTopic(String topic){
        Toast.makeText(this, "Subscribing to topic "+ topic, Toast.LENGTH_SHORT).show();
        mqttHandler.subscribe(topic);
    }

    public class ReceptorOperacion extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            //Se obtiene los valores que envio el servicio atraves de un intent
            String msgJson = intent.getStringExtra("msgJson");
            txtJson.setText(msgJson);

            try {
                JSONObject jsonObject = new JSONObject(msgJson);

                // Obtener valores de cada ingrediente y el estado del agua
                boolean aguaCaliente = jsonObject.getString("agua").equals("1");
                boolean hayCafe = jsonObject.getString("cafe").equals("1");
                boolean hayAzucar = jsonObject.getString("azucar").equals("1");
                boolean hayTe = jsonObject.getString("te").equals("1");

                if (aguaCaliente) {
                    imgTemp.setImageResource(R.drawable.termometro_rojo);
                } else {
                    imgTemp.setImageResource(R.drawable.termometro_azul);
                }
                switchTea.setChecked(hayTe);
                switchCoffee.setChecked(hayCafe);
                switchSugar.setChecked(hayAzucar);

                updateTitle(aguaCaliente, hayCafe, hayAzucar, hayTe);


            } catch (JSONException e) {
                Toast.makeText(context, "Error al procesar los datos", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

    }

    private void updateTitle(boolean aguaCaliente, boolean hayCafe, boolean hayAzucar, boolean hayTe) {
        if (aguaCaliente && hayCafe && hayAzucar && hayTe) {
            vTitle.setText(R.string.ready_to_use);
        } else if (!hayTe) {
            vTitle.setText(R.string.no_tea);
        } else if (!hayCafe) {
            vTitle.setText(R.string.no_coffee);
        } else if (!hayAzucar) {
            vTitle.setText(R.string.no_sugar);
        } else if (!aguaCaliente) {
            vTitle.setText(R.string.water_cold);
        }
    }

    public class ConnectionLost extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(),"Conexion Perdida",Toast.LENGTH_SHORT).show();
            connect();

        }

    }
}
