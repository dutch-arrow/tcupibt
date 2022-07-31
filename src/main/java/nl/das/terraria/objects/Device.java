/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 06 Aug 2021.
 */


package nl.das.terraria.objects;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigital;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;

/**
 *
 */
public class Device {

	private GpioController gpio;
	private String name;
	private GpioPinDigital pin;
	private PinState offState;
	private boolean lifetime;

	public Device() { }

	public Device(String name, boolean lifetime) {
		this(name, null, null, lifetime);
	}

	public Device(String name, Pin pin) {
		this(name, pin, PinState.LOW, false);
	}

	public Device(String name, Pin pin, PinState offState) {
		this(name, pin, offState, false);
	}

	public Device(String name, Pin pin, PinState offState, boolean lifetime) {
		this.name = name;
		this.lifetime = lifetime;
		this.offState = offState;
		if (pin != null) {
			this.gpio = GpioFactory.getInstance();
			this.pin = this.gpio.provisionDigitalOutputPin(pin, name, offState);
			this.pin.setShutdownOptions(true, offState);
		}
	}

	public String getName() {
		return this.name;
	}

	public void switchOn() {
		if (this.pin != null) {
			if (this.offState == PinState.LOW) {
				((GpioPinDigitalOutput)this.pin).high();
			} else {
				((GpioPinDigitalOutput)this.pin).low();
			}
		}
	}

	public void switchOff() {
		if (this.pin != null) {
			if (this.offState == PinState.LOW) {
				((GpioPinDigitalOutput)this.pin).low();
			} else {
				((GpioPinDigitalOutput)this.pin).high();
			}
		}
	}

	public boolean isOn() {
		if (this.pin != null) {
			if (this.offState == PinState.LOW) {
				return this.pin.isHigh();
			}
			return this.pin.isLow();
		}
		return false;
	}

	public boolean hasLifetime() {
		return this.lifetime;
	}

}
