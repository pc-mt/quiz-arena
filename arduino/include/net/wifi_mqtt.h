#pragma once

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiS3.h>

namespace net::wifi_mqtt {

// Verbindet mit WiFi und MQTT.
bool ensureConnected();

// Verbindet nur mit WiFi.
bool connectWiFi();

// Verbindet nur mit MQTT.
bool connectMqtt();

// Sendet die MAC-Adresse per MQTT.
bool publishMacAddress();

// Gibt die MAC-Adresse als String zurueck (z. B. "AA:BB:...").
String macAddressString();

// In jeder loop() aufrufen, damit MQTT aktiv bleibt und Nachrichten verarbeitet werden.
void service();

// Gibt die MQTT-Controller-ID zurueck (z. B. "uno-AABBCCDDEEFF").
String controllerId();

// One-shot: true genau einmal, wenn ein neuer Status angekommen ist.
bool statusChanged();

// True, wenn aktuell ein Spieler zugeordnet ist.
bool statusHasPlayer();

// Name des zugeordneten Spielers.
String statusPlayerName();

// Aktuelle Punkte des zugeordneten Spielers.
float statusPoints();

// Sendet einen RFID-Scan fuer den Login ans Backend.
bool publishRfidScan(const String &uid);

// One-shot: true genau einmal, wenn ein RFID-Scan fehlgeschlagen ist.
bool rfidScanFailed();

// Sendet Ready oder Not-Ready fuer diesen Controller.
bool publishReady(bool ready);

// Liefert den Ready-Status aus der letzten Statusantwort.
bool statusIsReady();

// ─── Game phase ──────────────────────────────────────────────────────────────

// Aktueller Spielzustand (LOBBY, COUNTDOWN, QUESTION, EVALUATION, RESULTS, ENDED).
String gameState();

// True nur waehrend QUESTION und vor dem Senden einer Antwort.
bool isQuestionActive();

// One-shot: true genau einmal, wenn sich der Spielzustand geaendert hat.
bool gameStateChanged();

// One-shot: true genau einmal, wenn eine Auswertung angekommen ist.
bool evaluationAvailable();

// In der letzten Auswertung erhaltene Punkte.
float lastPointsAwarded();

// Sendet eine Antwort (A, B, C oder D) fuer die aktuelle Frage.
bool publishAnswer(char option);

// ─── Countdown ────────────────────────────────────────────────────────────────

// Aktueller Countdown-Wert (3, 2, 1, 0).
int countdownValue();

// One-shot: true genau einmal pro neuem Countdown-Tick.
bool countdownActive();

// Setzt den Countdown-Status zurueck.
void clearCountdown();

} // namespace net::wifi_mqtt
