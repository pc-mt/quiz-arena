const env = window.__ENV__ || {};
const PREFIX = (env.MQTT_MESSAGE_PREFIX || "quiz-arena/").replace(/\/*$/, "/");
const USER = env.MQTT_USERNAME || "quiz-arena";
const PASS = env.MQTT_PASSWORD || "change-me-mqtt";
const BROKER_HOST = env.MQTT_BROKER_URL || window.location.hostname;
const BROKER_PORT = env.MQTT_BROKER_PORT || "9001";
const WS_URL = `ws://${BROKER_HOST}:${BROKER_PORT}`;

const CTRL_ID = `WEB-${Math.random().toString(16).slice(2, 10).toUpperCase()}`;

/* ─── DOM REFERENCES ─── */
const controllerIdEl      = document.getElementById("controllerId");
const controllerPlayerEl  = document.getElementById("controllerPlayer");
const controllerStatusEl  = document.getElementById("controllerStatus");
const controllerAvatarEl  = document.getElementById("controllerAvatar");
const controllerAnswersEl = document.getElementById("controllerAnswers");
const readyBtnEl          = document.getElementById("readyBtn");
const oledStateEl         = document.getElementById("oledState");
const oledReadyEl         = document.getElementById("oledReady");
const oledPointsEl        = document.getElementById("oledPoints");
const oledPointsGainedEl  = document.getElementById("oledPointsGained");
const ctrlHintEl          = document.getElementById("ctrlHint");
const mqttDotEl           = document.getElementById("mqttDot");
const mqttLabelEl         = document.getElementById("mqttLabel");

if (controllerIdEl) controllerIdEl.textContent = CTRL_ID;

/* ─── STATE ─── */
let currentSessionId  = null;
let currentQuestionId = null;
let isQuestionActive  = false;
let currentGameState  = "LOBBY";
let isPlayerReady     = false;
let currentPlayerName = "—";
let totalPoints       = 0.0;
let lastEvalKey       = null;

/* ─── UI HELPERS ─── */
const setIndicator = (ok, text) => {
  if (mqttDotEl) {
    mqttDotEl.classList.toggle("connected", ok);
    mqttDotEl.classList.toggle("disconnected", !ok);
  }
  if (mqttLabelEl) mqttLabelEl.textContent = text;
};

const setStatus = (text, variant) => {
  if (!controllerStatusEl) return;
  controllerStatusEl.textContent = text;
  controllerStatusEl.classList.remove("is-ready", "is-game");
  if (variant === "ready") controllerStatusEl.classList.add("is-ready");
  if (variant === "game")  controllerStatusEl.classList.add("is-game");
};

const setPlayer = (name) => {
  currentPlayerName = name || "—";
  if (controllerPlayerEl) controllerPlayerEl.textContent = name;
  if (controllerAvatarEl) {
    controllerAvatarEl.textContent = name && name !== "—"
      ? name.substring(0, 2).toUpperCase()
      : "?";
  }
};

const setHint = (text) => {
  if (ctrlHintEl) ctrlHintEl.textContent = text;
};

const toScore = (value) => {
  const n = Number(value);
  return Number.isFinite(n) ? n : null;
};

const formatScore = (value) => {
  const n = toScore(value);
  return n == null ? "0.0" : n.toFixed(1);
};

const updateOledScreen = () => {
  if (oledStateEl) oledStateEl.textContent = `STATE: ${currentGameState || "LOBBY"}`;
  if (oledReadyEl) oledReadyEl.textContent = isPlayerReady ? "BEREIT" : "WARTE...";
  if (oledPointsEl) oledPointsEl.textContent = `PTS: ${formatScore(totalPoints)}`;
};

const setPointsGained = (delta) => {
  if (!oledPointsGainedEl) return;
  if (delta == null || !Number.isFinite(delta)) {
    oledPointsGainedEl.textContent = "";
    return;
  }
  const sign = delta >= 0 ? "+" : "";
  oledPointsGainedEl.textContent = `${sign}${delta.toFixed(1)}`;
};

const clearPointsGained = () => {
  if (oledPointsGainedEl) oledPointsGainedEl.textContent = "";
};

/** In der Auswertung die richtige Taste markieren (A/B/C/D). Mit null wird zurueckgesetzt. */
const setCorrectAnswerHighlight = (correctKey) => {
  if (!controllerAnswersEl) return;
  const key = correctKey != null ? String(correctKey).toUpperCase().trim() : null;
  controllerAnswersEl.querySelectorAll(".ctrl-answer").forEach((btn) => {
    const btnKey = (btn.dataset.key || "").toUpperCase();
    btn.classList.toggle("is-correct", key !== null && btnKey === key);
  });
};


const setControllerMode = (mode) => {
  if (mode === "LOBBY") {
    if (readyBtnEl)          readyBtnEl.classList.remove("is-hidden");
    if (controllerAnswersEl) {
      controllerAnswersEl.classList.remove("is-hidden", "is-game", "is-locked");
    }
    setHint("Drücke den Button um dich bereit zu machen. Antwort-Buttons sind bereit.");
  } else {
    if (readyBtnEl)          readyBtnEl.classList.remove("is-hidden");
    if (controllerAnswersEl) {
      controllerAnswersEl.classList.remove("is-hidden");
      controllerAnswersEl.classList.add("is-game");
    }
    setHint("Wähle A, B, C oder D zum Antworten.");
  }
};

/* ─── MQTT HELPERS ─── */
const publishJson = (topic, payload, cb) => {
  if (!client.connected) {
    if (typeof cb === "function") cb(new Error("MQTT not connected"));
    return;
  }
  client.publish(topic, JSON.stringify(payload), { qos: 0 }, cb);
};

const requestStatus = () => {
  publishJson(`${PREFIX}controller/status/request`, { controller_id: CTRL_ID });
};

/* ─── STATUS RESPONSE ─── */
const applyStatus = (raw) => {
  try {
    const data = JSON.parse(raw);
    if (data.error) {
      setStatus(data.error);
      return;
    }
    if (currentGameState !== "LOBBY" && currentGameState !== "COUNTDOWN") return;

    if (data.has_player) {
      setPlayer(data.player_name || "Unbekannt");
      isPlayerReady = !!data.is_ready;
      const points = toScore(data.points);
      if (points != null) totalPoints = points;
      updateScreen();
      if (data.is_ready) {
        setStatus("✔ Bereit", "ready");
      } else {
        setStatus("Warte auf Start...");
      }
    } else {
      setPlayer("—");
      totalPoints = 0.0;
      lastEvalKey = null;
      setStatus((data.controller_status || "FREE").toUpperCase());
    }
    updateOledScreen();
  } catch (_err) {
    setStatus("Ungültige Status-Antwort");
  }
};

/* ─── BUTTON FLASH ─── */
const flashButton = (button) => {
  button.classList.add("active");
  setTimeout(() => button.classList.remove("active"), 180);
};

/* ─── UPDATE DISPLAY SCREEN (OLED) ─── */
const updateScreen = () => {
  if (!readyBtnEl) return;
  if (isPlayerReady) {
    readyBtnEl.classList.add("is-ready");
  } else {
    readyBtnEl.classList.remove("is-ready");
  }
  updateOledScreen();
};

/* ─── WIRE ANSWER BUTTONS ─── */
const wireAnswerButtons = () => {
  if (!controllerAnswersEl) return;
  controllerAnswersEl.querySelectorAll(".ctrl-answer").forEach((button) => {
    button.addEventListener("click", () => {
      flashButton(button);
      const key = button.dataset.key;
      if (!key) return;

      if ((currentGameState === "LOBBY" || currentGameState === "COUNTDOWN") && (key === "B" || key === "D")) {
        isPlayerReady = key === "B";
        updateScreen();
        setStatus(isPlayerReady ? "Ready wird gesendet..." : "Not Ready wird gesendet...");
        publishJson(
          `${PREFIX}player/ready`,
          { controller_id: CTRL_ID, ready: isPlayerReady },
          () => requestStatus()
        );
        return;
      }

      if (currentGameState !== "QUESTION" || !["A", "B", "C", "D"].includes(key)) {
        setStatus("Warte auf eine Frage...", "game");
        return;
      }
      if (!isQuestionActive || !currentSessionId || !currentQuestionId) {
        setStatus("Frage läuft, warte auf Fragedaten...", "game");
        return;
      }

      setStatus(`Antwort ${key} wird gesendet...`, "game");
      publishJson(`${PREFIX}game/answer`, {
        session_id: currentSessionId,
        controller_id: CTRL_ID,
        question_id: currentQuestionId,
        answer: key,
        answered_at: Date.now(),
      }, () => {
        setStatus(`Antwort ${key} gesendet ✔`, "game");
        isQuestionActive = false;
        if (controllerAnswersEl) controllerAnswersEl.classList.add("is-locked");
      });
    });
  });
};

/* ─── TOPICS ─── */
const responseTopic            = `${PREFIX}controller/${CTRL_ID}/status/response`;
const pingTopic                = `${PREFIX}controller/${CTRL_ID}/ping`;
const pongTopic                = `${PREFIX}controller/${CTRL_ID}/pong`;
const controllersUpdatedTopic  = `${PREFIX}controllers/updated`;
const gameQuestionTopic        = `${PREFIX}game/question`;
const gameStateTopic           = `${PREFIX}game/state`;
const gameEvaluationTopic      = `${PREFIX}game/evaluation`;

/* ─── INIT ─── */
setIndicator(false, "MQTT: DISCONNECTED");
setStatus("Verbinde...");
setControllerMode("LOBBY");
updateScreen();
updateOledScreen();
wireAnswerButtons();

/* ─── MQTT CONNECTION ─── */
const client = mqtt.connect(WS_URL, {
  username: USER,
  password: PASS,
  clientId: `web-controller-${CTRL_ID}`,
  clean: true,
  reconnectPeriod: 2000,
});

let heartbeatIntervalId = null;

client.on("connect", () => {
  setIndicator(true, "MQTT: Connected");
  setStatus("Registrierung...");

  client.subscribe(responseTopic, { qos: 0 });
  client.subscribe(pingTopic, { qos: 0 });
  client.subscribe(controllersUpdatedTopic, { qos: 0 });
  client.subscribe(gameQuestionTopic, { qos: 0 });
  client.subscribe(gameStateTopic, { qos: 0 });
  client.subscribe(gameEvaluationTopic, { qos: 0 });

  publishJson(`${PREFIX}controller/register`, {
    controller_id: CTRL_ID,
    controller_type: "WEB",
  });
  requestStatus();

  if (heartbeatIntervalId) clearInterval(heartbeatIntervalId);
  heartbeatIntervalId = setInterval(() => requestStatus(), 20000);
});

/* ─── MQTT MESSAGES ─── */
client.on("message", (topic, msg) => {
  const text = msg.toString();

  if (topic === responseTopic) {
    applyStatus(text);
    return;
  }

  if (topic === pingTopic) {
    try {
      const ping = JSON.parse(text);
      publishJson(pongTopic, {
        requestId: ping.requestId || "",
        ts: ping.ts != null ? ping.ts : Date.now(),
        fw: ping.fw || "v1.0",
      });
    } catch (_err) {
      publishJson(pongTopic, { requestId: "", ts: Date.now(), fw: "v1.0" });
    }
    return;
  }

  if (topic === controllersUpdatedTopic) {
    requestStatus();
    return;
  }

  if (topic === gameQuestionTopic) {
    try {
      const q = JSON.parse(text);
      currentSessionId  = q.session_id || null;
      currentQuestionId = q.question_id || null;
      isQuestionActive  = true;
      currentGameState  = "QUESTION";
      clearPointsGained();
      setCorrectAnswerHighlight(null);
      setControllerMode("GAME");
      if (controllerAnswersEl) controllerAnswersEl.classList.remove("is-locked");
      setStatus("Frage aktiv — antworte!", "game");
      setHint("Wähle A, B, C oder D zum Antworten.");
      updateOledScreen();
    } catch (_err) {
      setStatus("Frage empfangen", "game");
    }
    return;
  }

  if (topic === gameStateTopic) {
    try {
      const s = JSON.parse(text);
      currentGameState = (s.state || "").toUpperCase() || currentGameState;

      if (currentGameState !== "QUESTION") isQuestionActive = false;

      if (currentGameState === "COUNTDOWN") {
        setControllerMode("LOBBY");
        setStatus("Countdown läuft...", "ready");
        setHint("Noch Bereitschaft änderbar — Not Ready bricht den Countdown ab.");
      } else if (currentGameState === "EVALUATION") {
        setControllerMode("GAME");
        setStatus("Auswertung", "game");
        setHint("Warte auf die nächste Frage...");
      } else if (currentGameState === "RESULTS") {
        setControllerMode("GAME");
        setStatus("Partie terminée", "game");
        setHint("En attente d'une nouvelle partie...");
      } else if (currentGameState === "LOBBY") {
        setControllerMode("LOBBY");
        isPlayerReady = false;
        totalPoints = 0.0;
        lastEvalKey = null;
        clearPointsGained();
        setCorrectAnswerHighlight(null);
        updateScreen();
        setStatus("Lobby — mach dich bereit!");
        requestStatus();
      }
      updateOledScreen();
    } catch (_err) {
      // Ungueltiges Payload ignorieren.
    }
    return;
  }

  if (topic === gameEvaluationTopic) {
    isQuestionActive = false;
    currentGameState = "EVALUATION";
    setControllerMode("GAME");
    setStatus("Auswertung", "game");
    setHint("Warte auf die nächste Frage...");
    try {
      const evalData = JSON.parse(text);
      const correctOption = (evalData.correct_option || "").toString().toUpperCase().trim();
      if (["A", "B", "C", "D"].includes(correctOption)) {
        setCorrectAnswerHighlight(correctOption);
      } else {
        setCorrectAnswerHighlight(null);
      }
      const evalKey = `${evalData.session_id || ""}:${evalData.question_id || ""}`;
      const results = Array.isArray(evalData.results) ? evalData.results : [];
      const playerNameNorm = (currentPlayerName || "").trim().toLowerCase();
      const myResult = results.find((r) => {
        const n = (r && r.player_name ? String(r.player_name) : "").trim().toLowerCase();
        return n.length > 0 && n === playerNameNorm;
      });
      if (myResult && evalKey !== lastEvalKey) {
        const delta = toScore(myResult.points_awarded);
        if (delta != null) {
          totalPoints += delta;
          setPointsGained(delta);
        } else {
          clearPointsGained();
        }
        lastEvalKey = evalKey;
      } else {
        clearPointsGained();
      }
    } catch (_err) {
      // Fehlerhaftes Payload ignorieren.
    }
    updateOledScreen();
  }
});

/* ─── MQTT ERROR/CLOSE ─── */
client.on("error", () => {
  setIndicator(false, "MQTT: ERROR");
});

client.on("close", () => {
  if (heartbeatIntervalId) {
    clearInterval(heartbeatIntervalId);
    heartbeatIntervalId = null;
  }
  setIndicator(false, "MQTT: DISCONNECTED");
  setStatus("Verbindung getrennt");
  updateOledScreen();
});
