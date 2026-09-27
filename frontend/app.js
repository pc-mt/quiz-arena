/* ═══════════════════════════════════════════════════════════════════════════
   DOM-REFERENZEN & GLOBALER STATE
   ═══════════════════════════════════════════════════════════════════════════ */
const leftSections = document.querySelectorAll("[data-left-section]");
const mainTabs = document.querySelectorAll("[data-main-tab]");
const mainSections = document.querySelectorAll("[data-main-section]");
const menuToggle = document.getElementById("menuToggle");
const sidebarEl = document.querySelector(".sidebar");
const sidebarOverlay = document.getElementById("sidebarOverlay");

const toggleSidebar = (forceClose = false) => {
  if (!sidebarEl) return;
  const open = forceClose ? false : !sidebarEl.classList.contains("is-open");
  sidebarEl.classList.toggle("is-open", open);
  if (sidebarOverlay) sidebarOverlay.classList.toggle("is-open", open);
};

menuToggle?.addEventListener("click", () => toggleSidebar());
sidebarOverlay?.addEventListener("click", () => toggleSidebar(true));

// Passwort-Sichtbarkeit fuer Login- und Register-Felder umschalten.
document.addEventListener("click", (e) => {
  const btn = e.target.closest("[data-password-toggle]");
  if (!btn) return;
  const wrap = btn.closest(".password-field-wrap");
  const input = wrap?.querySelector("input");
  if (!input) return;
  const isVisible = input.type === "text";
  input.type = isVisible ? "password" : "text";
  btn.classList.toggle("is-visible", !isVisible);
  btn.setAttribute("aria-label", isVisible ? "Passwort anzeigen" : "Passwort verbergen");
  btn.setAttribute("title", isVisible ? "Passwort anzeigen" : "Passwort verbergen");
});
const loginBtn = document.getElementById("loginBtn");
const registerBtn = document.getElementById("registerBtn");
const goToRegister = document.getElementById("goToRegister");
const backToLoginFromRegister = document.getElementById("backToLoginFromRegister");
const backToLogin = document.getElementById("backToLogin");
const loginUsername = document.getElementById("loginUsername");
const loginPassword = document.getElementById("loginPassword");
const registerUsername = document.getElementById("registerUsername");
const registerRfid = document.getElementById("registerRfid");
const registerPassword = document.getElementById("registerPassword");
const registerPasswordConfirm = document.getElementById("registerPasswordConfirm");
const loginMessage = document.getElementById("loginMessage");
const registerMessage = document.getElementById("registerMessage");
const registerUsernameHint = document.getElementById("registerUsernameHint");
const saveController = document.getElementById("saveController");
const updateRfid = document.getElementById("updateRfid");
const saveRfid = document.getElementById("saveRfid");
const deleteRfid = document.getElementById("deleteRfid");
const controllerMessage = document.getElementById("controllerMessage");
const refreshLobby = document.getElementById("refreshLobby");
const closeSession = document.getElementById("closeSession");
const gameStartBtn = document.getElementById("gameStartBtn");
const sessionInfo = document.getElementById("sessionInfo");
const activePlayersList = document.getElementById("activePlayersList");
const gameSection = document.querySelector('[data-main-section="game"]');
const gameQuestionTitle = gameSection ? gameSection.querySelector("h2") : null;
const gameQuestionMeta = gameSection ? gameSection.querySelector(".muted") : null;
const gameQuestionDetails = gameSection ? gameSection.querySelector(".tiny") : null;
const gameTimerEl = gameSection ? gameSection.querySelector(".timer") : null;
const countdownCircleWrap = document.getElementById("countdownCircleWrap");
const countdownCircleNumber = document.getElementById("countdownCircleNumber");
const countdownCircleProgress = document.getElementById("countdownCircleProgress");
const abortCountdownBtn = document.getElementById("abortCountdownBtn");
const addBotBtn = document.getElementById("addBotBtn");
const evaluationSection = document.querySelector('[data-main-section="evaluation"]');
const evaluationCard = evaluationSection ? evaluationSection.querySelector(".card") : null;
const scoreTabs = document.querySelectorAll(".score-tab");
const highscoreTableBody = document.getElementById("highscoreTableBody");
const highscorePodium = document.getElementById("highscorePodium");
const gameConfigMessage = document.getElementById("gameConfigMessage");
const questionSelectionInfo = document.getElementById("questionSelectionInfo");
const categoryCheckboxes = document.querySelectorAll('[data-category-checkbox]');
const difficultyCheckboxes = document.querySelectorAll('[data-difficulty-checkbox]');
const questionCountPills = document.querySelectorAll(
  '.main-section[data-main-section="lobby"] .segment-row .pill[data-round-length]'
);
const controllerList = document.getElementById("controllerList");
const controllerEmpty = document.getElementById("controllerEmpty");
const openWebController = document.getElementById("openWebController");

let selectedController = null;
let currentSession = null;
let lastKnownGameState = null;
let lobbyRefreshTimer = null;
let lastSessionSnapshot = null;
let lastLobbyStatusSnapshot = null;
let lastControllersSnapshot = null;
let lastCategoryCountsSnapshot = null;
let currentUserGearMenuOpen = false;
let currentHighscoreMode = "Q5";
let selectedRoundLength = "Q5";
let gameTotal = 5;
let localCountdownSeconds = null;
let localCountdownInterval = null;
/** Frage aus MQTT waehrend COUNTDOWN; wird erst bei 0 eingeblendet. */
let pendingQuestionPayload = null;
let gameMqttClient = null;

const BOT_CREDENTIALS = [
  { username: "bot1", password: "bot123", displayName: "Bot 1" },
  { username: "bot2", password: "bot123", displayName: "Bot 2" },
  { username: "bot3", password: "bot123", displayName: "Bot 3" },
  { username: "bot4", password: "bot123", displayName: "Bot 4" },
  { username: "bot5", password: "bot123", displayName: "Bot 5" },
];
const BOT_USERNAMES = BOT_CREDENTIALS.map(b => b.username.toLowerCase());

const BOT_DIFFICULTY_CONFIG = {
  LEICHT: {
    EASY:   { accuracy: 0.70, minTime: 8000,  maxTime: 22000 },
    MEDIUM: { accuracy: 0.50, minTime: 12000, maxTime: 26000 },
    HARD:   { accuracy: 0.30, minTime: 18000, maxTime: 30000 },
  },
  MITTEL: {
    EASY:   { accuracy: 0.85, minTime: 4000,  maxTime: 12000 },
    MEDIUM: { accuracy: 0.70, minTime: 6000,  maxTime: 16000 },
    HARD:   { accuracy: 0.55, minTime: 10000, maxTime: 22000 },
  },
  SCHWER: {
    EASY:   { accuracy: 0.95, minTime: 2000,  maxTime: 8000 },
    MEDIUM: { accuracy: 0.85, minTime: 4000,  maxTime: 10000 },
    HARD:   { accuracy: 0.75, minTime: 6000,  maxTime: 14000 },
  },
};

const activeBots = new Map();
let currentGameQuestion = null;
let evaluationCountdownInterval = null;
/** Fragenanzahl pro Kategorie aus der API (Quelle fuer die Gesamtanzahl). */
let categoryCountsByCategoryId = null;
const ROUND_LENGTH_LIMITS = { Q5: 5, Q10: 10, Q20: 20 };

/* ═══════════════════════════════════════════════════════════════════════════
   UI-BASIS (SEITENWECHSEL, COUNTER, MESSAGES)
   ═══════════════════════════════════════════════════════════════════════════ */
const updateQuestionCounter = (index) => {
  if (!gameQuestionMeta) return;
  gameQuestionMeta.innerHTML = index
    ? `Frage <strong>${index}</strong> / ${gameTotal}`
    : gameQuestionMeta.textContent;
  const bar = gameSection?.querySelector(".q-progress-bar");
  if (bar) bar.style.width = index ? `${(index / gameTotal) * 100}%` : "0%";
};

const setActive = (buttons, sections, targetName) => {
  buttons.forEach((button) => {
    button.classList.toggle(
      "is-active",
      button.dataset.leftTab === targetName || button.dataset.mainTab === targetName
    );
  });
  sections.forEach((section) => {
    section.classList.toggle(
      "is-active",
      section.dataset.leftSection === targetName || section.dataset.mainSection === targetName
    );
  });
};

/** Login-Felder und Login-Meldung zuruecksetzen. */
const clearLoginForm = () => {
  if (loginUsername) loginUsername.value = "";
  if (loginPassword) loginPassword.value = "";
  if (loginMessage) setMessage(loginMessage, "", "");
};

// Register-Form resetten
/** Register-Felder und Register-Meldung zuruecksetzen. */
const clearRegisterForm = () => {
  if (registerUsername) registerUsername.value = "";
  if (registerPassword) registerPassword.value = "";
  if (registerPasswordConfirm) registerPasswordConfirm.value = "";
  if (registerRfid) registerRfid.value = "";
  if (registerMessage) setMessage(registerMessage, "", "");
  if (registerUsernameHint) registerUsernameHint.classList.remove("is-error");
};

// Linke Sidebar-Ansicht umschalten
const setLeftSection = (targetName) => {
  leftSections.forEach((section) => {
    section.classList.toggle(
      "is-active",
      section.dataset.leftSection === targetName
    );
  });
  if (targetName === "login") {
    clearLoginForm();
  } else if (targetName === "register") {
    clearRegisterForm();
  } else if (targetName === "controller") {
    loadAvailableControllers();
  }
};

// Hauptbereich (Tabs oben) umschalten
const activateMainTab = (targetName) => {
  setActive(mainTabs, mainSections, targetName);
  if (targetName === "highscores") {
    loadHighscores(currentHighscoreMode);
  }
};

goToRegister?.addEventListener("click", () => {
  setLeftSection("register");
});

backToLoginFromRegister?.addEventListener("click", () => {
  setLeftSection("login");
});

registerUsername?.addEventListener("input", () => {
  if (registerUsernameHint && (registerUsername?.value?.trim() || "").length >= 3) {
    registerUsernameHint.classList.remove("is-error");
  }
});

backToLogin?.addEventListener("click", () => {
  toggleSidebar(true);
  setLeftSection("login");
});

mainTabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    if (tab.dataset.mainTab === "controller") {
      openWebControllerPage();
      return;
    }
    activateMainTab(tab.dataset.mainTab);
  });
});

const setMessage = (el, text, type) => {
  if (!el) return;
  el.textContent = text || "";
  el.classList.toggle("is-success", type === "success");
  el.classList.toggle("is-error", type === "error");
};

/** Timeout-Handles fuer automatisch ausblendende Erfolgsmeldungen. */
const messageAutoClearTimeouts = new Map();
const FADE_OUT_MS = 400;
const SUCCESS_MESSAGE_DURATION_MS = 5000;

/** Setzt eine Erfolgsmeldung und blendet sie danach automatisch aus. */
const setMessageAutoClear = (el, text, type, durationMs = SUCCESS_MESSAGE_DURATION_MS) => {
  if (!el) return;
  const existing = messageAutoClearTimeouts.get(el);
  if (existing) clearTimeout(existing);
  setMessage(el, text, type);
  const timeoutId = setTimeout(() => {
    messageAutoClearTimeouts.delete(el);
    el.classList.add("form-message--fade-out");
    setTimeout(() => {
      setMessage(el, "", "");
      el.classList.remove("form-message--fade-out");
    }, FADE_OUT_MS);
  }, durationMs);
  messageAutoClearTimeouts.set(el, timeoutId);
};

/* ═══════════════════════════════════════════════════════════════════════════
   LOBBY-KONFIGURATION (KATEGORIEN, SCHWIERIGKEITEN, START-CHECKS)
   ═══════════════════════════════════════════════════════════════════════════ */
const hasAnyDifficultySelected = () =>
  Array.from(difficultyCheckboxes).some((checkbox) => checkbox.checked);

const getSelectedDifficulties = () =>
  Array.from(difficultyCheckboxes)
    .filter((checkbox) => checkbox.checked)
    .map((checkbox) => checkbox.dataset.difficulty);

const getSelectedDifficultyLabels = () =>
  getSelectedDifficulties().map((d) => d.toUpperCase());

const getSelectedCategoryIds = () =>
  Array.from(categoryCheckboxes)
    .filter((checkbox) => checkbox.checked)
    .map((checkbox) => Number.parseInt(checkbox.dataset.categoryId || "", 10))
    .filter((id) => !Number.isNaN(id) && id > 0);

const getCategoryQuestionCount = (checkbox, difficulties) => {
  const id = Number.parseInt(checkbox.dataset.categoryId || "", 10);
  if (!Number.isNaN(id) && categoryCountsByCategoryId && categoryCountsByCategoryId[id]) {
    return difficulties.reduce((sum, difficulty) => {
      const value = Number(categoryCountsByCategoryId[id][difficulty]) || 0;
      return sum + (Number.isNaN(value) ? 0 : value);
    }, 0);
  }
  return difficulties.reduce((sum, difficulty) => {
    const value = Number.parseInt(checkbox.dataset[difficulty] || "0", 10);
    return sum + (Number.isNaN(value) ? 0 : value);
  }, 0);
};

const getSelectedQuestionsTotal = (difficulties = getSelectedDifficulties()) =>
  Array.from(document.querySelectorAll("[data-category-checkbox]"))
    .filter((checkbox) => checkbox.checked)
    .reduce((sum, checkbox) => sum + getCategoryQuestionCount(checkbox, difficulties), 0);

const getRoundLimit = (roundLength = selectedRoundLength) =>
  ROUND_LENGTH_LIMITS[roundLength] || 5;

/** Prueft, ob die aktuelle Lobby-Konfiguration zum Starten ausreicht. */
const isGameConfigValid = () => {
  const categories = getSelectedCategoryIds();
  const difficulties = getSelectedDifficultyLabels();
  if (!categories.length || !difficulties.length) return false;
  const total = getSelectedQuestionsTotal();
  const limit = getRoundLimit();
  return total >= limit;
};

/** Speichert die Spielkonfiguration und startet danach die Partie. */
// Start-Flow: config -> start -> Spiel-Tab
const doStartGame = async () => {
  const token = getAuthToken();
  if (!token) {
    setMessage(gameConfigMessage, "Please login first.", "error");
    return;
  }
  const categories = getSelectedCategoryIds();
  const difficulties = getSelectedDifficultyLabels();
  if (!categories.length) {
    setMessage(gameConfigMessage, "Please select at least one category.", "error");
    return;
  }
  if (!difficulties.length) {
    setMessage(gameConfigMessage, "Please select at least one difficulty level.", "error");
    return;
  }
  const total = getSelectedQuestionsTotal();
  const limit = getRoundLimit();
  if (total < limit) {
    setMessage(
      gameConfigMessage,
      `Not enough questions for the selected configuration (${total} selected, ${limit} required). Add more categories or difficulties.`,
      "error"
    );
    return;
  }
  setMessage(gameConfigMessage, "Starting game...", "");
  try {
    await fetchJson("/api/game/config", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${normalizeToken(token)}`,
      },
      body: JSON.stringify({
        mode: selectedRoundLength,
        categories,
        difficulties,
      }),
    });
    await fetchJson("/api/game/start", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${normalizeToken(token)}`,
      },
    });
    setMessage(gameConfigMessage, "Countdown started.", "success");
    lastKnownGameState = "COUNTDOWN";
    activateMainTab("game");
    await loadSession();
  } catch (error) {
    await loadSession();
    const startedStates = ["COUNTDOWN", "QUESTION", "EVALUATION"];
    const currentState = (currentSession?.state || "").toUpperCase();
    if (startedStates.includes(currentState)) {
      activateMainTab("game");
      setMessage(
        gameConfigMessage,
        "Game started, but request response was delayed. Switched to Spiel.",
        "success"
      );
      return;
    }
    setMessage(gameConfigMessage, error.message || "Could not start game.", "error");
  }
};

const refreshQuestionSelectionInfo = () => {
  if (!questionSelectionInfo) return;
  const total = getSelectedQuestionsTotal();
  const limit = getRoundLimit();
  questionSelectionInfo.innerHTML = `<span class="q-count-num">${total}</span> / ${limit} Fragen verfügbar`;
};

/* ═══════════════════════════════════════════════════════════════════════════
   AUTH-HELPER (TOKEN, FETCH-WRAPPER)
   ═══════════════════════════════════════════════════════════════════════════ */
const normalizeToken = (raw) => {
  if (!raw || typeof raw !== "string") return "";
  let token = raw.trim();
  if (token.startsWith("Bearer ")) {
    token = token.slice(7).trim();
  }
  // Falls der Token mit Anfuehrungszeichen gespeichert wurde, hier bereinigen.
  token = token.replace(/^"+|"+$/g, "");
  return token;
};

const getAuthToken = () => normalizeToken(localStorage.getItem("authToken"));
const setAuthToken = (token) => localStorage.setItem("authToken", normalizeToken(token));
const clearAuthToken = () => {
  localStorage.removeItem("authToken");
  localStorage.removeItem("currentUserId");
};
const getCurrentUserId = () => {
  const id = localStorage.getItem("currentUserId");
  return id != null && id !== "" ? id : null;
};
const setCurrentUserId = (id) => {
  if (id != null) localStorage.setItem("currentUserId", String(id));
  else localStorage.removeItem("currentUserId");
};

const fetchJson = async (url, options = {}) => {
  const token = getAuthToken();
  const headers = {
    ...(options.headers || {}),
  };
  if (token && !headers.Authorization) {
    headers.Authorization = `Bearer ${normalizeToken(token)}`;
  }
  const fetchOptions = {
    cache: "no-store",
    ...options,
    headers,
  };
  const response = await fetch(url, fetchOptions);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(data.error || "Anfrage fehlgeschlagen");
  }
  return data;
};

const AVATAR_COLORS = ["av-blue", "av-sky", "av-indigo", "av-green", "av-gold", "av-rose"];

const GEAR_SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>';

const closeOpenGearMenus = () => {
  document.querySelectorAll(".player-gear-menu.is-open").forEach((m) => m.classList.remove("is-open"));
  currentUserGearMenuOpen = false;
};

// Zahnrad-Menue schliessen bei Klick ausserhalb oder mit Escape.
document.addEventListener("click", (e) => {
  if (e.target.closest(".player-gear-menu") || e.target.closest(".player-avatar-gear")) return;
  closeOpenGearMenus();
});
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") closeOpenGearMenus();
});

/* ═══════════════════════════════════════════════════════════════════════════
   PROFIL-BEARBEITUNG (MODAL + AVATAR-AUSWAHL)
   ═══════════════════════════════════════════════════════════════════════════ */
const editProfileOverlay = document.getElementById("editProfileOverlay");
const editProfileDisplayNameInput = document.getElementById("editProfileDisplayName");
const editProfilePencilBtn = document.getElementById("editProfilePencil");
const editProfileSaveBtn = document.getElementById("editProfileSave");
const editProfileAvatarImg = document.getElementById("editProfileAvatarImg");
const editProfileAvatarInitials = document.getElementById("editProfileAvatarInitials");
const editProfileGallery = document.getElementById("editProfileGallery");

const AVATAR_OPTIONS = [
  "Avatar/avatar_1.png", "Avatar/avatar_2.png", "Avatar/avatar_3.png", "Avatar/avatar_4.png",
  "Avatar/avatar_5.png", "Avatar/avatar_6.png", "Avatar/avatar_7.png", "Avatar/avatar_8.png",
  "Avatar/avatar_9.png", "Avatar/avatar_10.png",
];

let editProfileSelectedPhotoUrl = null;
let editProfileTargetUserId = null;

