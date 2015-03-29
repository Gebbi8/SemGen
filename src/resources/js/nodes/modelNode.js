/**
 * Represents a model node in the d3 graph
 */

ModelNode.prototype = new ParentNode();
ModelNode.prototype.constructor = ModelNode;
function ModelNode (graph, name) {
	ParentNode.prototype.constructor.call(this, graph, name, null, null, 16, "#BBB0AF", 20, "Model");
	this.fixed = true;
	
	this.addClassName("modelNode");
	
	this.addBehavior(Hull);
	this.addBehavior(ModelPopover);
}