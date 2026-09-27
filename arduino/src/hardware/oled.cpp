#include "hardware/oled.h"
#include "config.h"

namespace hw::oled {

static Adafruit_SH1106 g_display(OLED_RESET_PIN);

// --- Initialisierung ---
void begin() {
  g_display.begin(0x02, OLED_I2C_ADDRESS, OLED_RESET_PIN);
  g_display.clearDisplay();
  g_display.setRotation(0);
  g_display.display();

  g_display.setTextColor(WHITE);
  g_display.setFont(&FreeSans9pt7b);
}

// --- Anzeige ---
void showQuizArenaSplash() {
  g_display.clearDisplay();
  g_display.setTextColor(WHITE);
  g_display.setFont(&FreeSans9pt7b);
  g_display.setCursor(8, 26);
  g_display.print("QuizArena");
  g_display.drawLine(8, 34, 120, 34, WHITE);
  g_display.setCursor(26, 56);
  g_display.print("Controller");
  g_display.display();
  delay(1000);
}

// --- Hilfsanzeige ---
void showMessage(const String& line1, const String& line2) {
  g_display.clearDisplay();
  g_display.setCursor(0, 24);
  g_display.print(line1);
  if (line2.length() > 0) {
    g_display.setCursor(0, 48);
    g_display.print(line2);
  }
  g_display.display();
}

// --- Direkter Zugriff ---
Adafruit_SH1106& display() {
  return g_display;
}

} // namespace hw::oled
