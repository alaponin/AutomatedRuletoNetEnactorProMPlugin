package org.processmining.ArneRulePropagator.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.processmining.ArneRulePropagator.plugins.ArneSettings;
import org.processmining.ArneRulePropagator.plugins.ToolOption;
import org.processmining.contexts.uitopia.UIPluginContext;

public class ArneDialog extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2188252645147347781L;
	private ArneSettings settings;
	
	public ArneDialog(UIPluginContext context, final ArneSettings settings) {
		super(new GridLayout(3,2));
		//super(new GridLayout(1,1));
		this.settings = settings;
		
		Component space = Box.createHorizontalStrut(10);
		//this.add(space);
		final JLabel chooseATool = new JLabel();
		chooseATool.setText("Choose between ARNE and Petrify");
		this.add(chooseATool);
		final JCheckBox mCSizeCbxFake = new JCheckBox();
		
		mCSizeCbxFake.setVisible(false);
		
		this.add(mCSizeCbxFake);

		final JLabel test = new JLabel();
		test.setText("");
		//this.add(test);

		space = Box.createHorizontalStrut(1);
		//this.add(space);
		
		final JLabel arneChoosen = new JLabel();
		arneChoosen.setText(ToolOption.ARNE.toString());
		this.add(arneChoosen);

		space = Box.createHorizontalStrut(1);
		//this.add(space);
		
		final JCheckBox mCSizeCbx = new JCheckBox();
		mCSizeCbx.setBackground(new Color(150,150,150));
		mCSizeCbx.setHorizontalAlignment(SwingConstants.LEFT);
		mCSizeCbx.setSelected(settings.getToolOption().equals(ToolOption.ARNE));
		
		final JCheckBox mCSizeCbxPetrify = new JCheckBox();
		mCSizeCbxPetrify.setBackground(new Color(150,150,150));
		mCSizeCbxPetrify.setHorizontalAlignment(SwingConstants.LEFT);
		mCSizeCbxPetrify.setSelected(settings.getToolOption().equals(ToolOption.PETRIFY));
		
		
		mCSizeCbx.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (mCSizeCbx.isSelected()) {
					settings.setToolOption(ToolOption.ARNE);
					mCSizeCbxPetrify.setSelected(false);
				}
			}
		});
		this.add(mCSizeCbx);
		
		final JLabel petrifyChoosen = new JLabel();
		petrifyChoosen.setText(ToolOption.PETRIFY.toString());
		this.add(petrifyChoosen);
	

		space = Box.createHorizontalStrut(1);
		//this.add(space);
		
		
		mCSizeCbxPetrify.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (mCSizeCbxPetrify.isSelected()) {
					settings.setToolOption(ToolOption.PETRIFY);
					mCSizeCbx.setSelected(false);
				}
			}
		});
		this.add(mCSizeCbxPetrify);

		space = Box.createHorizontalStrut(1);
		//this.add(space);
	
		space = Box.createVerticalStrut(20);
		//this.add(space);
	}

}
