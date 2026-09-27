#include <Arduino.h>

#include "config.h"

#include "hardware/buttons.h"
#include "hardware/neopixel.h"
#include "hardware/oled.h"
#include "hardware/rfid.h"
#include "net/wifi_mqtt.h"

// Hauptprogramm: Initialisiert Hardware und verarbeitet Eingaben.

static bool   g_playerAssigned = false;
static bool   g_isReady        = false;
static bool   g_readyStateSet  = false;
static bool   g_answerSent     = false;
static char   g_answerOption   = ' ';
static float  g_totalPoints    = 0.0f;
static String g_lastSeenRfidUid;
static int    g_lastCountdown  = 0;
static uint32_t g_lastPulseMs  = 0;

// ─── OLED rendering ──────────────────────────────────────────────────────────

static void renderOled() {
  auto &d = hw::oled::display();
  d.clearDisplay();

  String name = net::wifi_mqtt::statusPlayerName();
  d.setCursor(0, 14);
  d.print(name.length() > 0 ? name : "Player");

  String gs = net::wifi_mqtt::gameState();
  d.setCursor(0, 32);
  if (gs == "COUNTDOWN") {
    if (g_lastCountdown > 0) {
      d.print("Start in ");
      d.print(g_lastCountdown);
      d.print("...");
    } else {
      d.print("Starting...");
    }
  } else if (gs == "QUESTION") {
    d.print(g_answerSent ? (String("Sent: ") + g_answerOption) : "Answer!");
  } else if (gs == "EVALUATION") {
    d.print("+");
    d.print(net::wifi_mqtt::lastPointsAwarded(), 1);
    d.print(" pts");
  } else if (gs == "RESULTS") {
    d.print("FINISHED");
  } else {
    d.print(g_isReady ? "READY" : "WAITING");
  }

  d.setCursor(0, 50);
  d.print("Pts: ");
  d.print(g_totalPoints, 1);
  d.display();
}

// ─── Answer submission ───────────────────────────────────────────────────────

static void submitAnswer(char option) {
  if (g_answerSent) return;
  if (!net::wifi_mqtt::isQuestionActive()) return;
  g_answerSent = true;
  g_answerOption = option;
  net::wifi_mqtt::publishAnswer(option);
  Serial.print("[Game] Answer: ");
  Serial.println(option);
  renderOled();
}

// ─── Button handling ─────────────────────────────────────────────────────────

static void handleButtons() {
  String gs = net::wifi_mqtt::gameState();
  // Antworten nur waehrend QUESTION senden.
  bool inGame = g_playerAssigned && gs == "QUESTION";
  // In LOBBY/COUNTDOWN kann Ready umgeschaltet werden.
  bool canToggleReady = g_playerAssigned && (gs == "LOBBY" || gs == "COUNTDOWN");

  if (hw::buttons::isPressed(PIN_BTN_BLUE)) {
    Serial.println("[BTN] BLUE pressed");
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 0, 120), 20);
    if (inGame) {
      submitAnswer('A');
    }
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_GREEN)) {
    Serial.println("[BTN] GREEN pressed");
    hw::neopixel::flash(hw::neopixel::strip().Color(0, 120, 0), 20);
    if (inGame) {
      submitAnswer('B');
    } else if (canToggleReady && !g_isReady) {
      g_isReady = true;
      g_readyStateSet = true;
      net::wifi_mqtt::publishReady(true);
      Serial.println("[Ready] READY");
      renderOled();
    }
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_YELLOW)) {
    Serial.println("[BTN] YELLOW pressed");
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 120, 0), 20);
    if (inGame) {
      submitAnswer('C');
    }
    return;
  }

  if (hw::buttons::isPressed(PIN_BTN_RED)) {
    Serial.println("[BTN] RED pressed");
    hw::neopixel::flash(hw::neopixel::strip().Color(120, 0, 0), 20);
    if (inGame) {
      submitAnswer('D');
    } else if (canToggleReady && g_isReady) {
      g_isReady = false;
      g_readyStateSet = true;
      net::wifi_mqtt::publishReady(false);
      Serial.println("[Ready] NOT READY");
      renderOled();
    }
    return;
  }

  hw::neopixel::off();
}

// ─── RFID handling ───────────────────────────────────────────────────────────

