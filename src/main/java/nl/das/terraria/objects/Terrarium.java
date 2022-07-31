/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 08 Aug 2021.
 */


package nl.das.terraria.objects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import nl.das.terraria.Util;

/**
 * Pi 3B+ - pi4j pin (device)
 * ==========================
 * pin 11 - GPIO-00  (light1)
 * pin 12 - GPIO-01  (light2)
 * pin 13 - GPIO-02  (light3)
 * pin 15 - GPIO-03  (light4)
 * pin 16 - GPIO-04  (uvlight)
 * pin 18 - GPIO-05  (light6)
 * pin 29 - GPIO-21  (pump)
 * pin 31 - GPIO-22  (sprayer)
 * pin 33 - GPIO-23  (mist)
 * pin 35 - GPIO-24  (fan_in)
 * pin 37 - GPIO-25  (fan_out)
 * pin 36 - GPIO-27  (temperature external DHT22)
 * pin  7 - GPIO-04  (temperature internal DS18B20)
 * pin  3 - GPIO-08  (LCD SDA)
 * pin  5 - GPIO-09  (LCD SCL)
 *
 */
public class Terrarium {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Terrarium.class);

	private static DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static int NR_OF_DEVICES = 11;
	public static final int NR_OF_RULESETS = 2;
	public static final int NR_OF_RULES = 2;
	public static final int NR_OF_ACTIONS_PER_RULE = 2;
	public static final int NR_OF_ACTIONS_PER_SPRAYERRULE = 4;
	public static final int ONPERIOD_ENDLESS = -1;
	public static final int ONPERIOD_UNTIL_IDEAL = -2;
	public static final int ONPERIOD_OFF = 0;
	public static int maxNrOfTraceDays = 30;

