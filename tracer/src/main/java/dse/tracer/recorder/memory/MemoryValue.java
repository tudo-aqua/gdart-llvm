package dse.tracer.recorder.memory;

import com.oracle.truffle.llvm.runtime.pointer.LLVMPointer;

public class MemoryValue {
    private Object value;

    public MemoryValue(Object value) {
        setValue(value);
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public Object getObject() {
        return value;
    }

    public boolean isPointer() {
        return value instanceof LLVMPointer;
    }
}
