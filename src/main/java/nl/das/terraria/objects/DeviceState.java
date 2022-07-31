/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 08 Aug 2021.
 */


package nl.das.terraria.objects;

import java.time.LocalTime;

import nl.das.terraria.Util;

/**
 *
 */
public class DeviceState {

//	private static Logger log = LoggerFactory.getLogger(DeviceState.class);

	private String name;
	private long onPeriod; // 0 off, -1 indefinite, -2 until ideal value has been reached, >0 endtime in Epoch-seconds
	private int lifetime; // in hours
	private boolean manual;

	public DeviceState() { }

	public DeviceState(String name) {
		this.name = name;
		this.onPeriod = 0;
		this.lifetime = 0;
		this.manual = false;
	}

	public void decreaseLifetime(int nrOfHours) {
		this.lifetime -= nrOfHours;
	}

	public String getName () {
		return this.name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public int getLifetime () {
		return this.lifetime;
	}

	public void setLifetime (int lifetime) {
		this.lifetime = lifetime;
	}

	public long getOnPeriod () {
		return this.onPeriod;
	}

	public void setOnPeriod (long onPeriod) {
		this.onPeriod = onPeriod;
	}

	public boolean isManual () {
		return this.manual;
	}

	public void setManual (boolean manual) {
		this.manual = manual;
	}

	public String toJson() {
		String end_time = null;
		String state = "";
		if (this.onPeriod == 0) {
			state = String.format("{\"device\":\"%s\",\"state\":\"off\",\"hours_on\":%d,\"manual\":\"%s\"}",
					this.name, this.lifetime, this.manual ? "yes" : "no");
		} else if(this.onPeriod == -1) {
			state = String.format("{\"device\":\"%s\",\"state\":\"on\",\"end_time\":\"no endtime\",\"hours_on\":%d,\"manual\":\"%s\"}",
					this.name, this.lifetime, this.manual ? "yes" : "no");
		} else if(this.onPeriod == -2) {
			state = String.format("{\"device\":\"%s\",\"state\":\"on\",\"end_time\":\"until ideal temperature is reached\",\"hours_on\":%d,\"manual\":\"%s\"}",
					this.name, this.lifetime, this.manual ? "yes" : "no");
		} else if (this.onPeriod > 0) {
			LocalTime t = Util.ofEpochSecond(this.onPeriod).toLocalTime();
			end_time = String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
			state = String.format("{\"device\":\"%s\",\"state\":\"on\",\"end_time\":\"%s\",\"hours_on\":%d,\"manual\":\"%s\"}",
					this.name, end_time, this.lifetime, this.manual ? "yes" : "no");
		}
		return state;
	}
}
