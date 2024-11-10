package com.eveexite.coffeemaker.presentation.main;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.eveexite.coffeemaker.R;


public class MainActivity extends Activity implements ISingletonActivities{
    private MqttHandler mqttHandler;
    private TextView vTitle;
    private ImageView imgTemp;
    private Switch switchTea;
    private Switch switchCoffee;
    private Switch switchSugar;
    private Button buttonOFF;

    public IntentFilter filterReceive;
    public IntentFilter filterConncetionLost;
    private final ReceptorOperacion receiver =new ReceptorOperacion();
    private final ConnectionLost connectionLost =new ConnectionLost();

    private final long DURATION= 1000;
    private final long VIBRATION = 500;


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
        imgTemp = findViewById(R.id.imgTemp);
        switchTea = findViewById(R.id.switch_tea);
        switchCoffee = findViewById(R.id.switch_coffee);
        switchSugar = findViewById(R.id.switch_sugar);
        buttonOFF = findViewById(R.id.fabPower);
        vTitle = findViewById(R.id.vTitle);

        ImageView header = findViewById(R.id.header);
        ImageView backgroundTitle = findViewById(R.id.backgroudTitle);
        ImageView coffeMaker = findViewById(R.id.coffeeMaker);

        header.setImageResource(R.drawable.bg_header);
        backgroundTitle.setImageResource(R.drawable.bg_msg);
        coffeMaker.setImageResource(R.drawable.coffeemaker);
        imgTemp.setImageResource(R.drawable.termometro_rojo);

        switchCoffee.setChecked(true);
        switchTea.setChecked(true);
        switchSugar.setChecked(true);
        switchTea.setEnabled(false);
        switchCoffee.setEnabled(false);
        switchSugar.setEnabled(false);

        // Animación de desplazamiento horizontal
        ObjectAnimator animator = ObjectAnimator.ofFloat(vTitle, "translationX", -50f, 50f);
        animator.setDuration(DURATION); // Duración de la animación
        animator.setRepeatCount(ObjectAnimator.INFINITE); // Repetir infinitamente
        animator.setRepeatMode(ObjectAnimator.REVERSE); // Ir y venir
        animator.start();


        buttonOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Acción al hacer clic en el botón de apagado
                Intent intent = new Intent(MainActivity.this, InicioActivity.class);
                startActivity(intent);
                System.out.println("Botón de apagado presionado");
                // cuando se presiona el boton se publica en el topico del boton y apague
                publishMessage(MqttHandler.TOPIC_POWER, "0");
                finish();
            }
        });
    }

    // pasarlo a la activity de inicio para que se suscriba al principio
    @Override
    public void connect() {
        mqttHandler.connect(MqttHandler.BROKER_URL, MqttHandler.CLIENT_ID, MqttHandler.USER, MqttHandler.PASS);
        try {

            Thread.sleep(DURATION);

            subscribeToTopic(MqttHandler.TOPIC_WATER_TEMP);
            subscribeToTopic(MqttHandler.TOPIC_READY);
            subscribeToTopic(MqttHandler.TOPIC_COFFEE);
            subscribeToTopic(MqttHandler.TOPIC_SUGAR);
            subscribeToTopic(MqttHandler.TOPIC_TEA);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    //Metodo que crea y configurar un broadcast receiver para comunicar el servicio que recibe los mensaje del servidor
    //con la activity principal
    @Override
    public void configBroadcastReciever()
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

    @Override
    public void publishMessage(String topic, String message){
        Toast.makeText(this, "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        mqttHandler.publish(topic,message);
    }

    @Override
    public void subscribeToTopic(String topic){
        Toast.makeText(this, "Subscribing to topic "+ topic, Toast.LENGTH_SHORT).show();
        mqttHandler.subscribe(topic);
    }

    public class ReceptorOperacion extends BroadcastReceiver {

        // pasar a texto plano
        public void onReceive(Context context, Intent intent) {
            //Se obtiene los valores que envio el servicio atraves de un intent
            String msg = intent.getStringExtra("msg");

            try {

                // Obtener valores de cada ingrediente y el estado del agua
                boolean aguaCaliente = true;
                boolean hayCafe = true;
                boolean hayAzucar = true;
                boolean hayTe = true;
                if (msg != null) {
                    aguaCaliente = !msg.equals("Agua Fria");
                    hayCafe = !msg.equals("Falta Cafe");
                    hayAzucar = !msg.equals("Falta Azucar");
                    hayTe = !msg.equals("Falta Te");
                }

                if (aguaCaliente) {
                    imgTemp.setImageResource(R.drawable.termometro_rojo);
                } else {
                    imgTemp.setImageResource(R.drawable.termometro_azul);
                    Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(VIBRATION);
                    }
                }
                switchTea.setEnabled(true);
                switchCoffee.setEnabled(true);
                switchSugar.setEnabled(true);
                switchTea.setChecked(hayTe);
                switchCoffee.setChecked(hayCafe);
                switchSugar.setChecked(hayAzucar);
                switchTea.setEnabled(false);
                switchCoffee.setEnabled(false);
                switchSugar.setEnabled(false);

                updateTitle(aguaCaliente, hayCafe, hayAzucar, hayTe);
            } catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    private void updateTitle(boolean aguaCaliente, boolean hayCafe, boolean hayAzucar, boolean hayTe) {
        if (aguaCaliente && hayCafe && hayAzucar && hayTe) {
            vTitle.setText(R.string.ready);
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
