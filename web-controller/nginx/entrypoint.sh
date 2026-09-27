#!/bin/sh
# NOTE: Keep LF line endings to avoid Docker exec issues on Windows.

# Schreibe Umgebungsvariablen in eine JS-Datei
cat <<EOF > /usr/share/nginx/html/env.js
window.__ENV__ = {
  MQTT_BROKER_URL:      "${MQTT_BROWSER_HOST:-localhost}",
  MQTT_BROKER_PORT:     "${MQTT_BROWSER_PORT:-9001}",
  MQTT_USERNAME:        "${MQTT_USERNAME}",
  MQTT_PASSWORD:        "${MQTT_PASSWORD}",
  MQTT_MESSAGE_PREFIX:  "${MQTT_MESSAGE_PREFIX:-quiz-arena/}",
};
EOF

# Starte NGINX
nginx -g 'daemon off;'
