# HTTP-API

REST-Routen des Backends: Verhalten, Request-/Response-JSON, Fehler. Die Reihenfolge entspricht dem typischen Einstieg in die Anwendung (Auth → Controller → Lobby → Game → Highscores → Bot).

---

# Auth (Nutzer)

Routen für Registrierung, Login, RFID und Profil. Ein Nutzer muss sich registrieren, um am Spiel teilzunehmen. Der Login liefert ein JWT; ist der Account bereits in einer aktiven Spielsitzung, wird die Anfrage abgelehnt.

---

## Registrierung

Legt einen neuen Nutzer an. Nach der Registrierung kann sich der Nutzer per Login anmelden.

**HTTP-Verb:** `POST`  
**URL:** `/api/auth/register`

**Request (JSON):**
```json
{
  "username": "junior",
  "password": "secret123",
  "rfid_uid": "04:9C:64:D2:4B:80"
}
```
`rfid_uid` ist optional.

**Erfolgreiche Antwort:** `201 Created`
```json
{
  "id": 42,
  "username": "junior",
  "display_name": "junior",
  "created_at": "2026-02-20T12:34:56"
}
```

**Mögliche Fehler:**

`400 Bad Request` – Ungültige Eingabe (z. B. username/password leer, Passwort zu kurz, RFID-Format falsch)
```json
{ "error": "Username is required" }
```

`409 Conflict` – Username oder RFID bereits vergeben
```json
{ "error": "Username already exists" }
```

`500 Internal Server Error`
```json
{ "error": "Internal server error" }
```

---

## Login

Authentifiziert den Nutzer und gibt ein JWT zurück. Ist der Account bereits in einer aktiven Spielsitzung angemeldet, wird die Anfrage mit `409` abgelehnt; die laufende Session muss zuerst verlassen werden.

**HTTP-Verb:** `POST`  
**URL:** `/api/auth/login`

**Request (JSON):**
```json
{
  "username": "junior",
  "password": "secret123"
}
```

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "id": 42,
  "username": "junior",
  "display_name": "junior",
  "rfid_uid": "04:9C:64:D2:4B:80",
  "token": "JWT_TOKEN_HERE"
}
```

**Mögliche Fehler:**

`400 Bad Request` – username oder password fehlt
```json
{ "error": "Username is required" }
```

`401 Unauthorized` – Nutzer unbekannt oder falsches Passwort
```json
{ "error": "Invalid password" }
```

`409 Conflict` – Account bereits in einer aktiven Spielsitzung
```json
{ "error": "Dieser Account ist bereits in einer aktiven Spielsitzung angemeldet. Bitte die laufende Session zuerst verlassen, bevor du dich erneut einloggst." }
```

`500 Internal Server Error`
```json
{ "error": "Internal server error" }
```

---

## RFID hinzufügen oder aktualisieren

Verknüpft die angegebene RFID-UID mit dem eingeloggten Konto. Pro Konto ist maximal eine RFID-UID erlaubt; jede UID darf nur einem Konto zugeordnet sein.

**HTTP-Verb:** `PUT`  
**URL:** `/api/auth/rfid`  
**Header:** `Authorization: Bearer <JWT>`

**Request (JSON):**
```json
{ "rfid_uid": "04:9C:64:D2:4B:80" }
```

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "RFID successfully linked to account",
  "rfid_uid": "04:9C:64:D2:4B:80"
}
```

**Mögliche Fehler:** `400` (ungültiges RFID-Format), `401` (kein/ungültiger Token), `409` (RFID bereits anderem Konto zugeordnet), `500`.

---

## RFID löschen

Entfernt die mit dem Konto verknüpfte RFID-UID.

