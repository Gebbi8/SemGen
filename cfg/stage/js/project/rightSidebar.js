/**
 *
 */

function RightSidebar(graph) {
	this.graph = graph;

	// Fix all nodes when ctrl + M is pressed
	$("#fixedNodes").bind('change', function(){
		graph.toggleFixedMode(this.checked);
	});

	$("#repulsion").change(function() {
		var charge = -1 * $("#repulsion").val();
		graph.setNodeCharge(parseInt(charge));

		// Increase link length as repulsion increases
        var length = 0.5 * $("#repulsion").val();
        graph.setLinkLength(parseInt(length));
	});

    $("#friction").change(function() {
        var friction = $("#friction").val();
        graph.setFriction(parseFloat(friction));
    });

    $("#gravity").bind('change', function() {
        var gravity = this.checked;
        graph.toggleGravity(gravity);

    });

	// Advanced d3 parameters disabled for users

    $("#advancedVizParams").click(function() {
        $("#linklengthli").toggle(300);
        $("#chargedistli").toggle(300);
        $(this).text(function(i, text){
            return text === "More parameters" ? "Fewer parameters" : "More parameters";
        })
    });

	$("#linklength").change(function() {
		var length = $("#linklength").val();
		graph.setLinkLength(parseInt(length));

	});
	$("#chargedist").change(function() {
		var length = $("#chargedist").val();
		graph.setChargeDistance(parseInt(length));

	});

	
	this.updateNodeDisplay = function(node) {
		//Node info box
		$("#nodemenuName").text(node.name);
		$("#nodemenuType").text(node.nodeType.nodeType);
		$("#nodemenuMetaID").text(node.srcobj.metadataid);
		$("#nodemenuFreeText").text(node.srcobj.description);
		node.updateInfo();
		
		$("#nodemenuRefTermURI").text("");

		//Hide if the node info content is empty
        $(".nodemenuRow").filter(function() {
            return $.trim($(this).children(".nodeInfoContent").text()) === '';
        }).hide();
	}
}
