package nl.das.terraria.objects;


public class Action {

	private String device;
	private int on_period;

	public Action() { }

	public Action(String device, int onPeriod) {
		this.device = device;
		this.on_period = onPeriod;
	}

	public int getOn_period() {
		return this.on_period;
	}

	public void setOn_period (int on_period) {
		this.on_period = on_period;
	}

	public String getDevice() {
		return this.device;
	}

	public void setDevice (String device) {
		this.device = device;
	}
}