**HTTP-Verb:** `DELETE`  
**URL:** `/api/auth/rfid`  
**Header:** `Authorization: Bearer <JWT>`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{ "message": "RFID successfully unlinked from account" }
```

**Mögliche Fehler:** `401` (kein/ungültiger Token), `404` (kein RFID mit dem Konto verknüpft), `500`.

---

## Profil aktualisieren

Aktualisiert Anzeigename und/oder Profilbild-URL des eingeloggten Nutzers.

**HTTP-Verb:** `PUT`  
**URL:** `/api/auth/profile`  
**Header:** `Authorization: Bearer <JWT>`, `Content-Type: application/json`

**Request (JSON):**
```json
{
  "display_name": "Neuer Name",
  "profile_photo_url": "/avatars/42.jpg"
}
```

**Erfolgreiche Antwort:** `200 OK` (z. B. Bestätigung oder aktualisierte Nutzerdaten).

**Mögliche Fehler:** `401` (kein/ungültiger Token), `500`.

---

# Controller

Routen für die Registrierung und Abfrage von Controllern (Web, Hardware). Controller müssen registriert sein, bevor ein Spieler sie beim Lobby-Join auswählen kann.

---

## Controller registrieren

Erstellt einen neuen Controller oder gibt den bestehenden mit derselben `controller_id` zurück.

**HTTP-Verb:** `POST`  
**URL:** `/api/controllers/register`

**Request (JSON):**
```json
{
  "controller_id": "web-abc123",
  "controller_type": "WEB"
}
```
`controller_type` ist optional (Default: `WEB`). Erlaubt: `WEB`, `HARDWARE`.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "id": 7,
  "controller_id": "web-abc123",
  "controller_type": "WEB",
  "status": "FREE",
  "assigned_user_id": null
}
```

**Mögliche Fehler:** `400` (Controller-ID fehlt, zu lang oder ungültiger Typ), `500`.

---

## Verfügbare Controller abrufen

Liefert alle Controller (z. B. freie für die Auswahl beim Join).

**HTTP-Verb:** `GET`  
**URL:** `/api/controllers/available`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK` – Array von Objekten wie oben (id, controller_id, controller_type, status, assigned_user_id).

**Mögliche Fehler:** `500`.

---

# Lobby

Routen für Session, Spielerliste, Beitreten, Verlassen, Kick, Reset und Ready-Status. Die Session wird beim ersten Aufruf von „Session abrufen“ angelegt bzw. zurückgegeben.

---

## Session

Erzeugt eine aktive Session, falls noch keine existiert, oder gibt die bestehende zurück. Wird vom Frontend beim Einstieg aufgerufen.

**HTTP-Verb:** `GET`  
**URL:** `/api/lobby/session`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "id": 1,
  "state": "LOBBY",
  "round_length": "Q5"
}
```

**Mögliche Fehler:** `500`.

---

## Lobby-Status

Liefert die aktuelle Session-Info und alle Spieler in der Lobby (inkl. Ready-Status, Controller, Verbindungsstatus). Wird vom Frontend per Polling genutzt.

**HTTP-Verb:** `GET`  
**URL:** `/api/lobby/status`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "session": { "id": 1, "state": "LOBBY", "round_length": "Q5" },
  "players": [
    {
      "user_id": 42,
      "username": "junior",
      "display_name": "junior",
      "is_ready": false,
      "controller_id": "web-abc123",
      "controller_type": "WEB",
      "controller_status": "ASSIGNED"
    }
  ]
}
```

**Mögliche Fehler:** `500`.

---

## Session beitreten

Fügt den eingeloggten Nutzer mit dem angegebenen Controller zur aktiven Session hinzu. Der Controller darf noch keinem anderen Nutzer zugeordnet sein.

**HTTP-Verb:** `POST`  
**URL:** `/api/lobby/join`  
**Header:** `Authorization: Bearer <JWT>`

**Request (JSON):**
```json
{
  "controller_id": "web-abc123",
  "controller_type": "WEB"
}
```
`controller_type` optional (Default: `WEB`).

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "Joined session",
  "session_id": 1,
  "controller_id": "web-abc123",
  "controller_type": "WEB"
}
```

**Mögliche Fehler:** `400` (Controller-ID fehlt), `401` (kein/ungültiger Token), `409` (Controller bereits vergeben), `500`.

---

## Session verlassen

Entfernt den eingeloggten Nutzer aus der aktiven Session.

**HTTP-Verb:** `POST`  
**URL:** `/api/lobby/leave`  
**Header:** `Authorization: Bearer <JWT>`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{ "message": "Left session" }
```

**Mögliche Fehler:** `401`, `404` (keine aktive Session), `500`.

---

## Spieler kicken

Entfernt einen anderen Spieler aus der Session (z. B. durch Host). Erfordert Authorization.

**HTTP-Verb:** `POST`  
**URL:** `/api/lobby/kick`  
**Header:** `Authorization: Bearer <JWT>`

**Request (JSON):**
```json
{ "user_id": 43 }
```

**Erfolgreiche Antwort:** `200 OK` (z. B. Bestätigung).

**Mögliche Fehler:** `400` (user_id fehlt), `401`, `403` (keine Berechtigung), `404`, `500`.

---

## Session zurücksetzen (Reset)

Beendet die aktive Session und leert die Lobby. Alle Spieler sind danach nicht mehr in der Session.

**HTTP-Verb:** `POST`  
**URL:** `/api/lobby/reset`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{ "message": "Session closed" }
```

