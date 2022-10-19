package dse.tracer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.ExecutionEventNode;
import com.oracle.truffle.api.instrumentation.ExecutionEventNodeFactory;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.LLVMAllocaConstInstruction;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToDoubleNode;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToFloatNode;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI16Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI1Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI32Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI64Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI8Node;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMBrUnconditionalNode;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMConditionalBranchNode;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMDispatchBasicBlockNode;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMFunctionRootNode;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMRetNode;
import com.oracle.truffle.llvm.runtime.nodes.func.LLVMCallNode;
import com.oracle.truffle.llvm.runtime.nodes.memory.LLVMGetElementPtrNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMAddressEqualsNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMEqNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMNeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMOrderedEqNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMOrderedGeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMOrderedGtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMOrderedLeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMOrderedLtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMOrderedNeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMSignedLeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMSignedLtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedEqNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedGeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedGtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedLeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedLtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedNeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnsignedLeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnsignedLtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode.LLVMNegateNode;
import com.oracle.truffle.llvm.runtime.nodes.others.LLVMUnreachableNode;

import dse.tracer.recorder.Recorder;

public class Listener implements ExecutionEventNodeFactory {
    private TruffleLogger logger = TruffleLogger.getLogger("symbolictracer", getClass().getName());
    private Recorder recorder;

    public Listener(Recorder recorder) {
        this.recorder = recorder;
    }

    public ExecutionEventNode create(final EventContext eventContext) {
        return new ExecutionEventNode() {

            @Override
            protected void onEnter(VirtualFrame frame) {
                Node node = eventContext.getInstrumentedNode();

                // DEBUG
                //logger.info("onEnter(): " + node.getClass().getName());

                // register events in recorder
                if (node instanceof LLVMDispatchBasicBlockNode) {
                } else if (node instanceof LLVMFunctionRootNode) {
                    recorder.recordFunctionRootNode(node, frame);
                } else if (node instanceof LLVMBrUnconditionalNode) {
                    // needed to handle phi instructions
                    recorder.recordBrUnconditionalNode((LLVMBrUnconditionalNode) node, frame);
                } else if (node instanceof LLVMConditionalBranchNode) {
                    // TODO: pass only evalutaion instead of stack frame
                    recorder.recordConditionalBranchNode((LLVMConditionalBranchNode) node, frame);
                } else if (node instanceof LLVMAllocaConstInstruction) {
                    // needed for e.g. arrays?
                } else if (node instanceof LLVMLoadNode) { // load value from address
                    recorder.recordLoadNode((LLVMLoadNode) node, frame);
                } else if (node instanceof LLVMStoreNode) { // store value in address
                    recorder.recordStoreNode((LLVMStoreNode) node, frame);
                } else if (node instanceof LLVMGetElementPtrNode) {
                    recorder.recordGetElementPtrNode((LLVMGetElementPtrNode) node, frame);
                } else if (node instanceof LLVMArithmeticNode) { // arithmetic operations (e.g. +, - , *)
                    recorder.recordArithmeticNode((LLVMArithmeticNode) node);
                } else if (node instanceof LLVMPointerCompareNode) { // long/pointer (native type) comparisons
                    recorder.recordPointerCompareNode((LLVMPointerCompareNode) node);
                } else if (node instanceof LLVMAddressEqualsNode) {
                    recorder.recordAddressEqualsNode(node);
                } else if (node instanceof LLVMNegateNode) {
                    recorder.recordNegateNode((LLVMNegateNode) node);
                } else if (node instanceof LLVMEqNode || node instanceof LLVMNeNode || node instanceof LLVMSignedLtNode
                        || node instanceof LLVMUnsignedLtNode || node instanceof LLVMSignedLeNode
                        || node instanceof LLVMUnsignedLeNode) {
                    recorder.recordBitvectorCompareNode(node);
                } else if (node instanceof LLVMUnorderedLtNode || node instanceof LLVMOrderedLtNode
                        || node instanceof LLVMUnorderedLeNode || node instanceof LLVMOrderedLeNode
                        || node instanceof LLVMUnorderedGtNode || node instanceof LLVMOrderedGtNode
                        || node instanceof LLVMUnorderedGeNode || node instanceof LLVMOrderedGeNode
                        || node instanceof LLVMOrderedLtNode || node instanceof LLVMUnorderedEqNode
                        || node instanceof LLVMOrderedEqNode || node instanceof LLVMUnorderedNeNode
                        || node instanceof LLVMOrderedNeNode) {
                    recorder.recordFloatingPointCompareNode(node);
                } else if (node instanceof LLVMCallNode) {
                    recorder.recordCallNode((LLVMCallNode) node, frame);
                } else if (node instanceof LLVMRetNode) {
                    // recorder.recordRetNode((LLVMRetNode)node);
                } else if (node instanceof LLVMToI64Node || node instanceof LLVMToI32Node
                        || node instanceof LLVMToI16Node || node instanceof LLVMToI8Node || node instanceof LLVMToI1Node
                        || node instanceof LLVMToDoubleNode || node instanceof LLVMToFloatNode) {
                    recorder.recordCastNode(node);
                } else if (node instanceof LLVMUnreachableNode) {
                    // exit to avoid exception due to reaching unreachable code
                    System.exit(0);
                } else {
                    logger.warning("unhandled node: " + node.getClass().getName());
                }
            }

            @Override
            public void onReturnValue(VirtualFrame frame, Object result) {
                Node node = eventContext.getInstrumentedNode();

                if (node instanceof LLVMCallNode) {
                    recorder.recordCallNodeReturn((LLVMCallNode) node, frame, eventContext);
                } else if (node instanceof LLVMRetNode) {
                    recorder.recordRetNodeReturn((LLVMRetNode) node, frame);
                } else if (node instanceof LLVMAllocaConstInstruction) {
                    /*
                     * // DEBUG
                     * logger.info("alloca return:");
                     * int slotCount = virtualFrame.getFrameDescriptor().getNumberOfSlots();
                     * for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                     * try {
                     * Object value = virtualFrame.getValue(slotIndex);
                     * logger.info("\tslot #" + slotIndex + ": " + value.getClass().getName());
                     * } catch (Exception exception) {
                     * logger.info("\tslot #" + slotIndex + ": ?");
                     * }
                     * }
                     */
                }
            }

            @Override
            public Object onUnwind(VirtualFrame frame, Object returnValue) {
                return returnValue;
            }
        };
    }
}