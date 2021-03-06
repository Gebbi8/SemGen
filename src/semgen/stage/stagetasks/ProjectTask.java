package semgen.stage.stagetasks;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Observable;

import javax.swing.JOptionPane;

import com.teamdev.jxbrowser.chromium.JSArray;
import com.teamdev.jxbrowser.chromium.JSObject;

import org.apache.commons.io.FilenameUtils;
import semgen.SemGen;
import semgen.search.CompositeAnnotationSearch;
import semgen.stage.serialization.ExtractionNode;
import semgen.stage.serialization.Node;
import semgen.stage.serialization.SearchResultSet;
import semgen.stage.serialization.StageState;
import semgen.stage.stagetasks.extractor.ModelExtractionGroup;
import semgen.utilities.SemGenError;
import semgen.utilities.file.LoadSemSimModel;
import semgen.utilities.file.SaveSemSimModel;
import semgen.utilities.file.SemGenOpenFileChooser;
import semgen.utilities.file.SemGenSaveFileChooser;
import semgen.visualizations.CommunicatingWebBrowserCommandReceiver;
import semsim.model.collection.SemSimModel;
import semsim.reading.ModelAccessor;
import semsim.reading.ModelClassifier.ModelType;

public class ProjectTask extends StageTask<ProjectWebBrowserCommandSender> {
	
	private ArrayList<ModelExtractionGroup> extractnodeworkbenchmap = new ArrayList<ModelExtractionGroup>(); 
	
	public ProjectTask() {
		super(0);
		_commandReceiver = new ProjectCommandReceiver();
		state = new StageState(Task.PROJECT, taskindex);
	}

	/**
	 * Receives commands from javascript
	 * @author Ryan
	 *
	 */
	protected class ProjectCommandReceiver extends CommunicatingWebBrowserCommandReceiver {
		
		public void onInitialized(JSObject jstaskobj) {
			jstask = jstaskobj;
		}
		
		
		/**
		 * Receives the add model command
		 */
		public void onAddModel() {
			SemGenOpenFileChooser sgc = new SemGenOpenFileChooser("Select models to load", true);

			for (ModelAccessor accessor : sgc.getSelectedFilesAsModelAccessors()) {
				boolean alreadyopen = false;
								
				for (ModelInfo info : _models) {
					if (info != null) {
						alreadyopen = info.accessor.equals(accessor);
					}
					if (alreadyopen) break;
				}
				if (alreadyopen) continue;
				
				LoadSemSimModel loader = new LoadSemSimModel(accessor, false);
				loader.run();
				SemSimModel semsimmodel = loader.getLoadedModel();
				if (SemGenError.showSemSimErrors()) {
					continue;
				}

				ModelInfo info = new ModelInfo(semsimmodel, accessor, _models.size());
				extractnodeworkbenchmap.add(new ModelExtractionGroup(info));
				addModeltoTask(info);
				// Tell the view to add a model
				_commandSender.addModel(info.modelnode);
			}
		}
		
		public void onAddModelByName(String source, String modelName) throws FileNotFoundException {
			if(source.equals(CompositeAnnotationSearch.SourceName)) {
				ModelAccessor file = new ModelAccessor(SemGen.examplespath + "AnnotatedModels/" + modelName + ".owl");
				LoadSemSimModel loader = new LoadSemSimModel(file, false);
				loader.run();
				SemSimModel semsimmodel = loader.getLoadedModel();
				if (SemGenError.showSemSimErrors()) {
					return;
				}
				ModelInfo info = new ModelInfo(semsimmodel, file, _models.size());
	
				extractnodeworkbenchmap.add(new ModelExtractionGroup(info));
				addModeltoTask(info);
				_commandSender.addModel(info.modelnode);
			}
		}
		
		public void onTaskClicked(JSArray modelindex, String task) {
			// Execute the proper task
			switch(task) {
				case "annotate":
					annotateModels(modelindex);

					break;
				case "save":
					saveModels(modelindex);
					break;
				case "export":
					exportModels(modelindex);				
					break;
				case "close":
					closeModels(modelindex);					
					
					break;
				default:
					JOptionPane.showMessageDialog(null, "Task: '" + task +"', coming soon :)");
					break;
			}

		}
		
