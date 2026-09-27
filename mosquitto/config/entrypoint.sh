#!/bin/sh
# NOTE: Keep LF line endings to avoid Docker exec issues on Windows.

# Ensure password file exists and credentials are always up to date.
if [ ! -f /mosquitto/config/passwordfile ]; then
  echo "Creating password file..."
  touch /mosquitto/config/passwordfile
fi

echo "Updating MQTT credentials for user: ${MQTT_USERNAME}"
mosquitto_passwd -b /mosquitto/config/passwordfile "${MQTT_USERNAME}" "${MQTT_PASSWORD}"

# Run the original Mosquitto command
echo "Starting Mosquitto..."
exec mosquitto -c /mosquitto/config/mosquitto.conf