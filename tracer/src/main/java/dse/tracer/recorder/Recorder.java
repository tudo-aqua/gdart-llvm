package dse.tracer.recorder;

import java.io.PrintStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.EventContext;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Env;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToDoubleNode;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToFloatNode;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI16Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI16Node.LLVMSignedCastToI16Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI1Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI32Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI32Node.LLVMSignedCastToI32Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI64Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI64Node.LLVMSignedCastToI64Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI8Node;
import com.oracle.truffle.llvm.runtime.nodes.cast.LLVMToI8Node.LLVMSignedCastToI8Node;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMBrUnconditionalNode;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMConditionalBranchNode;
import com.oracle.truffle.llvm.runtime.nodes.control.LLVMRetNode;
import com.oracle.truffle.llvm.runtime.nodes.func.LLVMCallNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMDoubleLiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMFloatLiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMI16LiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMI1LiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMI32LiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMI64LiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMI8LiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.literals.LLVMSimpleLiteralNode.LLVMNativePointerLiteralNode;
import com.oracle.truffle.llvm.runtime.nodes.memory.LLVMGetElementPtrNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMAddressEqualsNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode.PointerToI64Node;
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
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedLtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnorderedNeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnsignedLeNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMCompareNode.LLVMUnsignedLtNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode;
import com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode.LLVMNegateNode;
import com.oracle.truffle.llvm.runtime.nodes.vars.LLVMReadNode;
import com.oracle.truffle.llvm.runtime.nodes.vars.LLVMReadNode.LLVMI1ReadNode;
import com.oracle.truffle.llvm.runtime.nodes.vars.LLVMReadNode.LLVMObjectReadNode;
import com.oracle.truffle.llvm.runtime.nodes.vars.LLVMWriteNode;
import com.oracle.truffle.llvm.runtime.nodes.vars.LLVMWriteNode.LLVMWriteI1Node;
import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

import dse.tracer.Reflection;
import dse.tracer.Seeds;
import dse.tracer.TypeInfo;
import dse.tracer.recorder.event.AssumptionEvent;
import dse.tracer.recorder.event.DecisionEvent;
import dse.tracer.recorder.event.DeclarationEvent;
import dse.tracer.recorder.event.ErrorEvent;
import dse.tracer.recorder.event.Event;
import dse.tracer.recorder.memory.Memory;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.BitvectorBooleanExpression;
import gov.nasa.jpf.constraints.expressions.BitvectorComparator;
import gov.nasa.jpf.constraints.expressions.BitvectorExpression;
import gov.nasa.jpf.constraints.expressions.BitvectorOperator;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.ExplicitCastExpression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;

public class Recorder {
    private TruffleLogger logger = TruffleLogger.getLogger("symbolictracer", getClass().getName());
    private Seeds seeds;
    private List<Event> events = new ArrayList<>();
    private Memory memory = new Memory();
    private Map<TypeInfo, Integer> variableCounts = new HashMap<>();
    // private int variableCount = 0;

    private Stack<List<Object>> symbolicArguments = new Stack<>();
    private Stack<List<Object>> concreteArguments = new Stack<>();
    private Stack<Integer> returnSlots = new Stack<>();
    private Stack<String> returnFunctionNames = new Stack<>();
    private Stack<Object> returnValues = new Stack<>();

    private Env env;
    private PrintStream printStream;

    public Recorder(Seeds seeds, Env env) {
        this.seeds = seeds;
        this.env = env;
        this.printStream = new PrintStream(env.out());

        // initialize variable counts
        for (TypeInfo type : TypeInfo.values()) {
            variableCounts.put(type, 0);
        }
    }

