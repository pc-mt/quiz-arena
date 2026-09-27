# MQTT-Topics

Dokumentation aller MQTT-Topics des Quiz-Spiels: Funktion, Publisher, Subscriber, Payload (JSON). Präfix aller Topics: `quiz-arena/` (konfigurierbar über `MQTT_MESSAGE_PREFIX`). QoS ist durchgehend 0.

---

# Controller

Topics für Controller-Registrierung, Statusabfrage und Heartbeat (Ping/Pong). Controller (Web, Hardware, Bots) melden sich hier an und erhalten ihren Lobby-Status.

---

## Controller registrieren

Meldet einen Controller beim Backend an. Erzeugt oder aktualisiert den Eintrag; danach kann der Controller in der Lobby verwendet werden. Nach erfolgreicher Registrierung publiziert das Backend `controllers/updated` und sendet dem Controller eine Status-Response.

**Topic:** `quiz-arena/controller/register`  
**Publisher:** Web-Controller, Hardware-Controller, Bots (Frontend).  
**Subscriber:** Backend.

**Payload (Publisher → Backend):**
```json
{
  "controller_id": "WEB-ABC123",
  "controller_type": "WEB"
}
```
`controller_type` optional (Default: `WEB`). Erlaubt: `WEB`, `HARDWARE`.

**Backend-Reaktion:** Controller in DB anlegen/aktualisieren, dann Publish auf `controller/{controller_id}/status/response` (siehe unten) und auf `controllers/updated`.

---

## Status anfordern

Fordert den aktuellen Status für einen bestimmten Controller an (Spieler zugeordnet?, Ready?, Punkte). Wird vom Controller nach Registrierung und bei Bedarf (z. B. nach Lobby-Update) aufgerufen. Das Backend antwortet auf das controller-spezifische Response-Topic.

**Topic:** `quiz-arena/controller/status/request`  
**Publisher:** Web-Controller, Hardware, Bots.  
**Subscriber:** Backend.

**Payload (Publisher → Backend):**
```json
{
  "controller_id": "WEB-ABC123"
}
```

**Backend-Reaktion:** Publish auf `quiz-arena/controller/{controller_id}/status/response` mit Status-JSON oder Fehler-JSON.

---

## Status-Antwort (pro Controller)

Enthält den aktuellen Lobby-/Spiel-Status für genau einen Controller. Wird nur an den anfragenden Controller gesendet (Topic enthält dessen `controller_id`).

**Topic:** `quiz-arena/controller/{controller_id}/status/response`  
**Publisher:** Backend (als Antwort auf `controller/status/request`).  
**Subscriber:** Der jeweilige Controller (Web, Hardware, Bot) mit passender `controller_id`.

**Payload (Backend → Controller):**
```json
{
  "controller_id": "WEB-ABC123",
  "controller_status": "ASSIGNED",
  "has_player": true,
  "player_name": "junior",
  "is_ready": false,
  "points": 0.0
}
```
Bei Fehler z. B.:
```json
{
  "controller_id": "WEB-ABC123",
  "error": "Controller not found"
}
```

---

## Ping (Heartbeat)

Vom Backend an jeden in der aktiven Session eingetragenen Controller gesendet (periodisch und vor jeder neuen Frage). Controller müssen mit Pong antworten; ausbleibende Pongs führen zur Markierung als OFFLINE und ggf. zum Ausschluss vor der nächsten Frage.

**Topic:** `quiz-arena/controller/{controller_id}/ping`  
**Publisher:** Backend (HeartbeatManager).  
**Subscriber:** Der jeweilige Controller.

**Payload (Backend → Controller):**
```json
{
  "requestId": "uuid",
  "ts": 1772000000000,
  "fw": "v1.0"
}
```

---

## Pong (Heartbeat-Antwort)

Bestätigung, dass der Controller erreichbar ist. Muss auf dem Topic des eigenen Controllers gesendet werden.

**Topic:** `quiz-arena/controller/{controller_id}/pong`  
**Publisher:** Web-Controller, Hardware, Bots.  
**Subscriber:** Backend.

