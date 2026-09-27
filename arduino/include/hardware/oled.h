#pragma once

#include <Arduino.h>
#include <Adafruit_GFX.h>
#include <Fonts/FreeSans9pt7b.h>
#include "Adafruit_SH1106.hpp"

namespace hw::oled {

// Initialisiert das OLED-Display.
void begin();

// Zeigt den QuizArena-Startbildschirm.
void showQuizArenaSplash();

// Zeigt eine kurze Textmeldung auf dem Display.
void showMessage(const String& line1, const String& line2 = "");

// Direkter Zugriff auf das Display-Objekt.
Adafruit_SH1106& display();

} // namespace hw::oled