    public void recordFunctionRootNode(Node node, VirtualFrame frame) {
        memory.enterScope();
        logger.info("scope: " + node.getRootNode().getName() + " (enter)");

        String functionName = node.getRootNode().getName();
        List<Integer> argumentTargetSlots = StreamSupport.stream(node.getChildren().spliterator(), false)
                .filter(child -> child instanceof LLVMWriteNode)
                .map(writeNodeChild -> findTargetSlot(writeNodeChild))
                .collect(Collectors.toList());

        if (functionName.equals("@main")) {
            // TODO: handle main function arguments symbolically? Not needed for wrapper.
            // Object arguments[] = frame.getArguments();
        } else {
            List<Object> currentConcreteArguments = concreteArguments.pop();
            List<Object> currentSymbolicArguments = symbolicArguments.pop();

            if (functionName.equals("@__VERIFIER_assert")) {
                Expression<Integer> expression = (Expression<Integer>) currentSymbolicArguments.get(0);

                boolean satisfied = (int) currentConcreteArguments.get(0) != 0;

                // argument might actually get accessed for e.g. casting
                memory.setFrameSlot(argumentTargetSlots.get(0), expression);

                if (!satisfied) {
                    String message = "assertion violated: " + expression.toString();
                    //events.add(new ErrorEvent(message));
                    addEvent(new ErrorEvent(message));
                }
            } else if (functionName.equals("@__VERIFIER_error") || functionName.equals("@reach_error")) {
                //events.add(new ErrorEvent("error encountered"));
                addEvent(new ErrorEvent("error encountered"));
            } else if (functionName.equals("@__VERIFIER_assume")) {
                Expression<Boolean> assumption = (Expression<Boolean>) currentSymbolicArguments.get(0);
                boolean satisfied = (boolean) currentConcreteArguments.get(0);

                // argument might actually get accessed for e.g. casting
                memory.setFrameSlot(argumentTargetSlots.get(0), assumption);

                events.add(new AssumptionEvent(assumption, satisfied));
                logger.info("assume: " + assumption);
            } else {
                Iterator<Object> currentArgumentsIterator = currentSymbolicArguments.iterator();
                for (int argumentTargetSlot : argumentTargetSlots) {
                    Object argument = currentArgumentsIterator.next();
                    memory.setFrameSlot(argumentTargetSlot, argument);
                    logger.info("argument: #" + argumentTargetSlot + " = " + argument);
                }
            }
        }
    }

    public void recordLoadNode(LLVMLoadNode node, VirtualFrame frame) {
        // get source pointer from child node
        LLVMReadNode sourceReadNode = (LLVMReadNode) node.getChildren().iterator().next();
        LLVMPointer sourcePointer = (LLVMPointer) sourceReadNode.executeGeneric(frame);
        Object sourceObject = memory.getStackObject(sourcePointer);

        int targetSlot = findTargetSlot(node);

        memory.setFrameSlot(targetSlot, sourceObject);

        String type = sourceObject instanceof Expression ? ((Expression<?>) sourceObject).getType().getName()
                : "pointer";
        logger.info("load: #" + targetSlot + " = " + "[" + sourcePointer + "] | " + sourceObject + " (" + type + ")");
    }

    public void recordStoreNode(LLVMStoreNode node, VirtualFrame frame) {
        Iterator<Node> children = node.getChildren().iterator();

        // get target pointer from child node
        LLVMReadNode pointerNode = (LLVMReadNode) children.next();
        LLVMPointer pointer = (LLVMPointer) pointerNode.executeGeneric(frame);

        // extract object to write from child
        Node valueNode = children.next();
        Object symbolicValue = null;

        // literal
        if (valueNode instanceof LLVMSimpleLiteralNode) {
            // Type<?> type = null;
            if (valueNode instanceof LLVMI64LiteralNode) {
                long value = (long) Reflection.readField(valueNode, LLVMI64LiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.SINT64, value);
            } else if (valueNode instanceof LLVMI32LiteralNode) {
                int value = (int) Reflection.readField(valueNode, LLVMI32LiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.SINT32, value);
            } else if (valueNode instanceof LLVMI16LiteralNode) {
                short value = (short) Reflection.readField(valueNode, LLVMI16LiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.SINT16, value);
            } else if (valueNode instanceof LLVMI8LiteralNode) {
                byte value = (byte) Reflection.readField(valueNode, LLVMI8LiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.SINT8, value);
            } else if (valueNode instanceof LLVMI1LiteralNode) {
                boolean value = (boolean) Reflection.readField(valueNode, LLVMI1LiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.BOOL, value);
                //symbolicValue = Constant.create(BuiltinTypes.UINT1, value);
            } else if (valueNode instanceof LLVMDoubleLiteralNode) {
                double value = (double) Reflection.readField(valueNode, LLVMDoubleLiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.DOUBLE, value);
            } else if (valueNode instanceof LLVMFloatLiteralNode) {
                float value = (float) Reflection.readField(valueNode, LLVMFloatLiteralNode.class, "literal");
                symbolicValue = Constant.create(BuiltinTypes.FLOAT, value);
            } else if (valueNode instanceof LLVMNativePointerLiteralNode) {
                symbolicValue = (LLVMPointer) Reflection.readField(valueNode, LLVMNativePointerLiteralNode.class,
                        "literal");
            } else {
                logger.warning("unhandled literal child of " + node.getClass().getName() + ": "
                        + valueNode.getClass().getName());
            }
            // frame slot
        } else if (valueNode instanceof LLVMReadNode) {
            int valueSlot = (int) Reflection.readField(valueNode, LLVMReadNode.class, "slot");

            if (valueNode instanceof LLVMObjectReadNode) {
                // pointer
                symbolicValue = ((LLVMObjectReadNode) valueNode).executeGeneric(frame);
            } else {
                symbolicValue = memory.getFrameObject(valueSlot);
            }
        } else {
            logger.warning(
                    "unhandled read child of " + node.getClass().getName() + ": " + valueNode.getClass().getName());
        }

        memory.setStackObject(pointer, symbolicValue);

        // logging
        String symbolicValueTypeName = "?";
        if (symbolicValue instanceof Expression) {
            symbolicValueTypeName = ((Expression<?>) symbolicValue).getType().getName();
        } else if (symbolicValue instanceof LLVMPointer) {
            symbolicValueTypeName = "pointer";
        }
        logger.info("store: [" + pointer + "] = " + symbolicValue + " (" + symbolicValueTypeName + ")");
    }