**Payload (Controller → Backend):**
```json
{
  "requestId": "uuid",
  "ts": 1772000000000,
  "fw": "v1.0"
}
```

---

## RFID-Scan (Hardware)

Wird vom Hardware-Controller beim Lesen einer RFID-Karte publiziert. Das Backend prüft die UID, führt ggf. einen RFID-Login durch und antwortet auf das controller-spezifische Result-Topic; bei Erfolg wird zusätzlich `auth/rfid-login` für das Frontend publiziert.

**Topic:** `quiz-arena/controller/{controller_id}/rfid/scan`  
**Publisher:** Hardware-Controller.  
**Subscriber:** Backend.

**Payload (Controller → Backend):**
```json
{
  "rfid_uid": "04:9C:64:D2:4B:80"
}
```

---

## RFID-Ergebnis (pro Controller)

Rückmeldung an den Hardware-Controller, ob der RFID-Login erfolgreich war und welcher Spieler zugeordnet wurde.

**Topic:** `quiz-arena/controller/{controller_id}/rfid/result`  
**Publisher:** Backend.  
**Subscriber:** Hardware-Controller.

**Payload (Backend → Controller) bei Erfolg:**
```json
{
  "controller_id": "HW-01",
  "success": true,
  "message": "RFID login successful",
  "player_name": "junior",
  "session_id": 1,
  "timestamp": 1772000000000
}
```
Bei Fehler: `success: false`, `message` mit Fehlergrund (z. B. unbekannte Karte).

---

# Player

Topic für den Ready-Status. Spieler (über ihren Controller) melden, ob sie bereit sind; das Backend aktualisiert die Lobby und kann bei „Not Ready“ während des Countdowns den Countdown abbrechen.

---

## Ready-Status setzen

Setzt den Ready-Status des Spielers, der dem angegebenen Controller zugeordnet ist. Entspricht der HTTP-Route `POST /api/lobby/ready`, wird aber von Controllern (Web, Hardware, Bots) per MQTT genutzt. Backend aktualisiert die DB, publiziert `lobby/updated` und sendet bei „Not Ready“ während COUNTDOWN ggf. einen Countdown-Abbruch (game/state LOBBY).

**Topic:** `quiz-arena/player/ready`  
**Publisher:** Web-Controller, Hardware-Controller, Bots.  
**Subscriber:** Backend.

**Payload (Publisher → Backend):**
```json
{
  "controller_id": "WEB-ABC123",
  "ready": true
}
```
`ready: false` für „Not Ready“.

**Backend-Reaktion:** Lobby-Daten aktualisieren, `lobby/updated` publizieren, ggf. Countdown abbrechen und `game/state` auf LOBBY setzen.

---

# Lobby

Benachrichtigung, dass sich die Lobby oder die Controller-Liste geändert hat. Clients können danach per HTTP (z. B. `GET /api/lobby/status`) die aktuellen Daten abrufen.

---

## Lobby aktualisiert

Wird vom Backend publiziert, wenn sich die Lobby oder die Controller-Zuordnung ändert (Join, Leave, Kick, Ready, Reset, Controller registriert, RFID-Login). Enthält keine vollständige Spielerliste; das Frontend nutzt das Signal für Polling (z. B. erneuter Aufruf von `/api/lobby/status`).

**Topic:** `quiz-arena/lobby/updated`  
**Publisher:** Backend.  
**Subscriber:** Frontend (und ggf. andere Clients).

**Payload (Backend):**
```json
{
  "event": "lobby.updated",
  "timestamp": 1772000000000
}
```
Optional weitere Felder je nach Anlass (z. B. `controller_id`).

---

## Controller-Liste aktualisiert

Hinweis, dass sich die Liste der Controller geändert hat (Registrierung, Freigabe). Frontend kann z. B. `/api/controllers/available` erneut aufrufen.

**Topic:** `quiz-arena/controllers/updated`  
**Publisher:** Backend.  
**Subscriber:** Frontend, ggf. Controller.

