package org.processmining.ArneRulePropagator.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deckfour.uitopia.api.event.TaskListener.InteractionResult;
import org.processmining.ArneRulePropagator.dialogs.ArneDialog;
import org.processmining.ArneRulePropagator.models.DeclareTemplate;
import org.processmining.ArneRulePropagator.models.LTLFormula;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.declareminer.visualizing.ActivityDefinition;
import org.processmining.plugins.declareminer.visualizing.ConstraintDefinition;
import org.processmining.plugins.declareminer.visualizing.DeclareMap;
import org.processmining.plugins.declareminer.visualizing.Parameter;
import org.processmining.plugins.petrify.PetrifyDotG;
import org.processmining.plugins.petrify.PetrifyImportDotG;

import converter.ProceduralRepairer;
import converter.petrinet.CanNotConvertPNToAutomatonException;
import converter.utils.TSFileConverter;

@Plugin(name = "ARNE Automated Rule-to-Net Enactor", parameterLabels = { "Petri net", "Declare model", "Name of your configuration" },
	    returnLabels = { "Repaired Petri net", "Marking" }, returnTypes = { Petrinet.class, Marking.class })
public class ARNE {

	/**
	 * The method that does the heavy lifting for your plug-in.
	 *
	 * Note that this method only uses the boolean which is stored in the configuration.
	 * Nevertheless, it could have used the integer and/or the String as well.
	 *
	 * @param context The context where to run in.
	 * @param net The first input.
	 * @param ltlFormula The second input.
	 * @param configuration The configuration to use.
	 * @return The output.
	 * @throws Exception 
	 */
	private Object[] repairModel(PluginContext context, Petrinet net, DeclareMap ltlFormula, ArneSettings settings) throws Exception {
		Object[] repairedNetSystem = doHeavyLifting(context, net, ltlFormula, settings);
		System.out.println(((Petrinet) repairedNetSystem[0]).getTransitions());
		return repairedNetSystem;
		
	}


	private Object[] doHeavyLifting(PluginContext context, Petrinet net, DeclareMap ltlFormula, ArneSettings settings)
			throws Exception, CanNotConvertPNToAutomatonException, IOException, InterruptedException,
			FileNotFoundException {
		
		String allFormulas = parseConstraints(ltlFormula);
		Petrinet repairedNet = null;
		Marking marking = null;
		
		if (settings.getToolOption().equals(ToolOption.ARNE)) {
			
			repairedNet = ProceduralRepairer.repair(net, allFormulas);
			
		} else {
			String fileName = TSFileConverter.TS2File(net, allFormulas, "resources/sg_files/intersection.sg");
			File intersectionFile = new File(fileName);
			
			File petrifyPath = new File("resources/petrify-intel");
			ProcessBuilder builder = new ProcessBuilder(petrifyPath.getAbsolutePath(), "-dead", intersectionFile.getAbsolutePath(), "-o","final_nets/result.g");
			builder.directory(new File( "resources" ).getAbsoluteFile());
			builder.redirectErrorStream(true);
			
			Process process =  builder.start();
			

			int result = process.waitFor();
			File outputFile = new File("resources/final_nets/result.g");
			
			FileInputStream fis = null;
			fis = new FileInputStream(outputFile);
			
			PetrifyImportDotG opener = new PetrifyImportDotG();
			PetrifyDotG dotFile = new PetrifyDotG(outputFile.getAbsolutePath());	
		

			Object[] netAndMarking = opener.importFile(context, dotFile);

			Petrinet petrifiedNet = (Petrinet) netAndMarking[0];
			
			if (!petrifiedNet.equals(null)) {
				repairedNet = petrifiedNet;
			}
			fis.close();
			marking = (Marking) netAndMarking[1];
		}
			
	
		
		if (repairedNet == null) {
			return new Object[] {net, marking};
			
		} else {
			return new Object[] {repairedNet, marking};
			
		}
	}


	private String parseConstraints(DeclareMap ltlFormula) {
		String allFormulas = "";
		List<ConstraintDefinition> allConstraints = (List<ConstraintDefinition>) ltlFormula.getModel().getConstraintDefinitions();
		int constraintCount = 0;
		for (ConstraintDefinition constraint : allConstraints) {
			String currentFormula = LTLFormula.getFormulaByTemplate(getTemplate(constraint));
			for (Parameter p : constraint.getParameters()) {
				for (ActivityDefinition b : constraint.getBranches(p)) {
					currentFormula = currentFormula.replace("\"" + p.getName() + "\"", b.getName().replaceAll(" ", "_"));
				}
			}
			
			constraintCount++;
			if (currentFormula.startsWith("!")) {
				currentFormula = "("+currentFormula+")";
			}
			String ltlString = currentFormula.replace("\\/", "||").replace("/\\", "&&").replace("[]", "G").replace("<>", "F").replace("\"", "");
			String regex = "[(][\\ ]*![\\ ]*[(][a-zA-Z0-9_]*[\\ ]*[)][\\ ][U][\\ ]*[a-zA-Z0-9_]*[\\ ]*[)][\\ ,|]*[(,G]*[\\!][(][a-zA-Z0-9_]*[)]*";
			Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(ltlString);
	        List<String> responseFormulas = new ArrayList<>();
	        while (matcher.find()) {
	            responseFormulas.add(matcher.group());
	        }
	        for (String responseFormula : responseFormulas) {
	        	String newFormula = "("+responseFormula+")";
	        	ltlString=ltlString.replace(responseFormula, newFormula);
	        }
			allFormulas += ltlString;
			if (constraintCount < allConstraints.size()) {
				allFormulas += " && ";
			}
		}
		return allFormulas;
	}