static void handleRfid() {
  // Nach fehlgeschlagenem RFID-Scan dieselbe Karte erneut zulassen.
  if (net::wifi_mqtt::rfidScanFailed()) {
    g_lastSeenRfidUid = "";
    Serial.println("[RFID] Scan failed, allowing retry");
  }

  const String &uid = hw::rfid::lastUid();
  if (uid.length() == 0) return;
  if (uid == g_lastSeenRfidUid) return;  // Dieselbe Karte wurde bereits verarbeitet.
  
  g_lastSeenRfidUid = uid;
  Serial.print("[RFID] Scanned: ");
  Serial.println(uid);
  net::wifi_mqtt::publishRfidScan(uid);
  
  // RFID-Puffer leeren, damit kein Doppel-Scan verarbeitet wird.
  hw::rfid::clearLastUid();
}

// ─── Game state updates ──────────────────────────────────────────────────────

static void handleGameState() {
  if (!g_playerAssigned) return;

  if (net::wifi_mqtt::gameStateChanged()) {
    String gs = net::wifi_mqtt::gameState();

    if (gs == "QUESTION") {
      g_answerSent = false;
      g_answerOption = ' ';
      net::wifi_mqtt::clearCountdown();
      g_lastCountdown = 0;
    }

    if (gs == "LOBBY") {
      g_isReady = false;
      g_readyStateSet = false;
      g_answerSent = false;
      g_totalPoints = 0.0f;
      net::wifi_mqtt::clearCountdown();
      g_lastCountdown = 0;
    }

    renderOled();
  }

  // Countdown-Aenderungen direkt uebernehmen.
  if (net::wifi_mqtt::countdownActive()) {
    g_lastCountdown = net::wifi_mqtt::countdownValue();
    renderOled();
  }

  if (net::wifi_mqtt::evaluationAvailable()) {
    g_totalPoints += net::wifi_mqtt::lastPointsAwarded();
    renderOled();
  }
}

// Pulsierender LED-Effekt waehrend COUNTDOWN
static void handleCountdownPulse() {
  String gs = net::wifi_mqtt::gameState();
  if (gs != "COUNTDOWN" || !g_playerAssigned) {
    return;
  }

  uint32_t now = millis();
  if (now - g_lastPulseMs < 150) return;
  g_lastPulseMs = now;

  static uint8_t brightness = 0;
  static bool increasing = true;
  
  if (increasing) {
    brightness += 15;
    if (brightness >= 120) increasing = false;
  } else {
    brightness -= 15;
    if (brightness <= 15) increasing = true;
  }
  
  // Blaues Pulsieren waehrend COUNTDOWN.
  hw::neopixel::strip().fill(hw::neopixel::strip().Color(0, 0, brightness));
  hw::neopixel::strip().show();
}

// ─── Player status from backend ──────────────────────────────────────────────

static void checkPlayerStatus() {
  if (!net::wifi_mqtt::statusChanged()) return;

  if (net::wifi_mqtt::statusHasPlayer()) {
    g_playerAssigned = true;
    g_isReady = net::wifi_mqtt::statusIsReady();
    if (g_isReady) g_readyStateSet = true;

    float backendPts = net::wifi_mqtt::statusPoints();
    if (backendPts > g_totalPoints) g_totalPoints = backendPts;

    renderOled();
  } else if (g_playerAssigned) {
    g_playerAssigned = false;
    g_isReady = false;
    g_readyStateSet = false;
    g_answerSent = false;
    g_totalPoints = 0.0f;
    g_lastSeenRfidUid = "";   // Erneute Anmeldung mit derselben Karte erlauben.
    hw::oled::showQuizArenaSplash();
    Serial.println("[STATUS] Player unassigned");
  }
}

// ─── Setup & Loop ────────────────────────────────────────────────────────────

void setup() {
  Serial.begin(9600);
  while (!Serial) { delay(10); }

  hw::buttons::begin();
  hw::neopixel::begin();

  hw::oled::begin();
  hw::oled::showQuizArenaSplash();

  hw::rfid::begin();

  Serial.println("Setup complete.");

  if (net::wifi_mqtt::ensureConnected()) {
    Serial.println("MQTT connection OK (startup).");
  } else {
    Serial.println("MQTT connection FAILED (startup).");
  }
}

void loop() {
  net::wifi_mqtt::service();

  hw::rfid::service();

  handleButtons();

  handleRfid();

  handleGameState();

  handleCountdownPulse();

  checkPlayerStatus();
}
