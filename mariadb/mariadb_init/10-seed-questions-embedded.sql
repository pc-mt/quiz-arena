-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 10-seed-questions-embedded.sql
-- Embedded Systems (9 Fragen: 1x EASY, 3x MEDIUM, 5x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 9: EMBEDDED SYSTEMS (ID 9)
-- ============================================

-- EASY Level (1 Frage)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(49, 9, 'EASY', 'Wofür steht GPIO?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q49 (GPIO)
(193, 49, 'A', 'General Purpose Graph Output'),
(194, 49, 'B', 'Global Peripheral I/O'),
(195, 49, 'C', 'Generic Processing Gate Output'),
(196, 49, 'D', 'General Purpose Input/Output');

-- MEDIUM Level (3 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(51, 9, 'MEDIUM', 'Was ist PWM (Pulse Width Modulation)?', 'B', 1),
(52, 9, 'MEDIUM', 'Welcher Standard wird häufig für Kommunikation zwischen Mikrocontrollern und Sensoren genutzt?', 'A', 1),
(115, 9, 'MEDIUM', 'Was ist ein RTOS (Real-Time Operating System)?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q51 (PWM)
(201, 51, 'A', 'Permanent Web Memory'),
(202, 51, 'B', 'Pulsweitenmodulation - Variation der Impulsbreite für Analog-ähnliche Ausgaben (z.B. LED-Helligkeit)'),
(203, 51, 'C', 'Private WiFi Mode'),
(204, 51, 'D', 'Parallel Wire Multiplexing'),
-- Q52 (I2C/SPI)
(205, 52, 'A', 'I2C oder SPI'),
(206, 52, 'B', 'UART'),
(207, 52, 'C', 'CAN-Bus'),
(208, 52, 'D', 'Ethernet'),
-- Q115 (RTOS)
(457, 115, 'A', 'Ein Betriebssystem ausschließlich für Desktop-Computer'),
(458, 115, 'B', 'Ein Betriebssystem, das garantierte Antwortzeiten für zeitkritische Aufgaben bietet'),
(459, 115, 'C', 'Ein Dateisystem für Embedded-Speicher'),
(460, 115, 'D', 'Ein Protokoll zur Datenübertragung zwischen Mikrocontrollern');

-- HARD Level (5 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(53, 9, 'HARD', 'Warum nutzt man Interrupts in Embedded Systems?', 'C', 1),
(54, 9, 'HARD', 'Was ist ein Watchdog-Timer?', 'B', 1),
(116, 9, 'HARD', 'Was versteht man unter DMA (Direct Memory Access) in Embedded Systems?', 'B', 1),
(117, 9, 'HARD', 'Was ist der Unterschied zwischen Little-Endian und Big-Endian?', 'C', 1),
(118, 9, 'HARD', 'Was ist ein Bootloader in einem Embedded System?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q53 (Interrupts)
(209, 53, 'A', 'Um Code langsamer zu machen'),
(210, 53, 'B', 'Um mehr RAM zu reservieren'),
(211, 53, 'C', 'Um auf externe Ereignisse sofort zu reagieren statt ständig zu pollen - effizient und responsiv'),
(212, 53, 'D', 'Um SQL-Abfragen zu beschleunigen'),
-- Q54 (Watchdog)
(213, 54, 'A', 'Ein Sensor zur Temperaturüberwachung'),
(214, 54, 'B', 'Ein Timer, der das System zurücksetzt, wenn es hängen bleibt oder nicht reagiert'),
(215, 54, 'C', 'Ein GPIO-Pin mit spezieller Funktion'),
(216, 54, 'D', 'Eine Debug-Komponente für Fehleranalyse'),
-- Q116 (DMA)
(461, 116, 'A', 'Eine Methode zur Kompression von Speicherdaten'),
(462, 116, 'B', 'Ein Verfahren, bei dem Peripheriegeräte ohne CPU-Beteiligung direkt auf den Arbeitsspeicher zugreifen können'),
(463, 116, 'C', 'Eine spezielle Art von Cache-Speicher'),
(464, 116, 'D', 'Ein Debugging-Modus für Speicherfehler'),
-- Q117 (Endianness)
(465, 117, 'A', 'Little-Endian speichert Daten komprimiert, Big-Endian unkomprimiert'),
(466, 117, 'B', 'Little-Endian ist grundsätzlich schneller als Big-Endian'),
(467, 117, 'C', 'Sie beschreiben die Byte-Reihenfolge im Speicher – Little-Endian speichert das niederwertigste Byte zuerst'),
(468, 117, 'D', 'Big-Endian wird ausschließlich bei 64-Bit-Prozessoren verwendet'),
-- Q118 (Bootloader)
(469, 118, 'A', 'Ein Treiber für die serielle Kommunikation'),
(470, 118, 'B', 'Ein Programm zur grafischen Benutzeroberfläche'),
(471, 118, 'C', 'Ein kleines Programm, das beim Start die Firmware lädt und die Hardware initialisiert'),
(472, 118, 'D', 'Ein Testprogramm für die Speicherdiagnose');

-- Verify insertion
SELECT COUNT(*) as embedded_questions FROM questions WHERE category_id = 9;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 9 GROUP BY difficulty;
