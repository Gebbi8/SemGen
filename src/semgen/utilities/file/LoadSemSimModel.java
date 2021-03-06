package semgen.utilities.file;

import java.io.IOException;

import org.jdom.Document;
import org.semanticweb.owlapi.model.OWLException;

import JSim.util.Xcept;
import semgen.SemGen;
import semgen.annotation.workbench.routines.AutoAnnotate;
import semgen.utilities.SemGenJob;
import semsim.model.collection.SemSimModel;
import semsim.reading.CellMLreader;
import semsim.reading.JSimProjectFileReader;
import semsim.reading.MMLtoXMMLconverter;
import semsim.reading.ModelAccessor;
import semsim.reading.XMMLreader;
import semsim.reading.ModelClassifier;
import semsim.reading.ReferenceTermNamer;
import semsim.reading.SBMLreader;
import semsim.reading.SemSimOWLreader;
import semsim.reading.ModelClassifier.ModelType;
import semsim.utilities.ErrorLog;
import semsim.utilities.SemSimUtil;
import semsim.utilities.webservices.BioPortalSearcher;

public class LoadSemSimModel extends SemGenJob {
	private ModelAccessor modelaccessor;
	private boolean autoannotate = false;
	private SemSimModel semsimmodel;
	
	public LoadSemSimModel(ModelAccessor modelaccessor) {
		this.modelaccessor = modelaccessor;
	}
	
	public LoadSemSimModel(ModelAccessor modelaccessor, boolean autoannotate) {
		this.modelaccessor = modelaccessor;
		this.autoannotate = autoannotate;
	}
	
	public LoadSemSimModel(ModelAccessor modelaccessor, SemGenJob sga) {
		super(sga);
		this.modelaccessor = modelaccessor;
	}
	
	public LoadSemSimModel(ModelAccessor modelaccessor, boolean autoannotate, SemGenJob sga) {
		super(sga);
		this.modelaccessor = modelaccessor;
		this.autoannotate = autoannotate;
	}
	
	private void loadSemSimModelFromFile() {
		
    	setStatus("Reading " + modelaccessor.getShortLocation());

    	ModelType modeltype = ModelClassifier.classify(modelaccessor);
		try {
			switch (modeltype){
			
			case SEMSIM_MODEL:
				if(modelaccessor.modelIsPartOfArchive())
					ErrorLog.addError("Cannot load a SemSim model from within an archive file.", true, false);
				else
					semsimmodel = new SemSimOWLreader(modelaccessor.getFileThatContainsModel()).read();	
				break;
			
			case CELLML_MODEL:
				CellMLreader reader = new CellMLreader(modelaccessor);
				semsimmodel = reader.read();
				
				// If the semsim namespace is prefixed in the RDF, then we assume it was previously annotated
				// and we don't perform automatic OPB annotation based on units
				boolean previouslyannotated = (reader.rdfblock.rdf.getNsPrefixURI("semsim") != null);
				
				// If the model wasn't previously annotated in SemGen and autoannotate is turned on,
				// perform auto-annotation
				if((semsimmodel!=null) && semsimmodel.getErrors().isEmpty() && autoannotate && ! previouslyannotated) {
					setStatus("Annotating Physical Properties");
					AutoAnnotate.autoAnnotateWithOPB(semsimmodel);
				}
				// Otherwise collect any needed reference term names from BioPortal
				else{
					setStatus("Collecting annotations");
					nameOntologyTerms();
				}

				break;
				
			case SBML_MODEL:
				semsimmodel = new SBMLreader(modelaccessor).read();			
				nameOntologyTerms();
				break;
				
			case MML_MODEL:
				loadMML(modelaccessor);
				break;
				
			case MML_MODEL_IN_PROJ:
				loadMML(modelaccessor);
				break;
				
			default:
				ErrorLog.addError("SemGen did not recognize the model type for " + modelaccessor.getShortLocation(), true, false);
				return;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		if(semsimmodel != null){
			
			if( ! semsimmodel.getErrors().isEmpty()){
				for (String e : semsimmodel.getErrors()) {
					ErrorLog.addError(e,true, true);
				}
				return;
			}
			semsimmodel.setName(modelaccessor.getModelName());
			semsimmodel.setSourceModelType(modeltype);
			SemSimUtil.regularizePhysicalProperties(semsimmodel, SemGen.semsimlib);
		}
		else
			ErrorLog.addError(modelaccessor.getModelName() + " was an invalid model.", true, false);
		
	}


	private SemSimModel loadMML(ModelAccessor ma) throws Xcept, IOException, InterruptedException, OWLException{
		
		String srcText = ma.getLocalModelTextAsString();
		Document xmmldoc = MMLtoXMMLconverter.convert(srcText, ma.getModelName());
		
		if (ErrorLog.hasErrors()) return null;
		
		XMMLreader xmmlreader = new XMMLreader(modelaccessor, xmmldoc, srcText);		
		semsimmodel = xmmlreader.readFromDocument();
		
		if((semsimmodel == null) || (! semsimmodel.getErrors().isEmpty()))
			return semsimmodel;
		
		else{
			
			if(JSimProjectFileReader.getModelPreviouslyAnnotated(semsimmodel, ma)){
				//If annotations present, collect names of reference terms
				setStatus("Collecting annotations");
				nameOntologyTerms();
			}
			else if(autoannotate){
				// Otherwise auto-annotate, if turned on, 
				setStatus("Annotating physical properties");
				AutoAnnotate.autoAnnotateWithOPB(semsimmodel);
			}
		}
		
		return semsimmodel;
	}
	
	
	private void nameOntologyTerms(){
		if(semsimmodel.getErrors().isEmpty() && ReferenceTermNamer.getModelComponentsWithUnnamedAnnotations(semsimmodel, SemGen.semsimlib).size()>0){

			setStatus("Annotating with web services");
			boolean online = BioPortalSearcher.testBioPortalWebservice();
			
			if( ! online){
				ErrorLog.addError("Could not connect to BioPortal search service", false, false);
			}
				ReferenceTermNamer.getNamesForOntologyTermsInModel(semsimmodel, SemGen.termcache.getOntTermsandNamesCache(), SemGen.semsimlib);
//					SBMLAnnotator.setFreeTextDefinitionsForDataStructuresAndSubmodels(semsimmodel);
			}
		}

	@Override
	public void run() {
		loadSemSimModelFromFile();
		if (semsimmodel == null || ErrorLog.errorsAreFatal()) {
			abort();
			return;
		}
	}
	
	public SemSimModel getLoadedModel() {
		return semsimmodel;
	}
}
