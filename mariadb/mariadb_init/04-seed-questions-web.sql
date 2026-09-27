-- ====================================================
-- GEN1002 Informatik-Projekt - WiSe25/26 - Czekansky
-- 04-seed-questions-web.sql
-- Web-Technologien (8 Fragen: 5x EASY, 1x MEDIUM, 2x HARD)
-- ============================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- ============================================
-- CATEGORY 3: WEB-TECHNOLOGIEN (ID 3)
-- ============================================

-- EASY Level (5 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(13, 3, 'EASY', 'Wofür steht HTTP?', 'A', 1),
(14, 3, 'EASY', 'Welcher HTTP-Status-Code signalisiert einen erfolgreichen Request?', 'B', 1),
(75, 3, 'EASY', 'Was ist HTML?', 'A', 1),
(76, 3, 'EASY', 'Was ist die Aufgabe von CSS in der Webentwicklung?', 'B', 1),
(77, 3, 'EASY', 'Wofür steht die Abkürzung URL?', 'C', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q13 (HTTP)
(49, 13, 'A', 'Hypertext Transfer Protocol'),
(50, 13, 'B', 'High Throughput Transfer Process'),
(51, 13, 'C', 'Host Transfer Protocol'),
(52, 13, 'D', 'Hyperlink Transmission Package'),
-- Q14 (Status 200)
(53, 14, 'A', '301 Moved Permanently'),
(54, 14, 'B', '200 OK'),
(55, 14, 'C', '404 Not Found'),
(56, 14, 'D', '500 Internal Server Error'),
-- Q75 (HTML)
(297, 75, 'A', 'Eine Auszeichnungssprache zur Strukturierung von Webseiten'),
(298, 75, 'B', 'Eine Programmiersprache zur Serverkommunikation'),
(299, 75, 'C', 'Ein Datenbank-Verwaltungssystem'),
(300, 75, 'D', 'Ein Netzwerkprotokoll für E-Mail-Versand'),
-- Q76 (CSS)
(301, 76, 'A', 'Datenbankabfragen auf dem Server auszuführen'),
(302, 76, 'B', 'Das Aussehen und Layout von Webseiten zu gestalten'),
(303, 76, 'C', 'Formulardaten verschlüsselt zu übertragen'),
(304, 76, 'D', 'JavaScript-Code im Browser zu kompilieren'),
-- Q77 (URL)
(305, 77, 'A', 'Universal Resource Language'),
(306, 77, 'B', 'Unified Request Locator'),
(307, 77, 'C', 'Uniform Resource Locator'),
(308, 77, 'D', 'User Response Link');

-- MEDIUM Level (1 Frage)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(15, 3, 'MEDIUM', 'Wofür wird CORS (Cross-Origin Resource Sharing) primär benötigt?', 'D', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q15 (CORS)
(57, 15, 'A', 'Um HTML schneller im Browser zu rendern'),
(58, 15, 'B', 'Um Cookies automatisch zu deaktivieren'),
(59, 15, 'C', 'Um SQL-Injection auf dem Server zu verhindern'),
(60, 15, 'D', 'Um Cross-Origin Requests kontrolliert zu erlauben/zu verbieten');

-- HARD Level (2 Fragen)
INSERT IGNORE INTO questions (id, category_id, difficulty, question_text, correct_option, is_active)
VALUES
(17, 3, 'HARD', 'Was ist die Same-Origin-Policy im Browser?', 'B', 1),
(18, 3, 'HARD', 'Welcher HTTP-Header kontrolliert das Caching-Verhalten im Browser?', 'A', 1);

INSERT IGNORE INTO question_options (id, question_id, option_letter, option_text) VALUES
-- Q17 (Same-Origin)
(65, 17, 'A', 'Ein HTTP-Header zur Kompression von Ressourcen'),
(66, 17, 'B', 'Sicherheits-Regel des Browsers: Skripte dürfen nur auf Ressourcen mit gleicher Origin zugreifen'),
(67, 17, 'C', 'Ein Server-seitiger Datenbankindex'),
(68, 17, 'D', 'Ein MQTT-Topic-Präfix für IoT-Geräte'),
-- Q18 (Cache-Control)
(69, 18, 'A', 'Cache-Control'),
(70, 18, 'B', 'Content-Type'),
(71, 18, 'C', 'Authorization'),
(72, 18, 'D', 'Access-Control-Allow-Origin');

-- Verify insertion
SELECT COUNT(*) as web_questions FROM questions WHERE category_id = 3;
SELECT difficulty, COUNT(*) as count FROM questions WHERE category_id = 3 GROUP BY difficulty;
