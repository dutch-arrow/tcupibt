/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 28 Jul 2022.
 */


package nl.das.terraria.objects;

import javax.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class TerrariumConfig {
	private String[] deviceList;
	private int[] timersPerDevice;
	private Timer[] timers;
	private Ruleset[] rulesets;
	private SprayerRule sprayerRule;

	public String[] getDeviceList () {
		return this.deviceList;
	}
	public void setDeviceList (String[] deviceList) {
		this.deviceList = deviceList;
	}
	public int[] getTimersPerDevice () {
		return this.timersPerDevice;
	}
	public void setTimersPerDevice (int[] timersPerDevice) {
		this.timersPerDevice = timersPerDevice;
	}
	public Timer[] getTimers () {
		return this.timers;
	}
	public void setTimers (Timer[] timers) {
		this.timers = timers;
	}
	@JsonbTransient
	public void setTimer(int ix, Timer t) {
		this.timers[ix] = t;
	}
	public Ruleset[] getRulesets () {
		return this.rulesets;
	}
	public void setRulesets (Ruleset[] rulesets) {
		this.rulesets = rulesets;
	}
	@JsonbTransient
	public void setRuleset(int ix, Ruleset r) {
		this.rulesets[ix] = r;
	}
	public SprayerRule getSprayerRule () {
		return this.sprayerRule;
	}
	public void setSprayerRule (SprayerRule sprayerRule) {
		this.sprayerRule = sprayerRule;
	}

}
