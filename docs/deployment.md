# Deployment und Start

## Voraussetzungen

- Docker, Docker Compose
- Optional: Node (Frontend-Entwicklung), Java (Backend), PlatformIO (Arduino)

## Start mit Docker

```bash
docker compose up --build
```

Optional mit Watch (Backend bei Änderungen neu bauen):

```bash
docker compose up --build --watch
```

### Ports (nach Start)

| Dienst          | Port(s)     | URL (lokal)              |
|-----------------|------------|---------------------------|
| Frontend        | 80         | http://localhost         |
| Web-Controller  | 81         | http://localhost:81      |
| Backend (REST)  | 8080       | http://localhost:8080    |
| phpMyAdmin      | 8081       | http://localhost:8081    |
| MQTT (TCP)      | 1883       | —                        |
| MQTT (WebSocket)| 9001       | ws://localhost:9001      |
| MariaDB         | in `.env`  | nur im Docker-Netz        |

Das Frontend (Port 80) leitet `/api` an das Backend weiter. Der Web-Controller (Port 81) nutzt nur MQTT; er ruft keine REST-API auf.

---

## Lokal / Test mit Freund

Zum Testen zu zweit (ein Rechner = Lobby/Spielsteuerung, anderer = Controller):

1. **Auf dem Rechner, der Lobby und Spiel hostet:**  
   `docker compose up --build` starten.

2. **Frontend (Lobby, Start, Highscores):**  
   Im Browser öffnen: **http://localhost** (oder von einem anderen Gerät im gleichen Netz: **http://\<IP-des-Hosts\>**). Dort registrieren, einloggen, Session nutzen, Spiel starten.

3. **Web-Controller (zum Mitspielen):**  
   Im Browser öffnen: **http://localhost:81** (lokal) oder **http://\<IP-des-Hosts\>:81** (Freund/anderer Rechner). Der Controller verbindet sich per MQTT-WebSocket mit dem Broker; Standardfall ist `window.location.hostname` und Port 9001. Damit der Freund sich verbinden kann, muss der MQTT-WebSocket-Port **9001** vom Host erreichbar sein (Docker mappt ihn bereits; ggf. Firewall-Regel für 9001 setzen).

4. **Kurzfassung:**  
   - Host: http://localhost → Lobby, Spiel starten.  
   - Freund: http://\<Host-IP\>:81 → Web-Controller öffnen, anmelden (Login im Frontend auf dem Host oder RFID), Ready, Antworten.

---

## Umgebungsvariablen

- **MQTT:** Broker-URL, Port, Benutzername, Passwort, Topic-Präfix (`MQTT_MESSAGE_PREFIX`, z. B. `quiz-arena/`). Werte in `.env` bzw. `docker-compose.yml`.
- **DB:** Host, User, Passwort, Datenbankname, Port (z. B. `DB_HOST=mariadb`, `DB_PORT=3306`).
- Für Tests von außerhalb (z. B. Freund): Auf dem Host ggf. `MQTT_BROKER_URL` so setzen, dass Clients die erreichbare Host-IP nutzen können; Frontend und Web-Controller können per Nginx/Entrypoint mit Umgebungsvariablen injiziert werden (siehe jeweilige Konfiguration).

---

## Hardware (ESP32/Arduino)

- WiFi und MQTT in `arduino/include/secrets.h` und `config.h` eintragen (Broker = Host-IP, wenn Broker auf dem PC läuft).
- Firmware mit PlatformIO oder Arduino-IDE flashen.