    public void recordGetElementPtrNode(LLVMGetElementPtrNode node, VirtualFrame frame) {
        int targetSlot = findTargetSlot(node);
        LLVMPointer elementPointer = (LLVMPointer) node.executeGeneric(frame);
        memory.setFrameSlot(targetSlot, elementPointer);
        logger.info("elementptr: #" + targetSlot + " = " + elementPointer + " (pointer)");
    }

    public void recordConditionalBranchNode(LLVMConditionalBranchNode node, VirtualFrame frame) {
        // extract condition frame slot from child node using reflection
        LLVMReadNode readNode = null;
        Integer conditionSlot = null;
        for (Node childNode : node.getChildren()) {
            if (childNode instanceof LLVMReadNode) {
                readNode = (LLVMReadNode) childNode;
                conditionSlot = (int) Reflection.readField(readNode, LLVMReadNode.class, "slot");
                break;
            }
        }
        if (readNode == null || conditionSlot == null) {
            logger.severe("unable to extract condition from branch node");
        }

        try {
            Expression<Boolean> condition = (Expression<Boolean>) memory.getFrameObject(conditionSlot);
            boolean evaluation = node.executeCondition(frame);

            // handle phi node
            Node phiNode = null;
            for (Node childNode : node.getChildren()) {
                if (childNode instanceof LLVMWriteNode) {
                    phiNode = childNode;
                    break;
                }
            }
            if (phiNode != null) {
                int targetSlot = findTargetSlot(phiNode);
                if (phiNode instanceof LLVMWriteI1Node) {
                    Node valueNode = phiNode.getChildren().iterator().next();
                    if (valueNode instanceof LLVMI1LiteralNode) {
                        LLVMI1LiteralNode literalValueNode = (LLVMI1LiteralNode) valueNode;
                        boolean value = (boolean) Reflection.readField(literalValueNode, LLVMI1LiteralNode.class, "literal");
                        Expression<?> valueExpression = Constant.create(BuiltinTypes.BOOL, value);
                        memory.setFrameSlot(targetSlot, valueExpression);
                        logger.info("branch (phi): #" + targetSlot + " = " + valueExpression);
                    } else if (valueNode instanceof LLVMI1ReadNode) {
                        LLVMI1ReadNode readValueNode = (LLVMI1ReadNode) valueNode;
                        int valueSlot = (int) Reflection.readField(readValueNode, LLVMReadNode.class, "slot");
                        Expression<Boolean> valueExpression = (Expression<Boolean>) memory.getFrameObject(valueSlot);
                        memory.setFrameSlot(targetSlot, valueExpression);
                        logger.info("branch (phi): #" + targetSlot + " = " + valueExpression);
                    } else {
                        logger.severe("unhandled phi node (branch: #" + conditionSlot + ")");
                    }
                } else {
                    logger.severe("unhandled phi node (branch: #" + conditionSlot + ")");
                }
            }

            logger.info("branch: #" + conditionSlot + " | " + condition + " (" + evaluation + ")");

            // negate recorded condition if it evaluates to false
            if (!evaluation) {
                condition = Negation.create(condition);
            }
            // branch ID 0 for positive branch, 1 for negative branch
            //events.add(new DecisionEvent(condition, evaluation ? 0 : 1));
            addEvent(new DecisionEvent(condition, evaluation ? 0 : 1));

        } catch (Exception exception) {
            logger.severe(exception.toString() + " (branch: #" + conditionSlot + ")");
        }
    }

