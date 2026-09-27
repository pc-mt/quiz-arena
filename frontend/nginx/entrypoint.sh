#!/bin/sh
set -eu

cat > /usr/share/nginx/html/env.js <<EOF
window.__ENV__ = {
  MQTT_BROKER_URL:      "${MQTT_BROWSER_HOST:-localhost}",
  MQTT_BROKER_PORT:     "${MQTT_BROWSER_PORT:-9001}",
  MQTT_USERNAME:        "${MQTT_USERNAME}",
  MQTT_PASSWORD:        "${MQTT_PASSWORD}",
  MQTT_MESSAGE_PREFIX:  "${MQTT_MESSAGE_PREFIX:-quiz-arena/}"
};
EOF
