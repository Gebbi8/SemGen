package semgen.stage.stagetasks.extractor;

import semsim.model.collection.SemSimModel;
import semsim.model.collection.Submodel;
import semsim.model.computational.datastructures.DataStructure;
import semsim.model.computational.datastructures.MappableVariable;
import semsim.model.physical.PhysicalEntity;
import semsim.model.physical.PhysicalProcess;

public class ExtractAdd extends Extractor {	
	public ExtractAdd(SemSimModel source, SemSimModel target) {
		super(source);
		extraction.setName(target.getName());
		collectStructures(target);
	}

	public void addDataStructure(DataStructure dstoimport) {
		if (dstoimport instanceof MappableVariable) {
			if (((MappableVariable)dstoimport).getMappedFrom()!=null) {
				dstoimport = ((MappableVariable)dstoimport).getMappedFrom();
			}
		}
		includeDependency(sourcemodel.getAssociatedDataStructure(dstoimport.getName()));
	}
	
	@Override
	public void addSubmodel(Submodel sourceobj) {
		includeSubModel(sourcemodel.getSubmodel(sourceobj.getName()));
	}
	
	private void collectStructures(SemSimModel receivermodel) {
		for (Submodel sm : receivermodel.getSubmodels()) {
			this.includeSubModel(sourcemodel.getSubmodel(sm.getName()));
		}
		for (DataStructure ds : receivermodel.getAssociatedDataStructures()) {
			if (!ds.isExternal()) {
				DataStructure existingds = sourcemodel.getAssociatedDataStructure(ds.getName());
				this.includeDependency(existingds);
			}
		}
	}
	
	@Override
	public void addEntity(PhysicalEntity pe) {
		for (DataStructure dstoadd : gatherDatastructureswithPhysicalComponent(pe)) {
			addDataStructure(dstoadd);
		}
	}
	
	@Override
	public void addProcess(PhysicalProcess proc) {
		for (DataStructure dstoadd : gatherDatastructureswithPhysicalComponent(proc)) {
			addDataStructure(dstoadd);
		}

		for (PhysicalEntity participant : proc.getParticipants()) {
			participant.addToModel(extraction);
		}
	}
	
	public SemSimModel getNewExtractionModel() {
		return extraction;
	}
	
	@Override
	public SemSimModel run() {
		
		collectDataStructureInputs();
		replaceComputations();
		replaceSubmodelDataStructures();
		replaceSubmodels();
		buildExtraction();
		
		return extraction;
	}

}