    public void recordBrUnconditionalNode(LLVMBrUnconditionalNode node, VirtualFrame frame) {
        // handle phi node
        Node phiNode = null;
        for (Node childNode : node.getChildren()) {
            if (childNode instanceof LLVMWriteNode) {
                phiNode = childNode;
                break;
            }
        }
        if (phiNode != null) {
            int targetSlot = findTargetSlot(phiNode);
            if (phiNode instanceof LLVMWriteI1Node) {
                Node valueNode = phiNode.getChildren().iterator().next();
                if (valueNode instanceof LLVMI1LiteralNode) {
                    LLVMI1LiteralNode literalValueNode = (LLVMI1LiteralNode) valueNode;
                    boolean value = (boolean) Reflection.readField(literalValueNode, LLVMI1LiteralNode.class, "literal");
                    Expression<?> valueExpression = Constant.create(BuiltinTypes.BOOL, value);
                    memory.setFrameSlot(targetSlot, valueExpression);
                    logger.info("unconditional branch (phi): #" + targetSlot + " = " + valueExpression);
                } else if (valueNode instanceof LLVMI1ReadNode) {
                    LLVMI1ReadNode readValueNode = (LLVMI1ReadNode) valueNode;
                    int valueSlot = (int) Reflection.readField(readValueNode, LLVMReadNode.class, "slot");
                    Expression<Boolean> valueExpression = (Expression<Boolean>) memory.getFrameObject(valueSlot);
                    memory.setFrameSlot(targetSlot, valueExpression);
                    logger.info("unconditional branch (phi): #" + targetSlot + " = " + valueExpression);
                } else {
                    logger.severe("unhandled phi node (unconditional branch)");
                }
            } else {
                logger.severe("unhandled phi node (unconditional branch)");
            }
        }
    }

    public void recordArithmeticNode(LLVMArithmeticNode node) {
        // extract arithmetic operation from node
        Object operation = Reflection.readField(node, LLVMArithmeticNode.class, "op");
        String operationClassName = operation.getClass().getName();
        BitvectorOperator operator = null;

        switch (operationClassName) {
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$1":
                // LLVMArithmeticOp.ADD
                //operator = NumericOperator.PLUS;
                operator = BitvectorOperator.ADD;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$2":
                // LLVMArithmeticOp.MUL
                operator = BitvectorOperator.MUL;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$3":
                // LLVMArithmeticOp.SUB
                operator = BitvectorOperator.SUB;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$4":
                // LLVMArithmeticOp.DIV
                operator = BitvectorOperator.SDIV;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$5":
                // LLVMArithmeticOp.UDIV
                operator = BitvectorOperator.UDIV;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$6":
                // LLVMArithmeticOp.REM
                operator = BitvectorOperator.SREM;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$7":
                // LLVMArithmeticOp.UREM
                operator = BitvectorOperator.UREM;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$8":
                // LLVMArithmeticOp.AND
                operator = BitvectorOperator.AND;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$9":
                // LLVMArithmeticOp.OR
                operator = BitvectorOperator.OR;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$10":
                // LLVMArithmeticOp.XOR
                operator = BitvectorOperator.XOR;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$11":
                // LLVMArithmeticOp.SHL
                operator = BitvectorOperator.SHIFTL;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$12":
                // LLVMArithmeticOp.LSHR
                operator = BitvectorOperator.SHIFTUR;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMArithmeticNode$13":
                // LLVMArithmeticOp.ASHR
                operator = BitvectorOperator.SHIFTR;
                break;
            default:
                logger.warning("unhandled arithmetic operation: " + operationClassName);
        }

        int targetSlot = findTargetSlot(node);
        List<Object> inputs = findSymbolicInputs(node);
        Expression<?> left = (Expression<?>) inputs.get(0);
        Expression<?> right = (Expression<?>) inputs.get(1);
        Expression<?> result = BitvectorExpression.create(left, operator, right);

        memory.setFrameSlot(targetSlot, result);
        logger.info("arithmetic: #" + targetSlot + " = " + result);
    }

