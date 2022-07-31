/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 06 Jul 2022.
 */


package nl.das.terraria;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.das.terraria.Util;
import nl.das.terraria.objects.Terrarium;

/**
 *
 */
public class TracefileRollOverTest {

	public static Terrarium terrarium;

	@BeforeAll
	public static void beforeAll () throws IOException {
		Terrarium.traceFolder = "src/test/resources/tracefiles";
		Terrarium.maxNrOfTraceDays = 5;
		String json = Files.readString(Paths.get("src/test/resources/settings.json"));
		terrarium = Terrarium.getInstance(json);
		assertNotNull(terrarium, "Terrarium object cannot be null");
		terrarium.initMockDevices();
		terrarium.initDeviceState();
		terrarium.initSensors();
		terrarium.initRules();
		terrarium.setSensors(21, 26); // Ideal temperature, so rules will not be activated
		// Remove all tracefiles
		try {
			List<String> files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			for (String file : files) {
				Files.deleteIfExists(Paths.get(Terrarium.traceFolder + "/" + file));
			}
			files = Util.listTraceFiles(Terrarium.traceFolder, "temp_");
			for (String file : files) {
				Files.deleteIfExists(Paths.get(Terrarium.traceFolder + "/" + file));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	public void testCreateTraceFiles() {
		try {
			Path p = Paths.get(Terrarium.traceFolder);
			if (!Files.exists(p)) {
				Files.createDirectory(p);
				System.out.println("Directory created: " + p.toAbsolutePath());
			}

			LocalDateTime now = LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.setTrace(true);
			List<String> files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 1, files.size());

			now = LocalDateTime.of(LocalDate.of(2021, 8, 2), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.checkTrace();
			files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 2, files.size());

			now = LocalDateTime.of(LocalDate.of(2021, 8, 3), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.checkTrace();
			files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 3, files.size());

			now = LocalDateTime.of(LocalDate.of(2021, 8, 4), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.checkTrace();
			files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 4, files.size());

			now = LocalDateTime.of(LocalDate.of(2021, 8, 5), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.checkTrace();
			files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 5, files.size());

			now = LocalDateTime.of(LocalDate.of(2021, 8, 6), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.checkTrace();
			files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 5, files.size());
			assertEquals("Unexpected filename", "state_20210802", files.get(0));
			assertEquals("Unexpected filename", "state_20210803", files.get(1));
			assertEquals("Unexpected filename", "state_20210804", files.get(2));
			assertEquals("Unexpected filename", "state_20210805", files.get(3));
			assertEquals("Unexpected filename", "state_20210806", files.get(4));

			now = LocalDateTime.of(LocalDate.of(2021, 8, 7), LocalTime.of(5, 0, 0));
			terrarium.setNow(now);
			terrarium.checkTrace();
			files = Util.listTraceFiles(Terrarium.traceFolder, "state_");
			assertEquals("Unexpected # of state files", 5, files.size());
			assertEquals("Unexpected filename", "state_20210803", files.get(0));
			assertEquals("Unexpected filename", "state_20210804", files.get(1));
			assertEquals("Unexpected filename", "state_20210805", files.get(2));
			assertEquals("Unexpected filename", "state_20210806", files.get(3));
			assertEquals("Unexpected filename", "state_20210807", files.get(4));

			String[] lines = getContent(files.get(0)).split("\n");
			assertEquals("Unexpected # of lines", 13, lines.length);
			assertTrue("Unexpected content of first line (should be 'start')", lines[0].endsWith("start"));
			assertTrue("Unexpected content of last line (should be 'stop')", lines[12].endsWith("stop"));

			terrarium.setTrace(false);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getContent(String file) {
		String content = "<no content>";
		try {
			content = Files.readString(Paths.get(Terrarium.traceFolder + "/" + file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}
}