	/**
	 * The plug-in variant that runs in any context and uses the default configuration.
	 *
	 * @param context The context to run in.
	 * @param net The first input.
	 * @param ltlFormula The second input.
	 * @return The output.
	 * @throws Exception 
	 */
	@UITopiaVariant(affiliation = "University of Tartu/Fondazione Bruno Kessler", author = "Arne Laponin", email = "arne.laponin@gmail.com")
	@PluginVariant(variantLabel = "Your plug-in name, parameters", requiredParameterLabels = { 0, 1 })
	public Object[] repairModel(PluginContext context, Petrinet net, DeclareMap ltlFormula) throws Exception {
		// Get the default configuration.
	    //YourConfiguration configuration = new YourConfiguration(input1, input2);
		// Do the heavy lifting.
		ArneSettings settings = new ArneSettings();
	    return repairModel(context, net, ltlFormula, settings);
	}

	/**
	 * The plug-in variant that runs in a UI context and uses a dialog to get the configuration.
	 *
	 * @param context The context to run in.
	 * @param net The first input.
	 * @param ltlFormula The second input.
	 * @return The output.
	 * @throws Exception 
	 */
	@UITopiaVariant(affiliation = "University of Tartu/Fondazione Bruno Kessler", author = "Arne Laponin", email = "arne.laponin@gmail.com")
	@PluginVariant(variantLabel = "Your plug-in name, dialog", requiredParameterLabels = { 0, 1 })
	public Object[] repairModel(UIPluginContext context, Petrinet net, DeclareMap ltlFormula) throws Exception {
		// Get the default configuration.
		ArneSettings settings = new ArneSettings();
	    //YourConfiguration configuration = new YourConfiguration(input1, input2);
	    // Get a dialog for this configuration.
		ArneDialog dialog = new ArneDialog(context, settings);
	    //YourDialog dialog = new YourDialog(context, net, ltlFormula, null);
	    // Show the dialog. User can now change the configuration.
	    InteractionResult result = context.showWizard("ARNE settings", true, true, dialog);
	    // User has close the dialog.
	    if (result == InteractionResult.FINISHED) {
			// Do the heavy lifting.
	    	return repairModel(context, net, ltlFormula, settings);
	    }
	    // Dialog got canceled.
	    context.getFutureResult(0).cancel(true);
	    return null;
	}
	
	public static DeclareTemplate getTemplate(ConstraintDefinition constraint){
		if(constraint.getName().toLowerCase().equals("absence")){
			return DeclareTemplate.Absence;
		}
		if(constraint.getName().toLowerCase().equals("absence2")){
			return DeclareTemplate.Absence2;
		}
		if(constraint.getName().toLowerCase().equals("absence3")){
			return DeclareTemplate.Absence3;
		}
		if(constraint.getName().toLowerCase().equals("alternate precedence")){
			return DeclareTemplate.Alternate_Precedence;
		}
		if(constraint.getName().toLowerCase().equals("alternate response")){
			return DeclareTemplate.Alternate_Response;
		}
		if(constraint.getName().toLowerCase().equals("alternate succession")){
			return DeclareTemplate.Alternate_Succession;
		}
		if(constraint.getName().toLowerCase().equals("chain precedence")){
			return DeclareTemplate.Chain_Precedence;
		}
		if(constraint.getName().toLowerCase().equals("chain response")){
			return DeclareTemplate.Chain_Response;
		}
		if(constraint.getName().toLowerCase().equals("chain succession")){
			return DeclareTemplate.Chain_Succession;
		}
		if(constraint.getName().toLowerCase().equals("precedence")){
			return DeclareTemplate.Precedence;
		}
		if(constraint.getName().toLowerCase().equals("response")){
			return DeclareTemplate.Response;
		}
		if(constraint.getName().toLowerCase().equals("succession")){
			return DeclareTemplate.Succession;
		}
		if(constraint.getName().toLowerCase().equals("responded existence")){
			return DeclareTemplate.Responded_Existence;
		}
		if(constraint.getName().toLowerCase().equals("co-existence")){
			return DeclareTemplate.CoExistence;
		}
		if(constraint.getName().toLowerCase().equals("exclusive choice")){
			return DeclareTemplate.Exclusive_Choice;
		}
		if(constraint.getName().toLowerCase().equals("choice")){
			return DeclareTemplate.Choice;
		}
		if(constraint.getName().toLowerCase().equals("existence")){
			return DeclareTemplate.Existence;
		}
		if(constraint.getName().toLowerCase().equals("existence2")){
			return DeclareTemplate.Existence2;
		}
		if(constraint.getName().toLowerCase().equals("existence3")){
			return DeclareTemplate.Existence3;
		}
		if(constraint.getName().toLowerCase().equals("exactly1")){
			return DeclareTemplate.Exactly1;
		}
		if(constraint.getName().toLowerCase().equals("exactly2")){
			return DeclareTemplate.Exactly2;
		}	
		if(constraint.getName().toLowerCase().equals("init")){
			return DeclareTemplate.Init;
		}
		if(constraint.getName().toLowerCase().equals("not chain succession")){
			return DeclareTemplate.Not_Chain_Succession;
		}
		if(constraint.getName().toLowerCase().equals("not succession")){
			return DeclareTemplate.Not_Succession;
		}	 
		if(constraint.getName().toLowerCase().equals("not co-existence")){
			return DeclareTemplate.Not_CoExistence;
		}
		return null;
	}
}
