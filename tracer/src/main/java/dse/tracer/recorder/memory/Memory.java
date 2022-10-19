package dse.tracer.recorder.memory;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

import gov.nasa.jpf.constraints.api.Expression;


// TODO: MemoryValue needed? Use object directly?
public class Memory {
    private Stack<Map<Integer, MemoryValue>> frames;
    private Map<LLVMPointer, MemoryValue> stack;
    // TODO: heap

    public Memory() {
        frames = new Stack<>();
        stack = new HashMap<>();
    }

    public Object getFrameObject(int slot) {
        Map<Integer, MemoryValue> currentFrame = frames.peek();
        Object frameObject = currentFrame.get(slot).getObject();
        if (frameObject instanceof Expression) {
            //return copyExpression((Expression<?>)frameObject);
            return (Expression<?>)frameObject;
        } else {
            return frameObject;
        }
    }

    public void setFrameSlot(int slot, Object object) {
        Map<Integer, MemoryValue> currentFrame = frames.peek();
        currentFrame.put(slot, new MemoryValue(object));
    }

    /*
    public Expression<?> getFrameExpression(int slot, String scope) throws TypeError {
        MemoryValue value = frame.get(slot);
        if (!value.isPointer()) {
            return (Expression<?>)value.getObject();
        } else {
            throw new TypeError();
        }
    }

    public LLVMPointer getFramePointer(int slot) throws TypeError {
        MemoryValue value = frame.get(slot);
        if (value.isPointer()) {
            return (LLVMPointer)value.getObject();
        } else {
            throw new TypeError();
        }
    }
    */

    public Object getStackObject(LLVMPointer pointer) {
        return stack.get(pointer).getObject();
    }

    public void setStackObject(LLVMPointer pointer, Object value) {
        stack.put(pointer, new MemoryValue(value));
    }

    /*
    public Expression<?> getStackExpression(LLVMPointer pointer) throws TypeError {
        MemoryValue value = stack.get(pointer);
        if (!value.isPointer()) {
            return (Expression<?>)value.getObject();
        } else {
            throw new TypeError();
        }
    }

    public LLVMPointer getStackPointer(LLVMPointer pointer) throws TypeError {
        MemoryValue value = stack.get(pointer);
        if (value.isPointer()) {
            return (LLVMPointer)value.getObject();
        } else {
            throw new TypeError();
        }
    }
    */

    public void enterScope() {
        frames.push(new HashMap<>());
    }

    public void exitScope() {
        frames.pop();
    }

    private Expression<?> copyExpression(Expression<?> expression) {
        Expression<?>[] children = expression.getChildren();
        if (children.length == 0) {
            return expression.duplicate(children);
        } else {
            Expression<?>[] newChildren = new Expression<?>[children.length];
            for (int childIndex = 0; childIndex < children.length; childIndex++) {
                newChildren[childIndex] = copyExpression(children[childIndex]);
            }
            return expression.duplicate(newChildren);
        }
    }

    public class TypeError extends Exception {}
}