**Mögliche Fehler:** `500`.

---

## Ready-Status setzen

Setzt den Ready-Status des eingeloggten Spielers in der aktiven Session. Wenn alle aktiven Spieler ready sind, kann der Host das Spiel starten (oder Auto-Start nach Countdown).

**HTTP-Verb:** `POST`  
**URL:** `/api/lobby/ready`  
**Header:** `Authorization: Bearer <JWT>`

**Request (JSON):**
```json
{ "ready": true }
```
Für „Not Ready“: `{ "ready": false }`.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "Player marked as ready",
  "session_id": 1,
  "user_id": 42,
  "is_ready": true
}
```

**Mögliche Fehler:** `400` (Feld `ready` fehlt oder ist kein Boolean), `401`, `404` (keine aktive Session oder Spieler nicht in der Session), `500`.

---

# Game

Routen für Spielkonfiguration, Start, Countdown-Abbruch, Neustart und für die Bot-Antwortabfrage.

---

## Kategorien- und Fragenanzahl

Liefert die Anzahl verfügbarer Fragen pro Kategorie und Schwierigkeit (für die Konfigurations-UI).

**HTTP-Verb:** `GET`  
**URL:** `/api/game/category-question-counts`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK` – Objekt mit Kategorien-IDs und pro Schwierigkeit (EASY, MEDIUM, HARD) die Anzahl Fragen.

**Mögliche Fehler:** `500`.

---

## Spiel konfigurieren

Speichert die Spielkonfiguration der aktiven Session (Anzahl Fragen, Kategorien, Schwierigkeiten). Nur in Zustand LOBBY und nur vom Host. Die verfügbaren Fragen müssen mindestens der benötigten Anzahl entsprechen.

**HTTP-Verb:** `POST`  
**URL:** `/api/game/config`  
**Header:** `Authorization: Bearer <JWT>`, `Content-Type: application/json`

**Request (JSON):**
```json
{
  "mode": "Q5",
  "categories": [1, 2, 9],
  "difficulties": ["EASY", "MEDIUM"]
}
```
`mode`: `5`, `10`, `20` oder `Q5`, `Q10`, `Q20`. `difficulties`: `EASY`, `MEDIUM`, `HARD`.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "Game configuration saved",
  "session_id": 5,
  "round_length": "Q5",
  "questions_needed": 5,
  "questions_available": 14,
  "categories": [1, 2, 9],
  "difficulties": ["EASY", "MEDIUM"]
}
```

**Mögliche Fehler:** `400` (ungültiger Modus/Kategorien/Schwierigkeit), `401`, `403` (nur Host), `404` (keine aktive Session), `409` (Spiel läuft bereits), `422` (nicht genug Fragen), `500`.

---

## Spiel starten

Startet die Partie: Session geht in COUNTDOWN, MQTT wird publiziert. Nur in LOBBY, nur vom Host. Konfiguration muss gesetzt sein, mindestens ein Spieler in der Session, alle Spieler müssen ready sein.

**HTTP-Verb:** `POST`  
**URL:** `/api/game/start`  
**Header:** `Authorization: Bearer <JWT>`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "Game started",
  "session_id": 5,
  "state": "COUNTDOWN"
}
```

**Mögliche Fehler:** `400` (Konfiguration unvollständig, keine Spieler, nicht alle ready), `401`, `403` (nur Host), `404`, `409` (Spiel läuft bereits), `500`.

---

## Countdown abbrechen

Bricht den Countdown ab und setzt die Session zurück in LOBBY. Nur vom Host.

**HTTP-Verb:** `POST`  
**URL:** `/api/game/countdown/abort`  
**Header:** `Authorization: Bearer <JWT>`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "Countdown aborted",
  "session_id": 5,
  "state": "LOBBY"
}
```

**Mögliche Fehler:** `401`, `403`, `404`, `409` (kein Countdown), `500`.

---

## Spiel neu starten (Restart)

Setzt die Session nach Ende einer Partie (RESULTS) zurück in LOBBY, sodass eine neue Runde konfiguriert und gestartet werden kann.

**HTTP-Verb:** `POST`  
**URL:** `/api/game/restart`  
**Header:** `Authorization: Bearer <JWT>`

**Request:** Kein Body.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "message": "Game restarted",
  "session_id": 5
}
```

