-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 02-seed-questions-prog.sql
-- Programmierung (14 Fragen: 3x EASY, 5x MEDIUM, 6x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 1: PROGRAMMIERUNG (ID 1)
-- ============================================

-- EASY Level (3 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(1, 1, 'EASY', 'Wofür steht die Abkürzung IDE?', 'B', 1),
(2, 1, 'EASY', 'Welche der folgenden ist eine Programmiersprache?', 'C', 1),
(61, 1, 'EASY', 'Was ist eine Variable in der Programmierung?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q1 (IDE)
(1, 1, 'A', 'Internet Data Exchange'),
(2, 1, 'B', 'Integrated Development Environment'),
(3, 1, 'C', 'Internal Debug Engine'),
(4, 1, 'D', 'Interface Description Editor'),
-- Q2 (Programmiersprache)
(5, 2, 'A', 'HTML'),
(6, 2, 'B', 'CSS'),
(7, 2, 'C', 'Python'),
(8, 2, 'D', 'JSON'),
-- Q61 (Variable)
(241, 61, 'A', 'Ein Befehl zum Starten eines Programms'),
(242, 61, 'B', 'Ein benannter Speicherplatz, der einen Wert enthält'),
(243, 61, 'C', 'Eine spezielle Hardwarekomponente'),
(244, 61, 'D', 'Ein Algorithmus zur Datenverschlüsselung');

-- MEDIUM Level (5 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(3, 1, 'MEDIUM', 'Was macht der git-Befehl „git commit"?', 'C', 1),
(4, 1, 'MEDIUM', 'Welches Paradigma beschreibt die objektorientierte Programmierung (OOP)?', 'A', 1),
(62, 1, 'MEDIUM', 'Was versteht man unter Rekursion in der Programmierung?', 'A', 1),
(63, 1, 'MEDIUM', 'Welche Datenstruktur arbeitet nach dem LIFO-Prinzip (Last In, First Out)?', 'C', 1),
(64, 1, 'MEDIUM', 'Was bewirkt das Schlüsselwort „static" bei einer Methode in Java?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q3 (git commit)
(9, 3, 'A', 'Erstellt automatisch einen Merge Request'),
(10, 3, 'B', 'Löscht nicht getrackte Dateien'),
(11, 3, 'C', 'Speichert Änderungen als Snapshot in der Repository-Historie'),
(12, 3, 'D', 'Schreibt Änderungen direkt ins Remote-Repository'),
-- Q4 (OOP)
(13, 4, 'A', 'Ein Konzept, das Daten und Funktionen in „Objekten" kapselt'),
(14, 4, 'B', 'Ein funktionales Paradigma, das nur reine Funktionen nutzt'),
(15, 4, 'C', 'Eine Methode zur manuellen Speicherverwaltung'),
(16, 4, 'D', 'Ein Debugging-Verfahren für schnellere Kompilierung'),
-- Q62 (Rekursion)
(245, 62, 'A', 'Eine Funktion, die sich selbst aufruft'),
(246, 62, 'B', 'Eine Schleife, die rückwärts zählt'),
(247, 62, 'C', 'Ein Verfahren zur Sortierung von Arrays'),
(248, 62, 'D', 'Eine Methode zur Fehlerbehandlung'),
-- Q63 (LIFO / Stack)
(249, 63, 'A', 'Queue'),
(250, 63, 'B', 'Linked List'),
(251, 63, 'C', 'Stack'),
(252, 63, 'D', 'HashMap'),
-- Q64 (static in Java)
(253, 64, 'A', 'Die Methode kann nur einmal aufgerufen werden'),
(254, 64, 'B', 'Die Methode gehört zur Klasse und nicht zu einer Instanz'),
(255, 64, 'C', 'Die Methode wird automatisch asynchron ausgeführt'),
(256, 64, 'D', 'Die Methode ist nur innerhalb der Klasse sichtbar');

-- HARD Level (6 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(5, 1, 'HARD', 'Was beschreibt die Big-O-Notation primär?', 'A', 1),
(6, 1, 'HARD', 'Welches Principle beschreibt SOLID in der Softwareentwicklung richtig?', 'B', 1),
(65, 1, 'HARD', 'Welche Zeitkomplexität hat der Merge-Sort-Algorithmus im Worst Case?', 'B', 1),
(66, 1, 'HARD', 'Was versteht man unter dem Konzept der Closures in der Programmierung?', 'C', 1),
(67, 1, 'HARD', 'Was beschreibt das Konzept der Polymorphie in der objektorientierten Programmierung?', 'B', 1),
(68, 1, 'HARD', 'Welches Entwurfsmuster (Design Pattern) stellt sicher, dass eine Klasse nur eine einzige Instanz hat?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q5 (Big-O)
(17, 5, 'A', 'Die asymptotische Wachstumsrate des Aufwands in Abhängigkeit von der Eingabegröße n'),
(18, 5, 'B', 'Die exakte Laufzeit eines Algorithmus in Millisekunden'),
(19, 5, 'C', 'Den durchschnittlichen CPU-Takt eines Systems'),
(20, 5, 'D', 'Die Anzahl der möglichen Bugs im Code'),
-- Q6 (SOLID)
(21, 6, 'A', 'Nur Single Inheritance nutzen'),
(22, 6, 'B', 'Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, Dependency Inversion'),
(23, 6, 'C', 'Strukturelles Logging und Objekt Debugging'),
(24, 6, 'D', 'Statische Variable und Override-Limitierungen'),
-- Q65 (Merge-Sort Zeitkomplexität)
(257, 65, 'A', 'O(n)'),
(258, 65, 'B', 'O(n log n)'),
(259, 65, 'C', 'O(n²)'),
(260, 65, 'D', 'O(log n)'),
-- Q66 (Closures)
(261, 66, 'A', 'Ein Mechanismus zum Schließen offener Datenbankverbindungen'),
(262, 66, 'B', 'Eine Technik zum Komprimieren von Quellcode'),
(263, 66, 'C', 'Eine Funktion, die auf Variablen aus ihrem umgebenden Gültigkeitsbereich zugreift, auch wenn dieser nicht mehr aktiv ist'),
(264, 66, 'D', 'Ein Design-Pattern zur Entkopplung von Modulen'),
-- Q67 (Polymorphie)
(265, 67, 'A', 'Das Verbergen interner Implementierungsdetails vor dem Benutzer'),
(266, 67, 'B', 'Die Fähigkeit, dass Objekte unterschiedlicher Klassen auf dieselbe Nachricht unterschiedlich reagieren'),
(267, 67, 'C', 'Das Erstellen neuer Klassen durch Vererbung bestehender Klassen'),
(268, 67, 'D', 'Die Festlegung von Zugriffsrechten auf Klassenattribute'),
-- Q68 (Singleton Pattern)
(269, 68, 'A', 'Observer Pattern'),
(270, 68, 'B', 'Factory Pattern'),
(271, 68, 'C', 'Strategy Pattern'),
(272, 68, 'D', 'Singleton Pattern');

-- Verify insertion
SELECT COUNT(*) as prog_questions FROM questions WHERE category_id = 1;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 1 GROUP BY difficulty;
