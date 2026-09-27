-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 08-seed-questions-algo.sql
-- Algorithmen (13 Fragen: 6x EASY, 3x MEDIUM, 4x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 7: ALGORITHMEN (ID 7)
-- ============================================

-- EASY Level (6 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(37, 7, 'EASY', 'Welche Datenstruktur arbeitet nach dem FIFO-Prinzip (First In, First Out)?', 'C', 1),
(38, 7, 'EASY', 'Welche Datenstruktur arbeitet nach dem LIFO-Prinzip (Last In, First Out)?', 'A', 1),
(104, 7, 'EASY', 'Was ist ein Array?', 'B', 1),
(105, 7, 'EASY', 'Was ist die Zeitkomplexität einer linearen Suche im Worst Case?', 'C', 1),
(106, 7, 'EASY', 'Was ist ein Graph in der Informatik?', 'A', 1),
(107, 7, 'EASY', 'Was beschreibt Rekursion in der Programmierung?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q37 (FIFO)
(145, 37, 'A', 'Stack'),
(146, 37, 'B', 'Heap'),
(147, 37, 'C', 'Queue'),
(148, 37, 'D', 'Tree'),
-- Q38 (LIFO)
(149, 38, 'A', 'Stack'),
(150, 38, 'B', 'Queue'),
(151, 38, 'C', 'Linked List'),
(152, 38, 'D', 'Hash Table'),
-- Q104 (Array)
(413, 104, 'A', 'Ein Algorithmus zum Sortieren von Zahlen'),
(414, 104, 'B', 'Eine Datenstruktur, die Elemente gleichen Typs in zusammenhängendem Speicher mit Index-Zugriff speichert'),
(415, 104, 'C', 'Ein spezieller Baum mit genau zwei Kindern pro Knoten'),
(416, 104, 'D', 'Ein Netzwerkprotokoll zur Datenübertragung'),
-- Q105 (Lineare Suche)
(417, 105, 'A', 'O(1)'),
(418, 105, 'B', 'O(log n)'),
(419, 105, 'C', 'O(n)'),
(420, 105, 'D', 'O(n²)'),
-- Q106 (Graph)
(421, 106, 'A', 'Eine Datenstruktur bestehend aus Knoten (Vertices) und Kanten (Edges)'),
(422, 106, 'B', 'Ein Diagramm zur Darstellung von Aktienkursen'),
(423, 106, 'C', 'Ein Algorithmus zur Bilderkennung'),
(424, 106, 'D', 'Eine grafische Benutzeroberfläche'),
-- Q107 (Rekursion)
(425, 107, 'A', 'Eine Schleife, die rückwärts zählt'),
(426, 107, 'B', 'Das Kopieren von Daten in umgekehrter Reihenfolge'),
(427, 107, 'C', 'Das wiederholte Einlesen von Benutzereingaben'),
(428, 107, 'D', 'Eine Funktion, die sich selbst aufruft, um ein Problem in kleinere Teilprobleme zu zerlegen');

-- MEDIUM Level (3 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(39, 7, 'MEDIUM', 'Welche Laufzeit hat die binäre Suche im Worst Case?', 'A', 1),
(40, 7, 'MEDIUM', 'Welcher Sortier-Algorithmus hat im Durchschnitt O(n log n) Laufzeit?', 'B', 1),
(108, 7, 'MEDIUM', 'Welche Laufzeit hat der Zugriff auf ein Element in einer Hash-Tabelle im Durchschnitt?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q39 (Binary Search)
(153, 39, 'A', 'O(log n)'),
(154, 39, 'B', 'O(n)'),
(155, 39, 'C', 'O(n log n)'),
(156, 39, 'D', 'O(1)'),
-- Q40 (Sorting)
(157, 40, 'A', 'Bubble Sort'),
(158, 40, 'B', 'Merge Sort oder Quick Sort'),
(159, 40, 'C', 'Insertion Sort'),
(160, 40, 'D', 'Selection Sort'),
-- Q108 (Hash-Tabelle)
(429, 108, 'A', 'O(1)'),
(430, 108, 'B', 'O(log n)'),
(431, 108, 'C', 'O(n)'),
(432, 108, 'D', 'O(n log n)');

-- HARD Level (4 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(41, 7, 'HARD', 'Welche Voraussetzung muss erfüllt sein, damit der Dijkstra-Algorithmus funktioniert?', 'D', 1),
(42, 7, 'HARD', 'Welches Problem beschreibt das „Traveling Salesman Problem" (TSP)?', 'C', 1),
(109, 7, 'HARD', 'Was ist die Laufzeitkomplexität des Algorithmus von Kruskal zum Finden eines minimalen Spannbaums?', 'B', 1),
(110, 7, 'HARD', 'Was beschreibt das Konzept der dynamischen Programmierung?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q41 (Dijkstra)
(161, 41, 'A', 'Der Graph muss vollständig sein'),
(162, 41, 'B', 'Es dürfen nur negative Kanten existieren'),
(163, 41, 'C', 'Es dürfen nur ungerichtete Kanten existieren'),
(164, 41, 'D', 'Keine negativen Kantengewichte'),
-- Q42 (TSP)
(165, 42, 'A', 'Das Sortieren von Passwörtern'),
(166, 42, 'B', 'Die schnellste Route durch eine Datenbank'),
(167, 42, 'C', 'Die kürzeste Rundreise, die alle Orte genau einmal besucht'),
(168, 42, 'D', 'Die Komprimierung von Text-Dateien'),
-- Q109 (Kruskal)
(433, 109, 'A', 'O(V²), wobei V die Anzahl der Knoten ist'),
(434, 109, 'B', 'O(E log E), wobei E die Anzahl der Kanten ist'),
(435, 109, 'C', 'O(V + E)'),
(436, 109, 'D', 'O(n!)'),
-- Q110 (Dynamische Programmierung)
(437, 110, 'A', 'Die Kompilierung von Quellcode zur Laufzeit'),
(438, 110, 'B', 'Das automatische Anpassen von Algorithmen an verschiedene Hardware'),
(439, 110, 'C', 'Eine Methode, bei der überlappende Teilprobleme gelöst und deren Ergebnisse gespeichert werden, um Mehrfachberechnungen zu vermeiden'),
(440, 110, 'D', 'Die parallele Ausführung unabhängiger Programmteile auf mehreren Prozessoren');

-- Verify insertion
SELECT COUNT(*) as algo_questions FROM questions WHERE category_id = 7;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 7 GROUP BY difficulty;
