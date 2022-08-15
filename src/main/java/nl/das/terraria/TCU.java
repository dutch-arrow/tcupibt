/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 05 Aug 2021.
 */

package nl.das.terraria;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Properties;

import javax.bluetooth.UUID;

import nl.das.terraria.hw.LCD;
import nl.das.terraria.objects.Terrarium;
import nl.das.terraria.rest.BTServer;

public class TCU {

	public static void main (String[] args) throws InterruptedException {
		// Read property file
		Properties props = new Properties();
		try {
			props.load(new FileInputStream("config.properties"));
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		LocalDateTime now = LocalDateTime.now();
		System.out.println(Util.getDateTimeString() + "Start Initialization ...");
		System.err.println(Util.getDateTimeString() + "System started.");

		// Initialize the LCD
		LCD lcd = new LCD();
		lcd.init(2, 16);
		lcd.write(0, "Initialize....");

		System.out.println(Util.getDateTimeString() + " Starting the Bluetooth service");
		Thread svr = new Thread() {
		    @Override
			public void run(){
		        try {
					new BTServer(props.getProperty("host"), new UUID(props.getProperty("uuid"), false)).start();
				} catch (IOException e) {
					System.out.println(Util.getDateTimeString() + e.getMessage());
					e.printStackTrace();
				};
		      }

		};
		svr.start();
		// Retrieve the settings from disk
		Terrarium terrarium = null;
		try {
			String json = new String(Files.readAllBytes(Paths.get("settings.json")));
			terrarium = Terrarium.getInstance(json);
		} catch (NoSuchFileException e) {
			terrarium = Terrarium.getInstance();
			terrarium.init();
		} catch (IOException e) {
			e.printStackTrace();
		}
		terrarium.setNow(now);
		// Initialize the devices
		terrarium.initDevices();
		// Initialize device state
		terrarium.initDeviceState();
		// Retrieve the lifecycle values from disk
		try {
			String json = new String(Files.readAllBytes(Paths.get("lifecycle.txt")));
			String lns[] = json.split("\n");
			for (String ln : lns) {
				String lp[] = ln.split("=");
				terrarium.setDeviceLifecycle(lp[0], Integer.parseInt(lp[1]));
			}
		} catch (NoSuchFileException e) {
		} catch (IOException e) {
			System.out.println(Util.getDateTimeString() + e.getMessage());
			e.printStackTrace();
		}
		// Initialize the Temperature sensors
		terrarium.initSensors();
		int tterr = terrarium.getTerrariumTemperature();
		int troom =  terrarium.getRoomTemperature();
		lcd.displayLine1(troom, tterr);
		String ip = "App: " + props.getProperty("host");
		lcd.write(1, ip);
		// Check timers if devices should be on
		terrarium.initTimers(now);
		terrarium.initRules();
		// Get the current datetime
		int currentSec = now.getSecond();
		int currentMin = now.getMinute();
		int currentHour = now.getHour();
		// Start the loop
		System.out.println(Util.getDateTimeString() + " Initialization done, start loop");
		while (true) {
			now = LocalDateTime.now();
			terrarium.setNow(now);
			if (now.getSecond() != currentSec) {
				currentSec = now.getSecond();
				// A second has passed
				// Each second check devices
				terrarium.checkDevices();
				if (now.getMinute() != currentMin) {
					currentMin = now.getMinute();
					// A minute has passed
//					System.out.println(now.format(dtfmt) + " A minute has passed");
					// Each minute
					// - display temperature on LCD line 1
					terrarium.readSensorValues();
					tterr = terrarium.getTerrariumTemperature();
					troom = terrarium.getRoomTemperature();
					lcd.displayLine1(troom, tterr);
					if (!terrarium.isTraceOn()) {
						Util.traceTemperature(Terrarium.traceFolder + "/" +  Terrarium.traceTempFilename, now, "r=%d t=%d", troom, tterr);
					}
					// - check timers
					terrarium.checkTimers();
					// - check sprayerrule
					terrarium.checkSprayerRule();
					// - check rulesets
					terrarium.checkRules();
					// Check if tracing should be switched off (max 1 day)
					terrarium.checkTrace();
				}
				if (now.getHour() != currentHour) {
					// An hour has passed
//					System.out.println(now.format(dtfmt) + " An hour has passed");
					currentHour = now.getHour();
					if (!terrarium.isTraceOn()) {
						// Start trace on the whole hour
						terrarium.setTrace(true);
					}
					// Each hour
					// - decrement lifecycle value
					terrarium.decreaseLifetime(1);
					terrarium.saveLifecycleCounters();
				}
			}
		}
	}
}
