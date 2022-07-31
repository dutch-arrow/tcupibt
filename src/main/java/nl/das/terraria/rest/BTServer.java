/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 21 Jul 2022.
 */


package nl.das.terraria.rest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.bluetooth.DiscoveryAgent;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.json.bind.JsonbException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

import nl.das.terraria.Util;
import nl.das.terraria.objects.Ruleset;
import nl.das.terraria.objects.SprayerRule;
import nl.das.terraria.objects.Terrarium;
import nl.das.terraria.objects.Timer;

/**
 *
 */
public class BTServer {

	private StreamConnectionNotifier scn;
	private StreamConnection sc;

	public BTServer(String name, UUID uuid) throws IOException {
		LocalDevice.getLocalDevice().setDiscoverable(DiscoveryAgent.GIAC);
		String url = "btspp://localhost:" + uuid.toString() + ";name=" + name + ";encrypt=false;authenticate=false";
		// Create a server connection (a notifier)
		this.scn = (StreamConnectionNotifier) Connector.open(url);
	}

	public void start() throws IOException {
		while(true) {
			// Accept a new client connection
			this.sc = this.scn.acceptAndOpen();
			// New client connection accepted; get a handle on it
			RemoteDevice rd = RemoteDevice.getRemoteDevice(this.sc);
			System.out.println("New client connection... " + rd.getFriendlyName(false));
			// Read input message, in this example a String
			DataInputStream dataIn = this.sc.openDataInputStream();
			DataOutputStream dataOut = this.sc.openDataOutputStream();
			int chr;
			StringBuffer sb = new StringBuffer();
			while ((chr = dataIn.read()) != -1) {
				if (chr == 0x03) {
					handleCommand(sb.toString(), dataOut);
					sb = new StringBuffer();
				} else {
					sb.append((char)chr);
				}
			}
			System.out.println("Connection closed");
		}
	}

