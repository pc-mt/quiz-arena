package com.example.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

public class MqttService {

    private final MqttClient mqttClient;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);

    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "quiz-arena/";

    public MqttService(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    // --- Spielbezogene Topics ---
    public void publishGameStart() {
        JsonObject data = new JsonObject().put("action", "start");
        mqttClient.publish(mqttMessagePrefix + "game/start", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game start: {}", data);
    }

    public void publishGameStop() {
        JsonObject data = new JsonObject().put("action", "stop");
        mqttClient.publish(mqttMessagePrefix + "game/stop", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game stop: {}", data);
    }

    public void publishObjectCreated(String name) {
        JsonObject data = new JsonObject().put("name", name);
        mqttClient.publish(mqttMessagePrefix + "game/object/created", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published object created: {}", data);
    }

    public void publishDemoMessage(String message) {
        JsonObject data = new JsonObject().put("message", message);
        mqttClient.publish(mqttMessagePrefix + "demo/message", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published demo message: {}", data);
    }

    public void publishControllersUpdated() {
        JsonObject data = new JsonObject().put("event", "controllers.updated");
        mqttClient.publish(mqttMessagePrefix + "controllers/updated", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published controllers updated: {}", data);
    }

    public void publishLobbyUpdated() {
        JsonObject data = new JsonObject()
                .put("event", "lobby.updated")
                .put("timestamp", System.currentTimeMillis());
        mqttClient.publish(mqttMessagePrefix + "lobby/updated", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published lobby updated");
    }

    public void publishGameState(JsonObject data) {
        mqttClient.publish(mqttMessagePrefix + "game/state", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game state: {}", data.encode());
    }

    public void publishGameCountdown(JsonObject data) {
        mqttClient.publish(mqttMessagePrefix + "game/countdown", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game countdown: {}", data.encode());
    }

    public void publishGameQuestion(JsonObject data) {
        mqttClient.publish(mqttMessagePrefix + "game/question", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game question: {}", data.encode());
    }

    public void publishGameEvaluation(JsonObject data) {
        mqttClient.publish(mqttMessagePrefix + "game/evaluation", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game evaluation: {}", data.encode());
    }

    public void publishGameQuestionTimer(JsonObject data) {
        mqttClient.publish(mqttMessagePrefix + "game/question/timer", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game question timer: {}", data.encode());
    }

    // --- Generischer Publisher ---
    public void publishJson(String topic, JsonObject data) {
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published to {}: {}", topic, data.encode());
    }

    /** Sendet einen Heartbeat-Ping an genau einen Controller. */
    public void publishPing(String controllerId, String requestId, long ts) {
        String topic = mqttMessagePrefix + "controller/" + controllerId + "/ping";
        JsonObject data = new JsonObject()
                .put("requestId", requestId != null ? requestId : java.util.UUID.randomUUID().toString())
                .put("ts", ts)
                .put("fw", "v1.0");
        mqttClient.publish(topic, data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.debug("📡 MQTT ping to {}", controllerId);
    }
}
