-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 07-seed-questions-security.sql
-- IT-Sicherheit (11 Fragen: 1x EASY, 6x MEDIUM, 4x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 6: IT-SICHERHEIT (ID 6)
-- ============================================

-- EASY Level (1 Frage)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(31, 6, 'EASY', 'Was ist Phishing?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q31 (Phishing)
(121, 31, 'A', 'Betrug, um an Zugangsdaten über gefälschte Nachrichten/Seiten zu kommen'),
(122, 31, 'B', 'Ein legales Hacking-Zertifikat'),
(123, 31, 'C', 'Ein Kompressions-Algorithmus'),
(124, 31, 'D', 'Ein Firewall-Rule-Set');

-- MEDIUM Level (6 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(33, 6, 'MEDIUM', 'Wozu dient ein Salt beim Passwort-Hashing?', 'D', 1),
(34, 6, 'MEDIUM', 'Was ist eine Brute-Force-Attacke?', 'C', 1),
(98, 6, 'MEDIUM', 'Was ist ein Man-in-the-Middle-Angriff (MITM)?', 'B', 1),
(99, 6, 'MEDIUM', 'Was ist der Zweck einer Firewall?', 'A', 1),
(100, 6, 'MEDIUM', 'Was ist Cross-Site Scripting (XSS)?', 'D', 1),
(101, 6, 'MEDIUM', 'Was ist Zwei-Faktor-Authentifizierung (2FA)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q33 (Salt)
(129, 33, 'A', 'Ersetzt das Passwort durch Klartext'),
(130, 33, 'B', 'Verkürzt Hashes für schnellere Logins'),
(131, 33, 'C', 'Entfernt Sonderzeichen aus Passwörtern'),
(132, 33, 'D', 'Verhindert gleiche Hashes bei gleichen Passwörtern (Rainbow Tables)'),
-- Q34 (Brute-Force)
(133, 34, 'A', 'Ein Angriff auf MQTT über WebSockets'),
(134, 34, 'B', 'Das Manipulieren von DNS-Einträgen'),
(135, 34, 'C', 'Das systematische Durchprobieren aller möglichen Passwörter/Schlüssel'),
(136, 34, 'D', 'Ein Angriff durch defekte RAM-Bausteine'),
-- Q98 (MITM)
(389, 98, 'A', 'Ein Angriff, bei dem ein Server physisch gestohlen wird'),
(390, 98, 'B', 'Ein Angriff, bei dem der Angreifer die Kommunikation zwischen zwei Parteien abfängt und manipuliert'),
(391, 98, 'C', 'Ein Angriff durch Überlastung eines Servers mit Anfragen'),
(392, 98, 'D', 'Ein Angriff durch Manipulation von DNS-Einträgen im Cache'),
-- Q99 (Firewall)
(393, 99, 'A', 'Die Überwachung und Filterung des ein- und ausgehenden Netzwerkverkehrs nach definierten Regeln'),
(394, 99, 'B', 'Das Verschlüsseln aller Dateien auf der Festplatte'),
(395, 99, 'C', 'Das automatische Aktualisieren von Software'),
(396, 99, 'D', 'Die Komprimierung von Netzwerkpaketen für schnellere Übertragung'),
-- Q100 (XSS)
(397, 100, 'A', 'Das Kopieren einer Website auf einen anderen Server'),
(398, 100, 'B', 'Das gleichzeitige Öffnen mehrerer Browser-Tabs'),
(399, 100, 'C', 'Das Weiterleiten von HTTP-Anfragen an einen anderen Server'),
(400, 100, 'D', 'Das Einschleusen von schädlichem JavaScript-Code in eine Webseite, der im Browser anderer Nutzer ausgeführt wird'),
-- Q101 (2FA)
(401, 101, 'A', 'Die Verwendung von zwei verschiedenen Passwörtern nacheinander'),
(402, 101, 'B', 'Die doppelte Verschlüsselung von Daten'),
(403, 101, 'C', 'Ein Sicherheitsverfahren, das zwei unterschiedliche Authentifizierungsfaktoren kombiniert, z. B. Passwort und SMS-Code'),
(404, 101, 'D', 'Das Anmelden bei zwei verschiedenen Diensten gleichzeitig');

-- HARD Level (4 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(35, 6, 'HARD', 'Was ist SQL-Injection?', 'B', 1),
(36, 6, 'HARD', 'Welcher Sicherheits-Standard gilt für Webseiten mit sensiblen Daten?', 'A', 1),
(102, 6, 'HARD', 'Was ist ein Zero-Day-Exploit?', 'B', 1),
(103, 6, 'HARD', 'Was beschreibt das Prinzip der geringsten Privilegien (Principle of Least Privilege)?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q35 (SQL-Injection)
(137, 35, 'A', 'Ein Angriff auf MQTT durch fehlende Authentifizierung'),
(138, 35, 'B', 'Einschleusen von SQL-Code über Benutzereingaben, um Queries zu manipulieren'),
(139, 35, 'C', 'Ein Angriff durch defekte Datenbankzertifikate'),
(140, 35, 'D', 'Ein Angriff auf Bluetooth-Pairing'),
-- Q36 (HTTPS)
(141, 36, 'A', 'HTTPS mit gültigem TLS/SSL-Zertifikat und Verschlüsselung'),
(142, 36, 'B', 'HTTP mit starken Passwörtern'),
(143, 36, 'C', 'FTP mit 2FA (Two-Factor Authentication)'),
(144, 36, 'D', 'TELNET mit Firewall-Protection'),
-- Q102 (Zero-Day)
(405, 102, 'A', 'Ein Angriff, der exakt um Mitternacht ausgeführt wird'),
(406, 102, 'B', 'Die Ausnutzung einer Sicherheitslücke, die dem Hersteller noch nicht bekannt ist und für die kein Patch existiert'),
(407, 102, 'C', 'Ein Exploit, der nur an neu installierten Systemen funktioniert'),
(408, 102, 'D', 'Eine Schwachstelle, die nach null Tagen automatisch behoben wird'),
-- Q103 (Least Privilege)
(409, 103, 'A', 'Jeder Benutzer und jeder Prozess erhält nur die minimal notwendigen Zugriffsrechte, um seine Aufgabe zu erfüllen'),
(410, 103, 'B', 'Administratoren haben immer Zugriff auf alle Systeme'),
(411, 103, 'C', 'Sicherheitsrichtlinien gelten nur für externe Benutzer'),
(412, 103, 'D', 'Alle Benutzer teilen sich ein gemeinsames Administratorkonto');

-- Verify insertion
SELECT COUNT(*) as security_questions FROM questions WHERE category_id = 6;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 6 GROUP BY difficulty;
