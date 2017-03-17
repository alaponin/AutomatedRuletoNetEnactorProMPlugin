package org.processmining.ArneRulePropagator.plugins;

public class ArneSettings {
	
	private ToolOption toolOption;
	
	public ArneSettings() {
		this.toolOption = ToolOption.ARNE;
	}

	public ToolOption getToolOption() {
		return toolOption;
	}

	public void setToolOption(ToolOption toolOption) {
		this.toolOption = toolOption;
	}
	
	

}
