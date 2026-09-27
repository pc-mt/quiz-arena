-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 09-seed-questions-se.sql
-- Software Engineering (10 Fragen: 3x EASY, 4x MEDIUM, 3x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 8: SOFTWARE ENGINEERING (ID 8)
-- ============================================

-- EASY Level (3 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(43, 8, 'EASY', 'Was beschreibt eine User Story am besten?', 'B', 1),
(44, 8, 'EASY', 'Welche Methode wird häufig für iterative Entwicklung verwendet?', 'C', 1),
(111, 8, 'EASY', 'Was versteht man unter einem „Code Review"?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q43 (User Story)
(169, 43, 'A', 'Eine Datenbanktabelle'),
(170, 43, 'B', 'Eine kurze, User-zentrierte Anforderung in der Form „Als [Rolle] möchte ich [Aktion]"'),
(171, 43, 'C', 'Ein UML-Klassendiagramm'),
(172, 43, 'D', 'Ein Deployment-Skript'),
-- Q44 (Agile)
(173, 44, 'A', 'Waterfall Modell'),
(174, 44, 'B', 'Spiral Modell'),
(175, 44, 'C', 'Scrum oder Kanban'),
(176, 44, 'D', 'Big Bang Integration'),
-- Q111 (Code Review)
(441, 111, 'A', 'Eine automatisierte Code-Kompilierung'),
(442, 111, 'B', 'Die Überprüfung von Quellcode durch andere Entwickler vor dem Merge'),
(443, 111, 'C', 'Ein Performance-Test der Anwendung'),
(444, 111, 'D', 'Das Erstellen einer technischen Dokumentation');

-- MEDIUM Level (4 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(45, 8, 'MEDIUM', 'Was bedeutet Continuous Integration (CI)?', 'C', 1),
(46, 8, 'MEDIUM', 'Welcher der folgenden Begriffe gehört NICHT zu agilen Praktiken?', 'D', 1),
(112, 8, 'MEDIUM', 'Was beschreibt das SOLID-Prinzip „Single Responsibility"?', 'A', 1),
(113, 8, 'MEDIUM', 'Was ist der Hauptzweck von Unit Tests?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q45 (CI)
(177, 45, 'A', 'Manuelles Deployen auf Produktionsserver'),
(178, 45, 'B', 'Nur am Semesterende testen'),
(179, 45, 'C', 'Automatisches Bauen und Testen bei Code-Änderungen (z.B. bei jedem Push)'),
(180, 45, 'D', 'Nur Dokumentation generieren'),
-- Q46 (NOT Agile)
(181, 46, 'A', 'Sprint Planning'),
(182, 46, 'B', 'Daily Standup'),
(183, 46, 'C', 'User Stories'),
(184, 46, 'D', 'Warten mit vollständiger Anforderungsanalyse bis Projektstart'),
-- Q112 (Single Responsibility)
(445, 112, 'A', 'Eine Klasse soll genau eine Verantwortlichkeit haben und nur einen Grund zur Änderung'),
(446, 112, 'B', 'Jede Klasse muss exakt eine Methode besitzen'),
(447, 112, 'C', 'Es darf nur ein Entwickler an einer Klasse arbeiten'),
(448, 112, 'D', 'Jede Datei darf nur eine Klasse enthalten'),
-- Q113 (Unit Tests)
(449, 113, 'A', 'Die gesamte Anwendung end-to-end zu testen'),
(450, 113, 'B', 'Die Netzwerkverbindung zu überprüfen'),
(451, 113, 'C', 'Einzelne Komponenten oder Funktionen isoliert auf Korrektheit zu prüfen'),
(452, 113, 'D', 'Die Benutzeroberfläche visuell zu testen');

-- HARD Level (3 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(47, 8, 'HARD', 'Was ist „Trunk-based Development"?', 'A', 1),
(48, 8, 'HARD', 'Welcher Entwurfsmuster-Typ gehört zu den Behavioral Patterns?', 'B', 1),
(114, 8, 'HARD', 'Was besagt das Liskov Substitution Principle (LSP)?', 'B', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q47 (Trunk-based)
(185, 47, 'A', 'Arbeiten mit wenigen langlebigen Branches, häufige Integration in main - reduziert Merge-Konflikte'),
(186, 47, 'B', 'Arbeiten nur mit Tags statt Branches'),
(187, 47, 'C', 'Alles in einem Branch ohne Commits'),
(188, 47, 'D', 'Code wird nur per E-Mail ausgetauscht'),
-- Q48 (Design Patterns)
(189, 48, 'A', 'Singleton (Creational)'),
(190, 48, 'B', 'Observer oder Strategy (Behavioral)'),
(191, 48, 'C', 'Adapter (Structural)'),
(192, 48, 'D', 'Factory (Creational)'),
-- Q114 (LSP)
(453, 114, 'A', 'Subklassen dürfen nur private Methoden der Basisklasse überschreiben'),
(454, 114, 'B', 'Objekte einer Basisklasse müssen durch Objekte ihrer Subklassen ersetzbar sein, ohne die Korrektheit zu beeinträchtigen'),
(455, 114, 'C', 'Jede Klasse muss mindestens ein Interface implementieren'),
(456, 114, 'D', 'Abstrakte Klassen dürfen keine konkreten Methoden enthalten');

-- Verify insertion
SELECT COUNT(*) as se_questions FROM questions WHERE category_id = 8;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 8 GROUP BY difficulty;
