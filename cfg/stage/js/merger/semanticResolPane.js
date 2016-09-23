/**
 * 
 */
function SemanticResolutionPane(merger) {
	var pane = this;
	var task = merger;
	var resolutions = [];
	var choices = [];
	this.readyformerge = false;

	//Checks to see if requirements are met for merging.
	this.allchoicesmade = function () {
		var choices = pane.pollOverlaps();
		
		var disable = false;
		choices.forEach(function(c) {
			if (c==-1) disable = true; 
		});
		
		this.readyformerge = !disable;
		merger.readyforMerge();
	}
	
	//Create the resolution panel
	this.addResolutionPanel = function(overlap) {
		var dsradiobutton = function (nodeside, desc, id) {
			var nodetype = NodeTypeMap[desc.type];
			return '<label>' +
				'<div class="freetextDef">' + desc.description + '</div>' +

				'<input class="mergeResRadio" type="radio" name="mergeResRadio' + id + '">' +
				'</label>';
		};
		
		var t = document.querySelector('#overlapPanel');
		var clone = document.importNode(t.content, true);
		
		clone.id = 'res' + resolutions.length;
		clone.index = resolutions.length;

		clone.choice = -1;
		clone.radiobtns = [];
		
		var radioClick = function(val) {
			clone.choice = val;
			
			pane.allchoicesmade();
		}
		
		clone.radiobtns.push(clone.querySelector('.leftRes'));
		clone.querySelector('.leftRes').setAttribute("id", 'leftRes' + clone.index);
		clone.querySelector('.leftRes').innerHTML = dsradiobutton('leftNode', overlap.dsleft, clone.id);
		clone.querySelector('.leftRes').onclick = function() {radioClick(0);};
		
		
		clone.radiobtns.push(clone.querySelector('.rightRes'));
		clone.querySelector('.rightRes').innerHTML = dsradiobutton('rightNode', overlap.dsright, clone.id);
		clone.querySelector('.rightRes').onclick = function() {radioClick(1);};
		clone.querySelector('.rightRes').setAttribute("id", 'rightRes' + clone.index);

		clone.radiobtns.push(clone.querySelector('.ignoreRes'));
		clone.querySelector(".ignoreRes").innerHTML = '<label><div class="ignoreLabel">Ignore</div>' +
			'<input class="mergeResRadio" type="radio" name="mergeResRadio' + clone.id + '"></label>';
		clone.querySelector('.ignoreRes').onclick = function() {radioClick(2);};
		clone.querySelector('.ignoreRes').setAttribute("id", 'ignoreRes' + clone.index);
		
		clone.querySelector('.mergeResCollapsePane').setAttribute("data-target", "#collapsePane" + clone.id);
		clone.querySelector('.mergeResCollapse').setAttribute("id", "collapsePane" + clone.id);

		clone.querySelector('.leftCollapsePanel > .equation').innerHTML = overlap.dsleft.equation;
		clone.querySelector('.rightCollapsePanel > .equation').innerHTML = overlap.dsright.equation;
		clone.querySelector('.leftCollapsePanel > .varName').innerHTML = overlap.dsleft.name;
		clone.querySelector('.rightCollapsePanel > .varName').innerHTML = overlap.dsright.name;
		clone.querySelector('.leftCollapsePanel > .annotation').innerHTML = overlap.dsleft.annotation;
		clone.querySelector('.rightCollapsePanel > .annotation').innerHTML = overlap.dsright.annotation;
		clone.querySelector('.leftCollapsePanel > .unit').innerHTML = overlap.dsleft.unit;
		clone.querySelector('.rightCollapsePanel > .unit').innerHTML = overlap.dsright.unit;
		
		clone.querySelector('input.mergeResRadio').disabled = merger.mergecomplete;
		clone.querySelector('.previewResolutionBtn').setAttribute("onclick", 'sender.requestPreview(' + clone.index + ');');
		
		//Custom mappings can be removed
		if (overlap.custom) {
			clone.querySelector('.removeMappingBtn').style.display = 'inherit';
			clone.querySelector('.removeMappingBtn').onclick = function() {
				choices.splice(clone.index, 1);
				sender.removeCustomOverlap(clone.index);
			};
			clone.querySelector('.removeMappingBtn').disabled = merger.mergecomplete;
		}

		clone.setSelection = function(sel) {
			if (sel==-1) return;
			clone.choice = sel;
			document.querySelector('#' + clone.radiobtns[sel].id + ' .mergeResRadio').setAttribute('checked','true');
		}
		
		clone.poll = function() {
			return clone.choice;
		}
		
		resolutions.push(clone);
		document.querySelector('#modalContent #overlapPanels').appendChild(clone);
		
		$(".hideResolutionsBtn").click(function() {
			$('#taskModal').modal("hide");
		})
	}


	//Preview graphs
	this.leftgraph = new PreviewGraph("modelAStage");
	this.rightgraph = new PreviewGraph("modelBStage");

	var leftModelName = "";
	var rightModelName = "";

	this.initialize = function(nodes) {
		var nodearr = getSymbolArray(nodes);
		leftModelName = nodearr[0].id;
		rightModelName = nodearr[1].id;
		$(".leftModelName").append(leftModelName);
		$(".rightModelName").append(rightModelName);
		
		sender.requestOverlaps();
	};

	this.updateOverlaps = function(overlaps) {
		resolutions = [];
		$('#modalContent #overlapPanels').contents().remove();
		
		overlaps.forEach(function(d) {
			pane.addResolutionPanel(d);	
		});
		
		for (i=0; i<choices.length; i++) {
			resolutions[i].setSelection(choices[i]);
		}
		//Disable radio buttons if the merge has been performed.
		$('input.mergeResRadio').prop('disabled', merger.mergecomplete);
		pane.allchoicesmade();
		//This is done here to prevent synchronicity issues between this and the conflict pane
		sender.requestConflicts();
	}
	
	receiver.onShowOverlaps(function(data) {
		pane.updateOverlaps(data);
	});
	
	receiver.onShowPreview(function(data) {
		pane.leftgraph.initialize();
		pane.rightgraph.initialize();

		$("#fixedNodesA").removeAttr("checked");
		$("#fixedNodesB").removeAttr("checked");

		pane.leftgraph.setPreviewData(data.choices[0]);
		pane.rightgraph.setPreviewData(data.choices[1]);

		// Different hull colors for different models
		$("[id*=" + leftModelName +"] > .hull").attr("stroke", "rgb(96, 96, 191)").attr("fill", "rgb(96, 96, 191)");
	});

	// Prevent clicking on radio button from toggling collapse panel
	$(document).on("click", ".radio", function(e) {
		e.stopPropagation();
	});

	// Adjust preview window size
	$('#resizeHandle').mousedown(function(e) {
		e.preventDefault();
		$(document).mousemove(function(e) {
			$('.mergePreview').css("height", e.pageY-94);
			$('.modal-body').css("height", $(window).height()-e.pageY-94);
			pane.leftgraph.initialize();
			pane.rightgraph.initialize();
			
		});
		$(document).mouseup(function() {
			$(document).unbind('mousemove');
		});
	});
	
	this.pollOverlaps = function() {
		choices = [];
		resolutions.forEach(function(panel) {
			choices.push(panel.poll());
		});
		return choices;
	}

	$("#fixedNodesA").bind('change', function(){
		pane.leftgraph.toggleFixedMode(this.checked);
	});
	$("#fixedNodesB").bind('change', function(){
		pane.rightgraph.toggleFixedMode(this.checked);
	});
}

