package semgen.stage.serialization;

import java.util.ArrayList;

import semgen.SemGen;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.MappableVariable;

/**
 * Represents a dependency node in the d3 graph
 * 
 * @author Ryan
 *
 */
public class DependencyNode extends Node {
	public final static int SubmodelNamePart = 0;
	public final static int VariableNamePart = 1;
	
	public String nodeType;
	public ArrayList<Object> inputs;
	
	public DependencyNode(DataStructure dataStructure, String parentModel)
	{
		super(getNodeNameParts(dataStructure)[VariableNamePart]);
		
		this.nodeType = dataStructure.getPropertyType(SemGen.semsimlib).toString();

		inputs = new ArrayList<Object>();
		
		// Are there intra model inputs?
		if(dataStructure.getComputation() != null) {
			for(DataStructure input : dataStructure.getComputation().getInputs())
			{
				inputs.add(getNodeNameParts(input)[VariableNamePart]);
			}
		}
		
		// Are there inputs from other models?
		if(dataStructure instanceof MappableVariable) {
			for(MappableVariable input : ((MappableVariable)dataStructure).getMappedFrom())
			{
				String[] nameParts = getNodeNameParts(input);
				String submodelName = nameParts[SubmodelNamePart];
				String variableName = nameParts[VariableNamePart];
				inputs.add(new MappableVariableDependency(variableName, new String[] { parentModel, submodelName }));
			}
		}
	}
	
	/**
	 * The dependency name is a concatenation of the submodel and the variable name
	 * We need to extract out each part.
	 * @param dataStrcuture - datastructure to extract parts from
	 * @return Each name part.
	 */
	private static String[] getNodeNameParts(DataStructure dataStrcuture) {
		return dataStrcuture.getName().split("\\.");
	}
}
