-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 03-seed-questions-db.sql
-- Datenbanken (12 Fragen: 2x EASY, 7x MEDIUM, 3x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 2: DATENBANKEN (ID 2)
-- ============================================

-- EASY Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(7, 2, 'EASY', 'Wofür steht SQL?', 'D', 1),
(8, 2, 'EASY', 'Welcher SQL-Befehl wird verwendet, um Daten aus einer Tabelle abzurufen?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q7 (SQL)
(25, 7, 'A', 'Secure Query Language'),
(26, 7, 'B', 'Standard Query Link'),
(27, 7, 'C', 'System Question List'),
(28, 7, 'D', 'Structured Query Language'),
-- Q8 (SELECT)
(29, 8, 'A', 'SELECT'),
(30, 8, 'B', 'FETCH'),
(31, 8, 'C', 'RETRIEVE'),
(32, 8, 'D', 'QUERY');

-- MEDIUM Level (7 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(9, 2, 'MEDIUM', 'Was ist ein Foreign Key (FK) in einer Datenbank?', 'B', 1),
(10, 2, 'MEDIUM', 'Welche Normalform beschreibt das Entfernen von Redundanzen innerhalb von Attributen?', 'C', 1),
(69, 2, 'MEDIUM', 'Was bewirkt der SQL-Befehl JOIN?', 'B', 1),
(70, 2, 'MEDIUM', 'Was ist der Unterschied zwischen DELETE und TRUNCATE in SQL?', 'C', 1),
(71, 2, 'MEDIUM', 'Was beschreibt eine Transaktion in einer Datenbank?', 'B', 1),
(72, 2, 'MEDIUM', 'Welche Eigenschaft gehört NICHT zum ACID-Prinzip?', 'D', 1),
(73, 2, 'MEDIUM', 'Was ist ein Index in einer Datenbank?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q9 (Foreign Key)
(33, 9, 'A', 'Ein Index auf einer Tabelle zur Beschleunigung von Abfragen'),
(34, 9, 'B', 'Ein Feld, das auf einen Datensatz (Primärschlüssel) in einer anderen Tabelle verweist'),
(35, 9, 'C', 'Ein SQL-Statement zum Löschen von Einträgen'),
(36, 9, 'D', 'Eine komplette Kopie einer Datenbank'),
-- Q10 (2NF)
(37, 10, 'A', 'Erste Normalform (1NF)'),
(38, 10, 'B', 'Zweite Normalform (2NF)'),
(39, 10, 'C', 'Dritte Normalform (3NF)'),
(40, 10, 'D', 'Boyce-Codd Normalform (BCNF)'),
-- Q69 (JOIN)
(273, 69, 'A', 'Er erstellt eine neue Tabelle'),
(274, 69, 'B', 'Er verknüpft Datensätze aus zwei oder mehr Tabellen'),
(275, 69, 'C', 'Er löscht doppelte Einträge'),
(276, 69, 'D', 'Er erstellt einen Index auf einer Spalte'),
-- Q70 (DELETE vs TRUNCATE)
(277, 70, 'A', 'DELETE entfernt die Tabelle, TRUNCATE nur die Daten'),
(278, 70, 'B', 'Es gibt keinen Unterschied'),
(279, 70, 'C', 'DELETE kann mit WHERE gefiltert werden, TRUNCATE löscht alle Zeilen ohne Bedingung'),
(280, 70, 'D', 'TRUNCATE ist langsamer als DELETE'),
-- Q71 (Transaktion)
(281, 71, 'A', 'Eine einzelne SQL-Abfrage'),
(282, 71, 'B', 'Eine logische Einheit von Operationen, die entweder vollständig oder gar nicht ausgeführt wird'),
(283, 71, 'C', 'Die automatische Sicherung der Datenbank'),
(284, 71, 'D', 'Ein Bericht über alle Datenbankänderungen'),
-- Q72 (ACID)
(285, 72, 'A', 'Atomicity (Atomarität)'),
(286, 72, 'B', 'Consistency (Konsistenz)'),
(287, 72, 'C', 'Durability (Dauerhaftigkeit)'),
(288, 72, 'D', 'Efficiency (Effizienz)'),
-- Q73 (Index)
(289, 73, 'A', 'Eine Kopie der gesamten Tabelle'),
(290, 73, 'B', 'Ein SQL-Befehl zur Datenmanipulation'),
(291, 73, 'C', 'Eine Datenstruktur, die den Zugriff auf Tabellenzeilen beschleunigt'),
(292, 73, 'D', 'Eine Methode zur Verschlüsselung von Daten');

-- HARD Level (3 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(11, 2, 'HARD', 'Welche Normalform eliminiert transitive Abhängigkeiten vollständig?', 'C', 1),
(12, 2, 'HARD', 'Was beschreibt das CAP-Theorem in verteilten Datenbanksystemen?', 'A', 1),
(74, 2, 'HARD', 'Was beschreibt eine Stored Procedure in einer Datenbank?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q11 (3NF)
(41, 11, 'A', '1NF - Atomic Values'),
(42, 11, 'B', '2NF - No Partial Dependencies'),
(43, 11, 'C', '3NF - No Transitive Dependencies'),
(44, 11, 'D', 'BCNF - Stricter than 3NF'),
-- Q12 (CAP)
(45, 12, 'A', 'Consistency, Availability, Partition Tolerance - man kann maximal 2 erfüllen'),
(46, 12, 'B', 'Cache, Archive, Persistence - drei Datenspeicherschichten'),
(47, 12, 'C', 'Concurrency, Access, Performance - Messkriterien für Datenbanken'),
(48, 12, 'D', 'Clustering, Aggregation, Pruning - Optimierungstechniken'),
-- Q74 (Stored Procedure)
(293, 74, 'A', 'Eine vorkompilierte Sammlung von SQL-Anweisungen, die auf dem Datenbankserver gespeichert und ausgeführt wird'),
(294, 74, 'B', 'Ein automatisches Backup der Datenbank'),
(295, 74, 'C', 'Eine Methode zur Indexierung von Tabellen'),
(296, 74, 'D', 'Ein Protokoll zur Übertragung von Daten zwischen Datenbanken');

-- Verify insertion
SELECT COUNT(*) as db_questions FROM questions WHERE category_id = 2;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 2 GROUP BY difficulty;