//	public String[] deviceList   = {"light1", "light2", "light3", "light4", "uvlight", "light6", "pump", "sprayer", "mist", "fan_in", "fan_out"};
//	public int[] timersPerDevice = {1,         1,        1,        1,        1,         1,        3,      5,         3,      3,        3       };
//	public Timer[] timers;
//	public Ruleset[] rulesets = new Ruleset[NR_OF_RULESETS];
//	public SprayerRule sprayerRule;
	public static TerrariumConfig cfg = new TerrariumConfig();
	private static Map<String, Pin> devicePin;
	private boolean sprayerRuleActive = false;
	private long sprayerRuleDelayEndtime;
	private static Device[] devices = new Device[NR_OF_DEVICES];
	private static DeviceState[] devStates = new DeviceState[NR_OF_DEVICES];
	private boolean test = false;
	private Sensors sensors = new Sensors();
	private LocalDateTime now;
	private boolean traceOn = false;
	private long traceStartTime;
	private static int[] ruleActiveForDevice;
	private boolean fan_in_state = false;
	private boolean fan_out_state = false;
	private static Terrarium instance = null;

	public static String traceFolder = "tracefiles";
	public static String traceStateFilename;
	public static String traceTempFilename;

	static {
        Map<String, Pin> aMap = new HashMap<>();
        aMap.put("light1",  RaspiPin.GPIO_00);
        aMap.put("light2",  RaspiPin.GPIO_01);
        aMap.put("light3",  RaspiPin.GPIO_02);
        aMap.put("light4",  RaspiPin.GPIO_03);
        aMap.put("uvlight", RaspiPin.GPIO_04);
        aMap.put("light6",  RaspiPin.GPIO_05);
        aMap.put("pump",    RaspiPin.GPIO_21);
        aMap.put("sprayer", RaspiPin.GPIO_22);
        aMap.put("mist",    RaspiPin.GPIO_23);
        aMap.put("fan_in",  RaspiPin.GPIO_24);
        aMap.put("fan_out", RaspiPin.GPIO_25);
        devicePin = Collections.unmodifiableMap(aMap);
    };

	public Terrarium() {
		String[] deviceList   = {"light1", "light2", "light3", "light4", "uvlight", "light6", "pump", "sprayer", "mist", "fan_in", "fan_out"};
		int[] timersPerDevice = {1,         1,        1,        1,        1,         1,        3,      5,         3,      3,        3       };
		Terrarium.cfg.setDeviceList(deviceList);
		Terrarium.cfg.setTimersPerDevice(timersPerDevice);
		Terrarium.cfg.setRulesets(new Ruleset[NR_OF_RULESETS]);
	}

	public static Terrarium getInstance() {
		if (instance == null) {
			instance = new Terrarium();
		}
		return instance;
	}

	public static Terrarium getInstance(String json) {
		instance = new Terrarium();
		Jsonb jsonb = JsonbBuilder.create();
		cfg = jsonb.fromJson(json, TerrariumConfig.class);
		NR_OF_DEVICES = Terrarium.cfg.getDeviceList().length;
		devices = new Device[NR_OF_DEVICES];
		devStates = new DeviceState[NR_OF_DEVICES];
		ruleActiveForDevice = new int[NR_OF_DEVICES];
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			ruleActiveForDevice[i] = -1;
		}
		return instance;
	}

	/******************************** Special methods ******************************************/

	public void setNow(LocalDateTime now) {
		this.now = now;
	}

	public LocalDateTime getNow() {
		return this.now;
	}

	public void init() {
		// Count total number of timers
		int nrOfTimers = 0;
		for (int i : Terrarium.cfg.getTimersPerDevice()) {
			nrOfTimers += i;
		}
		Terrarium.cfg.setTimers(new Timer[nrOfTimers]);
		// Initialize Timers
		int timerIndex = 0;
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			for (int dix = 0; dix < Terrarium.cfg.getTimersPerDevice()[i]; dix++) {
				Terrarium.cfg.setTimer(timerIndex, new Timer(Terrarium.cfg.getDeviceList()[i], dix + 1, "00:00", "00:00", 0, 0));
				timerIndex++;
			}
		}
		// Initialize rulesets
		Terrarium.cfg.setRuleset(0, new Ruleset(1, "no", "", "", 0,
			new Rule[] {
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) }),
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) })
			}
		));
		Terrarium.cfg.setRuleset(1, new Ruleset(1, "no", "", "", 0,
			new Rule[] {
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) }),
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) })
			}
		));
		// Initialize sprayerrule
		Terrarium.cfg.setSprayerRule(new SprayerRule(0, new Action[] {
				new Action("no device", 0),
				new Action("no device", 0),
				new Action("no device", 0),
				new Action("no device", 0)
			}
		));
		saveSettings();
	}

	public void initDevices() {
		// Initialize devices
		for (int i = 0; i < Terrarium.cfg.getDeviceList().length; i++) {
			if (Terrarium.cfg.getDeviceList()[i].equalsIgnoreCase("uvlight")) {
				Terrarium.devices[i] = new Device(Terrarium.cfg.getDeviceList()[i], devicePin.get(Terrarium.cfg.getDeviceList()[i]), PinState.LOW, true);
			} else {
				Terrarium.devices[i] = new Device(Terrarium.cfg.getDeviceList()[i], devicePin.get(Terrarium.cfg.getDeviceList()[i]), PinState.LOW);
			}
		}
	}

	public void initMockDevices() {
		// Initialize devices
		Terrarium.devices[ 0] = new Device(Terrarium.cfg.getDeviceList()[ 0], false);
		Terrarium.devices[ 1] = new Device(Terrarium.cfg.getDeviceList()[ 1], false);
		Terrarium.devices[ 2] = new Device(Terrarium.cfg.getDeviceList()[ 2], false);
		Terrarium.devices[ 3] = new Device(Terrarium.cfg.getDeviceList()[ 3], false);
		Terrarium.devices[ 4] = new Device(Terrarium.cfg.getDeviceList()[ 4], true);
		Terrarium.devices[ 5] = new Device(Terrarium.cfg.getDeviceList()[ 5], false);
		Terrarium.devices[ 6] = new Device(Terrarium.cfg.getDeviceList()[ 6], false);
		Terrarium.devices[ 7] = new Device(Terrarium.cfg.getDeviceList()[ 7], false);
		Terrarium.devices[ 8] = new Device(Terrarium.cfg.getDeviceList()[ 8], false);
		Terrarium.devices[ 9] = new Device(Terrarium.cfg.getDeviceList()[ 9], false);
		Terrarium.devices[10] = new Device(Terrarium.cfg.getDeviceList()[10], false);
	}

	public String getProperties() {
		String json = "";
		json += "{\"tcu\":\"TERRARIUMPI\",\"nr_of_timers\":" + Terrarium.cfg.getTimers().length + ",\"nr_of_programs\":" + NR_OF_RULESETS + ",";
		json += "\"devices\": [";
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			json += "{\"device\":\"" + Terrarium.devices[i].getName() + "\", \"nr_of_timers\":" + Terrarium.cfg.getTimersPerDevice()[i] + ", \"lc_counted\":";
			json += (Terrarium.devices[i].hasLifetime() ? "true}" : "false}");
			if (i != (NR_OF_DEVICES - 1)) {
				json += ",";
			}
		}
		json += "]}";
		return json;
	}

	public void saveSettings() {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true).withNullValues(true));
		try {
			Files.deleteIfExists(Paths.get("settings.json"));
			Files.writeString(Paths.get("settings.json"), jsonb.toJson(Terrarium.cfg), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveLifecycleCounters() {
		try {
			String json = "";
			Files.deleteIfExists(Paths.get("lifecycle.txt"));
			for (int i = 0; i < NR_OF_DEVICES; i++) {
				if (Terrarium.devices[i].hasLifetime()) {
					json += Terrarium.devices[i].getName() + "=" + Terrarium.devStates[i].getLifetime() + "\n";
				}
			}
			Files.writeString(Paths.get("lifecycle.txt"), json, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setLifecycleCounter(String device, int value) {
		Terrarium.devStates[getDeviceIndex(device)].setLifetime(value);
		saveLifecycleCounters();
	}

	public void setTrace(boolean on) {
		if (on) {
			this.traceOn = on;
			this.traceStartTime = Util.now(this.now);
			traceStateFilename = Util.createStateTraceFile(traceFolder, this.now);
			traceTempFilename  = Util.createTemperatureTraceFile(traceFolder, this.now);
			Util.traceState(traceFolder + "/" + traceStateFilename, this.now, "start");
			Util.traceTemperature(traceFolder + "/" + traceTempFilename, this.now, "start");
			for (String d : Terrarium.cfg.getDeviceList()) {
				Util.traceState(traceFolder + "/" + traceStateFilename, this.now, "%s %s", d, isDeviceOn(d) ? "1" : "0");
			}
		} else if (this.traceOn) {
			Util.traceState(traceFolder + "/" + traceStateFilename, this.now, "stop");
			Util.traceTemperature(traceFolder + "/" + traceTempFilename, this.now, "stop");
			this.traceOn = on;
		}
	}

	public boolean isTraceOn() {
		return this.traceOn;
	}

	public void checkTrace () {
		// Max one day of tracing
		if ((Util.now(this.now)  >= (this.traceStartTime + (1440 * 60))) && isTraceOn()) {
			setTrace(false);
			setTrace(true);
		}

	}

	/********************************************* Sensors *********************************************/

	public void initSensors () {
		this.sensors = new Sensors();
		this.sensors.readSensorValues();
	}

	public void readSensorValues() {
		if (!this.test) {
			this.sensors.readSensorValues();
		}
	}

	public Sensors getSensors() {
		if (!this.test) {
			this.sensors.readSensorValues();
		}
		return this.sensors;
	}

	public void setSensors(int troom, int tterrarium) {
		this.test = true;
		this.sensors.getSensors()[0].setTemperature(troom);
		this.sensors.getSensors()[1].setTemperature(tterrarium);
	}

	public void setTestOff () {
		this.test = false;
	}

	public int getRoomTemperature() {
		return this.sensors.getSensors()[0].getTemperature();
	}

	public int getTerrariumTemperature() {
		return this.sensors.getSensors()[1].getTemperature();
	}


	/********************************************* Timers *********************************************/

	public Timer[] getTimersForDevice (String device) {
		Timer[] tmrs;
		if (device == "") {
			tmrs = Terrarium.cfg.getTimers();
		} else {
			int nr = Terrarium.cfg.getTimersPerDevice()[getDeviceIndex(device)];
			tmrs = new Timer[nr];
			int i = 0;
			for (Timer t : Terrarium.cfg.getTimers()) {
				if (t.getDevice().equalsIgnoreCase(device)) {
					tmrs[i] = t;
					i++;
				}
			}
		}
		return tmrs;
	}

	public void replaceTimers(Timer[] tmrs) {
		for (Timer tnew : tmrs) {
			for (int i = 0; i < Terrarium.cfg.getTimers().length; i++) {
				Timer told = Terrarium.cfg.getTimers()[i];
				if (told.getDevice().equalsIgnoreCase(tnew.getDevice()) && (told.getIndex() == tnew.getIndex())) {
					Terrarium.cfg.setTimer(i, tnew);
				}
			}
		}
	}

	public void initTimers(LocalDateTime now) {
		for (Timer t : Terrarium.cfg.getTimers()) {
			int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
			int timerMinutesOff = (t.getHour_off() * 60) + t.getMinute_off();
			int curMinutes = (now.getHour() * 60) + now.getMinute();
			if ((curMinutes >= timerMinutesOn) && (curMinutes <= timerMinutesOff)) {
				setDeviceOn(t.getDevice(), -1L);
			}
		}
	}

	/**
	 * Check the timers if a device needs to be switched on or off.
	 * These need to be executed every minute.
	 *
	 * A device can be switched on by a rule. If its is and it should now be switched on
	 * because of a timer then the rule should not interfere, so the rule should be
	 * deactivated until the device is switched off by the timer.
	 * Then the rule should be activated again.
	 */
	public void checkTimers() {
		for (Timer t : Terrarium.cfg.getTimers()) {
			if (t.getRepeat() != 0) { // Timer is not active
				if (t.getPeriod() == 0) { // Timer has an on and off
					int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
					int timerMinutesOff = (t.getHour_off() * 60) + t.getMinute_off();
					int curMinutes = (this.now.getHour() * 60) + this.now.getMinute();
					if (curMinutes == timerMinutesOn) {
						if (!isDeviceOn(t.getDevice())) {
							setDeviceOn(t.getDevice(), -1L);
							if (t.getDevice().equalsIgnoreCase("mist")) {
								this.fan_in_state = isDeviceOn("fan_in");
								setDeviceOff("fan_in");
								this.fan_out_state = isDeviceOn("fan_out");
								setDeviceOff("fan_out");
								// and deactivate the rules for fan_in and fan_out and switch them off
								setRuleActive("fan_in", 0);
								setRuleActive("fan_out", 0);
							} else if (t.getDevice().equalsIgnoreCase("fan_in")) {
								setRuleActive("fan_in", 0);
								setRuleActive("fan_out", 0);
							} else if (t.getDevice().equalsIgnoreCase("fan_out")) {
								setRuleActive("fan_in", 0);
								setRuleActive("fan_out", 0);
							}
						}
					} else if ((timerMinutesOff != 0) && (curMinutes == timerMinutesOff)) {
						if (t.getDevice().equalsIgnoreCase("mist")) {
							setDeviceOff(t.getDevice());
							if (this.fan_in_state) {
								setDeviceOn("fan_in", -1L);
								this.fan_in_state = false;
							}
							if (this.fan_out_state) {
								setDeviceOn("fan_out", -1L);
								this.fan_out_state = false;
							}
						} else {
							setDeviceOff(t.getDevice());
						}
						// Make the rules of all relevant devices active again
						for (int i = 0; i < Terrarium.ruleActiveForDevice.length; i++) {
							if (getRuleActive(Terrarium.devices[i].getName()) == 0) {
								setRuleActive(Terrarium.devices[i].getName(), 1);
							}
						}
					}
				} else { // Timer has an on and period
					int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
					int curMinutes = (this.now.getHour() * 60) + this.now.getMinute();
					long endtime = Util.now(this.now) + t.getPeriod();
					if (curMinutes == timerMinutesOn) {
						if (!isDeviceOn(t.getDevice())) {
							setDeviceOn(t.getDevice(), endtime);
						}
						if (t.getDevice().equalsIgnoreCase("sprayer")) {
							// If device is "sprayer" then activate sprayer rule
							this.sprayerRuleActive = true;
							// Set sprayerRuleDelayEndtime = start time in minutes + delay in minutes
							this.sprayerRuleDelayEndtime = (t.getHour_on() * 60) + t.getMinute_on();
							this.sprayerRuleDelayEndtime += Terrarium.cfg.getSprayerRule().getDelay();
							// and deactivate the rules for fan_in and fan_out and switch them off
							setRuleActive("fan_in", 0);
							setDeviceOff("fan_in");
							setRuleActive("fan_out", 0);
							setDeviceOff("fan_out");
						}
					}
				}
			}
		}
	}

	/**************************************************** Ruleset ******************************************************/

	public Ruleset getRuleset(int nr) {
		return Terrarium.cfg.getRulesets()[nr - 1];
	}

	public void replaceRuleset(int nr, Ruleset ruleset) {
		Terrarium.cfg.setRuleset(nr - 1, ruleset);
	}

	public int getRuleActive(String device) {
		return Terrarium.ruleActiveForDevice[getDeviceIndex(device)];
	}

	public void setRuleActive(String device, int value) {
		Terrarium.ruleActiveForDevice[getDeviceIndex(device)] = value;
	}

	public void initRules() {
		// Register device as being under control of a rule
		for (Ruleset rs : Terrarium.cfg.getRulesets()) {
			if (rs.getActive().equalsIgnoreCase("yes")) {
				for (Rule r : rs.getRules()) {
					for (Action a : r.getActions()) {
						if (!a.getDevice().equalsIgnoreCase("no device")) {
							setRuleActive(a.getDevice(), 1);
						}
					}
				}
			}
		}
		for (Action a : Terrarium.cfg.getSprayerRule().getActions()) {
			if (!a.getDevice().equalsIgnoreCase("no device")) {
				setRuleActive(a.getDevice(), 1);
			}
		}
	}

	/**
	 * Execute the rules as defined in both rulesets.
	 * These need to be executed every minute.
	 */
	public void checkRules() {
		if (!isSprayerRuleActive()) {
			for (Ruleset rs : Terrarium.cfg.getRulesets()) {
				if (rs.active(this.now)) {
					//
					for (Rule r : rs.getRules()) {
						if ((r.getValue() < 0) && (getTerrariumTemperature() < -r.getValue())) {
							for (Action a : r.getActions()) {
								executeAction(a);
							}
						} else if ((r.getValue() < 0) && (getTerrariumTemperature() >= rs.getTemp_ideal())) {
							for (Action a : r.getActions()) {
								if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice()) && (getRuleActive(a.getDevice()) == 1)
										&& (Terrarium.devStates[getDeviceIndex(a.getDevice())].getOnPeriod() != -1L)) {
									setDeviceOff(a.getDevice());
								}
							}
						} else if ((r.getValue() > 0) && (getTerrariumTemperature() > r.getValue())) {
							for (Action a : r.getActions()) {
								executeAction(a);
							}
						} else if ((r.getValue() > 0) && (getTerrariumTemperature() <= rs.getTemp_ideal())) {
							for (Action a : r.getActions()) {
								if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice()) && (getRuleActive(a.getDevice()) == 1)
										&& (Terrarium.devStates[getDeviceIndex(a.getDevice())].getOnPeriod() != -1L)) {
									setDeviceOff(a.getDevice());
								}
							}
						}
					}
				} else if (rs.getActive().equalsIgnoreCase("yes")) {
					for (Rule r : rs.getRules()) {
						for (Action a : r.getActions()) {
							if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice()) && (getRuleActive(a.getDevice()) == 1)) {
								setDeviceOff(a.getDevice());
							}
						}
					}
				}
			}
		}
	}

	private void executeAction(Action a) {
		if (!a.getDevice().equalsIgnoreCase("no device") && ((getRuleActive(a.getDevice()) == 1) || isSprayerRuleActive())) {
			long endtime = 0;
			if (a.getOn_period() > 0) {
				// onPeriod is seconds (max 3600)
				endtime = Util.now(this.now) + a.getOn_period();
			} else {
				endtime = a.getOn_period();
			}
			if (!isDeviceOn(a.getDevice())) {
				setDeviceOn(a.getDevice(), endtime);
			}
		}
	}

	public SprayerRule getSprayerRule () {
		return Terrarium.cfg.getSprayerRule();
	}

	public void setSprayerRule (SprayerRule sprayerRule) {
		Terrarium.cfg.setSprayerRule(sprayerRule);
	}

	/**
	 * Execute the rules as defined in sprayerrule.
	 * These need to be executed every minute.
	 */
	public void checkSprayerRule() {
		if (this.sprayerRuleActive) {
			int curminutes = (this.now.getHour() * 60) + this.now.getMinute();
			if (curminutes == this.sprayerRuleDelayEndtime) {
				for (Action a : Terrarium.cfg.getSprayerRule().getActions()) {
					if (!a.getDevice().equalsIgnoreCase("no device")) {
						executeAction(a);
					}
				}
				this.sprayerRuleActive = false;
			}
		}
	}

	public boolean isSprayerRuleActive() {
		return this.sprayerRuleActive;
	}

	/**************************************************** Device ******************************************************/

	public void initDeviceState() {
		// Initialize device states
		for (int i = 0; i< NR_OF_DEVICES; i++) {
			Terrarium.devStates[i] = new DeviceState(Terrarium.cfg.getDeviceList()[i]);
		}
	}

	public boolean isDeviceOn(String device) {
		return Terrarium.devStates[getDeviceIndex(device)].getOnPeriod() != 0L;
	}

	/**
	 * @param device
	 * @param endtime in Epoch seconds or -1 or -2
	 */
	public void setDeviceOn(String device, long endtime) {
		Terrarium.devices[getDeviceIndex(device)].switchOn();
		Terrarium.devStates[getDeviceIndex(device)].setOnPeriod(endtime);
		if (endtime > 0L) {
			String dt = Util.ofEpochSecond(endtime).format(dtfmt);
			Util.traceState(traceFolder + "/" + traceStateFilename, this.now, "%s 1 %s", device, dt);
		} else {
			Util.traceState(traceFolder + "/" + traceStateFilename, this.now, "%s 1 %d", device, endtime);
		}
	}

	public void setDeviceOff(String device) {
		Terrarium.devices[getDeviceIndex(device)].switchOff();
		Terrarium.devStates[getDeviceIndex(device)].setOnPeriod(ONPERIOD_OFF);
		Util.traceState(traceFolder + "/" + traceStateFilename, this.now, "%s 0", device);
	}

	public void setDeviceManualOn(String device) {
		Terrarium.devStates[getDeviceIndex(device)].setManual(true);
	}

	public void setDeviceManualOff(String device) {
		Terrarium.devStates[getDeviceIndex(device)].setManual(false);
	}

	public void setDeviceLifecycle(String device, int value) {
		Terrarium.devStates[getDeviceIndex(device)].setLifetime(value);
	}

	public void decreaseLifetime(int nrOfHours) {
		for (Device d : Terrarium.devices) {
			if (d.hasLifetime()) {
				Terrarium.devStates[getDeviceIndex(d.getName())].decreaseLifetime(nrOfHours);
				saveLifecycleCounters();
			}
		}
	}

	public String getState() {
		String json = "{\"trace\":\"" +  (this.traceOn ? "on" : "off") + "\",\"state\": [";
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			json += Terrarium.devStates[i].toJson();
			if (i != (NR_OF_DEVICES - 1)) {
				json += ",";
			}
		}
		json += "]}";
		return json;
	}

	public int getDeviceIndex(String device) {
		int ix = -1;
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			if (Terrarium.cfg.getDeviceList()[i].equalsIgnoreCase(device)) {
				ix = i;
				break;
			}
		}
		return ix;
	}

	/**
	 * Check if a device needs to be switched off when it has a onPeriod > 0
	 * This check needs to be done every second since the onPeriod is defined in Epoch-seconds.
	 */
	public void checkDevices() {
		for (DeviceState d : Terrarium.devStates) {
			if (d.getOnPeriod() > 0) {
				// Device has an end time defined
				if (Util.now(this.now) >= d.getOnPeriod()) {
					setDeviceOff(d.getName());
					if (!isSprayerRuleActive()) {
						// Make the rules of all relevant devices active again
						for (int i = 0; i < Terrarium.ruleActiveForDevice.length; i++) {
							if (getRuleActive(Terrarium.devices[i].getName()) == 0) {
								setRuleActive(Terrarium.devices[i].getName(), 1);
							}
						}
					}
				}
			}
		}
	}

	public Map<String, Pin> getDevicePin () {
		return devicePin;
	}

	public void setDevicePin (Map<String, Pin> devicePin) {
		Terrarium.devicePin = devicePin;
	}

	public Device[] getDevices () {
		return devices;
	}

	public void setDevices (Device[] devices) {
		Terrarium.devices = devices;
	}

	public DeviceState[] getDevStates () {
		return devStates;
	}

	public void setDevStates (DeviceState[] devStates) {
		Terrarium.devStates = devStates;
	}


}
