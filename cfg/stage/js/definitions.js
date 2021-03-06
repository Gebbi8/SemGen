/**
 * 
 */

var StageTasks = {
		PROJECT: {thumb: '../../src/semgen/icons/stageicon2020.png'},
		MERGER: {thumb: '../../src/semgen/icons/mergeicon2020.png'},
		EXTRACTOR: {thumb: '../../src/semgen/icons/extractoricon2020.png'}
};

var NodeType = {
		MODEL: {id: 0, nodeType: "Model", color: "#000000", canShowHide: false},
		SUBMODEL: {id: 1, nodeType: "Submodel", color: "#CA9485", canShowHide: false},
		STATE: {id: 2, nodeType: "State", color: "#1F77B4", canShowHide: true},
		RATE: {id: 3, nodeType: "Rate", color: "#2CA02C", canShowHide: true},
		CONSTITUTIVE: {id: 4, nodeType: "Constitutive", color: "#FF7F0E", canShowHide: true},
		ENTITY: {id: 5, nodeType: "Entity", color: "#1F77B4", canShowHide: true},
		PROCESS: {id: 6, nodeType: "Process", color: "#2CA02C", canShowHide: true},
		MEDIATOR: {id: 7, nodeType: "Entity", color: "#1F77B4", canShowHide: true},
		NULLNODE: {id: 8, nodeType: "Null", color: "#FFFFFF", canShowHide: true},
		EXTRACTION: {id: 9, nodeType: "Extraction", color: "#118888", canShowHide: false}
};

var NodeTypeMap = {
		"Model": NodeType.MODEL,
		"Submodel": NodeType.SUBMODEL,
		"State": NodeType.STATE,
		"Rate": NodeType.RATE,
		"Constitutive": NodeType.CONSTITUTIVE,
		"Entity": NodeType.ENTITY,
		"Process": NodeType.PROCESS,
		"Mediator": NodeType.MEDIATOR,
		"Null": NodeType.NULLNODE,
		"Extraction": NodeType.EXTRACTION
		};

var NodeTypeArray = [
		NodeType.MODEL,
		NodeType.SUBMODEL,
		NodeType.STATE,
		NodeType.RATE,
		NodeType.CONSTITUTIVE,
		NodeType.ENTITY,
		NodeType.PROCESS,
		NodeType.MEDIATOR,
		NodeType.NULLNODE,
		NodeType.EXTRACTION
		];


var defaultcharge = -180;
var defaultlinklength = 50;
var defaultchargedistance = 300;

var DisplayModes = {
	SHOWSUBMODELS: { id: 0, keys: [NodeType.MODEL, NodeType.SUBMODEL, NodeType.STATE, NodeType.RATE, NodeType.CONSTITUTIVE]},
	SHOWDEPENDENCIES: { id: 1, keys: [NodeType.MODEL, NodeType.STATE, NodeType.RATE, NodeType.CONSTITUTIVE]},
	SHOWPHYSIOMAP: { id: 2, keys: [NodeType.MODEL, NodeType.ENTITY, NodeType.PROCESS]}
};