const openEditProfileModal = (targetUserId, currentDisplayName, currentProfilePhotoUrl = null) => {
  if (!editProfileOverlay || !editProfileDisplayNameInput) return;
  editProfileTargetUserId = targetUserId != null ? String(targetUserId) : null;
  editProfileDisplayNameInput.value = currentDisplayName || "";
  editProfileDisplayNameInput.readOnly = true;
  editProfileSelectedPhotoUrl = currentProfilePhotoUrl || null;

  if (editProfileAvatarImg && editProfileAvatarInitials) {
    if (editProfileSelectedPhotoUrl) {
      editProfileAvatarImg.src = editProfileSelectedPhotoUrl;
      editProfileAvatarImg.alt = "Profilfoto";
      editProfileAvatarImg.classList.add("is-visible");
      editProfileAvatarInitials.classList.remove("is-visible");
      editProfileAvatarInitials.textContent = "";
    } else {
      editProfileAvatarImg.src = "";
      editProfileAvatarImg.classList.remove("is-visible");
      const initials = (currentDisplayName || "").trim().substring(0, 2).toUpperCase() || "?";
      editProfileAvatarInitials.textContent = initials;
      editProfileAvatarInitials.classList.add("is-visible");
    }
  }

  if (editProfileGallery) {
    editProfileGallery.innerHTML = "";
    AVATAR_OPTIONS.forEach((path) => {
      const item = document.createElement("button");
      item.type = "button";
      item.className = "edit-profile-gallery-item" + (path === editProfileSelectedPhotoUrl ? " is-selected" : "");
      item.dataset.avatarPath = path;
      const img = document.createElement("img");
      img.src = path;
      img.alt = "";
      img.onerror = () => { item.classList.add("no-img"); };
      item.appendChild(img);
      item.addEventListener("click", () => {
        editProfileGallery.querySelectorAll(".edit-profile-gallery-item").forEach((el) => el.classList.remove("is-selected"));
        item.classList.add("is-selected");
        editProfileSelectedPhotoUrl = path;
        if (editProfileAvatarImg && editProfileAvatarInitials) {
          editProfileAvatarImg.src = path;
          editProfileAvatarImg.classList.add("is-visible");
          editProfileAvatarInitials.classList.remove("is-visible");
          editProfileAvatarInitials.textContent = "";
        }
      });
      editProfileGallery.appendChild(item);
    });
  }

  editProfileOverlay.classList.add("is-open");
  editProfileOverlay.setAttribute("aria-hidden", "false");
};

const closeEditProfileModal = () => {
  if (!editProfileOverlay) return;
  editProfileOverlay.classList.remove("is-open");
  editProfileOverlay.setAttribute("aria-hidden", "true");
  if (editProfileDisplayNameInput) editProfileDisplayNameInput.readOnly = true;
  editProfileSelectedPhotoUrl = null;
  editProfileTargetUserId = null;
};

editProfilePencilBtn?.addEventListener("click", () => {
  if (!editProfileDisplayNameInput) return;
  editProfileDisplayNameInput.readOnly = !editProfileDisplayNameInput.readOnly;
  if (!editProfileDisplayNameInput.readOnly) editProfileDisplayNameInput.focus();
});

editProfileOverlay?.addEventListener("click", (e) => {
  if (e.target === editProfileOverlay) closeEditProfileModal();
});

