package com.eveexite.coffeemaker.presentation.main;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;


public class MqttHandler implements MqttCallback {
    public static final String BROKER_URL = "tcp://broker.emqx.io:1883";
    public static final String CLIENT_ID = "mqttx_f9bfd3ww";
    public static final String USER="";
    public static final String PASS="";

    public static final String TOPIC_WATER_TEMP = "CoffeeXpert/agua";
    public static final String TOPIC_COFFEE = "CoffeeXpert/cafe";
    public static final String TOPIC_SUGAR = "CoffeeXpert/azucar";
    public static final String TOPIC_TEA = "CoffeeXpert/te";
    public static final String TOPIC_POWER = "CoffeeXpert/encendido";
    public static final String TOPIC_READY = "CoffeeXpert/ready";

    public static final String ACTION_DATA_RECEIVE ="com.example.intentservice.intent.action.DATA_RECEIVE";
    public static final String ACTION_CONNECTION_LOST ="com.example.intentservice.intent.action.CONNECTION_LOST";

    private MqttClient client;
    private Context mContext;

    // Estados de sensores
    private boolean aguaFria = false;
    private boolean faltaCafe = false;
    private boolean faltaAzucar = false;
    private boolean faltaTe = false;
    private boolean powerOn = false; // Estado inicial de power

    // Método para verificar el estado de power
    public boolean isPowerOn() {
        return powerOn;
    }

    // Método para actualizar el estado de power
    public void updatePowerState(boolean state) {
        this.powerOn = state;
    }

    public MqttHandler(Context mContext){

        this.mContext = mContext;

    }

    public void connect( String brokerUrl, String clientId,String username, String password) {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(username);
            options.setPassword(password.toCharArray());


            MemoryPersistence persistence = new MemoryPersistence();
            client = new MqttClient(brokerUrl, clientId, persistence);
            client.setCallback(this);
            client.connect(options);

            // Subscribe to all relevant topics
            client.subscribe(TOPIC_WATER_TEMP);
            client.subscribe(TOPIC_COFFEE);
            client.subscribe(TOPIC_SUGAR);
            client.subscribe(TOPIC_TEA);
            client.subscribe(TOPIC_POWER);
            client.subscribe(TOPIC_READY);


            Log.d("MqttHandler", "Connected and subscribed to topics.");
        } catch (MqttException e) {
            Log.d("Aplicacion",e.getMessage()+ "  "+e.getCause());
        }
    }

    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            mqttMessage.setQos(2);
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void connectionLost(Throwable cause) {
        Log.d("MAIN ACTIVITY","conexion perdida"+ cause.getMessage().toString());

        Intent i = new Intent(ACTION_CONNECTION_LOST);
        mContext.sendBroadcast(i);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String msgContent = message.toString();
        Intent intent = new Intent(ACTION_DATA_RECEIVE);

        // Interpret the message based on the topic
        switch (topic) {
            case TOPIC_WATER_TEMP:
                aguaFria = msgContent.equals("Agua Fria");
                break;
            case TOPIC_COFFEE:
                faltaCafe = msgContent.equals("Falta Cafe");
                break;
            case TOPIC_SUGAR:
                faltaAzucar = msgContent.equals("Falta Azucar");
                break;
            case TOPIC_TEA:
                faltaTe = msgContent.equals("Falta Te");
                break;
            case TOPIC_READY:
                resetEstados();  // Resetear estados si todo está listo
                break;
            default:
                Log.w("MqttHandler", "Unknown topic received: " + topic);
                return;
        }

        intent.putExtra("mensaje", determinarMensaje());
        mContext.sendBroadcast(intent);
    }

    private String determinarMensaje() {
        if (aguaFria) return "Agua fría";
        if (faltaCafe) return "No hay café";
        if (faltaTe) return "No hay té";
        if (faltaAzucar) return "No hay azúcar";
        return "Listo para usar";
    }

    private void resetEstados() {
        aguaFria = false;
        faltaCafe = false;
        faltaAzucar = false;
        faltaTe = false;
    }


    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d("MqttHandler", "Message delivery complete");
    }
}

