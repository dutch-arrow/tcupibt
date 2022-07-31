/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 15 Aug 2021.
 */

package nl.das.terraria.hw;

import java.util.List;

import com.pi4j.component.temperature.TemperatureSensor;
import com.pi4j.io.w1.W1Master;
import com.pi4j.temperature.TemperatureScale;

/**
 * In order to work add this line to the /boot/firmware/usercfg.txt:
 *    device_tree_overlay=overlays/w1-gpio.dtbo
 */
public class DS18B20 {

	private W1Master w1Master;

	public DS18B20(W1Master w1Master) {
		this.w1Master = w1Master;
	}

	public double getTemperature () {

		// There is only 1 DS18B20 Temperature sensor on the w1 wire
		List<TemperatureSensor> devices = this.w1Master.getDevices(TemperatureSensor.class);
		if (devices.size() > 0) {
			for (TemperatureSensor d : devices) {
				if (d.getName().startsWith("28-")) {
					return d.getTemperature(TemperatureScale.CELSIUS);
				}
			}
		}
		return 0;
	}

}