**Payload (Backend):**
```json
{
  "event": "controllers.updated"
}
```

---

# Game

Topics für Spielzustand, Countdown, Fragen, Antworten, Auswertung und Timer. Das Backend ist der zentrale Publisher; Frontend und alle Controller subscriben auf die für sie relevanten Topics.

---

## Spielzustand

Meldet einen Zustandswechsel der aktiven Session (LOBBY, COUNTDOWN, QUESTION, EVALUATION, RESULTS). Wird bei jedem Wechsel vom Backend publiziert. Optional enthält die Nachricht bei RESULTS die Ergebnisliste.

**Topic:** `quiz-arena/game/state`  
**Publisher:** Backend.  
**Subscriber:** Frontend, Web-Controller, Hardware, Bots.

**Payload (Backend):**
```json
{
  "state": "LOBBY",
  "session_id": 1,
  "timestamp": 1772000000000
}
```
Mögliche `state`: `LOBBY`, `COUNTDOWN`, `QUESTION`, `EVALUATION`, `RESULTS`. Bei RESULTS kann `results` (Array mit Rang, user_id, player_name, total_points, …) mitgesendet werden.

---

## Countdown

Zählt die Sekunden bis zum Start der ersten Frage (z. B. 3, 2, 1). Wird vom Backend einmal pro Sekunde publiziert, bis der Countdown bei 1 endet; danach folgt der Wechsel zu QUESTION und die erste Frage.

**Topic:** `quiz-arena/game/countdown`  
**Publisher:** Backend.  
**Subscriber:** Frontend, Controller.

**Payload (Backend):**
```json
{
  "countdown": 3,
  "session_id": 1,
  "timestamp": 1772000000000
}
```
`countdown` nimmt typisch die Werte 3, 2, 1 an.

---

## Frage

Enthält die aktuelle Frage (Text, Kategorie, Schwierigkeit, Antwortoptionen A–D) und die Frage-ID. Wird vom Backend beim Übergang zu QUESTION und vor jeder neuen Frage publiziert. Controller und Bots nutzen die Frage-ID für die Antwort (game/answer).

**Topic:** `quiz-arena/game/question`  
**Publisher:** Backend.  
**Subscriber:** Frontend, Web-Controller, Hardware, Bots.

**Payload (Backend):**
```json
{
  "session_id": 1,
  "question_index": 1,
  "total_questions": 5,
  "question_id": 42,
  "text": "Fragentext?",
  "category": "Sport",
  "difficulty": "MEDIUM",
  "duration_sec": 30,
  "answers": {
    "A": "Antwort A",
    "B": "Antwort B",
    "C": "Antwort C",
    "D": "Antwort D"
  }
}
```
`difficulty`: EASY, MEDIUM, HARD.

---

## Frage-Timer

Sekunden-Restzeit pro Frage (z. B. 30 bis 0). Wird vom Backend regelmäßig (z. B. jede Sekunde) publiziert. Bei 0 schließt die Frage (Auswertung); spätere Antworten zählen nicht.

**Topic:** `quiz-arena/game/question/timer`  
**Publisher:** Backend.  
**Subscriber:** Frontend, Controller (für Anzeige).

**Payload (Backend):**
```json
{
  "session_id": 1,
  "question_id": 42,
  "seconds_left": 25,
  "timestamp": 1772000000000
}
```

---

## Antwort einreichen

Übermittelt die Antwort eines Spielers (oder Bots) auf die aktuelle Frage. Backend prüft Session/Frage, speichert die Antwort mit Zeitstempel und Zeitbucket für die Punkteberechnung. Doppelte Antworten vom gleichen Controller werden abgelehnt (kein zweiter Publish).

**Topic:** `quiz-arena/game/answer`  
**Publisher:** Web-Controller, Hardware-Controller, Bots.  
**Subscriber:** Backend.