**Mögliche Fehler:** `401`, `404`, `409` (Spiel nicht im Zustand RESULTS), `500`.

---

## Bot-Antwort abrufen

Wird von den Bots genutzt, um die richtige Antwortoption zu einer Frage zu erhalten. Der Bot entscheidet anhand seiner Schwierigkeit, ob er richtig antwortet und mit welcher Verzögerung.

**HTTP-Verb:** `GET`  
**URL:** `/api/game/bot/answer/:questionId`

**Parameter:** `questionId` – ID der aktuellen Frage.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "question_id": 42,
  "correct_option": "B"
}
```

**Mögliche Fehler:** `400` (ungültige questionId), `404` (Frage nicht gefunden), `500`.

---

### Bot-System (Logik)

Die einzige HTTP-Route für Bots ist `GET /api/game/bot/answer/:questionId`. Sie liefert nur die richtige Antwortoption. Die komplette Bot-Logik (Schwierigkeit, Korrektheit, Antwortzeit) liegt im Frontend.

**Schwierigkeitsstufen:** Pro Bot wählbar im Frontend (Lobby): **LEICHT**, **MITTEL** (Standard), **SCHWER**. Maximal 5 Bots pro Session; feste Bot-Accounts (z. B. bot1 … bot5).

**Ablauf:** Sobald eine Frage per MQTT kommt, ruft das Frontend für jeden Bot die Route auf, erhält `correct_option` und die Schwierigkeit der Frage (EASY, MEDIUM, HARD). Aus der Kombination **Bot-Schwierigkeit × Fragen-Schwierigkeit** wird eine Konfiguration gewählt (siehe Tabelle). Mit der **Präzision** (accuracy) wird per Zufall entschieden, ob der Bot richtig antwortet; bei falsch wird eine zufällige falsche Option gewählt. Die **Antwortzeit** ist eine gleichverteilte Zufallszahl im Intervall [minTime, maxTime] in Millisekunden. Nach dieser Verzögerung wird die Antwort wie bei einem normalen Spieler per MQTT (`game/answer`) gesendet, inkl. `response_time_ms`. Die Punkteberechnung erfolgt im Backend wie bei allen Spielern; Bots erscheinen nicht in den Highscores.

**Konfiguration (Frontend):** Präzision (Wahrscheinlichkeit richtige Antwort) und Antwortzeitbereich [min, max] in ms:

| Bot \ Frage | EASY (Präzision / min–max ms) | MEDIUM | HARD |
|-------------|------------------------------|--------|------|
| **LEICHT**  | 70 % / 8000–22000            | 50 % / 12000–26000 | 30 % / 18000–30000 |
| **MITTEL**  | 85 % / 4000–12000            | 70 % / 6000–16000  | 55 % / 10000–22000 |
| **SCHWER**  | 95 % / 2000–8000             | 85 % / 4000–10000  | 75 % / 6000–14000  |

Höhere Bot-Schwierigkeit → höhere Präzision und kürzere Antwortzeiten. Höhere Fragen-Schwierigkeit → niedrigere Präzision und längere Zeiten.

---

# Highscores

Routen für die Bestenlisten je Spielmodus (Q5, Q10, Q20).

---

## Highscores nach Modus

Liefert die Top-20-Ergebnisse für den angegebenen Modus. Sortierung: zuerst nach Punkten absteigend, dann nach Antwortzeit und Zeitstempel.

**HTTP-Verb:** `GET`  
**URL:** `/api/highscores/:mode`

**Parameter:** `mode` – einer von `Q5`, `Q10`, `Q20`.

**Erfolgreiche Antwort:** `200 OK`
```json
{
  "round_length": "Q5",
  "highscores": [
    {
      "rank": 1,
      "user_id": 4,
      "username": "junior",
      "display_name": "junior",
      "total_points": 4.7,
      "total_response_time_ms": 22600,
      "created_at": "2026-02-22 14:33:11.0"
    }
  ]
}
```

**Mögliche Fehler:** `400` (mode muss Q5, Q10 oder Q20 sein), `500`.

---

*Hinweis: Die Routen unter `/api/objects` (CRUD) gehören nicht zur Quiz-Spiel-Logik und sind hier nicht beschrieben.*
