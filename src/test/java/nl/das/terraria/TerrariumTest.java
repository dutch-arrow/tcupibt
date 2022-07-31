/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 14 Aug 2021.
 */


package nl.das.terraria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

import nl.das.terraria.objects.Terrarium;

/**
 *
 */
public class TerrariumTest {

	private static Terrarium terrarium;

	@BeforeAll
	public static void start () {
	}

	@BeforeEach
	public void before () {
	}

	@AfterAll
	public static void after () {
	}

	@AfterEach
	public void exit () {
	}

	@Test
	public void testSettings() throws IOException {
		// Get an empty instance
		Terrarium t = Terrarium.getInstance();
		// fill it with empty values
		t.init();
		// and persist it
		t.saveSettings();
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true).withNullValues(true));
		String json = jsonb.toJson(Terrarium.cfg);
		// now read the persisted data
		String jsonr = Files.readString(Paths.get("settings.json"));
		try {
			JSONAssert.assertEquals(json, jsonr, false);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testLifecycleCounter() throws IOException {
		String json = Files.readString(Paths.get("src/test/resources/settings.json"));
		terrarium = Terrarium.getInstance(json);
		assertNotNull(terrarium, "Terrarium object cannot be null");
		terrarium.initMockDevices();
		terrarium.initDeviceState();
		terrarium.initSensors();
		terrarium.initRules();
		terrarium.setTrace(false);
		terrarium.setSensors(21, 26); // Ideal temperature, so rules will not be activated

		// Initialize file content
		Files.deleteIfExists(Paths.get("lifecycle.txt"));
		Files.writeString(Paths.get("lifecycle.txt"), "uvlight=4400", StandardOpenOption.CREATE_NEW);

		// Retrieve the lifecycle values from disk
		try {
			String lcdata = Files.readString(Paths.get("lifecycle.txt"));
			String lns[] = lcdata.split("\n");
			assertEquals(lns.length, 1);
			assertEquals(lns[0], "uvlight=4400");
			for (String ln : lns) {
				String lp[] = ln.split("=");
				terrarium.setDeviceLifecycle(lp[0], Integer.parseInt(lp[1]));
			}
		} catch (NoSuchFileException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}
		assertEquals(terrarium.getDevStates()[terrarium.getDeviceIndex("uvlight")].getLifetime(), 4400);

		terrarium.decreaseLifetime(2);
		assertEquals(terrarium.getDevStates()[terrarium.getDeviceIndex("uvlight")].getLifetime(), 4398);
		// Retrieve the lifecycle values again from disk
		try {
			String lcdata = Files.readString(Paths.get("lifecycle.txt"));
			String lns[] = lcdata.split("\n");
			assertEquals(lns.length, 1);
			assertEquals(lns[0], "uvlight=4398");
			for (String ln : lns) {
				String lp[] = ln.split("=");
				terrarium.setDeviceLifecycle(lp[0], Integer.parseInt(lp[1]));
			}
		} catch (NoSuchFileException e) {
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testSwitchOnWithPeriod() throws IOException {
		String json = Files.readString(Paths.get("src/test/resources/settings.json"));
		terrarium = Terrarium.getInstance(json);
		assertNotNull(terrarium, "Terrarium object cannot be null");
		terrarium.initMockDevices();
		terrarium.initDeviceState();
		terrarium.initSensors();
		terrarium.initRules();
		terrarium.setTrace(false);
		terrarium.setSensors(21, 26); // Ideal temperature, so rules will not be activated
		{
		// Time: 08:00:00
		LocalDateTime now = LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(8, 0, 0));
		terrarium.setNow(now);
		terrarium.setDeviceOn("mist", Util.now(now) + 120);
		terrarium.checkDevices();
		assertTrue(terrarium.isDeviceOn("mist"));
		assertNotEquals(terrarium.getDevStates()[terrarium.getDeviceIndex("mist")].getOnPeriod(), 0);
		}
		{
		// Time: 08:01:00
		LocalDateTime now = LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(8, 1, 0));
		terrarium.setNow(now);
		terrarium.checkDevices();
		assertTrue(terrarium.isDeviceOn("mist"));
		assertNotEquals(terrarium.getDevStates()[terrarium.getDeviceIndex("mist")].getOnPeriod(), 0);
		}
		{
		// Time: 08:02:00
		LocalDateTime now = LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(8, 2, 0));
		terrarium.setNow(now);
		terrarium.checkDevices();
		assertFalse(terrarium.isDeviceOn("mist"));
		assertEquals(terrarium.getDevStates()[terrarium.getDeviceIndex("mist")].getOnPeriod(), 0);
		}
		{
		// Time: 08:03:00
		LocalDateTime now = LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(8, 3, 0));
		terrarium.setNow(now);
		terrarium.checkDevices();
		assertFalse(terrarium.isDeviceOn("mist"));
		assertEquals(terrarium.getDevStates()[terrarium.getDeviceIndex("mist")].getOnPeriod(), 0);
		}
	}
}
