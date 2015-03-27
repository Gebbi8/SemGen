package semgen.stage;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import semgen.search.CompositeAnnotationSearch;
import semgen.stage.serialization.SemSimModelSerializer;
import semgen.stage.serialization.SubModelNode;
import semgen.utilities.Workbench;
import semgen.utilities.file.LoadSemSimModel;
import semgen.utilities.file.SemGenOpenFileChooser;
import semgen.visualizations.CommunicatingWebBrowserCommandReceiver;
import semgen.visualizations.SemGenWebBrowserCommandSender;
import semsim.model.SemSimModel;
import semsim.model.physical.Submodel;

public class StageWorkbench extends Workbench {

	// Maps semsim model name to a semsim model
	private Map<String, SemSimModel> _models;
	
	// Used to send commands to the view
	private SemGenWebBrowserCommandSender _commandSender;
	
	public StageWorkbench() {
		_models = new HashMap<String, SemSimModel>();
	}
	
	/**
	 * Get an object that listens for javascript commands
	 * @return
	 */
	public CommunicatingWebBrowserCommandReceiver getCommandReceiver() {
		return new StageCommandReceiver();
	}
	
	/**
	 * Sets the object used to send commands to the view
	 * @param commandSender Object used to send commands to the view
	 */
	public void setCommandSender(SemGenWebBrowserCommandSender commandSender) {
		_commandSender = commandSender;
	}
	
	@Override
	public void initialize() {
		// TODO Auto-generated method stub

	}

	@Override
	public File saveModel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File saveModelAs() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setModelSaved(boolean val) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getCurrentModelName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getModelSourceFile() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Receives commands from javascript
	 * @author Ryan
	 *
	 */
	public class StageCommandReceiver extends CommunicatingWebBrowserCommandReceiver {

		/**
		 * Receives the add model command
		 */
		public void onAddModel() {
			SemGenOpenFileChooser sgc = new SemGenOpenFileChooser("Select models to load", true);
			for (File file : sgc.getSelectedFiles()) {
				SemSimModel semsimmodel = LoadSemSimModel.loadSemSimModelFromFile(file, false);
				_models.put(semsimmodel.getName(), semsimmodel);
				
				// Tell the view to add a model
				_commandSender.addModel(semsimmodel.getName());
			}
		}
		
		public void onTaskClicked(String modelName, String task) {
			// If the model doesn't exist throw an exception
			if(!_models.containsKey(modelName))
				throw new IllegalArgumentException(modelName);
			
			// Get the model
			SemSimModel model = _models.get(modelName);
			
			// Execute the proper task
			switch(task) {
				case "dependencies":
					_commandSender.showDependencyNetwork(model.getName(),
							SemSimModelSerializer.toJsonString(SemSimModelSerializer.getDependencyNetwork(model)));
					break;
				case "submodels":
					ArrayList<SubModelNode> submodelNetwork = SemSimModelSerializer.getSubmodelNetwork(model);
					if(submodelNetwork.isEmpty())
						JOptionPane.showMessageDialog(null, "'" + model.getName() + "' does not have any submodels");
					else
						_commandSender.showSubmodelNetwork(model.getName(), SemSimModelSerializer.toJsonString(submodelNetwork));
					break;
				default:
					JOptionPane.showMessageDialog(null, "Task: '" + task +"', coming soon :)");
			}
		}
		
		public void onSearch(String searchString) {
			System.out.println("onSearch is working!");
			Set<String> searchResults = CompositeAnnotationSearch.compositeAnnotationSearch(searchString);
			//send searchResults back to stage.js
		}
	}
}
