-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 06-seed-questions-os.sql
-- Betriebssysteme (15 Fragen: 5x EASY, 6x MEDIUM, 4x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 5: BETRIEBSSYSTEME (ID 5)
-- ============================================

-- EASY Level (5 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(25, 5, 'EASY', 'Was ist ein Prozess in einem Betriebssystem?', 'B', 1),
(26, 5, 'EASY', 'Welcher der folgenden ist ein modernes Desktop-Betriebssystem?', 'D', 1),
(89, 5, 'EASY', 'Was ist der Kernel eines Betriebssystems?', 'A', 1),
(90, 5, 'EASY', 'Was ist ein Dateisystem in einem Betriebssystem?', 'B', 1),
(91, 5, 'EASY', 'Was versteht man unter Multitasking?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q25 (Prozess)
(97, 25, 'A', 'Eine Datei im Dateisystem'),
(98, 25, 'B', 'Ein Programm in Ausführung mit eigenem Adressraum und Ressourcen'),
(99, 25, 'C', 'Ein Netzwerkpaket'),
(100, 25, 'D', 'Ein CPU-Register'),
-- Q26 (OS)
(101, 26, 'A', 'MS-DOS'),
(102, 26, 'B', 'Windows 3.1'),
(103, 26, 'C', 'AmigaOS'),
(104, 26, 'D', 'Linux oder Windows 10/11'),
-- Q89 (Kernel)
(353, 89, 'A', 'Der zentrale Teil des OS, der Hardware und Software verwaltet'),
(354, 89, 'B', 'Ein Grafikprozessor für Bildschirmausgabe'),
(355, 89, 'C', 'Ein spezielles Dateisystem für Linux'),
(356, 89, 'D', 'Ein Programm zum Kompilieren von Quellcode'),
-- Q90 (Dateisystem)
(357, 90, 'A', 'Ein Programm zur Textverarbeitung'),
(358, 90, 'B', 'Eine Struktur zur Organisation und Speicherung von Daten auf Datenträgern'),
(359, 90, 'C', 'Ein Netzwerkprotokoll für Datentransfer'),
(360, 90, 'D', 'Ein Register im Prozessor'),
-- Q91 (Multitasking)
(361, 91, 'A', 'Das gleichzeitige Nutzen mehrerer Bildschirme'),
(362, 91, 'B', 'Das Kopieren mehrerer Dateien gleichzeitig'),
(363, 91, 'C', 'Die Verwendung mehrerer Tastaturen'),
(364, 91, 'D', 'Die gleichzeitige bzw. quasi-gleichzeitige Ausführung mehrerer Prozesse');

-- MEDIUM Level (6 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(27, 5, 'MEDIUM', 'Wofür wird ein Mutex (Mutual Exclusion Lock) typischerweise verwendet?', 'A', 1),
(28, 5, 'MEDIUM', 'Was ist Scheduling im Kontext eines Betriebssystems?', 'C', 1),
(92, 5, 'MEDIUM', 'Was ist ein Kontextwechsel (Context Switch)?', 'A', 1),
(93, 5, 'MEDIUM', 'Was ist der Unterschied zwischen einem Prozess und einem Thread?', 'C', 1),
(94, 5, 'MEDIUM', 'Welches Scheduling-Verfahren teilt jedem Prozess ein festes Zeitquantum zu?', 'B', 1),
(95, 5, 'MEDIUM', 'Was versteht man unter Paging in der Speicherverwaltung?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q27 (Mutex)
(105, 27, 'A', 'Schutz kritischer Abschnitte (gegenseitiger Ausschluss zwischen Threads)'),
(106, 27, 'B', 'Schnelleres Kompilieren von Programmen'),
(107, 27, 'C', 'DNS-Auflösung von Domainnamen'),
(108, 27, 'D', 'Komprimierung von Dateien'),
-- Q28 (Scheduling)
(109, 28, 'A', 'Das Ändern von Datei-Ownerships'),
(110, 28, 'B', 'Das Erstellen von Backups'),
(111, 28, 'C', 'Die Verwaltung und Vergabe von CPU-Zeit an Prozesse/Threads'),
(112, 28, 'D', 'Die Synchronisation von Netzwerk-Uhren'),
-- Q92 (Context Switch)
(365, 92, 'A', 'Das Sichern und Wiederherstellen des Zustands eines Prozesses/Threads bei CPU-Wechsel'),
(366, 92, 'B', 'Das Wechseln zwischen verschiedenen Betriebssystemen'),
(367, 92, 'C', 'Das Ändern der Bildschirmauflösung'),
(368, 92, 'D', 'Das Umschalten zwischen verschiedenen Netzwerken'),
-- Q93 (Prozess vs. Thread)
(369, 93, 'A', 'Es gibt keinen Unterschied, beide Begriffe sind synonym'),
(370, 93, 'B', 'Ein Prozess kann nur einen Thread haben'),
(371, 93, 'C', 'Ein Thread ist eine leichtgewichtige Ausführungseinheit innerhalb eines Prozesses und teilt dessen Adressraum'),
(372, 93, 'D', 'Threads können nur auf Multi-Core-Systemen ausgeführt werden'),
-- Q94 (Round Robin)
(373, 94, 'A', 'First-Come-First-Served (FCFS)'),
(374, 94, 'B', 'Round-Robin-Scheduling'),
(375, 94, 'C', 'Shortest-Job-First (SJF)'),
(376, 94, 'D', 'Priority Scheduling'),
-- Q95 (Paging)
(377, 95, 'A', 'Das Sortieren von Dateien nach Größe'),
(378, 95, 'B', 'Das Komprimieren von Speicherinhalten'),
(379, 95, 'C', 'Das Verschlüsseln von Speicherbereichen'),
(380, 95, 'D', 'Die Aufteilung des virtuellen Speichers in gleich große Seiten, die auf Seitenrahmen im physischen Speicher abgebildet werden');

-- HARD Level (4 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(29, 5, 'HARD', 'Was ist ein Deadlock in der Prozess-Synchronisation?', 'C', 1),
(30, 5, 'HARD', 'Was ist die virtuelle Speicherverwaltung?', 'B', 1),
(96, 5, 'HARD', 'Welche der folgenden Bedingungen gehört NICHT zu den vier Coffman-Bedingungen für Deadlocks?', 'A', 1),
(97, 5, 'HARD', 'Was beschreibt das Konzept des Seitenfehlers (Page Fault) in der virtuellen Speicherverwaltung?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q29 (Deadlock)
(113, 29, 'A', 'Ein Speicherleck, das zu RAM-Erschöpfung führt'),
(114, 29, 'B', 'Ein schneller Kontextwechsel zwischen Prozessen'),
(115, 29, 'C', 'Wechselseitiges Warten blockierter Prozesse/Threads auf Ressourcen - ein Stillstand'),
(116, 29, 'D', 'Ein Timeout bei der Ping-Kommunikation'),
-- Q30 (Virtual Memory)
(117, 30, 'A', 'Auslagerung von Prozessen auf andere Rechner'),
(118, 30, 'B', 'Die Abstraktion des physischen Speichers durch Paging/Segmentierung - ermöglicht größere Adressräume'),
(119, 30, 'C', 'Das Verschlüsseln des RAM-Inhalts'),
(120, 30, 'D', 'Ein Datenbankfeature zur Speicheroptimierung'),
-- Q96 (Coffman-Bedingungen)
(381, 96, 'A', 'Preemption (Entzug von Ressourcen ist möglich)'),
(382, 96, 'B', 'Mutual Exclusion (gegenseitiger Ausschluss)'),
(383, 96, 'C', 'Hold and Wait (Halten und Warten)'),
(384, 96, 'D', 'Circular Wait (zirkuläres Warten)'),
-- Q97 (Page Fault)
(385, 97, 'A', 'Ein Fehler beim Kompilieren eines Programms'),
(386, 97, 'B', 'Ein Hardwaredefekt im RAM-Modul'),
(387, 97, 'C', 'Ein Zugriff auf eine Seite, die sich nicht im physischen Speicher befindet und erst vom Sekundärspeicher geladen werden muss'),
(388, 97, 'D', 'Ein Syntaxfehler in der Konfiguration des Dateisystems');

-- Verify insertion
SELECT COUNT(*) as os_questions FROM questions WHERE category_id = 5;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 5 GROUP BY difficulty;
