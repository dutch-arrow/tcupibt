/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 29 Jul 2022.
 */


package nl.das.terraria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import nl.das.terraria.objects.Ruleset;
import nl.das.terraria.objects.Sensors;
import nl.das.terraria.objects.SprayerRule;
import nl.das.terraria.objects.Terrarium;
import nl.das.terraria.objects.Timer;
import nl.das.terraria.rest.BTServer;
import nl.das.terraria.rest.Command;
import nl.das.terraria.rest.Response;

/**
 *
 */
public class CommandHandlingTest {

	private static Terrarium terrarium;
	private static Jsonb jsonb;

	@BeforeAll
	public static void beforeAll () throws IOException {
		String json = Files.readString(Paths.get("src/test/resources/settings.json"));
		terrarium = Terrarium.getInstance(json);
		assertNotNull(terrarium, "Terrarium object cannot be null");
		terrarium.setNow(LocalDateTime.now());
		terrarium.initMockDevices();
		terrarium.initDeviceState();
		terrarium.initSensors();
		terrarium.initRules();
		terrarium.setTrace(false);
		jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true).withNullValues(true));
	}

	@BeforeEach
	public void before () {
	}

	@AfterEach
	public void after () {
	}

	@AfterAll
	public static void afterAll () {
	}

	@Test
	public void testCommand() {
		String command = "{\"cmd\":\"getTimersForDevice\",\"data\":{\"device\":\"sprayer\"}}";
		Command cmd = jsonb.fromJson(command, Command.class);
		System.out.println("Data=" + cmd.getData().toString());

	}

	@Test
	public void testCommands() {
		try {
			terrarium.setSensors(21, 26);
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
			Terrarium.traceFolder = "src/test/resources/tracefiles";
			Command cmd = new Command();
			cmd.setCmd("getSensors");
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			String resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().toString();
			resJson = resJson.replaceAll("[0-9]+-[0-9]+-[0-9]+ [0-9]+:[0-9]+", "");
			String json = Files.readString(Paths.get("src/test/resources/get_sensors.json"));
			JSONAssert.assertEquals(json, resJson , false);
			JsonObject value;

			cmd.setCmd("setSensors");
			value = Json.createObjectBuilder().add("roomtemp", 22).add("terrtemp", 30).build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setSensors response");
			Sensors sensors = terrarium.getSensors();
			assertEquals(22, sensors.getRoomTemp(), "Unexpected room temperature");
			assertEquals(30, sensors.getTerrariumTemp(), "Unexpected terrarium temperature");

			cmd.setCmd("setTestOff");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setTestOff response");
			sensors = terrarium.getSensors();
			assertEquals(0, sensors.getRoomTemp(), "Unexpected room temperature");
			assertEquals(0, sensors.getTerrariumTemp(), "Unexpected terrarium temperature");

			cmd.setCmd("getState");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().toString();
			json = Files.readString(Paths.get("src/test/resources/get_state.json"));
			JSONAssert.assertEquals(json, resJson , false);

			assertFalse(terrarium.isDeviceOn("light1"),"Light1 is not off");
			cmd.setCmd("setDeviceOn");
			value = Json.createObjectBuilder().add("device", "light1").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setDeviceOn response");
			assertTrue(terrarium.isDeviceOn("light1"),"Light1 is not on");

			cmd.setCmd("setDeviceOff");
			value = Json.createObjectBuilder().add("device", "light1").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setDeviceOff response");
			assertFalse(terrarium.isDeviceOn("light1"),"Light1 is not off");

			cmd.setCmd("setDeviceOnFor");
			value = Json.createObjectBuilder().add("device", "light1").add("period", 30).build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setDeviceOnFor response");
			assertTrue(terrarium.isDeviceOn("light1"),"Light1 is not on");
			terrarium.setDeviceOff("light1");

			cmd.setCmd("setDeviceManualOn");
			value = Json.createObjectBuilder().add("device", "light1").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setDeviceManualOn response");
			resJson = terrarium.getState();
			json = Files.readString(Paths.get("src/test/resources/get_state_mon.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("setDeviceManualOff");
			value = Json.createObjectBuilder().add("device", "light1").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setDeviceManualOff response");
			resJson = terrarium.getState();
			json = Files.readString(Paths.get("src/test/resources/get_state_moff.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("setLifecycleCounter");
			value = Json.createObjectBuilder().add("device", "uvlight").add("hours", 4400).build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setLifecycleCounter response");
			resJson = terrarium.getState();
			json = Files.readString(Paths.get("src/test/resources/get_state_lc.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("getProperties");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().toString();
			json = Files.readString(Paths.get("src/test/resources/get_properties.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("setTraceOn");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setTraceOn response");
			resJson = terrarium.getState();
			json = Files.readString(Paths.get("src/test/resources/get_state_traceon.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("setTraceOff");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected setTraceOff response");
			resJson = terrarium.getState();
			json = Files.readString(Paths.get("src/test/resources/get_state_lc.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("getTimersForDevice");
			value = Json.createObjectBuilder().add("device", "sprayer").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().get("timers").toString();
			json = Files.readString(Paths.get("src/test/resources/get_timers.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("replaceTimers");
			JsonReader reader = Json.createReader(new StringReader(Files.readString(Paths.get("src/test/resources/replace_timers.json"))));
			JsonObject obj = reader.readObject();
			cmd.setData(obj);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected replaceTimers response");
			Timer[] timers = terrarium.getTimersForDevice("sprayer");
			assertEquals(1, timers[1].getRepeat(), "Unexpected result for replaceTimers");

			cmd.setCmd("getRuleset");
			value = Json.createObjectBuilder().add("rulesetnr", 1).build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().toString();
			json = Files.readString(Paths.get("src/test/resources/get_ruleset.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("saveRuleset");
			reader = Json.createReader(new StringReader(Files.readString(Paths.get("src/test/resources/replace_ruleset.json"))));
			obj = reader.readObject();
			cmd.setData(obj);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected saveRuleset response");
			Ruleset ruleset = terrarium.getRuleset(1);
			assertEquals(27, ruleset.getTemp_ideal(), "Unexpected ideal temp in ruleset");

			cmd.setCmd("getSprayerRule");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().toString();
			json = Files.readString(Paths.get("src/test/resources/get_sprayerrule.json"));
			JSONAssert.assertEquals(json, resJson , false);

			cmd.setCmd("setSprayerRule");
			reader = Json.createReader(new StringReader(Files.readString(Paths.get("src/test/resources/set_sprayerrule.json"))));
			obj = reader.readObject();
			cmd.setData(obj);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			assertNull(jsonb.fromJson(bos.toString(), Response.class).getResponse(), "Unexpected saveSprayerRule response");
			SprayerRule sprayerRule = terrarium.getSprayerRule();
			assertEquals(300, sprayerRule.getActions()[0].getOn_period(), "Unexpected sprayer rule result");

			cmd.setCmd("getTempTracefiles");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().get("files").toString();
			@SuppressWarnings("serial")
			List<String> tfiles = jsonb.fromJson(resJson, new ArrayList<String>(){}.getClass().getGenericSuperclass());
			assertEquals("temp_20220730", tfiles.get(0), "Unexpected getTempTraceFIles result");

			cmd.setCmd("getStateTracefiles");
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().get("files").toString();
			@SuppressWarnings("serial")
			List<String> sfiles = jsonb.fromJson(resJson, new ArrayList<String>(){}.getClass().getGenericSuperclass());
			assertEquals("state_20220730", sfiles.get(0), "Unexpected getStateTraceFIles result");

			cmd.setCmd("getTemperatureFile");
			value = Json.createObjectBuilder().add("fname", "temp_20220730").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().get("content").toString().replace("\\n", "\n").replace("\"", "");
			json = Files.readString(Paths.get("src/test/resources/tracefiles/temp_20220730"));
			assertEquals(json, resJson,"Unexpected content of temperature tracefile");

			cmd.setCmd("getStateFile");
			value = Json.createObjectBuilder().add("fname", "state_20220730").build();
			cmd.setData(value);
			bos = new ByteArrayOutputStream();
			BTServer.handleCommand(jsonb.toJson(cmd), bos);
			resJson = jsonb.fromJson(bos.toString(), Response.class).getResponse().get("content").toString().replace("\\n", "\n").replace("\"", "");
			json = Files.readString(Paths.get("src/test/resources/tracefiles/state_20220730"));
			assertEquals(json, resJson,"Unexpected content of state tracefile");

		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

}
