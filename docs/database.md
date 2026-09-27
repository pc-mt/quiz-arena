# Datenbank

Schema und Rolle der Tabellen (MariaDB). Vollständige Init-Skripte: `mariadb/mariadb_init/`.

---

## Tabellen

### `users` — Benutzerkonten

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | Auto-Increment |
| `username` | `VARCHAR(64)` UNIQUE NOT NULL | Login-Name |
| `password_hash` | `VARCHAR(255)` NOT NULL | BCrypt-Hash |
| `display_name` | `VARCHAR(80)` | Anzeigename (optional) |
| `profile_photo_url` | `VARCHAR(255)` | Avatar-URL (optional) |
| `rfid_uid` | `VARCHAR(64)` UNIQUE | RFID-UID (optional) |
| `created_at` / `updated_at` | `TIMESTAMP` | Automatisch gesetzt |

---

### `categories` — Fragekategorien

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | Auto-Increment |
| `name` | `VARCHAR(64)` UNIQUE NOT NULL | z. B. „Programmierung" |
| `description` | `VARCHAR(255)` | optional |

---

### `questions` — Fragenpool

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | Auto-Increment |
| `category_id` | `BIGINT UNSIGNED` FK → `categories` | |
| `difficulty` | `ENUM('EASY','MEDIUM','HARD')` NOT NULL | |
| `question_text` | `TEXT` NOT NULL | Fragetext |
| `correct_option` | `ENUM('A','B','C','D')` NOT NULL | Richtige Antwort |
| `is_active` | `TINYINT(1)` DEFAULT 1 | Ob die Frage verwendbar ist |

---

### `question_options` — Antwortoptionen pro Frage

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | Auto-Increment |
| `question_id` | `BIGINT UNSIGNED` FK → `questions` | CASCADE DELETE |
| `option_letter` | `ENUM('A','B','C','D')` NOT NULL | |
| `option_text` | `VARCHAR(255)` NOT NULL | Anzeigetext der Option |

---

### `scoring_time_buckets` — Zeitbuckets für Punktefaktor

Bestimmt den Multiplikator anhand der Antwortzeit. Je schneller, desto höher der Faktor.

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `TINYINT UNSIGNED` PK | |
| `bucket_code` | `ENUM(...)` UNIQUE | z. B. `T0_5`, `T5_10`, … `T30P` |
| `min_ms` | `INT UNSIGNED` NOT NULL | Untergrenze in ms |
| `max_ms` | `INT UNSIGNED` NULL | Obergrenze (NULL = offen) |
| `factor` | `DECIMAL(4,2)` NOT NULL | Multiplikator, z. B. `1.00`, `0.90` |

**Vordefinierte Werte:**

| Bucket | Zeitbereich | Faktor |
|--------|-------------|--------|
| T0_5 | 0 – 5 s | 1.00 |
| T5_10 | 5 – 10 s | 0.90 |
| T10_15 | 10 – 15 s | 0.80 |
| T15_20 | 15 – 20 s | 0.70 |
| T20_25 | 20 – 25 s | 0.60 |
| T25_30 | 25 – 30 s | 0.50 |
| T30P | > 30 s | 0.00 |

---

### `controllers` — Registrierte Controller

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | |
| `controller_id` | `VARCHAR(128)` UNIQUE NOT NULL | Eindeutige ID (z. B. `WEB-ABC123`) |
| `controller_type` | `ENUM('WEB','HARDWARE')` NOT NULL | |
| `status` | `ENUM('FREE','ASSIGNED','OFFLINE')` | Aktueller Status |
| `last_seen_at` | `TIMESTAMP` NULL | Letzter Pong-Zeitpunkt |
| `assigned_user_id` | `BIGINT UNSIGNED` FK → `users` | NULL wenn frei |

---

