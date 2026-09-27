#pragma once

#include <Arduino.h>
#include <Adafruit_NeoPixel.h>

namespace hw::neopixel {

// Startet den NeoPixel-Strip.
void begin();

// Setzt alle LEDs auf dieselbe Farbe.
void setAll(uint32_t color);

// Schaltet alle LEDs aus.
void off();

// Kurzer Blitz-Effekt fuer alle LEDs.
void flash(uint32_t color, uint16_t waitMs);

// Direkter Zugriff auf das Strip-Objekt.
Adafruit_NeoPixel& strip();

} // namespace hw::neopixel
