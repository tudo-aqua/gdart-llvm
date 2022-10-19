package dse.tracer.recorder.event;

import dse.tracer.TypeInfo;
import gov.nasa.jpf.constraints.api.Expression;

public class DeclarationEvent extends Event {
    private Expression<?> symbol;
    private TypeInfo type;

    public DeclarationEvent(Expression<?> symbol, TypeInfo type) {
        this.symbol = symbol;
        this.type = type;
    }

    @Override
    public String toString() {
        return "[DECLARE] (declare-fun " + symbol.toString().replace("'", "") + " () " + type.getSmtlibType() + ")";
    }
}
