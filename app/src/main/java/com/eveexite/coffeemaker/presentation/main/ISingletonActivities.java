package com.eveexite.coffeemaker.presentation.main;

public interface ISingletonActivities {
    public void connect();
    public void subscribeToTopic(String topic);
    public void configBroadcastReciever();
    public void publishMessage(String topic, String message);


}