		public void onCloseModels(JSArray modelindex) {
			closeModels(modelindex);
		}

		public void onSearch(String searchString) throws FileNotFoundException {
			SearchResultSet[] resultSets = {
					CompositeAnnotationSearch.compositeAnnotationSearch(searchString),
					// PMR results here
			};

			_commandSender.search(resultSets);
		}
		
		public void onMerge(JSArray model1, JSArray model2) {
			createMerger(model1, model2);
		}
		
		
		public void onQueryModel(Integer modelindex, String query) {
			ModelInfo modelInfo = _models.get(modelindex);
			switch (query) {
			case "hassubmodels":
				Boolean hassubmodels = !modelInfo.Model.getSubmodels().isEmpty();
				_commandSender.receiveReply(hassubmodels.toString());
				break;
			case "hasdependencies":
				Boolean hasdependencies = !modelInfo.Model.getAssociatedDataStructures().isEmpty();
				_commandSender.receiveReply(hasdependencies.toString());
				break;
			}
		}
		
		public void onRequestExtractions() {
			ArrayList<ArrayList<ExtractionNode>> extractions = new ArrayList<ArrayList<ExtractionNode>>();
			for (ModelExtractionGroup meg : extractnodeworkbenchmap) {
				extractions.add(meg.getExtractionArray());
			}
				_commandSender.loadExtractions(extractions);
		}
		
		public void onNewExtraction(Double sourceindex, JSArray nodes, String extractname) {
			ArrayList<Node<?>> jnodes = convertJSStageNodestoJava(nodes);
			createNewExtraction(sourceindex.intValue(), jnodes, extractname);
		}
		
		public void onNewPhysioExtraction(Double sourceindex, JSArray nodes, String extractname) {
			ArrayList<Node<?>> jnodes = convertJSStagePhysioNodestoJava(nodes);
			createNewExtraction(sourceindex.intValue(), jnodes, extractname);
		}
		
		public void onCreateExtractionExclude(Double sourceindex, JSArray nodes, String extractname) {
			ArrayList<Node<?>> jnodes = convertJSStageNodestoJava(nodes);
			createNewExtractionExcluding(sourceindex.intValue(), jnodes, extractname);
		}
		
		public void onCreatePhysioExtractionExclude(Double sourceindex, JSArray nodes, String extractname) {
			ArrayList<Node<?>> jnodes = convertJSStagePhysioNodestoJava(nodes);
			createNewExtractionExcluding(sourceindex.intValue(), jnodes, extractname);
		}
		
		public void onRemoveNodesFromExtraction(Double sourceindex, Double extraction, JSArray nodes) {
			ArrayList<Node<?>> jnodes = convertJSStageNodestoJava(nodes, sourceindex, extraction);
			removeNodesfromExtraction(sourceindex.intValue(), extraction.intValue(), jnodes);
		}
		
		public void onRemovePhysioNodesFromExtraction(Double sourceindex, Double extraction, JSArray nodes) {
			ArrayList<Node<?>> jnodes = convertJSStagePhysioNodestoJava(nodes,sourceindex, extraction);
			removeNodesfromExtraction(sourceindex.intValue(), extraction.intValue(), jnodes);
		}
		
		public void onAddNodestoExtraction(Double sourceindex, Double extraction, JSArray nodes) {
			ArrayList<Node<?>> jnodes = convertJSStageNodestoJava(nodes);
			addNodestoExtraction(sourceindex.intValue(), extraction.intValue(), jnodes);
			
		}
		
		public void onAddPhysioNodestoExtraction(Double sourceindex, Double extraction, JSArray nodes) {
			ArrayList<Node<?>> jnodes = convertJSStagePhysioNodestoJava(nodes);
			addNodestoExtraction(sourceindex.intValue(), extraction.intValue(), jnodes);
			
		}
		
		public void onSave(JSArray indicies) {
			saveModels(indicies);
		}
		