    public void recordCallNode(LLVMCallNode node, VirtualFrame frame) {
        // remember symbolic function call arguments
        symbolicArguments.push(findSymbolicInputs(node));

        // remember symbolic function call arguments (e.g. for assumptions)
        concreteArguments.push(findConcreteInputs(node, frame));
    }

    public void recordCallNodeReturn(LLVMCallNode node, VirtualFrame frame, EventContext context) {
        Integer returnSlot = findTargetSlot(node);
        Object returnValue = null;
        if (returnSlot != null) {
            returnValue = returnValues.pop();
        }
        String fromFunctionName = returnFunctionNames.pop();
        String toFunctionName = node.getRootNode().getName();
        logger.info("scope: " + toFunctionName + " (return)");

        if (fromFunctionName.startsWith("@__VERIFIER_nondet_")) {
            // inject concolic return value
            TypeInfo typeInfo = null;
            switch (fromFunctionName) {
                // TODO: add uchar, uint, ulong etc.
                case "@__VERIFIER_nondet_bool":
                    typeInfo = TypeInfo.BOOLEAN;
                    returnValue = seeds.nextBoolean();
                    break;
                case "@__VERIFIER_nondet_char":
                    //typeInfo = TypeInfo.CHARACTER;
                    typeInfo = TypeInfo.BYTE;
                    //returnValue = seeds.nextCharacter();
                    returnValue = seeds.nextByte();
                    break;
                case "@__VERIFIER_nondet_short":
                    typeInfo = TypeInfo.SHORT;
                    returnValue = seeds.nextShort();
                    break;
                case "@__VERIFIER_nondet_int":
                    typeInfo = TypeInfo.INTEGER;
                    returnValue = seeds.nextInteger();
                    break;
                case "@__VERIFIER_nondet_uint":
                    typeInfo = TypeInfo.INTEGER;
                    returnValue = seeds.nextInteger();
                    break;
                case "@__VERIFIER_nondet_long":
                    typeInfo = TypeInfo.LONG;
                    returnValue = seeds.nextLong();
                    break;
                case "@__VERIFIER_nondet_ulong":
                    typeInfo = TypeInfo.LONG;
                    returnValue = seeds.nextLong();
                    break;
                case "@__VERIFIER_nondet_float":
                    typeInfo = TypeInfo.FLOAT;
                    returnValue = seeds.nextFloat();
                    break;
                case "@__VERIFIER_nondet_double":
                    typeInfo = TypeInfo.DOUBLE;
                    returnValue = seeds.nextDouble();
                    break;
            }

            // add symbolic input variable to symbolic memory
            int variableCount = variableCounts.get(typeInfo);
            String variableName = typeInfo.getDsePrefix() + variableCount;
            variableCounts.put(typeInfo, variableCount + 1);

            Expression<?> variable = Variable.create(typeInfo.getJconstraintsType(), variableName);
            memory.setFrameSlot(returnSlot, variable);
            //events.add(new DeclarationEvent(variable, typeInfo));
            addEvent(new DeclarationEvent(variable, typeInfo));
            logger.info("symbolic input: #" + returnSlot + " = " + variable);
            logger.info("concrete input: #" + returnSlot + " = " + returnValue);

            // unwind to change return value
            throw context.createUnwind(returnValue);
        } else {
            // normal function return
            if (returnValue != null) {
                memory.setFrameSlot(returnSlot, returnValue);
                logger.info("return (" + fromFunctionName + "): #" + returnSlot + " = " + returnValue);
            }
        }
    }

    public void recordRetNodeReturn(LLVMRetNode node, VirtualFrame frame) {
        // remember name of function that is returning
        String functionName = node.getRootNode().getName();
        returnFunctionNames.push(functionName);

        // get return value while still in old scope, then exit scope
        Object returnValue = null;
        List<Object> inputs = findSymbolicInputs(node);
        // only set return value for non-void functions; return null for void functions
        if (!inputs.isEmpty()) {
            returnValue = inputs.get(0);
        }
        memory.exitScope();
        returnValues.push(returnValue);

        if (functionName.equals("@main")) {
            logger.info("return (main): " + returnValue);
        }
    }