### `game_sessions` — Spielsitzungen

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | |
| `host_user_id` | `BIGINT UNSIGNED` FK → `users` | Host (darf starten) |
| `round_length` | `ENUM('Q5','Q10','Q20')` NOT NULL | Anzahl Fragen |
| `allow_easy/medium/hard` | `TINYINT(1)` | Aktivierte Schwierigkeiten |
| `state` | `ENUM('LOBBY','COUNTDOWN','QUESTION','EVALUATION','RESULTS','ENDED','ABORTED')` | Aktueller Zustand |
| `started_at` / `ended_at` | `TIMESTAMP` NULL | |

---

### `game_session_categories` — Kategorien einer Session

Verbindungstabelle zwischen `game_sessions` und `categories` (n:m).

---

### `game_session_players` — Spieler in einer Session

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `game_session_id` | `BIGINT UNSIGNED` FK → `game_sessions` | PK (zusammengesetzt) |
| `user_id` | `BIGINT UNSIGNED` FK → `users` | PK (zusammengesetzt) |
| `controller_id` | `BIGINT UNSIGNED` FK → `controllers` | NULL wenn kein Controller |
| `is_ready` | `TINYINT(1)` DEFAULT 0 | Ready-Status |
| `joined_at` | `TIMESTAMP` | |

---

### `game_session_questions` — Fragen einer Runde

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `game_session_id` | FK → `game_sessions` | PK |
| `question_index` | `INT UNSIGNED` | Reihenfolge (1..round_length), PK |
| `question_id` | FK → `questions` | Die gewählte Frage |
| `asked_at` | `TIMESTAMP` NULL | Zeitpunkt der Ausgabe |

---

### `game_answers` — Eingereichte Antworten

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | |
| `game_session_id` | FK → `game_sessions` | |
| `user_id` | FK → `users` | |
| `question_id` | FK → `questions` | |
| `answered_option` | `ENUM('A','B','C','D')` NULL | NULL bei Timeout (keine Antwort) |
| `response_time_ms` | `INT UNSIGNED` NULL | Zeit zwischen Frage-Start und Antwort |
| `time_bucket` | `ENUM(...)` NULL | Berechneter Bucket |
| `is_correct` | `TINYINT(1)` DEFAULT 0 | |
| `points_awarded` | `DECIMAL(6,2)` DEFAULT 0.00 | Vergebene Punkte |
| UNIQUE | `(session, user, question)` | Pro Spieler nur eine Antwort je Frage |

---

### `game_session_results` — Endergebnis pro Spieler

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `game_session_id` | FK → `game_sessions` | PK |
| `user_id` | FK → `users` | PK |
| `total_points` | `DECIMAL(10,2)` | Gesamtpunkte |
| `total_response_time_ms` | `BIGINT UNSIGNED` NULL | Summe der Antwortzeiten (Tie-Breaker) |
| `correct_count` | `INT UNSIGNED` | Richtige Antworten |
| `answered_count` | `INT UNSIGNED` | Beantwortete Fragen |

---

### `highscores` — Bestenlisten

| Spalte | Typ | Beschreibung |
|--------|-----|--------------|
| `id` | `BIGINT UNSIGNED` PK | |
| `round_length` | `ENUM('Q5','Q10','Q20')` | Spielmodus |
| `user_id` | FK → `users` | |
| `game_session_id` | FK → `game_sessions` NULL | Verknüpfte Session |
| `total_points` | `DECIMAL(10,2)` | |
| `total_response_time_ms` | `BIGINT UNSIGNED` NULL | Tie-Breaker |

Ranking: `total_points DESC`, dann `total_response_time_ms ASC`. Bots erscheinen nicht in den Highscores.

---

## Beziehungen (Kurzfassung)

```
users ──────────┬──► game_session_players ◄─── game_sessions
                │               │
                │               └──► controllers
                │
                └──► game_answers ◄─── questions ◄─── categories
                                            │
                                      question_options

game_sessions ──► game_session_questions ──► questions
game_sessions ──► game_session_results ──► users
game_sessions ──► highscores ──► users
```
