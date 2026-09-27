#include "net/wifi_mqtt.h"
#include "secrets.h"
#include "hardware/oled.h"

namespace net::wifi_mqtt {

static WiFiClient   g_netClient;
static PubSubClient g_client(g_netClient);
static String       g_controllerId;
static bool         g_registered = false;
static uint32_t     g_lastStatusRequestMs = 0;

static bool   g_statusChanged   = false;
static bool   g_statusHasPlayer = false;
static bool   g_statusIsReady   = false;
static String g_statusPlayerName;
static float  g_statusPoints    = 0.0f;

// --- Spielstatus ---
static String g_gameState         = "LOBBY";
static bool   g_gameStateChanged  = false;
static String g_sessionId;
static String g_questionId;
static bool   g_questionActive    = false;
static bool   g_evalAvailable     = false;
static float  g_lastPointsAwarded = 0.0f;

// --- Sofort-Refresh und Countdown ---
static bool   g_needStatusRefresh = false;
static int    g_countdownValue    = 0;
static bool   g_countdownActive   = false;

// --- RFID-Scanstatus ---
static bool   g_rfidScanFailed    = false;

// ─── Helpers ─────────────────────────────────────────────────────────────────

static String computeControllerId() {
  if (g_controllerId.length() > 0) return g_controllerId;
  uint8_t mac[6];
  WiFi.macAddress(mac);
  char buf[13];
  sprintf(buf, "%02X%02X%02X%02X%02X%02X",
          mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  g_controllerId = String("uno-") + buf;
  return g_controllerId;
}

static String extractField(const String &json, const String &key) {
  const String pattern = String("\"") + key + "\":\"";
  int idx = json.indexOf(pattern);
  if (idx < 0) return "";
  idx += pattern.length();
  int end = json.indexOf('"', idx);
  if (end < 0) return "";
  return json.substring(idx, end);
}

// ─── MQTT callback ───────────────────────────────────────────────────────────

static void onMqttMessage(char* topic, byte* payload, unsigned int length) {
  String topicStr(topic);

  String payloadStr;
  payloadStr.reserve(length + 1);
  for (unsigned int i = 0; i < length; i++) {
    payloadStr += static_cast<char>(payload[i]);
  }

  const String prefix = String(MQTT_PREFIX);
  const String ctrlId = computeControllerId();

  // Ping direkt mit Pong beantworten.
  if (topicStr == prefix + "controller/" + ctrlId + "/ping") {
    String pongTopic = prefix + "controller/" + ctrlId + "/pong";
    g_client.publish(pongTopic.c_str(), "{\"fw\":\"v1.0\"}");
    return;
  }

  // RFID-Ergebnis protokollieren und ggf. am Display zeigen.
  if (topicStr == prefix + "controller/" + ctrlId + "/rfid/result") {
    bool ok = payloadStr.indexOf("\"success\":true") >= 0;
    String msg = extractField(payloadStr, "message");
    if (ok) {
      Serial.print("[RFID] Login OK: ");
      Serial.println(extractField(payloadStr, "player_name"));
      g_rfidScanFailed = false;
    } else {
      Serial.print("[RFID] ");
      Serial.println(msg);
      hw::oled::showMessage(msg);
      g_rfidScanFailed = true;
    }
    return;
  }

  // Spielzustand uebernehmen.
  if (topicStr == prefix + "game/state") {
    g_gameState = extractField(payloadStr, "state");
    if (g_gameState.length() == 0) {
      int idx = payloadStr.indexOf("\"state\":\"");
      if (idx < 0) g_gameState = "LOBBY";
    }
    if (g_gameState != "QUESTION") g_questionActive = false;
    g_gameStateChanged = true;
    // Nach Rueckkehr in LOBBY den Status sofort neu beim Backend anfragen.
    if (g_gameState == "LOBBY" && g_client.connected()) {
      String reqTopic = prefix + "controller/status/request";
      String reqPayload = String("{\"controller_id\":\"") + ctrlId + "\"}";
      g_client.publish(reqTopic.c_str(), reqPayload.c_str());
    }
    return;
  }

  // Frage-Info speichern (session_id und question_id).
  if (topicStr == prefix + "game/question") {
    g_sessionId     = extractField(payloadStr, "session_id");
    g_questionId    = extractField(payloadStr, "question_id");
    g_questionActive = true;
    g_gameState      = "QUESTION";
    g_gameStateChanged = true;

    // session_id und question_id koennen auch numerisch (ohne Quotes) kommen.
    if (g_sessionId.length() == 0) {
      String key = "\"session_id\":";
      int idx = payloadStr.indexOf(key);
      if (idx >= 0) {
        int s = idx + key.length();
        int e = s;
        while (e < (int)payloadStr.length() && payloadStr[e] != ',' && payloadStr[e] != '}') e++;
        g_sessionId = payloadStr.substring(s, e);
        g_sessionId.trim();
      }
    }
    if (g_questionId.length() == 0) {
      String key = "\"question_id\":";
      int idx = payloadStr.indexOf(key);
      if (idx >= 0) {
        int s = idx + key.length();
        int e = s;
        while (e < (int)payloadStr.length() && payloadStr[e] != ',' && payloadStr[e] != '}') e++;
        g_questionId = payloadStr.substring(s, e);
        g_questionId.trim();
      }
    }
    return;
  }

  // Auswertung einlesen und eigene Punkte suchen.
  if (topicStr == prefix + "game/evaluation") {
    g_questionActive = false;
    g_lastPointsAwarded = 0.0f;

    String playerName = g_statusPlayerName;
    playerName.toLowerCase();

    int searchFrom = 0;
    while (true) {
      int pnIdx = payloadStr.indexOf("\"player_name\":", searchFrom);
      if (pnIdx < 0) break;

      String foundName = extractField(payloadStr.substring(pnIdx), "player_name");
      String foundLower = foundName;
      foundLower.toLowerCase();

      if (foundLower == playerName) {
        int paIdx = payloadStr.indexOf("\"points_awarded\":", pnIdx);
        if (paIdx >= 0) {
          int s = payloadStr.indexOf(':', paIdx) + 1;
          while (s < (int)payloadStr.length() &&
                 (payloadStr[s] == ' ' || payloadStr[s] == '\t'))
            s++;
          int e = s;
          while (e < (int)payloadStr.length() &&
                 (isDigit(payloadStr[e]) || payloadStr[e] == '.' || payloadStr[e] == '-'))
            e++;
          g_lastPointsAwarded = payloadStr.substring(s, e).toFloat();
        }
        break;
      }
      searchFrom = pnIdx + 14;
    }

    g_evalAvailable = true;
    return;
  }

  // Statusantwort auswerten und lokale Spielerdaten aktualisieren.
  if (topicStr == prefix + "controller/" + ctrlId + "/status/response") {
    g_statusHasPlayer  = payloadStr.indexOf("\"has_player\":true") >= 0;
    g_statusIsReady    = payloadStr.indexOf("\"is_ready\":true")  >= 0;
    g_statusPlayerName = extractField(payloadStr, "player_name");

    g_statusPoints = 0.0f;
    int pIdx = payloadStr.indexOf("\"points\":");
    if (pIdx >= 0) {
      int start = payloadStr.indexOf(':', pIdx) + 1;
      while (start < (int)payloadStr.length() &&
             (payloadStr[start] == ' ' || payloadStr[start] == '\t'))
        start++;
      int end = start;
      while (end < (int)payloadStr.length() &&
             (isDigit(payloadStr[end]) || payloadStr[end] == '.' || payloadStr[end] == '-'))
        end++;
      g_statusPoints = payloadStr.substring(start, end).toFloat();
    }

    g_statusChanged = true;
    return;
  }

  // Bei Lobby-/Controller-Update sofort Status neu anfragen.
  if (topicStr == prefix + "controllers/updated" || topicStr == prefix + "lobby/updated") {
    g_needStatusRefresh = true;
    return;
  }

  // Countdown-Wert fuer die Anzeige uebernehmen.
  if (topicStr == prefix + "game/countdown") {
    int cIdx = payloadStr.indexOf("\"countdown\":");
    if (cIdx >= 0) {
      int s = payloadStr.indexOf(':', cIdx) + 1;
      while (s < (int)payloadStr.length() && (payloadStr[s] == ' ' || payloadStr[s] == '\t')) s++;
      int e = s;
      while (e < (int)payloadStr.length() && isDigit(payloadStr[e])) e++;
      g_countdownValue = payloadStr.substring(s, e).toInt();
      g_countdownActive = true;
    }
    return;
  }
}

// ─── Subscribe und Registrierung (nach jeder Verbindung einmal) ──────────────

static void subscribeAndRegister() {
  const String prefix = String(MQTT_PREFIX);
  const String ctrlId = computeControllerId();

  g_client.subscribe((prefix + "controller/" + ctrlId + "/ping").c_str());
  g_client.subscribe((prefix + "controller/" + ctrlId + "/status/response").c_str());
  g_client.subscribe((prefix + "controller/" + ctrlId + "/rfid/result").c_str());
  g_client.subscribe((prefix + "game/state").c_str());
  g_client.subscribe((prefix + "game/question").c_str());
  g_client.subscribe((prefix + "game/evaluation").c_str());
  g_client.subscribe((prefix + "game/countdown").c_str());
  g_client.subscribe((prefix + "controllers/updated").c_str());
  g_client.subscribe((prefix + "lobby/updated").c_str());

  String regTopic   = prefix + "controller/register";
  String regPayload = String("{\"controller_id\":\"") + ctrlId
                      + "\",\"controller_type\":\"HARDWARE\"}";
  g_client.publish(regTopic.c_str(), regPayload.c_str());

  g_registered = true;
  Serial.print("Registered as: ");
  Serial.println(ctrlId);
}

// ─── Oeffentliche API (Signaturen bleiben unveraendert) ──────────────────────

String macAddressString() {
  uint8_t mac[6];
  WiFi.macAddress(mac);

  char buf[18];
  sprintf(buf, "%02X:%02X:%02X:%02X:%02X:%02X",
          mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);

  return String(buf);
}

String controllerId() {
  return computeControllerId();
}

bool connectWiFi() {
  if (WiFi.status() == WL_CONNECTED) return true;

  Serial.print("Connecting to WiFi: ");
  Serial.println(WIFI_SSID);

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 40; // ca. 20 Sekunden
  while (WiFi.status() != WL_CONNECTED && retries-- > 0) {
    delay(500);
    Serial.print('.');
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi connected, IP: ");
    Serial.println(WiFi.localIP());
    return true;
  }

  Serial.println("WiFi connection failed!");
  return false;
}

bool connectMqtt() {
  if (!connectWiFi()) return false;

  g_client.setServer(MQTT_HOST, MQTT_PORT);
  g_client.setCallback(onMqttMessage);
  g_client.setBufferSize(1024);
  if (g_client.connected()) return true;

  Serial.print("Connecting to MQTT broker ");
  Serial.print(MQTT_HOST);
  Serial.print(':');
  Serial.println(MQTT_PORT);

  const String clientId = String("uno-") + macAddressString();

  for (int i = 0; i < 3; i++) {
    if (g_client.connect(clientId.c_str(), MQTT_USER, MQTT_PASS)) {
      Serial.println("MQTT connected!");
      subscribeAndRegister();
      return true;
    }

    Serial.print("MQTT connection failed, rc=");
    Serial.print(g_client.state());
    Serial.println(" -> retrying...");
    delay(2000);
  }

  Serial.println("MQTT connection could not be established.");
  return false;
}

bool ensureConnected() {
  return connectMqtt();
}

void service() {
  if (!g_client.connected()) {
    static uint32_t lastReconnectMs = 0;
    if (millis() - lastReconnectMs < 30000) return;
    lastReconnectMs = millis();
    g_registered = false;
    connectMqtt();
  }
  g_client.loop();

  // Sofort-Refresh bei Lobby-/Controller-Updates.
  if (g_client.connected() && g_needStatusRefresh) {
    g_needStatusRefresh = false;
    g_lastStatusRequestMs = millis();
    String topic   = String(MQTT_PREFIX) + "controller/status/request";
    String payload = String("{\"controller_id\":\"") + computeControllerId() + "\"}";
    g_client.publish(topic.c_str(), payload.c_str());
    Serial.println("[MQTT] Immediate status refresh requested");
  }

  // Zyklische Statusanfrage, damit der Controller sichtbar bleibt (alle 5s).
  if (g_client.connected() && millis() - g_lastStatusRequestMs >= 5000) {
    g_lastStatusRequestMs = millis();
    String topic   = String(MQTT_PREFIX) + "controller/status/request";
    String payload = String("{\"controller_id\":\"") + computeControllerId() + "\"}";
    g_client.publish(topic.c_str(), payload.c_str());
  }
}

bool publishMacAddress() {
  if (!ensureConnected()) {
    Serial.println("Cannot publish MAC address: MQTT not connected.");
    return false;
  }

  const String mac = macAddressString();
  const String payload = String("{\"mac\":\"") + mac + "\"}";

  Serial.print("Publishing MAC via MQTT: ");
  Serial.println(payload);

  const bool ok = g_client.publish(MQTT_TOPIC_MAC, payload.c_str());
  if (ok) {
    Serial.println("MAC address published successfully.");
  } else {
    Serial.println("MQTT publish() failed!");
  }
  return ok;
}

// ─── Status accessors ────────────────────────────────────────────────────────

bool statusChanged() {
  if (g_statusChanged) { g_statusChanged = false; return true; }
  return false;
}

bool statusHasPlayer()    { return g_statusHasPlayer; }
bool statusIsReady()      { return g_statusIsReady; }
String statusPlayerName() { return g_statusPlayerName; }
float statusPoints()      { return g_statusPoints; }

bool publishRfidScan(const String &uid) {
  if (!g_client.connected()) return false;
  String topic   = String(MQTT_PREFIX) + "controller/" + computeControllerId() + "/rfid/scan";
  String payload = String("{\"controller_id\":\"") + computeControllerId()
                   + "\",\"rfid_uid\":\"" + uid + "\"}";
  bool ok = g_client.publish(topic.c_str(), payload.c_str());
  if (ok) {
    Serial.print("[RFID] Scan sent: ");
    Serial.println(uid);
  }
  return ok;
}

bool rfidScanFailed() {
  if (g_rfidScanFailed) { g_rfidScanFailed = false; return true; }
  return false;
}

// ─── Game accessors ──────────────────────────────────────────────────────────

String gameState()       { return g_gameState; }
bool isQuestionActive()  { return g_questionActive; }

bool gameStateChanged() {
  if (g_gameStateChanged) { g_gameStateChanged = false; return true; }
  return false;
}

bool evaluationAvailable() {
  if (g_evalAvailable) { g_evalAvailable = false; return true; }
  return false;
}

float lastPointsAwarded() { return g_lastPointsAwarded; }

int countdownValue() { return g_countdownValue; }

bool countdownActive() { 
  if (g_countdownActive) { g_countdownActive = false; return true; }
  return false;
}

void clearCountdown() {
  g_countdownValue = 0;
  g_countdownActive = false;
}

bool publishAnswer(char option) {
  if (!g_client.connected()) return false;
  String topic   = String(MQTT_PREFIX) + "game/answer";
  String payload = String("{\"session_id\":") + g_sessionId
                   + ",\"controller_id\":\"" + computeControllerId()
                   + "\",\"question_id\":" + g_questionId
                   + ",\"answer\":\"" + String(option)
                   + "\",\"answered_at\":" + String(millis()) + "}";
  return g_client.publish(topic.c_str(), payload.c_str());
}

bool publishReady(bool ready) {
  if (!g_client.connected()) return false;
  String topic   = String(MQTT_PREFIX) + "player/ready";
  String payload = String("{\"controller_id\":\"") + computeControllerId()
                   + "\",\"ready\":" + (ready ? "true" : "false") + "}";
  return g_client.publish(topic.c_str(), payload.c_str());
}

} // namespace net::wifi_mqtt
