/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 08 Aug 2021.
 */


package nl.das.terrariumpi.objects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.annotation.JsonbTransient;
import nl.das.terrariumpi.Util;

/**
 * Pi 2B  - pi4j pin (device)
 * ==========================
 * pin 11 - GPIO-00  (light1)
 * pin 12 - GPIO-01  (light2)
 * pin 13 - GPIO-02  (light3)
 * pin 15 - GPIO-03  (light4)
 * pin 16 - GPIO-04  (light5)
 * pin 29 - GPIO-21  (pump)
 * pin 31 - GPIO-22  (sprayer)
 * pin 33 - GPIO-23  (mist)
 * pin 35 - GPIO-24  (fan_in)
 * pin 37 - GPIO-25  (fan_out)
 *
 */
public class Terrarium {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Terrarium.class);

	private static DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static final int NR_OF_DEVICES = 10;
	public static final int NR_OF_RULESETS = 2;
	public static final int NR_OF_RULES = 2;
	public static final int NR_OF_ACTIONS_PER_RULE = 2;
	public static final int NR_OF_ACTIONS_PER_SPRAYERRULE = 4;
	public static final int ONPERIOD_ENDLESS = -1;
	public static final int ONPERIOD_UNTILL_IDEAL = -2;
	public static final long ONPERIOD_OFF = 0L;

	public String[] deviceList   = {"light1", "light2", "light3", "light4", "light5", "pump", "sprayer", "mist", "fan_in", "fan_out"};
	public int[] timersPerDevice = {1,         1,        1,        1,        1,        3,      1,         3,      3,        3       };
	public Timer[] timers;
	public Ruleset[] rulesets = new Ruleset[NR_OF_RULESETS];
	public SprayerRule sprayerRule;
	@JsonbTransient private boolean sprayerRuleActive = false;
	@JsonbTransient private long sprayerRuleDelayEndtime;
	@JsonbTransient	private Device[] devices = new Device[NR_OF_DEVICES];
	@JsonbTransient private DeviceState[] devStates = new DeviceState[NR_OF_DEVICES];
	@JsonbTransient private boolean test = false;
	@JsonbTransient private Sensors sensors = new Sensors();
	@JsonbTransient private LocalDateTime now;
	@JsonbTransient private boolean traceOn = true;
	@JsonbTransient private long traceStartTime;
	@JsonbTransient private int[] ruleActiveForDevice = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

	@JsonbTransient private static Terrarium instance = null;

	public Terrarium() { }

	@JsonbTransient
	public static Terrarium getInstance() {
		if (instance == null) {
			instance = new Terrarium();
		}
		return instance;
	}

	@JsonbTransient
	public static Terrarium getInstance(String json) {
		Jsonb jsonb = JsonbBuilder.create();
		instance = jsonb.fromJson(json, Terrarium.class);

		return instance;
	}

	/******************************** Special methods ******************************************/

	@JsonbTransient
	public void setNow(LocalDateTime now) {
		this.now = now;
	}

	@JsonbTransient
	public LocalDateTime getNow() {
		return this.now;
	}

	@JsonbTransient
	public void init() {
		// Count total number of timers
		int nrOfTimers = 0;
		for (int i : this.timersPerDevice) {
			nrOfTimers += i;
		}
		this.timers = new Timer[nrOfTimers];
		// Initialize Timers
		int timerIndex = 0;
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			for (int dix = 0; dix < this.timersPerDevice[i]; dix++) {
				this.timers[timerIndex] = new Timer(this.deviceList[i], dix + 1, "00:00", "00:00", 0, 0);
				timerIndex++;
			}
		}
		// Initialize rulesets
		this.rulesets[0] = new Ruleset(1, "no", "", "", 0,
			new Rule[] {
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) }),
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) })
			}
		);
		this.rulesets[1] = new Ruleset(1, "no", "", "", 0,
			new Rule[] {
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) }),
				new Rule(0, new Action[] { new Action("no device", 0), new Action("no device", 0) })
			}
		);
		// Initialize sprayerrule
		this.sprayerRule = new SprayerRule(0, new Action[] {
				new Action("no device", 0),
				new Action("no device", 0),
				new Action("no device", 0),
				new Action("no device", 0)
			}
		);
		saveSettings();
	}

	@JsonbTransient
	public void initDevices() {
		// Initialize devices
		this.devices[0] = new Device(this.deviceList[0], RaspiPin.GPIO_00, PinState.HIGH);
		this.devices[1] = new Device(this.deviceList[1], RaspiPin.GPIO_01, PinState.HIGH);
		this.devices[2] = new Device(this.deviceList[2], RaspiPin.GPIO_02, PinState.HIGH);
		this.devices[3] = new Device(this.deviceList[3], RaspiPin.GPIO_03, PinState.HIGH);
		this.devices[4] = new Device(this.deviceList[4], RaspiPin.GPIO_04, PinState.HIGH, true);
		this.devices[5] = new Device(this.deviceList[5], RaspiPin.GPIO_21, PinState.LOW);
		this.devices[6] = new Device(this.deviceList[6], RaspiPin.GPIO_22, PinState.LOW);
		this.devices[7] = new Device(this.deviceList[7], RaspiPin.GPIO_23, PinState.LOW);
		this.devices[8] = new Device(this.deviceList[8], RaspiPin.GPIO_24, PinState.LOW);
		this.devices[9] = new Device(this.deviceList[9], RaspiPin.GPIO_25, PinState.LOW);
	}

	@JsonbTransient
	public void initMockDevices() {
		// Initialize devices
		this.devices[0] = new Device(this.deviceList[0], false);
		this.devices[1] = new Device(this.deviceList[1], false);
		this.devices[2] = new Device(this.deviceList[2], false);
		this.devices[3] = new Device(this.deviceList[3], false);
		this.devices[4] = new Device(this.deviceList[4], true);
		this.devices[5] = new Device(this.deviceList[5], false);
		this.devices[6] = new Device(this.deviceList[6], false);
		this.devices[7] = new Device(this.deviceList[7], false);
		this.devices[8] = new Device(this.deviceList[8], false);
		this.devices[9] = new Device(this.deviceList[9], false);
	}


	@JsonbTransient
	public String getProperties() {
		String json = "";
		json += "{\"tcu\":\"TERRARIUM\",\"nr_of_timers\":" + this.timers.length + ",\"nr_of_programs\":" + NR_OF_RULESETS + ",";
		json += "\"devices\": [";
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			json += "{\"device\":\"" + this.devices[i].getName() + "\", \"nr_of_timers\":" + this.timersPerDevice[i] + ", \"lc_counted\":";
			json += (this.devices[i].hasLifetime() ? "true}" : "false}");
			if (i != (NR_OF_DEVICES - 1)) {
				json += ",";
			}
		}
		json += "]}";
		return json;
	}

	@JsonbTransient
	public void saveSettings() {
		Jsonb jsonb = JsonbBuilder.create();
		try {
			Files.deleteIfExists(Paths.get("settings.json"));
			Files.writeString(Paths.get("settings.json"), jsonb.toJson(this), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@JsonbTransient
	public void saveLifecycleCounters() {
		try {
			String json = "";
			Files.deleteIfExists(Paths.get("lifecycle.txt"));
			for (int i = 0; i < NR_OF_DEVICES; i++) {
				if (this.devices[i].hasLifetime()) {
					json += this.devices[i].getName() + "=" + this.devStates[i].getLifetime() + "\n";
				}
			}
			Files.writeString(Paths.get("lifecycle.txt"), json, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@JsonbTransient
	public void setLifecycleCounter(String device, int value) {
		this.devStates[getDeviceIndex(device)].setLifetime(value);
		saveLifecycleCounters();
	}

	@JsonbTransient
	public void setTrace(boolean on) {
		this.traceOn = on;
		if (on) {
			this.traceStartTime = Util.now(this.now);
		}
	}

	@JsonbTransient
	public boolean isTraceOn() {
		return this.traceOn;
	}

	@JsonbTransient
	public void checkTrace () {
		// Max one day of tracing
		if (Util.now(this.now)  > (this.traceStartTime + (1440 * 60))) {
			Util.traceState(this.now, "stop");
			Util.traceTemperature(this.now, "stop");
			setTrace(false);
		}

	}

	/********************************************* Sensors *********************************************/

	@JsonbTransient
	public void initSensors () {
		this.sensors = new Sensors();
		this.sensors.readSensorValues();
	}

	@JsonbTransient
	public void readSensorValues() {
		if (!this.test) {
			this.sensors.readSensorValues();
		}
	}

	@JsonbTransient
	public Sensors getSensors() {
		if (!this.test) {
			this.sensors.readSensorValues();
		}
		return this.sensors;
	}

	@JsonbTransient
	public void setSensors(int troom, int tterrarium) {
		this.test = true;
		this.sensors.getSensors()[0].setTemperature(troom);
		this.sensors.getSensors()[1].setTemperature(tterrarium);
	}

	@JsonbTransient
	public void setTestOff () {
		this.test = false;
	}

	@JsonbTransient
	public int getRoomTemperature() {
		return this.sensors.getSensors()[0].getTemperature();
	}

	@JsonbTransient
	public int getTerrariumTemperature() {
		return this.sensors.getSensors()[1].getTemperature();
	}


	/********************************************* Timers *********************************************/

	@JsonbTransient
	public Timer[] getTimersForDevice (String device) {
		Timer[] tmrs;
		if (device == "") {
			tmrs = this.timers;
		} else {
			int nr = this.timersPerDevice[getDeviceIndex(device)];
			tmrs = new Timer[nr];
			int i = 0;
			for (Timer t : this.timers) {
				if (t.getDevice().equalsIgnoreCase(device)) {
					tmrs[i] = t;
					i++;
				}
			}
		}
		return tmrs;
	}

	@JsonbTransient
	public void replaceTimers(Timer[] tmrs) {
		for (Timer tnew : tmrs) {
			for (int i = 0; i < this.timers.length; i++) {
				Timer told = this.timers[i];
				if (told.getDevice().equalsIgnoreCase(tnew.getDevice()) && (told.getIndex() == tnew.getIndex())) {
					this.timers[i] = tnew;
				}
			}
		}
	}

	public void initTimers(LocalDateTime now) {
		for (Timer t : this.timers) {
			int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
			int timerMinutesOff = (t.getHour_off() * 60) + t.getMinute_off();
			int curMinutes = (now.getHour() * 60) + now.getMinute();
			if ((curMinutes >= timerMinutesOn) && (curMinutes <= timerMinutesOff)) {
				setDeviceOn(t.getDevice(), -1L);
			}
		}
	}

	@JsonbTransient
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
		for (Timer t : this.timers) {
			if (t.getRepeat() != 0) { // Timer is not active
				if (t.getPeriod() == 0) { // Timer has an on and off
					int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
					int timerMinutesOff = (t.getHour_off() * 60) + t.getMinute_off();
					int curMinutes = (this.now.getHour() * 60) + this.now.getMinute();
					if (curMinutes == timerMinutesOn) {
						if (!isDeviceOn(t.getDevice())) {
							setDeviceOn(t.getDevice(), -1L);
							if (t.getDevice().equalsIgnoreCase("mist")) {
								setDeviceOff("fan_in");
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
						setDeviceOff(t.getDevice());
						// Make the rules of all relevant devices active again
						for (int i = 0; i < this.ruleActiveForDevice.length; i++) {
							if (getRuleActive(this.devices[i].getName()) == 0) {
								setRuleActive(this.devices[i].getName(), 1);
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

	private Timer getSprayerTimer() {
		for (int i = 0; i < this.timers.length; i++) {
			if (this.timers[i].getDevice().equalsIgnoreCase("sprayer") && (this.timers[i].getIndex() == 1)) {
				return this.timers[i];
			}
		}
		return null;
	}

	/**************************************************** Ruleset ******************************************************/

	@JsonbTransient
	public Ruleset getRuleset(int nr) {
		return this.rulesets[nr - 1];
	}

	@JsonbTransient
	public void replaceRuleset(int nr, Ruleset ruleset) {
		this.rulesets[nr - 1] = ruleset;
	}

	@JsonbTransient
	public int getRuleActive(String device) {
		return this.ruleActiveForDevice[getDeviceIndex(device)];
	}

	@JsonbTransient
	public void setRuleActive(String device, int value) {
		this.ruleActiveForDevice[getDeviceIndex(device)] = value;
	}

	@JsonbTransient
	public void initRules() {
		// Register device as being under control of a rule
		for (Ruleset rs : this.rulesets) {
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
		for (Action a : this.sprayerRule.getActions()) {
			if (!a.getDevice().equalsIgnoreCase("no device")) {
				setRuleActive(a.getDevice(), 1);
			}
		}
		// Set sprayerRuleDelayEndtime = start time in minutes + delay in minutes
		Timer t = getSprayerTimer();
		this.sprayerRuleDelayEndtime = (t.getHour_on() * 60) + t.getMinute_off();
		this.sprayerRuleDelayEndtime += this.sprayerRule.getDelay();
	}

	@JsonbTransient
	/**
	 * Execute the rules as defined in both rulesets.
	 * These need to be executed every minute.
	 */
	public void checkRules() {
		for (Ruleset rs : this.rulesets) {
			if (rs.active(this.now)) {
				//
				for (Rule r : rs.getRules()) {
					if ((r.getValue() < 0) && (getTerrariumTemperature() < -r.getValue())) {
						for (Action a : r.getActions()) {
							executeAction(a);
						}
					} else if ((r.getValue() < 0) && (getTerrariumTemperature() >= rs.getIdealTemp())) {
						for (Action a : r.getActions()) {
							if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice()) && (getRuleActive(a.getDevice()) == 1)) {
								setDeviceOff(a.getDevice());
							}
						}
					} else if ((r.getValue() > 0) && (getTerrariumTemperature() > r.getValue())) {
						for (Action a : r.getActions()) {
							executeAction(a);
						}
					} else if ((r.getValue() > 0) && (getTerrariumTemperature() <= rs.getIdealTemp())) {
						for (Action a : r.getActions()) {
							if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice()) && (getRuleActive(a.getDevice()) == 1)) {
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

	@JsonbTransient
	private void executeAction(Action a) {
		if (!a.getDevice().equalsIgnoreCase("no device") && (getRuleActive(a.getDevice()) == 1)) {
			long endtime = 0;
			if (a.getOnPeriod() > 0) {
				// onPeriod is seconds (max 3600)
				endtime = Util.now(this.now) + a.getOnPeriod();
			} else {
				endtime = a.getOnPeriod();
			}
			if (!isDeviceOn(a.getDevice())) {
				setDeviceOn(a.getDevice(), endtime);
			}
		}
	}

	@JsonbTransient
	/**
	 * Execute the rules as defined in sprayerrule.
	 * These need to be executed every minute.
	 */
	public void checkSprayerRule() {
		if (this.sprayerRuleActive) {
			int curminutes = (this.now.getHour() * 60) + this.now.getMinute();
			if (curminutes == this.sprayerRuleDelayEndtime) {
				for (Action a : this.sprayerRule.getActions()) {
					if (!a.getDevice().equalsIgnoreCase("no device")) {
						executeAction(a);
						// Make the temperature rule temporarily inactive
						if (getRuleActive(a.getDevice()) == 1) {
							setRuleActive(a.getDevice(), 0);
						}
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

	@JsonbTransient
	public void initDeviceState() {
		// Initialize device states
		for (int i = 0; i< NR_OF_DEVICES; i++) {
			this.devStates[i] = new DeviceState(this.deviceList[i]);
		}
	}

	@JsonbTransient
	public boolean isDeviceOn(String device) {
		return this.devStates[getDeviceIndex(device)].getOnPeriod() != 0L;
	}

	/**
	 * @param device
	 * @param endtime in Epoch seconds or -1 or -2
	 */
	@JsonbTransient
	public void setDeviceOn(String device, long endtime) {
		this.devices[getDeviceIndex(device)].switchOn();
		this.devStates[getDeviceIndex(device)].setOnPeriod(endtime);
		if (endtime > 0L) {
			String dt = Util.ofEpochSecond(endtime).format(dtfmt);
			Util.traceState(this.now, "%s 1 %s", device, dt);
		} else {
			Util.traceState(this.now, "%s 1 %d", device, endtime);
		}
	}

	@JsonbTransient
	public void setDeviceOff(String device) {
		this.devices[getDeviceIndex(device)].switchOff();
		this.devStates[getDeviceIndex(device)].setOnPeriod(ONPERIOD_OFF);
		Util.traceState(this.now, "%s 0", device);
	}

	@JsonbTransient
	public void setDeviceManualOn(String device) {
		this.devStates[getDeviceIndex(device)].setManual(true);
	}

	@JsonbTransient
	public void setDeviceManualOff(String device) {
		this.devStates[getDeviceIndex(device)].setManual(false);
	}

	@JsonbTransient
	public void setDeviceLifecycle(String device, int value) {
		this.devStates[getDeviceIndex(device)].setLifetime(value);
	}

	@JsonbTransient
	public void decreaseLifetime(int nrOfHours) {
		for (Device d : this.devices) {
			if (d.hasLifetime()) {
				this.devStates[getDeviceIndex(d.getName())].decreaseLifetime(nrOfHours);
				saveLifecycleCounters();
			}
		}
	}

	@JsonbTransient
	public String getState() {
		String json = "{\"trace\":\"" +  (this.traceOn ? "on" : "off") + "\",\"state\": [";
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			json += this.devStates[i].toJson();
			if (i != (NR_OF_DEVICES - 1)) {
				json += ",";
			}
		}
		json += "]}";
		return json;
	}

	@JsonbTransient
	public int getDeviceIndex (String device) {
		int ix = -1;
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			if (this.deviceList[i].equalsIgnoreCase(device)) {
				ix = i;
				break;
			}
		}
		return ix;
	}

	@JsonbTransient
	/**
	 * Check if a device needs to be switched off when it has a onPeriod > 0
	 * This check needs to be done every second since the onPeriod is defined in Epoch-seconds.
	 */
	public void checkDevices() {
		for (DeviceState d : this.devStates) {
			if (d.getOnPeriod() > 0) {
				// Device has an end time defined
				if (Util.now(this.now) >= d.getOnPeriod()) {
					setDeviceOff(d.getName());
					// Make the rules of all relevant devices active again
					for (int i = 0; i < this.ruleActiveForDevice.length; i++) {
						if (getRuleActive(this.devices[i].getName()) == 0) {
							setRuleActive(this.devices[i].getName(), 1);
						}
					}
				}
			}
		}
	}

	/********************************************* Getters and Setters ******************************************************/

	public Timer[] getTimers () {
		return this.timers;
	}

	public void setTimers (Timer[] timers) {
		this.timers = timers;
	}

	public Ruleset[] getRulesets () {
		return this.rulesets;
	}

	public void setRulesets (Ruleset[] rulesets) {
		this.rulesets = rulesets;
	}

	public SprayerRule getSprayerRule () {
		return this.sprayerRule;
	}

	public void setSprayerRule (SprayerRule sprayerRule) {
		this.sprayerRule = sprayerRule;
	}

	public DeviceState[] getDeviceStates() {
		return this.devStates;
	}
}