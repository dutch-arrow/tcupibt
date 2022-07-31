/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 11 Aug 2021.
 */


package nl.das.terraria.objects;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.json.bind.annotation.JsonbTransient;

import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.w1.W1Master;

import nl.das.terraria.hw.DHT22;
import nl.das.terraria.hw.DS18B20;

/**
 *
 */
public class Sensors {

	@SuppressWarnings("unused")
	private String clock;
	private Sensor[] sensors = new Sensor[2];
	// Initialize the W1 bus (connected on GPIO 7)
	@JsonbTransient private W1Master w1Master = new W1Master();
	// Initialize the Temperature sensor
	@JsonbTransient private DS18B20 terrarium = new DS18B20(this.w1Master);
	@JsonbTransient private DHT22 room = new DHT22(RaspiPin.GPIO_27);

	public Sensors() {
		this.clock = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-y HH:mm"));
		this.sensors[0] = new Sensor("room", 0, 0);
		this.sensors[1] = new Sensor("terrarium", 0, 0);
	}

	public void readSensorValues() {
		this.clock = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-y HH:mm"));
		this.sensors[0].setTemperature((int) Math.round(this.room.getTemperature()));
		this.sensors[0].setHumidity((int) Math.round(this.room.getHumidity()));
		this.sensors[1].setTemperature((int) Math.round(this.terrarium.getTemperature()));
	}

	@JsonbTransient
	public int getTerrariumTemp() {
		return this.sensors[1].getTemperature();
	}

	@JsonbTransient
	public int getRoomTemp() {
		return this.sensors[0].getTemperature();
	}

	public String getClock () {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-y HH:mm"));
	}

	public void setClock (String clock) {
		this.clock = clock;
	}

	public Sensor[] getSensors () {
		return this.sensors;
	}

	public void setSensors (Sensor[] sensors) {
		this.sensors = sensors;
	}

	public class Sensor {
		private String location;
		private int temperature;
		private int humidity;

		public Sensor(String loc, int t, int h) {
			this.location = loc;
			this.temperature = t;
			this.humidity = h;
		}

		public String getLocation () {
			return this.location;
		}
		public void setLocation (String location) {
			this.location = location;
		}
		public int getTemperature () {
			return this.temperature;
		}
		public void setTemperature (int temperature) {
			this.temperature = temperature;
		}
		public int getHumidity () {
			return this.humidity;
		}
		public void setHumidity (int humidity) {
			this.humidity = humidity;
		}


	}
}
