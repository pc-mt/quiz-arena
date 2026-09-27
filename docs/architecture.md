# Gesamtarchitektur

## Komponenten

- **Frontend**: Webanwendung (Port 80): Lobby, Spiel, Highscores, Controller-Verwaltung. Nutzt REST-API (per Nginx-Proxy) und MQTT (WebSocket, Port 9001).
- **Web-Controller**: Separate Web-App (Port 81): Anzeige für Spieler (Ready, Antworten A–D, Punkte). Verbindung nur per MQTT; keine REST-Calls.
- **Backend**: REST-API (Java/Vert.x, Port 8080): Session-, Spiel- und Lobby-Logik, Auth, RFID. Publiziert und abonniert MQTT-Topics.
- **MQTT-Broker** (Ports 1883/9001): Zentraler Nachrichtenbus für Echtzeit-Ereignisse (Spielzustände, Countdown, Fragen, Antworten, Ready, Ping/Pong, RFID).
- **Datenbank**: MariaDB (Docker-intern): Nutzer, Controller, Sessions, Fragen, Antworten, Highscores, Bewertungs-Zeitbuckets.
- **Hardware**: ESP32/Arduino (optional): Taster, NeoPixel, OLED, RFID; verhält sich wie ein Web-Controller, nur per MQTT.

---

## Komponentendiagramm

```
┌─────────────────────────────────────────────────────────────────┐
│                         Docker-Host                             │
│                                                                 │
│  ┌──────────────┐   REST /api   ┌──────────────────────────┐   │
│  │   Frontend   │ ────────────► │       Backend            │   │
│  │  (Port 80)   │               │   (Java/Vert.x, 8080)    │   │
│  │              │ ◄──────────── │                          │   │
│  └──────┬───────┘               └────────────┬─────────────┘   │
│         │                                    │                  │
│         │  MQTT (WS 9001)                    │  MQTT (TCP 1883) │
│         ▼                                    ▼                  │
│  ┌──────────────┐               ┌──────────────────────────┐   │
│  │ Web-Ctrl.    │               │      MQTT-Broker         │   │
│  │  (Port 81)   │ ◄───────────► │  (Mosquitto 1883/9001)   │   │
│  └──────────────┘               └──────────────────────────┘   │
│                                            ▲                    │
│                                            │ TCP 1883           │
│                                 ┌──────────┴──────────┐        │
│                                 │  MariaDB (intern)   │        │
│                                 └─────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
         ▲                                   ▲
         │  MQTT (WS 9001)                   │  MQTT (TCP 1883)
         │                                   │
  ┌──────┴──────┐                   ┌────────┴────────┐
  │   Browser   │                   │  ESP32/Arduino  │
  │  (Spieler)  │                   │  (Hardware-Ctrl)│
  └─────────────┘                   └─────────────────┘
```

**Kommunikationswege zusammengefasst:**

| Von → Nach | Protokoll | Zweck |
|-----------|-----------|-------|
| Frontend → Backend | REST (HTTP/Nginx-Proxy) | Login, Join, Start, Highscores |
| Frontend ↔ Broker | MQTT (WebSocket 9001) | Spielzustand empfangen, Lobby-Updates |
| Web-Controller ↔ Broker | MQTT (WebSocket 9001) | Ready senden, Antworten senden, Fragen empfangen |
| Hardware ↔ Broker | MQTT (TCP 1883) | Wie Web-Controller + RFID-Scan |
| Backend ↔ Broker | MQTT (TCP 1883) | Alle Spielereignisse publizieren und abonnieren |
| Backend ↔ MariaDB | JDBC (intern) | Daten lesen/schreiben |

---

## Spielablauf (Sequenz)

Ein typischer Durchlauf von Spielstart bis Ergebnis:

```
Browser/Frontend          Backend               MQTT-Broker         Controller
      │                      │                       │                    │
      │── GET /api/lobby/session ──►│                │                    │
      │◄── session {id, state} ─────│                │                    │
      │                      │                       │                    │
      │  [Nutzer registriert/einloggt, Controller wählt, beitritt]        │
      │── POST /api/lobby/join ──►  │                │                    │
      │◄── {joined} ────────────────│                │                    │
      │                      │      │◄── controller/register ────────────│
      │                      │──────│──► controllers/updated ───────────►│
      │                      │      │                │                    │
      │  [Spieler drückt Ready]      │                │                    │
      │── POST /api/lobby/ready ──► │                │                    │
      │                      │──────│──► lobby/updated ──────────────────►│
      │                      │                       │                    │
      │── POST /api/game/start ───► │                │                    │
      │◄── {state: COUNTDOWN} ──────│                │                    │
      │                      │──────│──► game/state {COUNTDOWN} ─────────►│
      │                      │──────│──► game/countdown {3,2,1} ──────────►│
      │                      │──────│──► game/question {text, A-D} ───────►│
      │                      │──────│──► game/question/timer {30..0} ──────►│
      │                      │                       │                    │
      │                      │      │◄── game/answer {B, 8500ms} ─────────│
      │                      │──────│──► game/evaluation {correct, points}►│
      │                      │                       │                    │
      │  [nach allen Fragen]         │                │                    │
      │                      │──────│──► game/state {RESULTS} ────────────►│
      │── GET /api/highscores/Q5 ──►│                │                    │
      │◄── [{rank,username,points}] ─│               │                    │
```

**Zustände der Session:**

`LOBBY` → `COUNTDOWN` → `QUESTION` → `EVALUATION` → `QUESTION` → … → `RESULTS`

Ein Restart (`POST /api/game/restart`) setzt die Session von `RESULTS` zurück nach `LOBBY`.

---

## Ablauf — Kurzfassung

- **Session**: Frontend ruft `GET /api/lobby/session` auf; legt Session an oder gibt bestehende zurück.
- **Login & Join**: Nutzer loggt sich ein (JWT), wählt freien Controller, tritt der Session bei.
- **Controller**: Web/Hardware registriert sich per MQTT (`controller/register`); erhält Status und Spielereignisse.
- **Ready**: Spieler setzt Ready per MQTT (`player/ready`) oder per HTTP (`POST /api/lobby/ready`).
- **Spielablauf**: Lobby → Countdown → Fragen → Auswertung → Ergebnisse. Steuerung über Frontend; Controller empfangen Fragen/Auswertung per MQTT und senden Antworten per MQTT.
- **RFID**: Hardware-Controller scannt Karte → Backend löst RFID-Login aus → Frontend wird per MQTT benachrichtigt.
