package org.processmining.ArneRulePropagator.plugins;

public enum ToolOption {
	ARNE("ARNE"),
	PETRIFY("Petrify");
	
	private final String text;
	
	private ToolOption(final String text) {
        this.text = text;
    }
	
	@Override
    public String toString() {
        return text;
    }
}
