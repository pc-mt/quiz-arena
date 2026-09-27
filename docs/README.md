# Dokumentation — QuizArena

Überblick über die Projektdokumentation. Alle Beschreibungen sind sachlich und auf Deutsch (oder Englisch).

## Inhalt

| Datei | Beschreibung |
|-------|--------------|
| [architecture.md](architecture.md) | Gesamtarchitektur: Frontend, Web-Controller, Backend, MQTT, Datenbank, Hardware. |
| [http-api.md](http-api.md) | REST-API: Routen (Auth, Controller, Lobby, Game, Highscores, Bot), Request-/Response-Bodies, Fehler. |
| [mqtt-topics.md](mqtt-topics.md) | MQTT-Topics: Controller, Player, Lobby, Game, Auth; Payloads, Publisher/Subscriber. |
| [database.md](database.md) | Datenbankschema (MariaDB): Tabellen und Beziehungen. |
| [deployment.md](deployment.md) | Projekt starten (Docker), Ports, lokaler Test mit Freund (Frontend + Web-Controller), Umgebungsvariablen, Hardware. |

## Konventionen

- Jede Route bzw. jedes Topic wird beschrieben mit: **Funktion**, **gesendetes/empfangenes JSON**, **mögliche Fehler**.
- Beispiel Login: Die Login-Route fügt den Nutzer der Session hinzu, sofern noch nicht verbunden; bei erneutem Login wird die Anfrage abgelehnt und ein Fehler zurückgegeben.
