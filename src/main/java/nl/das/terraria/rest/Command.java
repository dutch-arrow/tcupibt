/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 23 Jul 2022.
 */


package nl.das.terraria.rest;

import javax.json.JsonObject;

/**
 *
 */
public class Command {

	private String cmd;
	private JsonObject data;

	public String getCmd () {
		return this.cmd;
	}
	public void setCmd (String cmd) {
		this.cmd = cmd;
	}
	public JsonObject getData () {
		return this.data;
	}
	public void setData (JsonObject data) {
		this.data = data;
	}
}
