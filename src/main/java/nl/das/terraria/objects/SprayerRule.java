package nl.das.terraria.objects;

public class SprayerRule {

	private Integer delay;
	private Action[] actions;

	public SprayerRule() { }

	public SprayerRule(int delay, Action[] actions) {
		this.delay = delay;
		this.actions = actions;
	}

	public int getDelay() {
		return this.delay;
	}

	public Action[] getActions() {
		return this.actions;
	}

	public void setDelay (Integer delay) {
		this.delay = delay;
	}

	public void setActions (Action[] actions) {
		this.actions = actions;
	}
}