		public void onChangeTask(Double index) {
			switchTask(index.intValue());
		}
		
		public void onConsoleOut(String msg) {
			System.out.println(msg);
		}
		
		public void onConsoleOut(Double msg) {
			System.out.println(msg.toString());
		}
		
		public void onConsoleOut(boolean msg) {
			System.out.println(msg);
		}
	}
	
	protected void annotateModels(JSArray indicies) {
		for (int i=0; i < indicies.length(); i++) {
			JSArray address = indicies.get(i).asArray();
			int indexedtomodel = address.get(0).asNumber().getInteger();
			int modelindex = address.get(1).asNumber().getInteger();
			
			
			ModelAccessor accessor = null;	
			if (indexedtomodel==-1) {
				accessor = _models.get(modelindex).accessor;
			}
			else {
				ModelExtractionGroup meg = this.extractnodeworkbenchmap.get(indexedtomodel);
				accessor = meg.getAccessorbyIndexAlways(modelindex);
			}
			if (accessor == null) continue;
			SemGen.gacts.NewAnnotatorTab(accessor);
		}
	}
	
	protected void exportModels(JSArray indicies) {
		
		for (int i=0; i < indicies.length(); i++) {
			StageRootInfo<?> modelinfo = null;
			JSArray address = indicies.get(i).asArray();
			int indexedtomodel = address.get(0).asNumber().getInteger();
			int modelindex = address.get(1).asNumber().getInteger();
			
			if (indexedtomodel==-1) {
				modelinfo = this.getModel(modelindex);
				String selectedtype = "owl";  // Default extension type
				ModelType modtype = modelinfo.Model.getSourceModelType();
				
				if(modtype==ModelType.MML_MODEL_IN_PROJ || modtype==ModelType.MML_MODEL) selectedtype = "proj";
				else if(modtype==ModelType.CELLML_MODEL) selectedtype = "cellml";
				else if(modtype==ModelType.SBML_MODEL) selectedtype = "sbml";
				
				String suggestedparentfilename = FilenameUtils.removeExtension(modelinfo.accessor.getFileThatContainsModel().getName());
				String modelnameinarchive = modelinfo.accessor.getModelName();
				
				SemGenSaveFileChooser filec = new SemGenSaveFileChooser(SemGenSaveFileChooser.ALL_WRITABLE_TYPES, selectedtype, modelnameinarchive, suggestedparentfilename);
				ModelAccessor ma = filec.SaveAsAction(modelinfo.Model);
			
				if (ma != null)	{			
					SaveSemSimModel.writeToFile(modelinfo.Model, ma, ma.getFileThatContainsModel(), filec.getFileFilter());	
				}
			}
			else {
				modelinfo =  this.extractnodeworkbenchmap.get(indexedtomodel).getExtractionInfo(modelindex);
				if (modelinfo.accessor==null) {
					modelinfo.accessor = this.extractnodeworkbenchmap.get(indexedtomodel).saveExtraction(modelindex);
					if (modelinfo.accessor==null) continue;
				}
			}
			

		}			
	}
	
	protected void saveModels(JSArray indicies) {
		for (int i = 0; i < indicies.length(); i++) {
			JSArray saveset = indicies.get(i).asArray();
			Integer basemodelindex = saveset.get(0).asNumber().getInteger();
			Integer targetindex = saveset.get(1).asNumber().getInteger();
			if (basemodelindex==-1) {
				
			}
			else {
				extractnodeworkbenchmap.get(basemodelindex).saveExtraction(targetindex);
			}
		}
	}
	