editProfileSaveBtn?.addEventListener("click", async () => {
  if (!editProfileTargetUserId) {
    closeEditProfileModal();
    return;
  }
  const displayName = editProfileDisplayNameInput?.value?.trim() ?? "";
  try {
    await fetchJson(`/api/auth/profile/${encodeURIComponent(editProfileTargetUserId)}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        display_name: displayName,
        profile_photo_url: editProfileSelectedPhotoUrl || "",
      }),
    });
    closeEditProfileModal();
    await loadLobbyStatus();
    setMessage(loginMessage, "Profil wurde gespeichert.", "success");
  } catch (err) {
    setMessage(loginMessage, err.message || "Speichern fehlgeschlagen.", "error");
  }
});

/* ═══════════════════════════════════════════════════════════════════════════
   SPIELERLISTE RENDERN (LOBBY)
   ═══════════════════════════════════════════════════════════════════════════ */
const renderPlayers = (players, hostUserId = null, currentUserId = null) => {
  if (!activePlayersList) return;
  if (document.querySelector(".player-gear-menu.is-open")) currentUserGearMenuOpen = true;
  activePlayersList.innerHTML = "";

  if (!players || players.length === 0) {
    currentUserGearMenuOpen = false;
    const empty = document.createElement("div");
    empty.className = "muted";
    empty.style.padding = "12px 0";
    empty.textContent = "Noch keine aktiven Spieler.";
    activePlayersList.appendChild(empty);
    return;
  }

  players.forEach((player, index) => {
    const item = document.createElement("div");
    item.className = "player-item";

    const playerName = player.display_name || player.username || "Unbekannt";
    const isBot = BOT_USERNAMES.includes((player.username || "").toLowerCase());
    
    const avatar = document.createElement("div");
    avatar.className = `player-avatar ${AVATAR_COLORS[index % AVATAR_COLORS.length]}`;
    
    const avatarUrl = isBot ? "Avatar/bot.jpeg" : player.profile_photo_url;
    if (avatarUrl) {
      const img = document.createElement("img");
      img.src = avatarUrl;
      img.alt = playerName;
      img.className = "player-avatar-img";
      avatar.appendChild(img);
    } else {
      avatar.textContent = playerName.substring(0, 2).toUpperCase();
    }

    const isCurrentUser =
      currentUserId != null && String(player.user_id) === String(currentUserId);
    const botState = isBot ? activeBots.get(player.username.toLowerCase()) : null;
    const currentBotDifficulty = botState?.difficulty || "MITTEL";

    const wrap = document.createElement("div");
    wrap.className = "player-avatar-wrap" + (index === 0 ? " player-gear-menu-up" : "");
    const gearBtn = document.createElement("button");
    gearBtn.type = "button";
    gearBtn.className = "player-avatar-gear";
    gearBtn.innerHTML = GEAR_SVG;
    gearBtn.setAttribute("aria-label", "Einstellungen");
    const menu = document.createElement("div");
    menu.className = "player-gear-menu";
    
    if (isBot) {
      menu.innerHTML = `
        <div class="bot-difficulty-label">Schwierigkeit</div>
        <label class="bot-difficulty-option">
          <input type="radio" name="bot-diff-${player.user_id}" value="LEICHT" ${currentBotDifficulty === "LEICHT" ? "checked" : ""} />
          <span>Leicht</span>
        </label>
        <label class="bot-difficulty-option">
          <input type="radio" name="bot-diff-${player.user_id}" value="MITTEL" ${currentBotDifficulty === "MITTEL" ? "checked" : ""} />
          <span>Mittel</span>
        </label>
        <label class="bot-difficulty-option">
          <input type="radio" name="bot-diff-${player.user_id}" value="SCHWER" ${currentBotDifficulty === "SCHWER" ? "checked" : ""} />
          <span>Schwer</span>
        </label>
        <div class="bot-menu-divider"></div>
        <button type="button" class="player-gear-menu-item player-gear-menu-item--red" data-action="abmelden">Abmelden</button>
      `;
    } else {
      menu.innerHTML = `
        <button type="button" class="player-gear-menu-item player-gear-menu-item--blue" data-action="edit-profile">Edit profile</button>
        <button type="button" class="player-gear-menu-item player-gear-menu-item--red" data-action="abmelden">Abmelden</button>
      `;
    }

    gearBtn.addEventListener("click", (e) => {
      e.stopPropagation();
      closeOpenGearMenus();
      menu.classList.toggle("is-open");
      currentUserGearMenuOpen = menu.classList.contains("is-open");
    });

    if (isBot) {
      menu.querySelectorAll('input[type="radio"]').forEach(radio => {
        radio.addEventListener("change", async (e) => {
          const newDifficulty = e.target.value;
          const bs = activeBots.get(player.username.toLowerCase());
          if (bs) {
            bs.difficulty = newDifficulty;
            console.log(`[BOT] ${player.username} difficulty changed to ${newDifficulty}`);
            menu.classList.remove("is-open");
            currentUserGearMenuOpen = false;
            await loadLobbyStatus();
          }
        });
      });
    }

    menu.addEventListener("click", async (e) => {
      e.stopPropagation();
      const action = e.target.closest("[data-action]")?.dataset?.action;
      const isSelf = isCurrentUser;
      if (action === "edit-profile" && !isBot) {
        menu.classList.remove("is-open");
        currentUserGearMenuOpen = false;
        const targetUserId = player.user_id ?? null;
        const currentDisplayName = player.display_name ?? player.username ?? "";
        const currentPhotoUrl = player.profile_photo_url || null;
        openEditProfileModal(targetUserId, currentDisplayName, currentPhotoUrl);
      } else if (action === "abmelden") {
        menu.classList.remove("is-open");
        currentUserGearMenuOpen = false;
        
        if (isBot) {
          const bs = activeBots.get(player.username.toLowerCase());
          if (bs?.odeurClient) {
            bs.odeurClient.end();
          }
          activeBots.delete(player.username.toLowerCase());
          try {
            await fetchJson("/api/lobby/kick", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ user_id: player.user_id }),
            });
          } catch (_e) {}
          await loadLobbyStatus();
          return;
        }

        const token = getAuthToken();
        if (!token) {
          setMessage(loginMessage, "Nicht angemeldet.", "error");
          setLeftSection("login");
          return;
        }
        try {
          if (isSelf) {
            await fetchJson("/api/lobby/leave", { method: "POST" });
            clearAuthToken();
            setCurrentUserId(null);
            setLeftSection("login");
            clearLoginForm();
          } else {
            await fetchJson("/api/lobby/kick", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ user_id: player.user_id }),
            });
          }
          await loadSession();
          await loadLobbyStatus();
          setMessage(loginMessage, "Erfolgreich abgemeldet.", "success");
        } catch (err) {
          setMessage(
            loginMessage,
            err.message || "Abmeldung fehlgeschlagen.",
            "error"
          );
        }
      }
    });
    if (currentUserGearMenuOpen && isCurrentUser) {
      menu.classList.add("is-open");
      currentUserGearMenuOpen = false;
    }
    wrap.appendChild(avatar);
    wrap.appendChild(gearBtn);
    wrap.appendChild(menu);

    const info = document.createElement("div");
    info.style.flex = "1";
    const nameEl = document.createElement("div");
    nameEl.className = "player-name";
    
    if (isBot) {
      const nameText = document.createElement("span");
      nameText.textContent = playerName;
      nameEl.appendChild(nameText);
      
      const diffBadgeInline = document.createElement("span");
      diffBadgeInline.className = `bot-difficulty-inline bot-diff-${currentBotDifficulty.toLowerCase()}`;
      diffBadgeInline.textContent = currentBotDifficulty === "LEICHT" ? "Leicht" 
                                  : currentBotDifficulty === "SCHWER" ? "Schwer" 
                                  : "Mittel";
      nameEl.appendChild(diffBadgeInline);
    } else {
      nameEl.textContent = hostUserId != null && player.user_id === hostUserId
        ? `${playerName} (HOST)`
        : playerName;
    }
    const ctrlEl = document.createElement("div");
    ctrlEl.className = "player-ctrl";
    const controllerType = player.controller_type || "WEB";
    const controllerId = player.controller_id || "—";
    ctrlEl.textContent = `${controllerType}-Controller (${controllerId})`;
    info.appendChild(nameEl);
    info.appendChild(ctrlEl);

    const controllerStatus = (player.controller_status || "").toUpperCase();
    const badge = document.createElement("span");
    if (controllerStatus === "OFFLINE") {
      badge.className = "offline-badge";
      badge.textContent = "Offline";
    } else if (player.is_ready) {
      badge.className = "ready-badge";
      badge.textContent = "✓ Bereit";
    } else {
      badge.className = "waiting-badge";
      badge.textContent = "⏳ Wartend";
    }

    item.appendChild(wrap);
    item.appendChild(info);
    item.appendChild(badge);
    activePlayersList.appendChild(item);
  });
};

/* ═══════════════════════════════════════════════════════════════════════════
   SPIELANSICHT (COUNTDOWN, FRAGE, AUSWERTUNG, ENDERGEBNIS)
   ═══════════════════════════════════════════════════════════════════════════ */
const stopLocalCountdown = () => {
  if (localCountdownInterval) {
    window.clearInterval(localCountdownInterval);
    localCountdownInterval = null;
  }
  localCountdownSeconds = null;
  pendingQuestionPayload = null;
  if (countdownCircleWrap) {
    countdownCircleWrap.classList.remove("is-visible");
    countdownCircleWrap.setAttribute("aria-hidden", "true");
  }
  if (countdownCircleProgress) countdownCircleProgress.classList.remove("is-running");
};

/** Countdown 3 -> 2 -> 1 als einzelner Kreis mit zentraler Zahl. */
const startLocalCountdown = () => {
  if (localCountdownInterval) return;
  localCountdownSeconds = 3;
  if (countdownCircleWrap) {
    countdownCircleWrap.classList.add("is-visible");
    countdownCircleWrap.setAttribute("aria-hidden", "false");
  }
  if (countdownCircleNumber) countdownCircleNumber.textContent = "3";
  if (gameTimerEl) gameTimerEl.textContent = "⏱ 3";
  if (countdownCircleProgress) {
    countdownCircleProgress.classList.remove("is-running");
    countdownCircleProgress.offsetHeight;
    countdownCircleProgress.classList.add("is-running");
  }

  localCountdownInterval = window.setInterval(() => {
    localCountdownSeconds -= 1;
    if (localCountdownSeconds <= 0) {
      if (localCountdownInterval) {
        window.clearInterval(localCountdownInterval);
        localCountdownInterval = null;
      }
      localCountdownSeconds = null;
      if (countdownCircleWrap) {
        countdownCircleWrap.classList.remove("is-visible");
        countdownCircleWrap.setAttribute("aria-hidden", "true");
      }
      if (countdownCircleProgress) countdownCircleProgress.classList.remove("is-running");
      const pending = pendingQuestionPayload;
      pendingQuestionPayload = null;
      if (pending) renderGameQuestion(pending);
      return;
    }
    if (countdownCircleNumber) countdownCircleNumber.textContent = String(localCountdownSeconds);
    if (gameTimerEl) gameTimerEl.textContent = `⏱ ${localCountdownSeconds}`;
  }, 1000);
};

const renderGameQuestion = (payload) => {
  if (!payload || !gameSection) return;
  const answers = payload.answers || {};
  currentGameQuestion = payload;
  stopLocalCountdown();
  activateMainTab("game");

  updateQuestionCounter(payload.question_index || 1);
  if (gameQuestionTitle) {
    gameQuestionTitle.textContent = payload.text || "Frage läuft";
  }
  if (gameQuestionDetails) {
    const category = payload.category || "Unbekannt";
    const difficulty = payload.difficulty || "UNKNOWN";
    gameQuestionDetails.innerHTML = `<span class="q-tag cat">${category}</span><span class="q-tag diff">${difficulty}</span>`;
  }
  if (gameTimerEl) {
    const duration = Number(payload.duration_sec) || 30;
    gameTimerEl.textContent = `⏱ ${duration} Sekunden`;
  }

  const setAnswerBtn = (selector, key, text) => {
    const el = gameSection.querySelector(selector);
    if (!el) return;
    el.innerHTML = `<div class="a-key">${key}</div><span>${text || "—"}</span>`;
  };
  setAnswerBtn(".answer.a", "A", answers.A);
  setAnswerBtn(".answer.b", "B", answers.B);
  setAnswerBtn(".answer.c", "C", answers.C);
  setAnswerBtn(".answer.d", "D", answers.D);
};

const stopEvaluationCountdown = () => {
  if (evaluationCountdownInterval != null) {
    clearInterval(evaluationCountdownInterval);
    evaluationCountdownInterval = null;
  }
};

const renderEvaluationView = (payload = {}) => {
  if (!evaluationCard) return;
  stopEvaluationCountdown();

  const correctLetter = payload.correct_option || "—";
  const correctText = payload.correct_answer_text || null;
  const correctFull =
    payload.correct_answer_full ||
    (correctLetter && correctText ? `${correctLetter} - ${correctText}` : correctLetter);
  const results = Array.isArray(payload.results) ? payload.results : [];
  const formatResponseTime = (ms) => {
    if (ms == null || ms === "") return "—";
    const sec = Number(ms) / 1000;
    return Number.isNaN(sec) ? "—" : `${sec.toFixed(1).replace(".", ",")} s`;
  };

  const formatFactor = (f) => {
    if (f == null || f === "") return "—";
    const n = Number(f);
    return Number.isNaN(n) ? "—" : n.toFixed(2).replace(".", ",");
  };

  const rowsHtml = results.length
    ? results
      .map((row, idx) => {
        const name = row.player_name || "Unbekannt";
        const answered = row.answered_option ? `Antwort: ${row.answered_option}` : "Keine Antwort";
        const isCorrect = !!row.is_correct;
        const points = Number(row.points_awarded || 0).toFixed(2);
        const timeStr = formatResponseTime(row.response_time_ms);
        const baseStr = row.base_points != null ? String(row.base_points) : "—";
        const factorStr = formatFactor(row.time_factor);
        const ptsClass = isCorrect ? "pts-ok" : "pts-none";
        const ptsText = isCorrect ? `+${points} Pkt` : "0 Pkt";
        const rankClass = idx === 0 ? "r1" : idx === 1 ? "r2" : idx === 2 ? "r3" : "";
        const initials = name.substring(0, 2).toUpperCase();
        const avColor = AVATAR_COLORS[idx % AVATAR_COLORS.length];
        return `
          <div class="result-row">
            <div class="result-rank ${rankClass}">${idx + 1}</div>
            <div class="player-avatar ${avColor}" style="width:32px;height:32px;border-radius:8px;font-size:13px;">${initials}</div>
            <div class="result-info">
              <div class="result-name">${name}</div>
              <div class="result-answer">${answered}</div>
              <div class="result-detail">Zeit: ${timeStr} · Basis: ${baseStr} · Faktor: ${factorStr}</div>
            </div>
            <div class="result-pts ${ptsClass}">${ptsText}</div>
          </div>
        `;
      })
      .join("")
    : `<div class="result-row"><div class="result-info"><div class="result-name">Keine Antworten</div><div class="result-answer">Für diese Frage liegen noch keine Daten vor.</div></div><div class="result-pts pts-none">0 Pkt</div></div>`;

  evaluationCard.innerHTML = `
    <div class="card-title" style="margin-bottom:20px;">
      <div class="ct-icon blue">📊</div>
      Auswertung
    </div>
    <div class="eval-correct-flag">
      <div class="ecf-label">✓ Richtige Antwort</div>
      <div class="ecf-answer">${correctFull}</div>
    </div>
    <div class="result-list">
      ${rowsHtml}
    </div>
    <p class="evaluation-countdown" id="evaluationCountdownEl">Weiter in 3 Sekunden …</p>
  `;

  const countdownEl = document.getElementById("evaluationCountdownEl");
  let secondsLeft = 3;
  const updateCountdownText = () => {
    if (secondsLeft <= 0) {
      stopEvaluationCountdown();
      if (countdownEl) countdownEl.textContent = "Weiterleitung...";
      return;
    }
    if (countdownEl) {
      countdownEl.textContent = secondsLeft === 1 ? "Weiter in 1 Sekunde" : `Weiter in ${secondsLeft} Sekunden`;
    }
    secondsLeft--;
  };
  updateCountdownText();
  evaluationCountdownInterval = setInterval(updateCountdownText, 1000);
};

const renderFinalResults = (results) => {
  currentGameQuestion = null;
  stopLocalCountdown();
  stopEvaluationCountdown();
  activateMainTab("evaluation");
  if (!evaluationCard) return;

  const initials = (name) => (name || "??").slice(0, 2).toUpperCase();

  const podiumOrder = [1, 0, 2];
  const podiumHtml = Array.isArray(results) && results.length >= 2
    ? `<div class="podium">${podiumOrder.map(idx => {
        const r = results[idx];
        if (!r) return "";
        const cls = idx === 0 ? "p1" : idx === 1 ? "p2" : "p3";
        const pts = Number(r.total_points || 0).toFixed(2).replace(".", ",");
        const name = r.player_name || "Unbekannt";
        return `<div class="podium-item ${cls}">
          <div class="podium-avatar">${initials(name)}</div>
          <div class="podium-name">${name}</div>
          <div class="podium-score">${pts}</div>
          <div class="podium-block"></div>
          <div class="podium-rank">#${idx + 1}</div>
        </div>`;
      }).join("")}</div>`
    : "";

  const rows = Array.isArray(results) && results.length
    ? results.map((r, i) => {
        const rankClass = i === 0 ? "rank-1" : i === 1 ? "rank-2" : i === 2 ? "rank-3" : "";
        const pts = Number(r.total_points || 0).toFixed(2).replace(".", ",");
        const correct = r.correct_count || 0;
        const answered = r.answered_count || 0;
        const timeSec = (Number(r.total_response_time_ms || 0) / 1000).toFixed(1).replace(".", ",");
        const name = r.player_name || "Unbekannt";
        return `<tr class="${rankClass}">
          <td><div class="rank-pill">${i + 1}</div></td>
          <td>${name}</td>
          <td>${correct} / ${answered}</td>
          <td>${timeSec}s</td>
          <td><strong>${pts}</strong></td>
        </tr>`;
      }).join("")
    : `<tr><td colspan="5" style="text-align:center;">Keine Ergebnisse</td></tr>`;

  evaluationCard.innerHTML = `
    <div class="final-results-header">
      <span class="final-trophy">🏆</span>
      <h2>Endergebnis</h2>
    </div>
    ${podiumHtml}
    <table class="hs-table">
      <thead>
        <tr>
          <th>Rang</th>
          <th>Spieler</th>
          <th>Richtig</th>
          <th>Zeit</th>
          <th>Punkte</th>
        </tr>
      </thead>
      <tbody>${rows}</tbody>
    </table>
    <div style="margin-top:20px; text-align:center; display:flex; gap:12px; justify-content:center; flex-wrap:wrap;">
      <button class="btn btn-primary" id="restartGameBtn" style="width:auto; padding:12px 32px;">Neues Spiel</button>
      <button class="btn" id="closeSessionBtn" style="width:auto; padding:12px 32px; background:#c0392b; color:#fff;">Session schließen</button>
    </div>
  `;

  const restartBtn = document.getElementById("restartGameBtn");
  if (restartBtn) restartBtn.addEventListener("click", async () => {
    restartBtn.disabled = true;
    restartBtn.textContent = "Wird neu gestartet...";
    try {
      await fetchJson("/api/game/restart", { method: "POST" });
      if (gameConfigMessage) setMessage(gameConfigMessage, "", "");
      await loadSession();
      await loadLobbyStatus();
      activateMainTab("lobby");
    } catch (err) {
      // Bei Fehler den Button-Zustand nicht veraendern.
    } finally {
      restartBtn.disabled = false;
      restartBtn.textContent = "Neues Spiel";
    }
  });

  const closeBtn = document.getElementById("closeSessionBtn");
  if (closeBtn) closeBtn.addEventListener("click", async () => {
    closeBtn.disabled = true;
    try {
      await fetchJson("/api/lobby/reset", { method: "POST" });
      location.reload();
    } catch (err) {
      closeBtn.disabled = false;
    }
  });
};

const updateGameViewBySessionState = (state) => {
  const normalizedState = (state || "").toUpperCase();
  if (!gameQuestionTitle || !gameTimerEl || !gameQuestionMeta) return;

  if (normalizedState === "COUNTDOWN") {
    currentGameQuestion = null;
    stopEvaluationCountdown();
    activateMainTab("game");
    gameQuestionTitle.textContent = "Spiel startet...";
    gameQuestionMeta.textContent = "Countdown läuft";
    if (gameQuestionDetails) gameQuestionDetails.textContent = "";
    gameTotal = getRoundLimit(currentSession?.round_length || selectedRoundLength);
    updateQuestionCounter(0);
    startLocalCountdown();
    if (abortCountdownBtn) abortCountdownBtn.style.display = "block";
    return;
  }

  if (normalizedState === "QUESTION") {
    if (abortCountdownBtn) abortCountdownBtn.style.display = "none";
    stopEvaluationCountdown();
    activateMainTab("game");
    if (!currentGameQuestion) {
      gameQuestionTitle.textContent = "Frage läuft…";
      gameQuestionMeta.textContent = "Antworten über Controller";
      if (gameQuestionDetails) gameQuestionDetails.textContent = "";
      gameTimerEl.textContent = "⏱ 30 Sekunden";
    }
    return;
  }

  if (normalizedState === "EVALUATION") {
    if (abortCountdownBtn) abortCountdownBtn.style.display = "none";
    currentGameQuestion = null;
    stopLocalCountdown();
    activateMainTab("evaluation");
    // Evaluation-Inhalt nur aus game.evaluation uebernehmen, nicht hier ueberschreiben.
    return;
  }

  if (normalizedState === "RESULTS") {
    if (abortCountdownBtn) abortCountdownBtn.style.display = "none";
    currentGameQuestion = null;
    stopLocalCountdown();
    return;
  }

  currentGameQuestion = null;
  stopLocalCountdown();
  if (abortCountdownBtn) abortCountdownBtn.style.display = "none";
  gameQuestionTitle.textContent = "Warte auf Spielstart";
  gameQuestionMeta.textContent = "Drücke \"Spiel starten\" im Lobby-Bereich";
  if (gameQuestionDetails) gameQuestionDetails.textContent = "";
  gameTimerEl.textContent = "⏱ --";
};

/* ═══════════════════════════════════════════════════════════════════════════
   MQTT-LISTENER (SPIEL- UND LOBBY-EVENTS)
   ═══════════════════════════════════════════════════════════════════════════ */
const startGameMqttListener = () => {
  if (gameMqttClient || typeof window.mqtt === "undefined") return;

  const env = window.__ENV__ || {};
  const prefix = (env.MQTT_MESSAGE_PREFIX || "quiz-arena/").replace(/\/*$/, "/");
  const stateTopic = `${prefix}game/state`;
  const countdownTopic = `${prefix}game/countdown`;
  const questionTopic = `${prefix}game/question`;
  const evaluationTopic = `${prefix}game/evaluation`;
  const questionTimerTopic = `${prefix}game/question/timer`;
  const lobbyUpdatedTopic = `${prefix}lobby/updated`;
  const rfidAuthTopic = `${prefix}auth/rfid-login`;
  const brokerHost = env.MQTT_BROKER_URL || window.location.hostname;
  const brokerPort = env.MQTT_BROKER_PORT || "9001";
  const brokerUrl = `ws://${brokerHost}:${brokerPort}`;

  gameMqttClient = window.mqtt.connect(brokerUrl, {
    username: env.MQTT_USERNAME || "quiz-arena",
    password: env.MQTT_PASSWORD || "change-me-mqtt",
    clientId: `frontend-game-${Math.random().toString(16).slice(2, 10)}`,
    clean: true,
    reconnectPeriod: 2000,
  });

  gameMqttClient.on("connect", () => {
    gameMqttClient.subscribe([stateTopic, countdownTopic, questionTopic, evaluationTopic, questionTimerTopic, lobbyUpdatedTopic, rfidAuthTopic], { qos: 0 });
  });

  // Zentrale MQTT-Nachrichtenverarbeitung
  gameMqttClient.on("message", (topic, message) => {
    let payload = null;
    try {
      payload = JSON.parse(message.toString());
    } catch (_err) {
      return;
    }

    // game/state
    if (topic === stateTopic && payload?.state) {
      const s = String(payload.state || "").toUpperCase();
      if (s === "RESULTS" && Array.isArray(payload.results)) {
        renderFinalResults(payload.results);
      } else {
        if (s === "LOBBY") {
          if (lastKnownGameState === "COUNTDOWN") {
            const stoppedBy = payload.stopped_by || payload.aborted_by;
            const stoppedMsg = stoppedBy
              ? `Countdown gestoppt von ${stoppedBy}.`
              : "Countdown gestoppt.";
            setMessage(gameConfigMessage, stoppedMsg, "error");
          }
          activateMainTab("lobby");
        }
        updateGameViewBySessionState(payload.state);
      }
      lastKnownGameState = s;
      return;
    }

    if (topic === countdownTopic) {
      return;
    }

    // game/question
    if (topic === questionTopic) {
      if (localCountdownInterval) {
        pendingQuestionPayload = payload;
        return;
      }
      renderGameQuestion(payload);
      return;
    }

    // game/question/timer
    if (topic === questionTimerTopic && payload?.seconds_left != null) {
      const secondsLeft = Number(payload.seconds_left);
      if (!Number.isNaN(secondsLeft) && gameTimerEl) {
        gameTimerEl.textContent = `⏱ ${secondsLeft} Sekunden`;
      }
      return;
    }

    // game/evaluation
    if (topic === evaluationTopic) {
      stopLocalCountdown();
      activateMainTab("evaluation");
      renderEvaluationView(payload);
      return;
    }

    // lobby/updated
    if (topic === lobbyUpdatedTopic) {
      loadLobbyStatus();
      loadAvailableControllers();
      return;
    }

    // auth/rfid-login
    if (topic === rfidAuthTopic) {
      // RFID-Autologin: Token nur setzen, wenn noch keiner vorhanden ist.
      const existingToken = getAuthToken();
      if (!existingToken && payload?.token && payload?.user_id) {
        console.log("[RFID] Auto-login received for user:", payload.username);
        setAuthToken(payload.token);
        setCurrentUserId(payload.user_id);
        setLeftSection("controller");
        loadLobbyStatus();
        loadAvailableControllers();
        setMessage(loginMessage, `RFID-Login erfolgreich: ${payload.display_name || payload.username}`, "success");
      }
      return;
    }
  });
};

/* ═══════════════════════════════════════════════════════════════════════════
   WEB-CONTROLLER FENSTER
   ═══════════════════════════════════════════════════════════════════════════ */
const openWebControllerPage = () => {
  const base = `${window.location.protocol}//${window.location.hostname}`;
  const url = `${base}:81/controller.html`;
  const width = 420;
  const height = 720;
  const left = Math.max(0, window.screenX + (window.outerWidth - width) / 2);
  const top = Math.max(0, window.screenY + (window.outerHeight - height) / 2);
  const features = [
    "noopener",
    "noreferrer",
    "toolbar=no",
    "location=no",
    "status=no",
    "menubar=no",
    "scrollbars=yes",
    "resizable=yes",
    `width=${Math.round(width)}`,
    `height=${Math.round(height)}`,
    `left=${Math.round(left)}`,
    `top=${Math.round(top)}`,
  ].join(",");
  window.open(url, "_blank", features);
};

openWebController?.addEventListener("click", () => {
  openWebControllerPage();
});

/* ═══════════════════════════════════════════════════════════════════════════
   LOBBY-DATEN LADEN (SESSION, SPIELER, CONTROLLER, HIGHSCORES)
   ═══════════════════════════════════════════════════════════════════════════ */
const loadSession = async (optionalSession) => {
  try {
    const session = optionalSession !== undefined && optionalSession !== null
      ? optionalSession
      : await fetchJson("/api/lobby/session");
    currentSession = session;
    if (sessionInfo) {
      const state = session.state ? session.state : "LOBBY";
      sessionInfo.textContent = `Session #${session.id} • ${state}`;
    }
    const normalizedState = (session.state || "").toUpperCase();
    lastKnownGameState = normalizedState;
    const startedStates = ["COUNTDOWN", "QUESTION"];
    if (startedStates.includes(normalizedState)) {
      activateMainTab("game");
    } else if (normalizedState === "EVALUATION") {
      activateMainTab("evaluation");
    }
    updateGameViewBySessionState(normalizedState);
  } catch (error) {
    if (sessionInfo) {
      sessionInfo.textContent = "Session konnte nicht geladen werden.";
    }
    lastKnownGameState = "LOBBY";
    updateGameViewBySessionState("LOBBY");
  }
};

// Spielerliste fuer die Lobby laden
const loadLobbyStatus = async (optionalStatus) => {
  try {
    const status = optionalStatus !== undefined && optionalStatus !== null
      ? optionalStatus
      : await fetchJson("/api/lobby/status");
    const hostUserId = status.session && status.session.host_user_id != null
      ? status.session.host_user_id
      : null;
    const currentUserId = getCurrentUserId();
    renderPlayers(status.players || [], hostUserId, currentUserId);
  } catch (error) {
    renderPlayers([]);
  }
};

// Verfuegbare Controller als Auswahlkarten rendern
const renderControllers = (controllers) => {
  if (!controllerList || !controllerEmpty) return;
  controllerList.innerHTML = "";

  if (!controllers || controllers.length === 0) {
    controllerEmpty.textContent = "Kein Controller verfügbar. Öffne den Web‑Controller.";
    return;
  }

  controllerEmpty.textContent = "";
  controllers.forEach((controller) => {
    const item = document.createElement("div");
    item.className = "controller-item";
    item.dataset.controllerId = controller.controller_id;
    item.dataset.controllerType = controller.controller_type || "WEB";

    const info = document.createElement("div");
    const nameEl = document.createElement("div");
    nameEl.className = "ctrl-name";
    nameEl.textContent =
      controller.controller_type === "HARDWARE" ? "Hardware‑Controller" : "Web‑Controller";
    const idEl = document.createElement("div");
    idEl.className = "ctrl-id";
    idEl.textContent = `ID: ${controller.controller_id}`;
    info.appendChild(nameEl);
    info.appendChild(idEl);

    const dot = document.createElement("div");
    dot.className = "dot free";

    const check = document.createElement("div");
    check.className = "select-check";
    check.textContent = "✔";

    item.appendChild(info);
    item.appendChild(dot);
    item.appendChild(check);

    item.addEventListener("click", () => {
      if (selectedController && selectedController.id === controller.controller_id) {
        item.classList.remove("is-selected");
        selectedController = null;
        setMessage(controllerMessage, "", "");
        return;
      }
      controllerList.querySelectorAll(".controller-item").forEach((c) => {
        c.classList.remove("is-selected");
      });
      item.classList.add("is-selected");
      selectedController = {
        id: controller.controller_id,
        type: controller.controller_type || "WEB",
      };
      setMessage(controllerMessage, "", "");
    });

    controllerList.appendChild(item);
  });

  // Auswahl nach Refresh wiederherstellen (Liste wird durch Polling neu aufgebaut).
  if (selectedController && selectedController.id) {
    const toSelect = controllerList.querySelector(
      `[data-controller-id="${selectedController.id}"]`
    );
    if (toSelect) {
      toSelect.classList.add("is-selected");
    }
  }
};

const formatHighscoreDate = (value) => {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString("de-DE", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const renderHighscores = (rows) => {
  if (!highscoreTableBody) return;
  highscoreTableBody.innerHTML = "";

  if (highscorePodium) {
    if (rows && rows.length >= 2) {
      const podiumOrder = [1, 0, 2];
      highscorePodium.innerHTML = podiumOrder.map(idx => {
        const entry = rows[idx];
        if (!entry) return "";
        const cls = idx === 0 ? "p1" : idx === 1 ? "p2" : "p3";
        const name = entry.display_name || entry.username || "Unbekannt";
        const ini = name.slice(0, 2).toUpperCase();
        const pts = entry.total_points ?? 0;
        return `<div class="podium-item ${cls}">
          <div class="podium-avatar">${ini}</div>
          <div class="podium-name">${name}</div>
          <div class="podium-score">${pts}</div>
          <div class="podium-block"></div>
          <div class="podium-rank">#${idx + 1}</div>
        </div>`;
      }).join("");
      highscorePodium.style.display = "";
    } else {
      highscorePodium.innerHTML = "";
      highscorePodium.style.display = "none";
    }
  }

  if (!rows || rows.length === 0) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 4;
    td.className = "muted";
    td.textContent = "Keine Highscores vorhanden.";
    tr.appendChild(td);
    highscoreTableBody.appendChild(tr);
    return;
  }

  rows.forEach((entry, index) => {
    const rank = entry.rank || index + 1;
    const tr = document.createElement("tr");
    if (rank <= 3) tr.classList.add(`rank-${rank}`);

    const rankCell = document.createElement("td");
    rankCell.innerHTML = `<div class="rank-pill">${rank}</div>`;

    const playerCell = document.createElement("td");
    playerCell.textContent = entry.display_name || entry.username || "Unbekannt";

    const pointsCell = document.createElement("td");
    pointsCell.textContent = `${entry.total_points ?? 0}`;

    const dateCell = document.createElement("td");
    dateCell.textContent = formatHighscoreDate(entry.created_at);

    tr.appendChild(rankCell);
    tr.appendChild(playerCell);
    tr.appendChild(pointsCell);
    tr.appendChild(dateCell);
    highscoreTableBody.appendChild(tr);
  });
};

const loadHighscores = async (mode = currentHighscoreMode) => {
  currentHighscoreMode = mode;
  try {
    const data = await fetchJson(`/api/highscores/${mode}`);
    renderHighscores(data.highscores || []);
  } catch (_error) {
    renderHighscores([]);
  }
};

scoreTabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    scoreTabs.forEach((b) => b.classList.remove("is-active"));
    tab.classList.add("is-active");
    const mode = tab.dataset.mode || "Q5";
    loadHighscores(mode);
  });
});

