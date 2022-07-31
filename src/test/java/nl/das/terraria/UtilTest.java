/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 14 Aug 2021.
 */

package nl.das.terraria;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.das.terraria.Util;

/**
 *
 */
public class UtilTest {

	@BeforeAll
	public static void beforeAll () {
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
	public void test_cvtPeriodToString() {
		LocalDateTime t = LocalDateTime.of(2021, 8, 1, 13, 42, 28);
		// Time = 13:42:28
		assertEquals("until 13:42:28", Util.cvtPeriodToString(t.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(Instant.now()))), "Incorrect time");
		// Time = 09:42:00
		t = LocalDateTime.of(2021, 8, 1, 9, 42, 0);
		assertEquals("until 09:42:00", Util.cvtPeriodToString(t.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(Instant.now()))), "Incorrect time");
		// Time = 23:00:00
		t = LocalDateTime.of(2021, 8, 1, 23, 0, 0);
		assertEquals("until 23:00:00", Util.cvtPeriodToString(t.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(Instant.now()))), "Incorrect time");
	}

}
