-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 05-seed-questions-network.sql
-- Netzwerke (17 Fragen: 6x EASY, 9x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 4: NETZWERKE (ID 4)
-- ============================================

-- EASY Level (6 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(19, 4, 'EASY', 'Welches Protokoll nutzt typischerweise Port 80?', 'C', 1),
(20, 4, 'EASY', 'Was ist eine IP-Adresse?', 'A', 1),
(78, 4, 'EASY', 'Was ist ein DNS-Server?', 'B', 1),
(79, 4, 'EASY', 'Welches Gerät verbindet verschiedene Netzwerke miteinander?', 'D', 1),
(80, 4, 'EASY', 'Was ist ein LAN?', 'A', 1),
(81, 4, 'EASY', 'Welches Protokoll wird zum Versenden von E-Mails verwendet?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q19 (Port 80)
(73, 19, 'A', 'HTTPS'),
(74, 19, 'B', 'FTP'),
(75, 19, 'C', 'HTTP'),
(76, 19, 'D', 'SSH'),
-- Q20 (IP-Adresse)
(77, 20, 'A', 'Eine eindeutige numerische Adresse zum Identifizieren von Geräten in einem Netzwerk'),
(78, 20, 'B', 'Ein Passwort für Netzwerk-Authentifizierung'),
(79, 20, 'C', 'Ein Routing-Protokoll'),
(80, 20, 'D', 'Eine Datenbankverbindungs-Zeichenkette'),
-- Q78 (DNS)
(309, 78, 'A', 'Ein Server, der Firewalls konfiguriert'),
(310, 78, 'B', 'Ein Server, der Domainnamen in IP-Adressen auflöst'),
(311, 78, 'C', 'Ein Server, der E-Mails verschlüsselt'),
(312, 78, 'D', 'Ein Server, der Bandbreite im Netzwerk begrenzt'),
-- Q79 (Router)
(313, 79, 'A', 'Hub'),
(314, 79, 'B', 'Repeater'),
(315, 79, 'C', 'Switch'),
(316, 79, 'D', 'Router'),
-- Q80 (LAN)
(317, 80, 'A', 'Ein lokales Netzwerk, z. B. in einem Gebäude oder Campus'),
(318, 80, 'B', 'Ein weltweites öffentliches Netzwerk wie das Internet'),
(319, 80, 'C', 'Ein drahtloses Mobilfunknetz'),
(320, 80, 'D', 'Ein virtuelles privates Netzwerk (VPN)'),
-- Q81 (SMTP)
(321, 81, 'A', 'HTTP'),
(322, 81, 'B', 'FTP'),
(323, 81, 'C', 'SMTP'),
(324, 81, 'D', 'DNS');

-- MEDIUM Level (9 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(21, 4, 'MEDIUM', 'Was ist NAT (Network Address Translation)?', 'A', 1),
(22, 4, 'MEDIUM', 'Welches OSI-Modell-Layer arbeitet mit IP-Adressen?', 'B', 1),
(82, 4, 'MEDIUM', 'Welche Aufgabe hat das ARP-Protokoll?', 'A', 1),
(83, 4, 'MEDIUM', 'Was unterscheidet TCP von UDP?', 'B', 1),
(84, 4, 'MEDIUM', 'Welche Subnetzmaske gehört zu einem /24-Netzwerk?', 'D', 1),
(85, 4, 'MEDIUM', 'Was beschreibt DHCP?', 'A', 1),
(86, 4, 'MEDIUM', 'Auf welcher OSI-Schicht arbeitet ein Switch?', 'C', 1),
(87, 4, 'MEDIUM', 'Was ist die Funktion von ICMP?', 'B', 1),
(88, 4, 'MEDIUM', 'Was ist der Unterschied zwischen einem Hub und einem Switch?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q21 (NAT)
(81, 21, 'A', 'Übersetzt private IPs zu einer öffentlichen IP (und Ports) - ermöglicht mehreren Geräten einen Internetzugang'),
(82, 21, 'B', 'Verschlüsselt Datenpakete auf Layer 2 automatisch'),
(83, 21, 'C', 'Synchronisiert Uhren im Netzwerk'),
(84, 21, 'D', 'Verhindert Paketverlust durch automatische Retransmission'),
-- Q22 (OSI Layer 3)
(85, 22, 'A', 'Layer 2 (Data Link)'),
(86, 22, 'B', 'Layer 3 (Network)'),
(87, 22, 'C', 'Layer 4 (Transport)'),
(88, 22, 'D', 'Layer 7 (Application)'),
-- Q82 (ARP)
(325, 82, 'A', 'Es ordnet IP-Adressen den zugehörigen MAC-Adressen zu'),
(326, 82, 'B', 'Es verschlüsselt Datenpakete zwischen zwei Hosts'),
(327, 82, 'C', 'Es steuert den Datenfluss auf der Transportschicht'),
(328, 82, 'D', 'Es verwaltet DNS-Einträge im lokalen Netzwerk'),
-- Q83 (TCP vs UDP)
(329, 83, 'A', 'TCP ist schneller, da es keine Header verwendet'),
(330, 83, 'B', 'TCP bietet eine zuverlässige, verbindungsorientierte Übertragung; UDP ist verbindungslos und schneller'),
(331, 83, 'C', 'UDP garantiert die Reihenfolge der Pakete, TCP nicht'),
(332, 83, 'D', 'TCP wird nur für E-Mail verwendet, UDP nur für Webseiten'),
-- Q84 (Subnetzmaske /24)
(333, 84, 'A', '255.0.0.0'),
(334, 84, 'B', '255.255.0.0'),
(335, 84, 'C', '255.255.255.128'),
(336, 84, 'D', '255.255.255.0'),
-- Q85 (DHCP)
(337, 85, 'A', 'Ein Protokoll, das IP-Adressen und Netzwerkkonfiguration automatisch an Geräte verteilt'),
(338, 85, 'B', 'Ein Verfahren zur Verschlüsselung von DNS-Anfragen'),
(339, 85, 'C', 'Ein Routing-Algorithmus für große Netzwerke'),
(340, 85, 'D', 'Ein Standard zur Fehlerbehebung in TCP-Verbindungen'),
-- Q86 (Switch OSI-Schicht)
(341, 86, 'A', 'Schicht 1 (Physical)'),
(342, 86, 'B', 'Schicht 3 (Network)'),
(343, 86, 'C', 'Schicht 2 (Data Link)'),
(344, 86, 'D', 'Schicht 4 (Transport)'),
-- Q87 (ICMP)
(345, 87, 'A', 'Verschlüsselung von Netzwerkpaketen auf Layer 3'),
(346, 87, 'B', 'Senden von Fehlermeldungen und Diagnoseinformationen im Netzwerk (z. B. Ping)'),
(347, 87, 'C', 'Automatische Vergabe von IP-Adressen an Clients'),
(348, 87, 'D', 'Auflösung von Hostnamen in IP-Adressen'),
-- Q88 (Hub vs Switch)
(349, 88, 'A', 'Ein Hub arbeitet auf Layer 3, ein Switch auf Layer 1'),
(350, 88, 'B', 'Ein Hub kann verschlüsseln, ein Switch nicht'),
(351, 88, 'C', 'Ein Switch sendet Daten an alle Ports, ein Hub nur an den Zielport'),
(352, 88, 'D', 'Ein Hub sendet Daten an alle Ports, ein Switch nur an den Zielport');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(23, 4, 'HARD', 'Wozu dient TLS/SSL primär?', 'D', 1),
(24, 4, 'HARD', 'Was beschreibt das TCP-Handshake-Verfahren (3-Way Handshake)?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q23 (TLS/SSL)
(89, 23, 'A', 'Komprimierung von Bildern in HTTP'),
(90, 23, 'B', 'Routing über mehrere Hops in großen Netzwerken'),
(91, 23, 'C', 'Load Balancing auf Layer 4 (Transport)'),
(92, 23, 'D', 'Verschlüsselung und Integrität bei der Verbindung'),
-- Q24 (TCP Handshake)
(93, 24, 'A', 'Ein Verfahren zum Komprimieren von TCP-Paketen'),
(94, 24, 'B', 'Das Beenden einer TCP-Verbindung nach Datenaustausch'),
(95, 24, 'C', 'Ein Verfahren zum Aufbau einer zuverlässigen Verbindung: SYN, SYN-ACK, ACK'),
(96, 24, 'D', 'Ein DNS-Auflösungsalgorithmus');

-- Verify insertion
SELECT COUNT(*) as network_questions FROM questions WHERE category_id = 4;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 4 GROUP BY difficulty;