    public void recordBitvectorCompareNode(Node node) {
        int targetSlot = findTargetSlot(node);

        // choose comparison operator based on node class
        BitvectorComparator operator = null;
        if (node instanceof LLVMEqNode) {
            operator = BitvectorComparator.EQ;
        } else if (node instanceof LLVMNeNode) {
            operator = BitvectorComparator.NE;
        } else if (node instanceof LLVMSignedLtNode) {
            operator = BitvectorComparator.SLT;
        } else if (node instanceof LLVMUnsignedLtNode) {
            operator = BitvectorComparator.ULT;
        } else if (node instanceof LLVMSignedLeNode) {
            operator = BitvectorComparator.SLE;
        } else if (node instanceof LLVMUnsignedLeNode) {
            operator = BitvectorComparator.ULE;
        } else {
            logger.warning("unhandled bitvector comparison: " + node.getClass().getName());
        }

        List<Object> inputs = findSymbolicInputs(node);
        Expression<?> left = (Expression<?>) inputs.get(0);
        Expression<?> right = (Expression<?>) inputs.get(1);
        Expression<Boolean> result = BitvectorBooleanExpression.create(left, operator, right);

        // negate resulting expression if parent node is a NegateNode that was generated
        // as part of the operation itself (e.g. "not ult" to create "uge")
        if (node.getParent() instanceof LLVMNegateNode) {
            result = Negation.create(result);
        }

        memory.setFrameSlot(targetSlot, result);
        logger.info("compare: #" + targetSlot + " = " + result);
    }

    public void recordFloatingPointCompareNode(Node node) {
        int targetSlot = findTargetSlot(node);

        // choose comparison operator based on node class
        NumericComparator operator = null;
        if (node instanceof LLVMUnorderedLtNode || node instanceof LLVMOrderedLtNode) {
            operator = NumericComparator.LT;
        } else if (node instanceof LLVMOrderedLeNode) { // TODO: LLVMUnorderedLeNode doesn't exist?
            operator = NumericComparator.LE;
        } else if (node instanceof LLVMUnorderedGtNode || node instanceof LLVMOrderedGtNode) {
            operator = NumericComparator.GT;
        } else if (node instanceof LLVMUnorderedGeNode || node instanceof LLVMOrderedGeNode) {
            operator = NumericComparator.GE;
        } else if (node instanceof LLVMUnorderedEqNode || node instanceof LLVMOrderedEqNode) {
            operator = NumericComparator.EQ;
        } else if (node instanceof LLVMUnorderedNeNode || node instanceof LLVMOrderedNeNode) {
            operator = NumericComparator.NE;
        } else {
            logger.warning("unhandled floating point comparison: " + node.getClass().getName());
        }

        List<Object> inputs = findSymbolicInputs(node);
        Expression<?> left = (Expression<?>) inputs.get(0);
        Expression<?> right = (Expression<?>) inputs.get(1);
        Expression<Boolean> result = new NumericBooleanExpression(left, operator, right);

        // negate resulting expression if parent node is a NegateNode that was generated
        // as part of the operation itself (e.g. "not ult" to create "uge")
        if (node.getParent() instanceof LLVMNegateNode) {
            result = Negation.create(result);
        }

        memory.setFrameSlot(targetSlot, result);
        logger.info("compare (float): #" + targetSlot + " = " + result);
    }

