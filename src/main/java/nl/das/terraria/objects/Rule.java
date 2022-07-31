package nl.das.terraria.objects;

public class Rule {

	private int value;
	private Action[] actions;

	public Rule() { }

	public Rule(int value, Action[] actions) {
		this.value = value;
		this.actions = actions;
	}

	public int getValue() {
		return this.value;
	}

	public Action[] getActions() {
		return this.actions;
	}

	public void setValue (int value) {
		this.value = value;
	}

	public void setActions (Action[] actions) {
		this.actions = actions;
	}
}