questionCountPills.forEach((pill) => {
  pill.addEventListener("click", () => {
    const nextRoundLength = pill.dataset.roundLength || "Q5";
    const total = getSelectedQuestionsTotal();
    const nextLimit = getRoundLimit(nextRoundLength);
    if (total > nextLimit) {
      setMessage(
        gameConfigMessage,
        `Please reduce selected categories first. Current total (${total}) exceeds ${nextLimit}.`,
        "error"
      );
      return;
    }

    questionCountPills.forEach((p) => p.classList.remove("is-active"));
    pill.classList.add("is-active");
    selectedRoundLength = nextRoundLength;
    setMessage(gameConfigMessage, "", "");
    refreshQuestionSelectionInfo();
  });
});

const syncCatItemVisual = (checkbox) => {
  const wrapper = checkbox.closest(".cat-item");
  if (wrapper) wrapper.classList.toggle("is-checked", checkbox.checked);
};

categoryCheckboxes.forEach((checkbox) => {
  checkbox.addEventListener("change", () => {
    const selectedDifficulties = getSelectedDifficulties();
    if (checkbox.checked && !hasAnyDifficultySelected()) {
      checkbox.checked = false;
      syncCatItemVisual(checkbox);
      setMessage(
        gameConfigMessage,
        "Please select at least one difficulty level before choosing categories.",
        "error"
      );
      return;
    }

    if (checkbox.checked) {
      const total = getSelectedQuestionsTotal(selectedDifficulties);
      const limit = getRoundLimit();
      if (total > limit) {
        checkbox.checked = false;
        syncCatItemVisual(checkbox);
        setMessage(
          gameConfigMessage,
          `Category selection exceeds ${limit} questions for current setup.`,
          "error"
        );
        refreshQuestionSelectionInfo();
        return;
      }
    }
    syncCatItemVisual(checkbox);
    setMessage(gameConfigMessage, "", "");
    refreshQuestionSelectionInfo();
  });
});

