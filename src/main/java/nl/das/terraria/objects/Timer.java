/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 18 June 2021.
 */

package nl.das.terraria.objects;

/**
 * {"device":"light1","index":1,"hour_on":9,"minute_on":0,"hour_off":21,"minute_off":0,"repeat":1,"period":0}
 */
public class Timer {

	private String device;
	private int index;
	private int hour_on;
	private int minute_on;
	private int hour_off;
	private int minute_off;
	private int repeat;
	private int period;

	public Timer() { }

	public Timer(String device, int index, String on, String off, int repeat, int period) {
		this.device = device;
		this.index = index;
		this.hour_on = Integer.parseInt(on.split(":")[0]);
		this.minute_on = Integer.parseInt(on.split(":")[1]);
		this.hour_off = Integer.parseInt(off.split(":")[0]);
		this.minute_off = Integer.parseInt(off.split(":")[1]);
		this.repeat = repeat;
		this.period = period;
	}

	public String getDevice () {
		return this.device;
	}

	public void setDevice (String device) {
		this.device = device;
	}

	public int getIndex () {
		return this.index;
	}

	public void setIndex (int index) {
		this.index = index;
	}

	public int getHour_on () {
		return this.hour_on;
	}

	public void setHour_on (int hour_on) {
		this.hour_on = hour_on;
	}

	public int getMinute_on () {
		return this.minute_on;
	}

	public void setMinute_on (int minute_on) {
		this.minute_on = minute_on;
	}

	public int getHour_off () {
		return this.hour_off;
	}

	public void setHour_off (int hour_off) {
		this.hour_off = hour_off;
	}

	public int getMinute_off () {
		return this.minute_off;
	}

	public void setMinute_off (int minute_off) {
		this.minute_off = minute_off;
	}

	public int getRepeat () {
		return this.repeat;
	}

	public void setRepeat (int repeat) {
		this.repeat = repeat;
	}

	public int getPeriod () {
		return this.period;
	}

	public void setPeriod (int period) {
		this.period = period;
	}


}
