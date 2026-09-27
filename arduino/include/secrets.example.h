#pragma once

// Copy this file to secrets.h and replace the placeholder values.
// secrets.h is ignored by Git and must never be committed.

static constexpr const char* WIFI_SSID     = "your-wifi-ssid";
static constexpr const char* WIFI_PASSWORD = "your-wifi-password";

static constexpr const char* MQTT_HOST     = "your-mqtt-host";
static constexpr uint16_t    MQTT_PORT     = 1883;
static constexpr const char* MQTT_USER     = "quiz-arena";
static constexpr const char* MQTT_PASS     = "your-mqtt-password";

static constexpr const char* MQTT_PREFIX    = "quiz-arena/";
static constexpr const char* MQTT_TOPIC_MAC = "quiz-arena/arduino/mac";