const syncDiffChipVisual = (checkbox) => {
  const wrapper = checkbox.closest(".diff-chip");
  if (!wrapper) return;
  wrapper.classList.toggle("is-on", checkbox.checked);
  const span = wrapper.querySelector("span");
  if (span) {
    const base = span.textContent.replace("✓ ", "").trim();
    span.textContent = checkbox.checked ? `✓ ${base}` : base;
  }
};

difficultyCheckboxes.forEach((checkbox) => {
  checkbox.addEventListener("change", () => {
    const selectedDifficulties = getSelectedDifficulties();
    if (selectedDifficulties.length === 0) {
      const hasSelectedCategories = Array.from(categoryCheckboxes).some((c) => c.checked);
      if (hasSelectedCategories) {
        checkbox.checked = true;
        syncDiffChipVisual(checkbox);
        setMessage(
          gameConfigMessage,
          "Please keep at least one difficulty selected while categories are selected.",
          "error"
        );
        return;
      }
      syncDiffChipVisual(checkbox);
      setMessage(gameConfigMessage, "", "");
      refreshQuestionSelectionInfo();
      return;
    }

    const total = getSelectedQuestionsTotal(selectedDifficulties);
    const limit = getRoundLimit();
    if (total > limit) {
      checkbox.checked = !checkbox.checked;
      syncDiffChipVisual(checkbox);
      setMessage(
        gameConfigMessage,
        `This difficulty change would exceed ${limit} questions. Adjust categories first.`,
        "error"
      );
      return;
    }
    syncDiffChipVisual(checkbox);
    setMessage(gameConfigMessage, "", "");
    refreshQuestionSelectionInfo();
  });
});

const loadAvailableControllers = async (optionalList) => {
  try {
    const list = optionalList !== undefined && optionalList !== null
      ? optionalList
      : await fetchJson("/api/controllers/available");
    renderControllers(Array.isArray(list) ? list : []);
  } catch (error) {
    renderControllers([]);
  }
};


