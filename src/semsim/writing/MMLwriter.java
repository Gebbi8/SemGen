package semsim.writing;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

import semsim.ErrorLog;
import semsim.SemSimUtil;
import semsim.model.SemSimModel;
import semsim.model.computational.RelationalConstraint;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.Decimal;
import semsim.model.computational.datastructures.MappableVariable;
import semsim.model.computational.datastructures.SemSimInteger;
import semsim.model.computational.units.UnitOfMeasurement;
import JSim.aserver.ASModel;
import JSim.aserver.ASServer;
import JSim.data.NamedVal;
import JSim.util.UtilIO;
import JSim.util.Xcept;

public class MMLwriter extends ModelWriter{ 

	public MMLwriter(SemSimModel model) {
		super(model);
	}
	
	public String writeToString(){	
		String output = "";

		// If the SemSim model contains Functional Submodels ala CellML models, output the CellML code first,
		// then attempt to translate to MML
		// NEED BETTER ERROR HANDLING FOR CellML 1.1 models
		if(semsimmodel.getFunctionalSubmodels().size()>0){
			System.out.println("Using CellML as intermediary language");
			String tempcontent = new CellMLwriter(semsimmodel).writeToString();
			String srcText = UtilIO.readText(tempcontent.getBytes());
			int srcType = ASModel.TEXT_CELLML;
		    int destType = ASModel.TEXT_MML;
		    
		    // create server
		    NamedVal.NList soptions = new NamedVal.NList();
			try {
				soptions.setVal("buildDir", "");
				ASServer server = ASServer.create(soptions, null, null);
				String options = "sbml";
	
			    // translate
				String xmlstring = server.translateModelText(srcType, destType, srcText, options);
			    return xmlstring;
			} 
			catch (Xcept e) {
		    	ErrorLog.addError("Sorry. There was a problem encoding " + semsimmodel.getName() + 
		    			"\nThe JSim API threw an exception.", true);
				e.printStackTrace();
				return null;
			}
		}

		// Write the header
		output = output.concat("// Autogenerated by SemSim API version " + sslib.getSemSimVersion() + "\n\n");
		output = output.concat("JSim v1.1\n\n");
		output = output.concat("import nsrunit;\n");
		output = output.concat("unit conversion off;\n\n");
		
		// Print the custom units declarations
		
		// First find the "fundamental" units and declare them first
		Set<UnitOfMeasurement> nonfundamentalunits = new HashSet<UnitOfMeasurement>();
		nonfundamentalunits.addAll(semsimmodel.getUnits());	

		for (UnitOfMeasurement unit : semsimmodel.getUnits()) {
			if(unit.hasCustomDeclaration()){
				String dec = unit.getCustomDeclaration();
				if (dec.trim().endsWith("fundamental;")) {
					output = output.concat(dec + "\n");
					nonfundamentalunits.remove(unit);
				}
			}
		}
		for (UnitOfMeasurement unit : nonfundamentalunits) {
			String code = unit.getComputationalCode();
			
			if (unit.hasCustomDeclaration()) {
				output = output.concat(unit.getCustomDeclaration() + "\n");
			}
			// If going from a CellML file with units not recognized by JSim, declare them in the output
			else{
				Boolean testprefixed = false;
				for(String prefix : sslib.getUnitPrefixes()) {
					
					if(unit.getComputationalCode().startsWith(prefix)){
						String minusprefix = code.replaceFirst(prefix, "");
						testprefixed = sslib.jsimHasUnit(minusprefix);
					}
				}

				if(!sslib.jsimHasUnit(code) && !code.equals("") && !testprefixed
						&& !code.equals("dimensionless") && !code.contains("*") && !code.contains("/") && !code.contains("^")){
					output = output.concat("unit " + code + " = dimensionless;\n");  // Hack until units stuff w/Jsim gets sorted
				}
			}
		}
		output = output.concat("\nmath MODEL{\n");

		// Print the Domain declarations
		for (DataStructure domaindatastr : semsimmodel.getSolutionDomains()) {
			String name = domaindatastr.getName();
					
			// Domains are without units. Unit conversion turned off in outputted MML.
			output = output.concat("\trealDomain " + name + " " + domaindatastr.getUnit().getComputationalCode() + ";");
			
			// if the solution domain parameters aren't included in the
			// ontology, include them in the code output
			String[] vals = new String[]{"0","100","0.1"};
			String[] suffixes = new String[]{".min",".max",".delta"};
			for(int y=0;y<vals.length;y++){
				if(!semsimmodel.containsDataStructure(name + suffixes))
					output = output.concat(" extern " + name + suffixes[y] + ";"); 
				else
					output = output.concat(" " + semsimmodel.getDataStructure(name + suffixes[y]).getComputation().getComputationalCode());
			}
		}
		
		output = output.concat("\n\n\n\t// Variable and parameter declarations\n\n");

		Set<String> domainnames = semsimmodel.getSolutionDomainNames();

		// Print the Decimal variable declarations
		ArrayList<String> decnames = new ArrayList<String>();
		for(DataStructure dec : semsimmodel.getDecimals()) decnames.add(dec.getName());
		
		CaseInsensitiveComparator byVarName = new CaseInsensitiveComparator();
		Collections.sort(decnames, byVarName);
		
		for (String onedecimalstr : decnames) {
			Decimal onedecimal = (Decimal) semsimmodel.getDataStructure(onedecimalstr);
			
			// If the codeword is declared
			if (onedecimal.isDeclared() && !onedecimal.isSolutionDomain()
					// If the codeword is part of a solution domain declaration, don't declare it.
					&& !domainnames.contains(onedecimal.getName().replace(".delta", ""))
					&& !domainnames.contains(onedecimal.getName().replace(".min", ""))
					&& !domainnames.contains(onedecimal.getName().replace(".max",""))) {
				String unitcode = "";
				
				if(onedecimal.getUnit()!=null){
					unitcode = onedecimal.getUnit().getComputationalCode();
				}
				String declaration = "real";
				// If the codeword is a realState variable
				if (onedecimal.isDiscrete()) {
					declaration = "realState";
				}
				
				// If the codeword is an extern variable
				if(onedecimal.getComputation()!=null){
					if(onedecimal.getComputation().getComputationalCode()==null){
						declaration = "extern real";
					}
				}
				else if(onedecimal instanceof MappableVariable) declaration = "real";

				if (semsimmodel.getSolutionDomainNames().size() == 1) {
					for (String oned : semsimmodel.getSolutionDomainNames()) {
						output = output.concat("\t" + declaration + " " + onedecimal.getName() + "(" + oned + ") " + unitcode + ";" + "\n");
					}
				} else {
					output = output.concat("\t" + declaration + " " + onedecimal.getName() + " " + unitcode + ";" + "\n");
				}
			}
		}

		// Print the Integer variable declarations
		ArrayList<String> intnames = new ArrayList<String>();
		for(DataStructure integer : semsimmodel.getIntegers()) intnames.add(integer.getName());
		
		Collections.sort(intnames, byVarName);
		
		for (String oneintstr : intnames) {
			SemSimInteger oneint = (SemSimInteger) semsimmodel.getDataStructure(oneintstr);
			if (oneint.isDeclared() && !oneint.isSolutionDomain()
					&& !domainnames.contains(oneint.getName().replace(".delta", ""))
					&& !domainnames.contains(oneint.getName().replace(".min", ""))
					&& !domainnames.contains(oneint.getName().replace(".max",""))) {
				String unitcode = "";
				if(oneint.getUnit()!=null){
					unitcode = oneint.getUnit().getComputationalCode();
				}
				String declaration = "int";
				if (oneint.isDiscrete()) {
					declaration = "intState";
				}
				if (oneint.getComputation().getComputationalCode().equals("")) {
					if (domainnames.contains(oneint.getName().replace(".delta", ""))
							|| domainnames.contains(oneint.getName().replace(".min",""))
							|| domainnames.contains(oneint.getName().replace(".max",""))) {
						declaration = "extern";
					} else {
						declaration = "extern int";
					}
				}

				if (domainnames.size() == 1) {
					String base = semsimmodel.getNamespace();
					for (String onedom1 : domainnames) {
						onedom1 = onedom1.replace(base, "");
						output = output.concat("\t" + declaration + " " + oneint.getName()
								+ "(" + onedom1.replace(base, "") + ") "
								+ unitcode + ";" + "\n");
					}
				} 
				else {
					output = output.concat("\t" + declaration + " " + oneint.getName() + " "
							+ unitcode + ";" + "\n");
				}
			}
		}

		// Print the Initial conditions for the state variables
		output = output.concat("\n\n\t// Boundary conditions\n\n");

		String longestcodeword = "";
		
		ArrayList<String> alldsarray = new ArrayList<String>(semsimmodel.getDataStructureNames());
		Collections.sort(alldsarray, byVarName);
		for (String onedsstr : alldsarray) {
			DataStructure onedatastr = semsimmodel.getDataStructure(onedsstr);
			if (onedatastr.hasStartValue() && onedatastr.hasSolutionDomain()) {
				output = output.concat("\twhen (" + onedatastr.getSolutionDomain().getName()
						+ " = " + onedatastr.getSolutionDomain().getName() + ".min){ " 
						+ onedatastr.getName() + " = " + onedatastr.getStartValue() + "; }\n");
			}
			if (onedatastr.getName().length() > longestcodeword.length()) {
				longestcodeword = onedatastr.toString();
			}
		}

		// Print the Equations
		output = output.concat("\n\n\t// Equations\n\n");
		
		// Logic for this might be a little different than previous versions
		for (String onedsstr : alldsarray) {
			DataStructure ds = semsimmodel.getDataStructure(onedsstr);
			if (ds.isDeclared() && !ds.isSolutionDomain() 
					&& !domainnames.contains(ds.getName().replace(".delta", ""))
					&& !domainnames.contains(ds.getName().replace(".min", ""))
					&& !domainnames.contains(ds.getName().replace(".max",""))
					&& ds.getComputation().getComputationalCode()!=null){
				
				String code = ds.getComputation().getComputationalCode();
				output = output.concat("\t" + code + getLineEnd(code) + "\n");
			}
		}
		
		if(!semsimmodel.getRelationalConstraints().isEmpty()) output = output.concat("\n\n\t// Relational constraints\n\n");
		for(RelationalConstraint rel : semsimmodel.getRelationalConstraints()){
			output = output.concat("\t" + rel.getComputationalCode() + getLineEnd(rel.getComputationalCode()) + "\n");
		}

		output = output.concat("}\n");
		output = output.concat("// END OF SIMULATION CODE\n");
		output = output.concat("\n\n\n");

		// Print the reference table of codeword annotations.
		output = output.concat("/*\n");
		output = output.concat("CODEWORD DEFINITIONS\n");
		output = output.concat("-------------------------------\n");
		for (String onedsstr : alldsarray) {
			DataStructure ds = semsimmodel.getDataStructure(onedsstr);
			output = output.concat(ds.getName() + "\n");
			if(ds.getDescription()!=null){
				output = output.concat("   " + ds.getDescription() + "\n");
			}
			output = output.concat("\n");
		}
		output = output.concat("-------------------------------\n");
		output = output.concat("*/\n");
		return output;

	}
	
	private String getLineEnd(String code){
		if (code.endsWith("}")) {
			return "";
		}
		return ";";
	}
	
	public void writeToFile(File destination){
		SemSimUtil.writeStringToFile(writeToString(), destination);
	}
	
	public void writeToFile(URI destination){
		SemSimUtil.writeStringToFile(writeToString(), new File(destination));
	}
}