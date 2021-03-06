
Stage.prototype = new Task();
Stage.prototype.constructor = Stage;
function Stage(graph, stagestate) {
	Task.prototype.constructor.call(this, graph, stagestate);

	var stage = this;
	var nodes = this.nodes;
	this.taskindex = 0;
	
	stage.graph.depBehaviors = [];
	stage.graph.ghostBehaviors = [];
	stage.extractions = {};

	$("#addModelButton, .stageSearch").show();

    $('[data-toggle="tooltip"]').tooltip({delay: {show: 1000, hide: 50}});

    this.leftsidebar = new LeftSidebar(graph);


	var leftsidebar = this.leftsidebar;

	var droploc;
	
	var trash = new StageDoodad(this.graph, "trash", 0.05, 0.9, 2.0, 2.0, "glyphicon glyphicon-scissors", "");
	this.graph.doodads.push(trash);
	

	document.addEventListener("click", function(e) {
		$('contextmenu').hide();
	});
	
	// Adds a model node to the d3 graph
	receiver.onAddModel(function (model) {
		console.log("Adding model " + model.name);
		var modelnode = stage.addModelNode(model, [DragToMerge]);
		stage.extractions[modelnode.modelindex] = {modextractions: []};
		stage.leftsidebar.addModeltoList(model);
	});

	//Remove the named model node
	receiver.onRemoveModel(function(modelindex) {
		if (modelindex[0] == -1) {
			var model = stage.getModelNodebyIndex(modelindex[1]);
			sender.consoleOut("Removing model " + model.name);
			leftsidebar.removeModelfromList(model.id);
			delete nodes[model.id];
			delete stage.extractions[model];
			
			graph.update();
		}
		else {
			stage.removeExtraction(modelindex[0], modelindex[1]);
		}
		leftsidebar.updateModelPanel(null);
		
	});

	// Show search results on stage
	receiver.onSearch(function (searchResults) {
		console.log("Showing search results");

		// Remove all elements from the result list
		var searchResultsList = $(".searchResults");
		searchResultsList.empty();

		// Create UI for the results
		searchResults.forEach(function (searchResultSet ) {
			searchResultSet.results.sort(function (a, b) {
				return a.toLowerCase().localeCompare(b.toLowerCase());
			});

			searchResultsList.append(makeResultSet(searchResultSet));
		});
	});

	receiver.onReceiveReply(function (reply) {
		CallWaiting(reply);
	});

	$("#addModelButton").click(function() {
		event.stopPropagation();
		sender.addModel();
	});

	$("#addModel").click(function() {
		event.stopPropagation();
		sender.addModel();
	});

	// When you mouseover the search element show the search box and results
	$(".stageSearch").mouseover(function (){
		$(".stageSearch .searchValueContainer").css('display', 'inline-block');
	});

	// When you mouseout of the search element hide the search box and results
	$(".stageSearch").mouseout(function (){
		$(".stageSearch .searchValueContainer").hide();
	});

	$(".searchString").keyup(function() {
		if( $(this).val() ) {
			$(".stageSearch .searchValueContainer .searchResults").show();
			sender.search($( this ).val());
		}
		else {
			$(".stageSearch .searchValueContainer .searchResults").hide();
		}
	});
	
	$("#saveModel").click(function() {
		var modelstosave = [], count = 0;
		for (i in stage.selectedModels) {
				var model = stage.selectedModels[i];
			
				if (!model.saved && model.selected) {
					modelstosave.push(model.getIndexAddress());
					count++;
				}
		}
		if (count==0) return;
		sender.save(extractstosave);
	});
	
	//******************EXTRACTION FUNCTIONS*************************//
	
	var promptForExtractionName = function() {
		var name = prompt("Enter name for extraction.", "");
		for (x in stage.nodes) {
			if (x==name) return promptForExtractionName();
		}
		
		return name;
	}
	
	//Remove the extraction node
	this.removeExtraction = function(modelindex, extract) {
		var extraction = stage.extractions[modelindex].modextractions[extract];
		sender.consoleOut("Removing extraction " + extraction.name);
		
		stage.extractions[modelindex].modextractions[extract] = null;
		delete stage.nodes[extraction.id];
		graph.update();
	};
	
	var onExtractionAction = function(node) {
		//Don't add any extraction actions to a model node.
		
		if (node.nodeType == NodeType.MODEL) return;
		node.drag.push(function(selections) {
			if (trash.isOverlappedBy(node, 2.0)) {
				$("#trash").prop("background-color", "red");
			}
			else {
				$("#trash").prop("background-color", "transparent");
			}
			
			for (i in stage.nodes) {
				var ithnode = stage.nodes[i];
				if (ithnode.hullContainsPoint([node.xpos(), node.ypos()])) {
					if ( ithnode.nodeType==NodeType.MODEL || ithnode != node.srcnode.getRootParent() ) {
						ithnode.rootElement.select(".hull").style("stroke","red");
					}
					else {
						ithnode.rootElement.select(".hull").style("stroke","goldenrod");
					}
				}
				else {
					ithnode.rootElement.select(".hull").style("stroke", ithnode.nodeType.color);
				}
			}

		});
		
		node.dragEnd.push(function(selections) {
			stage.graph.shiftIsPressed = false;
			droploc = [node.xpos(), node.ypos()];

			//Reset hull colors
			for (x in stage.nodes) {
				stage.nodes[x].rootElement.select(".hull").style("stroke", stage.nodes[x].nodeType.color);
			}
			var root = node.srcnode.getRootParent();
			
			// Ensure all selected nodes share the same parent as the first selected node
			var extractarray = [];
			for (x in selections) {
				var selnode = selections[x].srcnode;
				if (selnode.nodeType == NodeType.MODEL && root!=selnode.getRootParent()) continue;
				extractarray.push(selnode);
			}
			
			//If the node is dragged to the trash
			if (trash.isOverlappedBy(node, 2.0)) {
					droploc= stage.graph.getCenter();
					
					if (root.nodeType!=NodeType.MODEL) {
						var srcmodindex = root.sourcenode.modelindex;
						//If an extraction is dragged to the trash, delete it
						if (root == node.srcnode) {
							sender.closeModels([root.getIndexAddress()]);
							return;
						}
						
						if (root.displaymode==DisplayModes.SHOWPHYSIOMAP.id) {
							sender.removePhysioNodesFromExtraction(srcmodindex, root.modelindex, extractarray);
						}
						else {
							sender.removeNodesFromExtraction(srcmodindex, root.modelindex, extractarray);
						}
					}
					return;
			}
			
			var destinationnode = null;
			for (i in stage.nodes) {
				var ithnode = stage.nodes[i];
				
				if (ithnode.hullContainsPoint([node.xpos(), node.ypos()])) {
					//if a model hull contains the dropped ghost node, do nothing.
					if ( ithnode.nodeType==NodeType.MODEL ) {
						return;
					}
					else {
						destinationnode = ithnode;
						break;
					}
				}
			}
			if (destinationnode!=root && destinationnode!=null) {
				return;
			}
			//Check to see if node is inside an extraction hull
			if (destinationnode!=null) {
				sender.addNodestoExtraction(destinationnode.sourcenode.modelindex, destinationnode.modelindex, extractarray);
				return;
			}
			stage.createExtraction(extractarray,root);

		});
	}
	
	this.createExtraction = function(extractarray, root) {
		//If it's dropped in empty space, create a new extraction
		var name = promptForExtractionName();
			
		//Don't create extraction if user cancels
		if (name==null) return;
		
		var baserootindex = root.modelindex;
		if (root.displaymode==DisplayModes.SHOWPHYSIOMAP.id) {
			sender.newPhysioExtraction(baserootindex, extractarray, name);
		}
		else {
			sender.newExtraction(baserootindex, extractarray, name);
		}
	}

	this.createExtractionandExclude = function(extractarray, root) {
		//If it's dropped in empty space, create a new extraction
		var name = promptForExtractionName();
			
		//Don't create extraction if user cancels
		if (name==null) return;
		
		if (root.displaymode==DisplayModes.SHOWPHYSIOMAP.id) {
			sender.createPhysioExtractionExclude(root.modelindex, extractarray, name);
		}
		else {
			sender.createExtractionExclude(root.modelindex, extractarray, name);
		}
	}
	
	this.applytoExtractions = function(dothis) {
		for (x in stage.extractions) {
			for (y in stage.extractions[x]) {
				dothis(stage.extractions[x][y]);
			}
		}
	}
	
	//Apply to children until the function returns true
	this.applytoExtractionsUntilTrue = function(funct) {
		for (x in stage.extractions) {
			for (y in stage.extractions[x]) {
				if (dothis(stage.extractions[x][y])) return true;
			}
		}
		return false;
	}
	
	this.graph.ghostBehaviors.push(onExtractionAction);
	
	//On an extraction event from the context menu, make a new extraction
	$('#stage').on('extract', function(e, caller) {
		if (!caller.selected) stage.selectNode(caller);
		var root = caller.getRootParent();
		stage.createExtraction(stage.selectedNodes,root);
	});
	
	$('#stage').on('extractexclude', function(e, caller) {
		if (!caller.selected) stage.selectNode(caller);
		var root = caller.getRootParent();
		stage.createExtractionandExclude(stage.selectedNodes,root);
	});
	
	this.addExtractionNode = function(basenodeindex, newextraction) {
		var basenode = stage.getModelNodebyIndex(basenodeindex);
		var extractionnode = new ExtractedModel(stage.graph, newextraction, basenode);
		extractionnode.addBehavior(DragToMerge);
		stage.extractions[basenodeindex].modextractions.push(extractionnode);
		stage.nodes[newextraction.id] = extractionnode;
		if (droploc!=null) {
			extractionnode.setLocation(droploc[0], droploc[1]);
		}
		extractionnode.createVisualization(DisplayModes.SHOWSUBMODELS.id, false);
		stage.graph.update();
		stage.selectNode(extractionnode);
	}
	
	this.setExtractionNode = function(basemodelindex, index, extraction) {
		var extractionnode = new ExtractedModel(stage.graph, extraction);
		droploc = [stage.extractions[basemodelindex].modextractions[index].xpos(), stage.extractions[basemodelindex].modextractions[index].ypos()];
		stage.extractions[basemodelindex].modextractions[index] = extractionnode;
		stage.nodes[extractionnode.id] = extractionnode;
		if (droploc!=null) {
			extractionnode.setLocation(droploc[0], droploc[1]);
		}
		extractionnode.createVisualization(DisplayModes.SHOWSUBMODELS.id, true);
		stage.graph.update();
		stage.selectNode(extractionnode);
	}

	receiver.onLoadExtractions(function(extractions) {
		for (x in extractions) {
			for (y in extractions[x]) {
				stage.addExtractionNode(x, extractions[x][y]);
			}
		}
	});
	
	receiver.onNewExtraction(function(sourceindex, newextraction) {
		stage.addExtractionNode(sourceindex, newextraction);
	});
	
	receiver.onModifyExtraction(function(sourceindex, index, extraction) {
		stage.setExtractionNode(sourceindex, index, extraction);
	});
}