/** Fragenanzahl aus dem Backend laden und Kategorie-Labels aktualisieren. */
const loadCategoryQuestionCounts = async (optionalData) => {
  try {
    const data = optionalData !== undefined && optionalData !== null
      ? optionalData
      : await fetchJson("/api/game/category-question-counts");
    const counts = data.counts || [];
    if (!Array.isArray(counts) || counts.length === 0) {
      setCategoryCountsPlaceholder("—");
      return;
    }
    const checkboxes = document.querySelectorAll("[data-category-checkbox]");
    const newMap = {};
    counts.forEach((c) => {
      const id = Number(c.category_id);
      if (!Number.isNaN(id)) {
        newMap[id] = {
          easy: Number(c.easy) || 0,
          medium: Number(c.medium) || 0,
          hard: Number(c.hard) || 0,
        };
      }
    });
    if (Object.keys(newMap).length > 0) {
      categoryCountsByCategoryId = newMap;
    }
    checkboxes.forEach((checkbox) => {
      const id = Number.parseInt(checkbox.dataset.categoryId || "", 10);
      if (Number.isNaN(id)) return;
      const item = counts.find((c) => Number(c.category_id) === id);
      if (!item) return;
      const e = Number(item.easy) || 0;
      const m = Number(item.medium) || 0;
      const h = Number(item.hard) || 0;
      checkbox.dataset.easy = String(e);
      checkbox.dataset.medium = String(m);
      checkbox.dataset.hard = String(h);
      const wrapper = checkbox.closest("label") || checkbox.closest(".cat-item");
      if (wrapper) {
        const countsEl = wrapper.querySelector(".cat-counts");
        if (countsEl) {
          countsEl.textContent = `${e} leicht · ${m} mittel · ${h} schwer`;
        }
      }
    });
    refreshQuestionSelectionInfo();
  } catch (_err) {
    setCategoryCountsPlaceholder("—");
  }
};

/** Setzt Platzhalter in den Kategorie-Labels (z. B. bei API-Fehler). */
const setCategoryCountsPlaceholder = (text) => {
  document.querySelectorAll(".cat-counts").forEach((el) => {
    if (el) el.textContent = text;
  });
};

/** Initialisiert Frageanzahlen aus data-* bis die API-Daten da sind. */
const initCategoryCountsFromDOM = () => {
  const map = {};
  document.querySelectorAll("[data-category-checkbox]").forEach((cb) => {
    const id = Number.parseInt(cb.dataset.categoryId || "", 10);
    if (Number.isNaN(id) || id <= 0) return;
    map[id] = {
      easy: Number.parseInt(cb.dataset.easy || "0", 10),
      medium: Number.parseInt(cb.dataset.medium || "0", 10),
      hard: Number.parseInt(cb.dataset.hard || "0", 10),
    };
  });
  if (Object.keys(map).length > 0) categoryCountsByCategoryId = map;
};

const initLobby = async () => {
  startGameMqttListener();
  initCategoryCountsFromDOM();
  try {
    const [session, status, controllers, categoryData] = await Promise.all([
      fetchJson("/api/lobby/session"),
      fetchJson("/api/lobby/status"),
      fetchJson("/api/controllers/available"),
      fetchJson("/api/game/category-question-counts"),
    ]);
    await loadSession(session);
    lastSessionSnapshot = JSON.stringify(session);
    await loadLobbyStatus(status);
    lastLobbyStatusSnapshot = JSON.stringify(status);
    await loadAvailableControllers(Array.isArray(controllers) ? controllers : []);
    lastControllersSnapshot = JSON.stringify(controllers);
    await loadCategoryQuestionCounts(categoryData);
    lastCategoryCountsSnapshot = JSON.stringify(categoryData);
  } catch (_err) {
    await loadSession();
    await loadLobbyStatus();
    await loadAvailableControllers();
    await loadCategoryQuestionCounts();
  }
  await loadHighscores(currentHighscoreMode);
  if (lobbyRefreshTimer) {
    window.clearInterval(lobbyRefreshTimer);
  }
  refreshQuestionSelectionInfo();
  // DOM nur bei echten Aenderungen aktualisieren (reduziert Flackern durch Polling).
  const pollOnce = async () => {
    try {
      const [session, status, controllers, categoryData] = await Promise.all([
        fetchJson("/api/lobby/session"),
        fetchJson("/api/lobby/status"),
        fetchJson("/api/controllers/available"),
        fetchJson("/api/game/category-question-counts"),
      ]);
      const sessionSnap = JSON.stringify(session);
      if (sessionSnap !== lastSessionSnapshot) {
        lastSessionSnapshot = sessionSnap;
        await loadSession(session);
      }
      const statusSnap = JSON.stringify(status);
      if (statusSnap !== lastLobbyStatusSnapshot) {
        lastLobbyStatusSnapshot = statusSnap;
        await loadLobbyStatus(status);
      }
      const ctrlSnap = JSON.stringify(controllers);
      if (ctrlSnap !== lastControllersSnapshot) {
        lastControllersSnapshot = ctrlSnap;
        await loadAvailableControllers(Array.isArray(controllers) ? controllers : []);
      }
      const catSnap = JSON.stringify(categoryData);
      if (catSnap !== lastCategoryCountsSnapshot) {
        lastCategoryCountsSnapshot = catSnap;
        await loadCategoryQuestionCounts(categoryData);
      }
    } catch (_err) {}
  };
  lobbyRefreshTimer = window.setInterval(pollOnce, 1000);
};

/* ═══════════════════════════════════════════════════════════════════════════
   AUTH-FLOWS (LOGIN / REGISTRIERUNG)
   ═══════════════════════════════════════════════════════════════════════════ */
// Login
loginBtn?.addEventListener("click", async () => {
  const username = loginUsername?.value?.trim() || "";
  const password = loginPassword?.value || "";

  if (!username || !password) {
    setMessage(loginMessage, "Bitte Benutzername und Passwort eingeben.", "error");
    return;
  }

  setMessage(loginMessage, "Login läuft...", "");
  clearAuthToken();

  try {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.error || "Login fehlgeschlagen");
    }
    const token = normalizeToken(data.token);
    if (!token) {
      throw new Error("Login erfolgreich, aber Token fehlt");
    }
    setAuthToken(token);
    if (data.id != null) setCurrentUserId(data.id);
    setLeftSection("controller");
    setMessageAutoClear(controllerMessage, "Login erfolgreich.", "success");
  } catch (error) {
    clearAuthToken();
    setMessage(loginMessage, error.message || "Login fehlgeschlagen", "error");
  }
});

// Registrierung
registerBtn?.addEventListener("click", async () => {
  const username = registerUsername?.value?.trim() || "";
  const password = registerPassword?.value || "";
  const passwordConfirm = registerPasswordConfirm?.value || "";
  const rfidUid = registerRfid?.value?.trim() || "";

  if (registerUsernameHint) registerUsernameHint.classList.remove("is-error");

  if (!username || !password) {
    setMessage(registerMessage, "Bitte Benutzername und Passwort eingeben.", "error");
    if (!username && registerUsernameHint) registerUsernameHint.classList.add("is-error");
    return;
  }
  if (username.length < 3) {
    setMessage(registerMessage, "Der Benutzername muss mindestens 3 Zeichen enthalten.", "error");
    if (registerUsernameHint) registerUsernameHint.classList.add("is-error");
    return;
  }
  if (password !== passwordConfirm) {
    setMessage(registerMessage, "Passwörter stimmen nicht überein.", "error");
    return;
  }

  setMessage(registerMessage, "Registrierung läuft...", "");

  try {
    const payload = { username, password };
    if (rfidUid) {
      payload.rfid_uid = rfidUid;
    }
    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(data.error || "Registrierung fehlgeschlagen");
    }
    setLeftSection("login");
    setMessageAutoClear(loginMessage, "Registrierung erfolgreich.", "success");
  } catch (error) {
    setMessage(registerMessage, error.message || "Registrierung fehlgeschlagen", "error");
  }
});

/* ═══════════════════════════════════════════════════════════════════════════
   CONTROLLER-ZUORDNUNG & RFID-AKTIONEN
   ═══════════════════════════════════════════════════════════════════════════ */
// RFID speichern/aktualisieren
saveRfid?.addEventListener("click", async () => {
  const token = getAuthToken();
  if (!token) {
    setMessage(controllerMessage, "Bitte zuerst einloggen.", "error");
    return;
  }
  const rfidUid = updateRfid?.value?.trim() || "";
  if (!rfidUid) {
    setMessage(controllerMessage, "Bitte RFID-Karte eingeben.", "error");
    return;
  }
  setMessage(controllerMessage, "RFID wird gespeichert...", "");
  try {
    await fetchJson("/api/auth/rfid", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ rfid_uid: rfidUid }),
    });
    setMessage(controllerMessage, "RFID-Karte gespeichert.", "success");
  } catch (error) {
    setMessage(controllerMessage, error.message || "RFID speichern fehlgeschlagen", "error");
  }
});

// RFID loeschen
deleteRfid?.addEventListener("click", async () => {
  const token = getAuthToken();
  if (!token) {
    setMessage(controllerMessage, "Bitte zuerst einloggen.", "error");
    return;
  }
  setMessage(controllerMessage, "RFID wird gelöscht...", "");
  try {
    await fetchJson("/api/auth/rfid", {
      method: "DELETE",
    });
    if (updateRfid) updateRfid.value = "";
    setMessage(controllerMessage, "RFID-Karte gelöscht.", "success");
  } catch (error) {
    setMessage(controllerMessage, error.message || "RFID löschen fehlgeschlagen", "error");
  }
});

