
Stage.prototype = new Task();
Stage.prototype.constructor = Stage;
function Stage(graph) {
	Task.prototype.constructor.call(this, graph);

	var stage = this;
	var nodes = this.nodes;
	
	$("#addModelButton").show();
	$(".stageSearch").show();
	
	this.leftsidebar = new LeftSidebar(graph);
	this.rightsidebar = new RightSidebar(graph);

	var leftsidebar = this.leftsidebar;

	// Adds a model node to the d3 graph
	receiver.onAddModel(function (modelName) {
		console.log("Adding model " + modelName);
		stage.addModelNode(modelName, [DragToMerge]);
		
	});

	//Remove the named model node
	receiver.onRemoveModel(function(modelName) {
		sender.consoleOut("Removing model " + modelName);
		delete nodes[modelName];
		leftsidebar.updateModelPanel(null);
		graph.update();
	});

	// Adds a dependency network to the d3 graph
	receiver.onShowDependencyNetwork(function (modelName, dependencyNodeData) {
		console.log("Showing dependencies for model " + modelName);
		graph.displaymode = DisplayModes.SHOWDEPENDENCIES;
		var modelNode = stage.getModelNode(modelName);
		modelNode.setChildren(dependencyNodeData, function (data) {
			return new DependencyNode(graph, data, modelNode);
		});
	});

	// Adds a submodel network to the d3 graph
	receiver.onShowSubmodelNetwork(function (modelName, submodelData) {
		console.log("Showing submodels for model " + modelName);
		graph.displaymode = DisplayModes.SHOWSUBMODELS;
		var modelNode = stage.getModelNode(modelName);
		modelNode.setChildren(submodelData, function (data) {
			return new SubmodelNode(graph, data, modelNode);
		});
	});

	// Adds a PhysioMap network to the d3 graph
	receiver.onShowPhysioMapNetwork(function (modelName, physiomapData) {
		console.log("Showing PhysioMap for model " + modelName);
		graph.displaymode = DisplayModes.SHOWPHYSIOMAP;

		var modelNode = stage.getModelNode(modelName);
		modelNode.setChildren(physiomapData, function (data) {
			return new PhysioMapNode(graph, data, modelNode);
		});
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

	receiver.onReceiveReply(function (reply) {
		CallWaiting(reply);
	});

	$("#addModelButton").click(function() {
		sender.addModel();
	});

	$("#addModel").click(function() {
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
			$(".stageSearch .searchValueContainer .searchResults").show()
			sender.search($( this ).val());
		}
		else {
			$(".stageSearch .searchValueContainer .searchResults").hide()
		}
	});
}

Stage.prototype.onModelSelection = function(node) {
	this.leftsidebar.updateModelPanel(node);
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
        $(item).data("source", searchResultSet.source)
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