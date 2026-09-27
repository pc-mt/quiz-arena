#pragma once

#include <Arduino.h>

namespace hw::buttons {

// Initialisiert die Button-Pins.
void begin();

// Liefert true, wenn der Button gedrueckt ist, sonst false.
bool isPressed(uint8_t pin);

} // namespace hw::buttons
