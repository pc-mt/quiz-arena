#include "hardware/rfid.h"
#include "config.h"

namespace hw::rfid {

static MFRC522 g_rfid(PIN_RFID_CS, PIN_RFID_RST);
static bool g_readerDetected = false;
static uint32_t g_lastDetectCheckMs = 0;
static uint32_t g_lastReadMs = 0;
static String g_lastUid;

void begin() {

  SPI.begin();

  pinMode(PIN_RFID_CS, OUTPUT);
  g_rfid.PCD_Init();

  if (detectReaderOnce()) {
    Serial.println("RFID reader detected.");
    g_readerDetected = true;
  } else {
    Serial.println("RFID reader NOT detected (check wiring/SPI/CS/RST).");
    g_readerDetected = false;
  }

  g_rfid.PCD_DumpVersionToSerial();
}

void service() {
  const uint32_t now = millis();

  // Nur im eingestellten Intervall pruefen.
  if (!(now - g_lastDetectCheckMs >= RFID_DETECT_INTERVAL_MS)) return;

  const bool detectedNow = detectReaderOnce();
  if (detectedNow) g_lastDetectCheckMs = now;

  if (!g_readerDetected && detectedNow) {
    g_readerDetected = true;
    Serial.println("RFID reader detected. Scanning for cards...");
    g_rfid.PCD_Init();
    g_rfid.PCD_DumpVersionToSerial();
  }

  if (g_readerDetected && !detectedNow) {
    g_readerDetected = false;
    Serial.println("RFID reader is no longer reachable.");
  }

  // Ohne Reader keine Kartenpruefung.
  if (!g_readerDetected) return;

  // Wenn keine Karte erkannt wird, direkt zurueck.
  if (!(g_rfid.PICC_IsNewCardPresent() && g_rfid.PICC_ReadCardSerial())) return;

  const String currentUid = uidToString(g_rfid.uid);
  Serial.println("RFID tag UID: " + currentUid);
  g_lastUid = currentUid;
  g_lastReadMs = now;

  g_rfid.PICC_HaltA();
  g_rfid.PCD_StopCrypto1();
}

bool isReaderDetected() {
  return g_readerDetected;
}

const String& lastUid() {
  return g_lastUid;
}

void clearLastUid() {
  g_lastUid = "";
}

bool detectReaderOnce() {
  byte v = g_rfid.PCD_ReadRegister(MFRC522::VersionReg);
  // 0x00 oder 0xFF deutet auf einen fehlgeschlagenen Registerzugriff hin.
  return !(v == 0x00 || v == 0xFF);
}

String uidToString(const MFRC522::Uid &uid) {
  String out;
  out.reserve(uid.size * 2);
  for (byte i = 0; i < uid.size; i++) {
    if (uid.uidByte[i] < 0x10) out += "0";
    out += String(uid.uidByte[i], HEX);
  }
  out.toUpperCase();
  return out;
}

} // namespace hw::rfid