// Gewaehlten Controller zuordnen
saveController?.addEventListener("click", async () => {
  const token = getAuthToken();
  if (!token) {
    setMessage(controllerMessage, "Bitte zuerst einloggen.", "error");
    return;
  }
  if (!selectedController) {
    setMessage(controllerMessage, "Bitte einen Controller auswählen.", "error");
    return;
  }

  setMessage(controllerMessage, "Controller wird gespeichert...", "");
  try {
    await fetchJson("/api/lobby/join", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${normalizeToken(token)}`,
      },
      body: JSON.stringify({
        controller_id: selectedController.id,
        controller_type: selectedController.type,
      }),
    });
    setMessage(controllerMessage, "Controller gespeichert.", "success");
    toggleSidebar(true);
    await loadLobbyStatus();
    await loadAvailableControllers();
    setLeftSection("login");
  } catch (error) {
    const msg = error.message || "Speichern fehlgeschlagen";
    if (msg.includes("Unauthorized")) {
      // In derselben Ansicht bleiben, aber den echten Backend-Fehler anzeigen.
      setMessage(controllerMessage, "Unauthorized: authentication required", "error");
      return;
    }
    setMessage(controllerMessage, msg, "error");
  }
});

/* ═══════════════════════════════════════════════════════════════════════════
   LOBBY-AKTIONEN (REFRESH, RESET, START, COUNTDOWN-ABBRUCH)
   ═══════════════════════════════════════════════════════════════════════════ */
// Lobby manuell aktualisieren
refreshLobby?.addEventListener("click", () => {
  loadLobbyStatus();
  loadAvailableControllers();
});

// Session komplett zuruecksetzen
closeSession?.addEventListener("click", async () => {
  try {
    await fetchJson("/api/lobby/reset", { method: "POST" });
    await loadSession();
    await loadLobbyStatus();
    await loadAvailableControllers();
  } catch (error) {
    setMessage(loginMessage, error.message || "Reset fehlgeschlagen", "error");
  }
});

// Countdown abbrechen
abortCountdownBtn?.addEventListener("click", async () => {
  if (!getAuthToken()) return;
  abortCountdownBtn.disabled = true;
  try {
    await fetchJson("/api/game/restart", { method: "POST" });
    stopLocalCountdown();
    await loadSession();
    await loadLobbyStatus();
    activateMainTab("lobby");
    if (gameConfigMessage) setMessage(gameConfigMessage, "", "");
  } catch (err) {
    setMessage(gameConfigMessage, err.message || "Abbrechen fehlgeschlagen.", "error");
  } finally {
    abortCountdownBtn.disabled = false;
  }
});

// Spielstart per Button
gameStartBtn?.addEventListener("click", async () => {
  await doStartGame();
});

/* ═══════════════════════════════════════════════════════════════════════════
   BOT MANAGEMENT
   ═══════════════════════════════════════════════════════════════════════════ */

const getNextAvailableBot = (players) => {
  const playerUsernames = (players || []).map(p => (p.username || "").toLowerCase());
  for (const bot of BOT_CREDENTIALS) {
    if (!playerUsernames.includes(bot.username.toLowerCase())) {
      return bot;
    }
  }
  return null;
};

// Bot anlegen, einloggen, verbinden und automatisch in die Lobby setzen
const addBot = async () => {
  const env = window.__ENV__ || {};
  const prefix = (env.MQTT_MESSAGE_PREFIX || "quiz-arena/").replace(/\/*$/, "/");
  const brokerHost = env.MQTT_BROKER_URL || window.location.hostname;
  const brokerPort = env.MQTT_BROKER_PORT || "9001";
  const brokerUrl = `ws://${brokerHost}:${brokerPort}`;

  // 1) Aktuellen Lobby-Status laden
  let statusData = null;
  try {
    statusData = await fetchJson("/api/lobby/status");
  } catch (e) {
    console.error("Failed to fetch lobby status for bot:", e);
    return;
  }

  // 2) Freien Bot-Account bestimmen
  const bot = getNextAvailableBot(statusData?.players);
  if (!bot) {
    alert("Alle Bots sind bereits in der Session (max 5).");
    return;
  }

  if (addBotBtn) addBotBtn.disabled = true;

  try {
    // 3) Bot einloggen (oder bei Bedarf automatisch registrieren)
    let loginRes;
    try {
      loginRes = await fetchJson("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: bot.username, password: bot.password }),
      });
    } catch (loginErr) {
      if (loginErr.message && (loginErr.message.includes("Invalid") || loginErr.message.includes("not found"))) {
        console.log(`[BOT] ${bot.username} not found, registering...`);
        await fetchJson("/api/auth/register", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: bot.username, password: bot.password }),
        });
        loginRes = await fetchJson("/api/auth/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username: bot.username, password: bot.password }),
        });
      } else {
        throw loginErr;
      }
    }

    // 4) Token und Controller-ID fuer den Bot vorbereiten
    const botToken = loginRes.token;
    if (!botToken) throw new Error("No token received for bot");

    const botCtrlId = `BOT-${bot.username.toUpperCase()}-${Math.random().toString(16).slice(2, 6).toUpperCase()}`;

    const botMqtt = window.mqtt.connect(brokerUrl, {
      username: env.MQTT_USERNAME || "quiz-arena",
      password: env.MQTT_PASSWORD || "change-me-mqtt",
      clientId: `bot-${botCtrlId}`,
      clean: true,
      reconnectPeriod: 2000,
    });

    const botState = {
      username: bot.username,
      token: botToken,
      odeurId: botCtrlId,
      odeurClient: botMqtt,
      sessionId: null,
      questionId: null,
      difficulty: "MITTEL",
    };

    // 5) MQTT connect: subscriben, Controller registrieren, Session join, ready senden
    botMqtt.on("connect", async () => {
      console.log(`[BOT] ${bot.username} MQTT connected`);

      botMqtt.subscribe(`${prefix}controller/${botCtrlId}/status/response`, { qos: 0 });
      botMqtt.subscribe(`${prefix}controller/${botCtrlId}/ping`, { qos: 0 });
      botMqtt.subscribe(`${prefix}game/question`, { qos: 0 });
      botMqtt.subscribe(`${prefix}game/state`, { qos: 0 });

      botMqtt.publish(
        `${prefix}controller/register`,
        JSON.stringify({ controller_id: botCtrlId, controller_type: "WEB" }),
        { qos: 0 }
      );

      await new Promise(r => setTimeout(r, 300));

      try {
        await fetchJson("/api/lobby/join", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${botToken}`,
          },
          body: JSON.stringify({ controller_id: botCtrlId, controller_type: "WEB" }),
        });
        console.log(`[BOT] ${bot.username} joined session`);
      } catch (joinErr) {
        console.error(`[BOT] ${bot.username} failed to join:`, joinErr);
        botMqtt.end();
        return;
      }

      await new Promise(r => setTimeout(r, 200));

      botMqtt.publish(
        `${prefix}player/ready`,
        JSON.stringify({ controller_id: botCtrlId, ready: true }),
        { qos: 0 }
      );
      console.log(`[BOT] ${bot.username} set to READY`);

      activeBots.set(bot.username, botState);
      await loadLobbyStatus();
    });

    // 6) MQTT-Nachrichten verarbeiten (ping/pong + Fragen beantworten)
    botMqtt.on("message", async (topic, msg) => {
      // ping -> pong
      if (topic.endsWith("/ping")) {
        try {
          const ping = JSON.parse(msg.toString());
          botMqtt.publish(
            `${prefix}controller/${botCtrlId}/pong`,
            JSON.stringify({ requestId: ping.requestId || "", ts: ping.ts || Date.now(), fw: "v1.0" }),
            { qos: 0 }
          );
        } catch (_e) {}
        return;
      }

      // Frage erhalten -> Bot-Antwort berechnen und senden
      if (topic === `${prefix}game/question`) {
        try {
          const question = JSON.parse(msg.toString());
          const questionId = question.question_id;
          const sessionId = question.session_id;
          const questionDifficulty = (question.difficulty || "MEDIUM").toUpperCase();
          
          if (!questionId || !sessionId) return;
          
          console.log(`[BOT] ${bot.username} received question ${questionId} (difficulty: ${questionDifficulty})`);
          botState.sessionId = sessionId;
          botState.questionId = questionId;

          const answerRes = await fetchJson(`/api/game/bot/answer/${questionId}`);
          const correctOption = answerRes.correct_option;
          
          if (!correctOption) {
            console.error(`[BOT] ${bot.username} could not get correct answer`);
            return;
          }

          // Schwierigkeit + Antwortzeit simulieren
          const botDifficulty = botState.difficulty || "MITTEL";
          const config = BOT_DIFFICULTY_CONFIG[botDifficulty]?.[questionDifficulty] 
                      || BOT_DIFFICULTY_CONFIG.MITTEL.MEDIUM;
          
          const { accuracy, minTime, maxTime } = config;
          const responseTimeMs = Math.floor(Math.random() * (maxTime - minTime + 1)) + minTime;
          
          const isCorrect = Math.random() < accuracy;
          let finalAnswer = correctOption;
          
          if (!isCorrect) {
            const allOptions = ["A", "B", "C", "D"];
            const wrongOptions = allOptions.filter(o => o !== correctOption);
            finalAnswer = wrongOptions[Math.floor(Math.random() * wrongOptions.length)];
          }
          
          console.log(`[BOT] ${bot.username} (${botDifficulty}) will answer "${finalAnswer}" (correct: ${isCorrect}) in ${responseTimeMs}ms`);
          
          setTimeout(() => {
            botMqtt.publish(
              `${prefix}game/answer`,
              JSON.stringify({
                session_id: sessionId,
                controller_id: botCtrlId,
                question_id: questionId,
                answer: finalAnswer,
                response_time_ms: responseTimeMs
              }),
              { qos: 0 }
            );
            console.log(`[BOT] ${bot.username} answered "${finalAnswer}" with simulated time ${responseTimeMs}ms`);
          }, 100);
          
        } catch (err) {
          console.error(`[BOT] ${bot.username} error handling question:`, err);
        }
      }
    });

    // 7) Verbindungsevents fuer Logging/Cleanup
    botMqtt.on("error", (err) => {
      console.error(`[BOT] ${bot.username} MQTT error:`, err);
    });

    botMqtt.on("close", () => {
      console.log(`[BOT] ${bot.username} MQTT disconnected`);
      activeBots.delete(bot.username);
    });

  } catch (err) {
    console.error(`[BOT] Failed to add bot ${bot.username}:`, err);
    alert(`Fehler beim Hinzufügen des Bots: ${err.message}`);
  } finally {
    if (addBotBtn) addBotBtn.disabled = false;
  }
};

addBotBtn?.addEventListener("click", addBot);

initLobby();
