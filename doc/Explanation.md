## Detailed Explanation

### Backend

The backend is a Java application built using Vert.x, a toolkit for building reactive applications on the JVM.
It exposes a REST API to interact with the data stored in the MariaDB database.

#### Key Components

- **MainVerticle.java**: The main entry point of the backend application.
- **ObjectController.java**: Handles HTTP requests and routes them to the appropriate service.
- **ObjectService.java**: Contains the business logic for handling the incoming data.
- **ObjectRepository.java**: Interacts with the MariaDB database to perform CRUD operations.

### Frontend

The frontend is a simple web application built with plain HTML and Bootstrap for styling.
It interacts with the backend via AJAX requests to perform CRUD operations on the available data.

#### Key Files

- **index.html**: The main HTML file for the frontend.
- **styles.css**: Custom CSS styles for the frontend.
- **controller/controller.html**: A basic web-controller.

### Arduino Uno / ESP32 Microcontroller

The microcontroller is programmed to implement the hardware controller.
It sends data via MQTT to the MQTT broker, which then forwards it to the backend application.

#### Key Files

- **platformio.ini**: PlatformIO Project Configuration File used to build your project.
- **zip_project.py**: Helper script to package your project for use in MICRO.
- **src/main.cpp**: The main program file for the microcontroller.

#### Wiring

The current wiring diagram for the arduino based experiments is provided here:
![arduino_wiring](/doc/arduino_wiring.png)

Thus, the following connections are in use:

| Module / Part              | Signal        | Arduino pin (in code) | Note                                                                                                 |
|----------------------------|---------------|----------------------:|------------------------------------------------------------------------------------------------------|
| NeoPixel (4 LEDs)          | DIN (Data In) |                **D2** | RGB-LEDs to display e.g. the pressed button color.                                                   |
| NeoPixel                   | +V            |              **3.3V** | Power supply for NeoPixels (use a proper supply).                                                    |
| Button Green               | Signal        |                **D4** | Button between D4 and GND[^1].                                                                       |
| Button Red                 | Signal        |                **D5** | Button between D5 and GND[^1].                                                                       |
| Button Yellow              | Signal        |                **D6** | Button between D6 and GND[^1].                                                                       |
| Button Blue                | Signal        |                **D7** | Button between D7 and GND[^1].                                                                       |
| Buttons                    | Common return |               **GND** | All buttons can share the same GND.                                                                  |
| OLED (SH1106/SSD1306, I2C) | SDA           |                **A4** | On UNO R4: `Wire.begin()` uses A4/A5 fixed by hardware; on ESP32 you would use `Wire.begin(A4, A5)`. |
| OLED (SH1106/SSD1306, I2C) | SCL           |                **A5** | I2C clock.                                                                                           |
| OLED                       | VCC           |              **3.3V** | Depends on your module (many run on 3.3–5V).                                                         |
| OLED                       | GND           |               **GND** | Common ground.                                                                                       |
| RFID RC522 (SPI)           | RST           |                **D9** | Reset pin.                                                                                           |
| RFID RC522 (SPI)           | CS / SDA (SS) |               **D10** | RC522 “SDA” is chip-select (CS/SS).                                                                  |
| RFID RC522 (SPI)           | MOSI          |               **D11** | UNO R4: SPI[^2].                                                                                     |
| RFID RC522 (SPI)           | MISO          |               **D12** | UNO R4: SPI[^2].                                                                                     |
| RFID RC522 (SPI)           | SCK           |               **D13** | UNO R4: SPI[^2].                                                                                     |
| RFID RC522                 | VCC           |              **3.3V** | RC522 typically requires 3.3V (not 5V).                                                              |
| RFID RC522                 | GND           |               **GND** | Common ground.                                                                                       |

[^1]: The code uses `INPUT_PULLUP` so all buttons are active on a low signal.
[^2]: The Arduino UNO R4 uses fixed SPI pins; this must match your board's SPI mapping.

#### Commands and Helpers

After installing the [`mosquitto-clients`](https://mosquitto.org/download/) (and replacing the `test` values with those found in your `mqtt_credentials.txt`), you may run the following command to test you connection:

```sh
mosquitto_sub -h localhost -p 1883 -u quiz-arena -P YOUR_MQTT_PASSWORD -t "test/#" -v -d
```

To simplify uploading to a MICRO station, you also may run the following command, which will create a `.zip`-archive in your current directory, ready to be uploaded.

```sh
python3 ./zip_projekt.py
```

## Usage

### Testing the API

You can use tools like [Postman](https://www.postman.com/) or [curl](https://curl.se/) to test the API endpoints.

#### Create an Object

```bash
curl -X POST http://localhost:8080/api/create -d "message=your_message"
```

#### Get All Objects

```bash
curl http://localhost:8080/api/objects
```

#### Update an Object

```bash
curl -X PUT http://localhost:8080/api/update/1 -d "message=

updated_message"
```

#### Delete an Object

```bash
curl -X DELETE http://localhost:8080/api/delete/1
```

### Sending MQTT Messages

You can use tools like [MQTT Explorer](http://mqtt-explorer.com/) or `mosquitto_pub` to send MQTT messages.

```bash
mosquitto_pub -h localhost -t test/topic -m "your message" -u "your_mqtt_username" -P "your_mqtt_password"
```