	/**
	 * @param string
	 * @throws IOException
	 */
	public static void handleCommand (String command, OutputStream out) throws IOException {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true));
		// Analyze command
		Command cmd = jsonb.fromJson(command, Command.class);
		String res;
		try {
			switch(cmd.getCmd()) {
			case "getSensors": {
				res = jsonb.toJson(Terrarium.getInstance().getSensors());
				break;
			}
			case "setSensors": {
				int rt = cmd.getData().getInt("roomtemp", 0);
				if( rt == 0) {
					throw new CommandException("Integer parameter 'roomtemp' not found.");
				}
				int tt = cmd.getData().getInt("terrtemp", 0);
				if (tt == 0) {
					throw new CommandException("Integer parameter 'terrtemp' not found.");
				}
				Terrarium.getInstance().setSensors(rt, tt);
				res = "{}";
				break;
			}
			case "setTestOff": {
				Terrarium.getInstance().setTestOff();
				res = "{}";
				break;
			}
			case "getState": {
				res = Terrarium.getInstance().getState();
				break;
			}
			case "setDeviceOn": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceOn(prm, -1);
				res = "{}";
				break;
			}
			case "setDeviceOff": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceOff(prm);
				res = "{}";
				break;
			}
			case "setDeviceOnFor": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				int per = cmd.getData().getInt("period", -1);
				if (per == -1) {
					throw new CommandException("Integer parameter 'period' not found.");
				}
				if ((per <= 0) || (per > 3600)) {
					throw new CommandException("Integer parameter 'period' must be > 0 and < 3600 seconds.");
				}
				Terrarium.getInstance().setDeviceOn(prm, per);
				res = "{}";
				break;
			}
			case "setDeviceManualOn": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceManualOn(prm);
				res = "{}";
				break;
			}
			case "setDeviceManualOff": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				Terrarium.getInstance().setDeviceManualOff(prm);
				res = "{}";
				break;
			}
			case "setLifecycleCounter": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				int hrs = cmd.getData().getInt("hours", -1);
				if (hrs == -1) {
					throw new CommandException("Integer parameter 'period' not found.");
				}
				if (hrs <= 0 ) {
					throw new CommandException("Integer parameter 'period' must be > 0 hours.");
				}
				Terrarium.getInstance().setLifecycleCounter(prm, hrs);
				res = "{}";
				break;
			}
			case "getProperties": {
				res = Terrarium.getInstance().getProperties();
				break;
			}
			case "setTraceOn": {
				Terrarium.getInstance().setNow(LocalDateTime.now());
				Terrarium.getInstance().setTrace(true);
				res = "{}";
				break;
			}
			case "setTraceOff": {
				Terrarium.getInstance().setTrace(false);
				res = "{}";
				break;
			}
			case "getTimersForDevice": {
				String prm = cmd.getData().getString("device", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'device' not found.");
				}
				res = jsonb.toJson(Terrarium.getInstance().getTimersForDevice(prm));
				break;
			}
			case "replaceTimers": {
				JsonArray ja = cmd.getData().getJsonArray("timers");
				if (ja == null) {
					throw new CommandException("JsonArray parameter 'timers' does not contain an array of Timer objects.");
				}
				try {
					Timer[] timers = jsonb.fromJson(ja.toString(), Timer[].class);
					Terrarium.getInstance().replaceTimers(timers);
					Terrarium.getInstance().saveSettings();
					res = "{}";
				} catch (JsonbException e) {
					throw new CommandException("JsonArray parameter 'timers' does not contain an array of Timer json objects.");
				}
				break;
			}
			case "getRuleset": {
				int prm = cmd.getData().getInt("rulesetnr", 0);
				if( prm == 0) {
					throw new CommandException("Integer parameter 'rulesetnr' not found.");
				}
				res = jsonb.toJson(Terrarium.getInstance().getRuleset(prm));
				break;
			}
			case "saveRuleset": {
				JsonObject obj = cmd.getData().getJsonObject("ruleset");
				if (obj == null) {
					throw new CommandException("JsonObject parameter 'ruleset' not found.");
				}
				int prm = cmd.getData().getInt("rulesetnr", 0);
				if( prm == 0) {
					throw new CommandException("Integer parameter 'rulesetnr' not found.");
				}
				Ruleset ruleset;
				try {
					ruleset = jsonb.fromJson(obj.toString(), Ruleset.class);
					Terrarium.getInstance().replaceRuleset(prm, ruleset);
					Terrarium.getInstance().saveSettings();
					res = "{}";
				} catch (JsonbException e) {
					throw new CommandException("JsonObject parameter 'ruleset' does not contain a Ruleset json object.");
				}
				break;
			}
			case "getSprayerRule": {
				res = jsonb.toJson(Terrarium.getInstance().getSprayerRule());
				break;
			}
			case "setSprayerRule": {
				try {
					SprayerRule sprayerRule = jsonb.fromJson(cmd.getData().toString(), SprayerRule.class);
					Terrarium.getInstance().setSprayerRule(sprayerRule);;
					Terrarium.getInstance().saveSettings();
					res = "{}";
				} catch (JsonbException e) {
					throw new CommandException("Data does not contain a SprayerRule json object.");
				}
				break;
			}
			case "getTempTracefiles": {
				List<String> files = new ArrayList<>();
				files = Util.listTraceFiles(Terrarium.traceFolder, "temp_");
				res = jsonb.toJson(files);
				break;
			}
			case "getStateTracefiles": {
				List<String> files = new ArrayList<>();
				files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
				res = jsonb.toJson(files);
				break;
			}
			case "getTemperatureFile": {
				String content = "";
				String prm = cmd.getData().getString("fname", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'fname' not found.");
				}
				content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + cmd.getData().getString("fname")));
				res = content;
				break;
			}
			case "getStateFile": {
				String content = "";
				String prm = cmd.getData().getString("fname", "");
				if( prm.length() == 0) {
					throw new CommandException("String parameter 'fname' not found.");
				}
				content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + cmd.getData().getString("fname")));
				res = content;
				break;
			}
			default:
				throw new CommandException("Command '" + cmd.getCmd() + "' is not implemented.");
			}
			// Construct response
			out.write(res.getBytes());
			out.write(0x03); // ETX character
		} catch (Exception e) {
			if (e instanceof CommandException) {
				System.err.println(e.getMessage());
				res = "{\"error\":\"" + e.getMessage() + "\"}";
			} else {
				e.printStackTrace();
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				res = "{\"error\":\"" + sw.toString() + "\"}";
			}
			out.write(res.getBytes());
			out.write(0x03); // ETX character
		}
	}
}