**Payload (Publisher → Backend):**
```json
{
  "session_id": 1,
  "controller_id": "WEB-ABC123",
  "question_id": 42,
  "answer": "B",
  "answered_at": 1772000000000,
  "response_time_ms": 8500
}
```
`answer`: A, B, C oder D. `response_time_ms`: Zeit zwischen Frage-Start und Antwort (für Punkteberechnung). Bei Bots wird diese Zeit vom Frontend simuliert.

**Backend-Reaktion:** Antwort in DB speichern; bei Zeitablauf oder wenn alle aktiven Spieler geantwortet haben → Auswertung durchführen und `game/evaluation` publizieren.

---

## Auswertung

Enthält die richtige Option, die Auflösungstexte und pro Spieler das Ergebnis (richtig/falsch, vergebene Punkte, Antwortzeit). Wird vom Backend nach Abschluss einer Frage (Timeout oder alle haben geantwortet) einmal publiziert. Danach folgt nach 3 Sekunden die nächste Frage oder RESULTS.

**Topic:** `quiz-arena/game/evaluation`  
**Publisher:** Backend.  
**Subscriber:** Frontend, Web-Controller, Hardware.

**Payload (Backend):**
```json
{
  "session_id": 1,
  "question_id": 42,
  "correct_option": "B",
  "correct_answer_text": "Antwort B",
  "correct_answer_full": "B - Antwort B",
  "reason": "timeout",
  "results": [
    {
      "user_id": 10,
      "player_name": "junior",
      "answered_option": "B",
      "is_correct": true,
      "points_awarded": 2.7,
      "response_time_ms": 8500,
      "time_bucket": "5-10",
      "base_points": 3,
      "time_factor": 0.9
    }
  ],
  "timestamp": 1772000000000
}
```
`reason` z. B. `timeout` oder `all_answered`.

---

## Spiel starten (Signal)

Wird vom Backend beim Start einer Partie (nach HTTP `POST /api/game/start`) publiziert. Enthält nur ein kurzes Signal; der tatsächliche Zustand steht in `game/state` (COUNTDOWN).

**Topic:** `quiz-arena/game/start`  
**Publisher:** Backend.  
**Subscriber:** Optional (Frontend/Controller können darauf reagieren).

**Payload (Backend):**
```json
{
  "action": "start"
}
```

---

## Spiel beenden (Signal)

Wird vom Backend beim Beenden einer Partie (z. B. Session-Reset, Lobby-Leave) publiziert. Enthält nur ein Signal; der Zustand wechselt danach typisch zu LOBBY (`game/state`).

**Topic:** `quiz-arena/game/stop`  
**Publisher:** Backend.  
**Subscriber:** Optional (Frontend/Controller).

**Payload (Backend):**
```json
{
  "action": "stop"
}
```

---

# Auth

Topic für RFID-Login: Das Frontend kann sich mit dem Token automatisch anmelden, sobald ein Nutzer am Hardware-Controller per RFID eingeloggt wurde.

---

## RFID-Login (Token für Frontend)

Wird vom Backend nach erfolgreichem RFID-Scan (Topic `controller/{id}/rfid/scan`) publiziert. Enthält das JWT und Nutzerdaten, damit das Frontend den Nutzer automatisch anmelden und z. B. auf die Controller-Seite wechseln kann.

**Topic:** `quiz-arena/auth/rfid-login`  
**Publisher:** Backend.  
**Subscriber:** Frontend.

**Payload (Backend):**
```json
{
  "user_id": 42,
  "username": "junior",
  "display_name": "junior",
  "token": "JWT_TOKEN_HIER",
  "timestamp": 1772000000000
}
```
Das Frontend speichert den Token und kann die Session/Lobby-Ansicht aktualisieren.

---

# Sonstige Topics

Diese Topics werden vom Backend bereitgestellt, gehören aber nicht zum Kernablauf des Quiz-Spiels.

- **`quiz-arena/demo/message`** – Backend publiziert: `{ "message": "…" }`. Für Demo-/Testzwecke.
- **`quiz-arena/game/object/created`** – Backend publiziert: `{ "name": "…" }`. Gehört zur Objekt-CRUD-API (`/api/objects`), nicht zum Quiz.

