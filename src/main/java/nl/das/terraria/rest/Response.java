/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 05 Aug 2022.
 */


package nl.das.terraria.rest;

import java.util.UUID;

import javax.json.JsonObject;

/**
 *
 */
public class Response {

	private UUID msgId;
	private String command;
	private JsonObject response;

	public Response() { }

	public Response(UUID msgId, String cmd) {
		this.msgId = msgId;
		this.command = cmd;
	}

	public UUID getMsgId () {
		return this.msgId;
	}
	public void setMsgId (UUID msgId) {
		this.msgId = msgId;
	}
	public String getCommand () {
		return this.command;
	}
	public void setCommand (String command) {
		this.command = command;
	}
	public JsonObject getResponse () {
		return this.response;
	}
	public void setResponse (JsonObject response) {
		this.response = response;
	}


}