    public void recordPointerCompareNode(Node node) {
        // extract pointer comparison operation from node
        Object operation = Reflection.readField(node, LLVMPointerCompareNode.class, "op");
        String operationClassName = operation.getClass().getName();

        // choose comparison operator based on operation class
        BitvectorComparator operator = null;
        switch (operationClassName) {
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode$1":
                operator = BitvectorComparator.SLT;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode$2":
                operator = BitvectorComparator.SLE;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode$3":
                operator = BitvectorComparator.ULE;
                break;
            case "com.oracle.truffle.llvm.runtime.nodes.op.LLVMPointerCompareNode$4":
                operator = BitvectorComparator.ULT;
                break;
            default:
                logger.warning("unhandled pointer bitvevtor comparison: " + operationClassName);
        }

        List<Object> inputs = findSymbolicInputs(node);
        Expression<?> left = (Expression<?>) inputs.get(0);
        Expression<?> right = (Expression<?>) inputs.get(1);
        Expression<Boolean> result = BitvectorBooleanExpression.create(left, operator, right);

        // negate resulting expression if parent node is a NegateNode that was generated
        // as part of the operation itself (e.g. "not ult" to create "uge")
        if (node.getParent() instanceof LLVMNegateNode) {
            result = Negation.create(result);
        }

        int targetSlot = findTargetSlot(node);
        memory.setFrameSlot(targetSlot, result);
        logger.info("compare (long/pointer): #" + targetSlot + " = " + result);
    }

    public void recordAddressEqualsNode(Node node) {
        List<Object> inputs = findSymbolicInputs(node);

        // TODO: handle native pointer comparison case
        // Object left = inputs.get(0);
        // Object right = inputs.get(1);
        // if (left instanceof LLVMNativePointer) {
        // long pointerValue = ((LLVMNativePointer)left).asNative();
        // }

        Expression<?> left = (Expression<?>) inputs.get(0);
        Expression<?> right = (Expression<?>) inputs.get(1);
        BitvectorComparator operator = BitvectorComparator.EQ;
        Expression<Boolean> result = BitvectorBooleanExpression.create(left, operator, right);

        // negate resulting expression if parent node is a NegateNode that was generated
        // as part of the operation itself (i.e. "not eq" to create "neq")
        if (node.getParent() instanceof LLVMNegateNode) {
            result = Negation.create(result);
        }

        int targetSlot = findTargetSlot(node);
        memory.setFrameSlot(targetSlot, result);
        logger.info("compare (long/address): #" + targetSlot + " = " + result);
    }

    public void recordNegateNode(LLVMNegateNode node) {
        // just pass the negated child node to the appropriate comparison node handler,
        // which already checks for negation
        Node negatedChild = node.getChildren().iterator().next();
        if (negatedChild instanceof LLVMSignedLeNode || negatedChild instanceof LLVMUnsignedLeNode
                || negatedChild instanceof LLVMSignedLtNode || negatedChild instanceof LLVMUnsignedLtNode) {
            recordBitvectorCompareNode(negatedChild);
        } else if (negatedChild instanceof LLVMPointerCompareNode) {
            recordPointerCompareNode(negatedChild);
        } else if (negatedChild instanceof LLVMAddressEqualsNode) {
            recordAddressEqualsNode(negatedChild);
        } else {
            logger.warning("unhandled negation: " + negatedChild.getClass().getName());
        }
    }

    public void recordCastNode(Node node) {
        int targetSlot = findTargetSlot(node);
        Expression<?> fromExpression = (Expression<?>) findSymbolicInputs(node).iterator().next();
        Type<?> toType = null;
        boolean fromSigned = false;

        if (node instanceof LLVMToI1Node) {
            toType = BuiltinTypes.BOOL;
            //toType = BuiltinTypes.UINT1;
        } else if (node instanceof LLVMToI8Node) {
            toType = BuiltinTypes.SINT8;
            fromSigned = node instanceof LLVMSignedCastToI8Node;
        } else if (node instanceof LLVMToI16Node) {
            toType = BuiltinTypes.SINT16;
            fromSigned = node instanceof LLVMSignedCastToI16Node;
        } else if (node instanceof LLVMToI32Node) {
            toType = BuiltinTypes.SINT32;
            fromSigned = node instanceof LLVMSignedCastToI32Node;
        } else if (node instanceof LLVMToI64Node) {
            toType = BuiltinTypes.SINT64;
            fromSigned = node instanceof LLVMSignedCastToI64Node;
        } else if (node instanceof LLVMToDoubleNode) {
            toType = BuiltinTypes.DOUBLE;
        } else if (node instanceof LLVMToFloatNode) {
            toType = BuiltinTypes.FLOAT;
        } else {
            logger.warning("unhandled cast node: " + node.getClass().getName());
        }

        Expression<?> castExpression = ExplicitCastExpression.create(fromExpression, toType, fromSigned);
        memory.setFrameSlot(targetSlot, castExpression);
        logger.info("cast: #" + targetSlot + " = (" + toType.getName() + ")" + fromExpression);
    }