	protected void closeModels(JSArray indicies) {
		for (int i=0; i < indicies.length(); i++) {
			JSArray address = indicies.get(i).asArray();
			int indexedtomodel = address.get(0).asNumber().getInteger();
			int modelindex = address.get(1).asNumber().getInteger();
			
			if (indexedtomodel==-1) {
				removeModel(modelindex);
			}
			else {
				ModelExtractionGroup meg = this.extractnodeworkbenchmap.get(indexedtomodel);
				meg.removeExtraction(modelindex);
			}
			_commandSender.removeModel(new Integer[]{indexedtomodel, modelindex});
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		
	}

	@Override
	public Task getTaskType() {
		return Task.PROJECT;
	}

	@Override
	public Class<ProjectWebBrowserCommandSender> getSenderInterface() {
		return ProjectWebBrowserCommandSender.class;
	}


	@Override
	public void closeTask() {
		
	}
	
	
	//EXTRACTION FUNCTIONS
	public void createNewExtraction(Integer infoindex, ArrayList<Node<?>> nodestoextract, String extractname) {
		ModelExtractionGroup group = this.extractnodeworkbenchmap.get(infoindex);
		ExtractionNode extraction = group.createExtraction(extractname, nodestoextract);
		
		_commandSender.newExtraction(infoindex, extraction);
	}	
	
	public void createNewExtractionExcluding(Integer infoindex, ArrayList<Node<?>> nodestoexclude, String extractname) {
		ModelExtractionGroup group = this.extractnodeworkbenchmap.get(infoindex);
		ExtractionNode extraction = group.createExtractionExcluding(extractname, nodestoexclude);
		_commandSender.newExtraction(infoindex, extraction);
	}
	
	public void addNodestoExtraction(Integer infoindex, Integer extractionindex, ArrayList<Node<?>> nodestoadd) {
		ModelExtractionGroup group = this.extractnodeworkbenchmap.get(infoindex);
		ExtractionNode extraction = group.addNodestoExtraction(extractionindex, nodestoadd);
		_commandSender.modifyExtraction(infoindex, extractionindex, extraction);
	}
	
	public void removeNodesfromExtraction(Integer infoindex, Integer extractionindex, ArrayList<Node<?>> nodestoremove) {
		ModelExtractionGroup group = this.extractnodeworkbenchmap.get(infoindex);
		ExtractionNode extraction = group.removeNodesfromExtraction(extractionindex, nodestoremove);
		_commandSender.modifyExtraction(infoindex, extractionindex, extraction);
	}
	

	protected void removeExtraction(Double sourceindex, Double indextoremove) {		
		this.extractnodeworkbenchmap.get(sourceindex.intValue()).removeExtraction(indextoremove.intValue());	
	}

	//Convert Javascript Node objects to Java Node objects
	public ArrayList<Node<?>> convertJSStageNodestoJava(JSArray nodearray, Double modelindex, Double extractionindex) {
		ModelExtractionGroup group = this.extractnodeworkbenchmap.get(modelindex.intValue());
		ArrayList<Node<?>> javanodes = new ArrayList<Node<?>>();
		for (int i = 0; i < nodearray.length(); i++) {
			JSObject val = nodearray.get(i).asObject();
			javanodes.add(group.getNodebyHash(val.getProperty("hash").asNumber().getInteger(), val.getProperty("id").getStringValue(), extractionindex.intValue()));
		}
		return javanodes;
	}

	//Convert Javascript Node objects to Java Node objects
	public ArrayList<Node<?>> convertJSStagePhysioNodestoJava(JSArray nodearray, Double modelindex, Double extractionindex) {
		ModelExtractionGroup group = this.extractnodeworkbenchmap.get(modelindex.intValue());
		ArrayList<Node<?>> javanodes = new ArrayList<Node<?>>();
		for (int i = 0; i < nodearray.length(); i++) {
			JSObject val = nodearray.get(i).asObject();
			javanodes.add(group.getPhysioMapNodebyHash(val.getProperty("hash").asNumber().getInteger(), val.getProperty("id").getStringValue(), extractionindex.intValue()));
		}
		return javanodes;
	}

	
	protected StageRootInfo<?> getInfobyAddress(JSArray address) {
		if (address.get(0).asNumber().getInteger()==-1) {
			return _models.get(address.get(1).asNumber().getInteger());
		}
		else {
			return this.extractnodeworkbenchmap.get(address.get(0).asNumber().getInteger()).getExtractionInfo(address.get(1).asNumber().getInteger());
		}
	}
}
