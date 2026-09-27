#pragma once

#include <Arduino.h>
#include <SPI.h>
#include <MFRC522.h>

namespace hw::rfid {

// Initialisiert SPI und den RC522-RFID-Reader.
void begin();

// Muss in `loop()` regelmaessig aufgerufen werden (Erkennung und Scan).
void service();

// Liefert true, wenn der Reader erreichbar ist.
bool isReaderDetected();

// Liefert die zuletzt gelesene UID als HEX-String (z. B. "04A1...").
const String& lastUid();

// Loescht die letzte UID, damit dieselbe Karte erneut erkannt wird.
void clearLastUid();

// Hilfsfunktion: UID in einen HEX-String umwandeln.
String uidToString(const MFRC522::Uid &uid);

// Prueft einmal, ob der Reader antwortet.
bool detectReaderOnce();

} // namespace hw::rfid