//For objects that must be loaded after the rest of the stage is loaded
Stage.prototype.onInitialize = function() {
	var stage = this;

	if (stage.state.models.length > 0) {
		stage.state.models.forEach(function(model) {
			stage.addModelNode(model, [DragToMerge]);
			stage.extractions[model.modelindex] = {modextractions: []};
		});		

	}
	sender.requestExtractions();
	$('#taskModal').hide();
	this.setSavedState(true);
}

Stage.prototype.onModelSelection = function(node) {
	this.leftsidebar.updateModelPanel(node);
}


Stage.prototype.setSavedState = function (issaved) {
	Task.prototype.setSavedState.call(issaved);
	this.setSaved(this.isSaved());
	$('#saveModel').prop('disabled', issaved);
}


function makeResultSet(searchResultSet) {
	var resultSet = $(
		"<li class='searchResultSet'>" +
			"<label>" + searchResultSet.source + "</label>" +
		"</li>"
	);

    var list = document.createElement('ul');
    for(var i = 0; i < searchResultSet.results.length; i++) {
        var item = document.createElement('li');
        item.className = "searchResultSetValue";
        item.appendChild(document.createTextNode(searchResultSet.results[i]));
        list.appendChild(item);
        $(item).data("source", searchResultSet.source);
        $(item).click(function() {
			var modelName = $(this).text().trim();
			var source = $(this).data("source");
			sender.addModelByName(source, modelName);

			// Hide the search box
			$(".stageSearch .searchValueContainer").hide();
		});
    }

    resultSet.append(list);
    return resultSet;
};

Stage.prototype.getTaskType = function() { return StageTasks.PROJECT; }