    private Integer findTargetSlot(Node node) {
        // search parent nodes for LLVMWriteNode
        Node currentNode = node;
        while (!(currentNode instanceof LLVMWriteNode)) {
            Node parent = currentNode.getParent();
            if (parent != null) {
                currentNode = currentNode.getParent();
            } else {
                // no LLVMWriteNode found (e.g. return from void method)
                return null;
            }

        }

        // extract target frame slot using reflection
        int targetSlot = (int) Reflection.readField(currentNode, LLVMWriteNode.class, "slot");
        return targetSlot;
    }

    private List<Object> findSymbolicInputs(Node node) {
        List<Object> inputs = new ArrayList<>();
        for (Node child : node.getChildren()) {
            // literal
            if (child instanceof LLVMSimpleLiteralNode) {
                if (child instanceof LLVMI64LiteralNode) {
                    long value = (long) Reflection.readField(child, LLVMI64LiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.SINT64, value));
                } else if (child instanceof LLVMI32LiteralNode) {
                    int value = (int) Reflection.readField(child, LLVMI32LiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.SINT32, value));
                } else if (child instanceof LLVMI16LiteralNode) {
                    short value = (short) Reflection.readField(child, LLVMI16LiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.SINT16, value));
                } else if (child instanceof LLVMI8LiteralNode) {
                    byte value = (byte) Reflection.readField(child, LLVMI8LiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.SINT8, value));
                } else if (child instanceof LLVMI1LiteralNode) {
                    boolean value = (boolean) Reflection.readField(child, LLVMI1LiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.BOOL, value));
                    //inputs.add(Constant.create(BuiltinTypes.UINT1, value));
                } else if (child instanceof LLVMDoubleLiteralNode) {
                    double value = (double) Reflection.readField(child, LLVMDoubleLiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.DOUBLE, value));
                } else if (child instanceof LLVMFloatLiteralNode) {
                    float value = (float) Reflection.readField(child, LLVMFloatLiteralNode.class, "literal");
                    inputs.add(Constant.create(BuiltinTypes.FLOAT, value));
                } else {
                    logger.warning("unhandled literal input of " + node.getClass().getName() + ": "
                            + child.getClass().getName());
                }
            } else if (child instanceof LLVMReadNode) {
                // frame slot
                LLVMReadNode valueReadNode = (LLVMReadNode) child;
                int valueSlot = (int) Reflection.readField(valueReadNode, LLVMReadNode.class, "slot");
                inputs.add(memory.getFrameObject(valueSlot));
            } else if (child instanceof PointerToI64Node) {
                // TODO: handle if it's actually a pointer
                // get long value from child read nodes
                inputs.addAll(findSymbolicInputs(child));
            } else {
                // logger.warning("unhandled input node: " + node.getClass().getName());
            }
        }
        return inputs;
    }

    private List<Object> findConcreteInputs(Node node, VirtualFrame frame) {
        List<Object> concreteInputs = new ArrayList<>();
        for (Node child : node.getChildren()) {
            if (child instanceof LLVMExpressionNode) {
                Object concreteInput = ((LLVMExpressionNode) child).executeGeneric(frame);

                // ignore implicit stack reference argument
                if (!(concreteInput instanceof LLVMStack)) {
                    concreteInputs.add(concreteInput);
                }
            }
        }
        return concreteInputs;
    }

    public String getOutput() {
        StringBuilder output = new StringBuilder();
        for (Event event : events) {
            output.append(event.toString() + "\n");
        }
        output.append("[ENDOFTRACE]\n");
        return output.toString();
    }

    private void addEvent(Event event) {
        events.add(event);
        printStream.println(event.toString());
    }
}

// DEBUG
// String nodeName = "Negate";
// logger.info("--- " + nodeName + ": " + node);
// logger.info("--- " + nodeName + " parent: " +
// node.getParent().getClass().getName());
// logger.info("--- " + nodeName + " root: " + node.getRootNode().getName());
// for (Node child : node.getChildren()) {
// logger.info("--- " + nodeName + " child: " + child.getClass().getName());
// for (Node grandchild : child.getChildren()) {
// logger.info("--- " + nodeName + " grandchild: " +
// grandchild.getClass().getName());
// }
// }
