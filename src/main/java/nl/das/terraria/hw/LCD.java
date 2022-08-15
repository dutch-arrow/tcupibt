/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 16 Aug 2021.
 */


package nl.das.terraria.hw;

import java.io.IOException;

import com.pi4j.component.lcd.impl.I2CLcdDisplay;
import com.pi4j.io.i2c.I2CBus;

/**
 *
 */
public class LCD {

	I2CLcdDisplay lcd;
	int nrOfRows;
	int nrOfCols;
	boolean notConnected;

	public LCD() { }

	public void init(int rows, int cols) {
		this.nrOfRows = rows;
		this.nrOfCols = cols;
		try {
			this.lcd = new I2CLcdDisplay(rows, cols, I2CBus.BUS_1, 0x27, 3, 0, 1, 2, 7, 6, 5, 4);
			this.notConnected = false;
			this.lcd.setBacklight(true, true);
			this.lcd.clear();
		} catch (IOException e) {
			this.notConnected = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void write(int row, String text) {
		if (!this.notConnected) {
			this.lcd.write(row, text);
		}
	}

	public void clear() {
		if (!this.notConnected) {
			this.lcd.clear();
		}
	}

	public void clear(int row) {
		if (!this.notConnected) {
			this.lcd.clear(row);
		}
	}

	public void writeDegrees(int row, int col) {
		if (!this.notConnected) {
			this.lcd.write(row, col, (byte) 0xDF);
		}
	}

	public void displayLine1 (int troom, int tterr) {
		if (!this.notConnected) {
			this.lcd.clear(0);
			char degrees = 0xDF;
			this.lcd.write(0, String.format("K:%2d%cC T:%2d%cC", troom, degrees, tterr, degrees));
		}
	}
}
