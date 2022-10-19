package dse.tracer;

import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;

public enum TypeInfo {
    BOOLEAN("Bool", BuiltinTypes.BOOL, "__bool_"),
    //BOOLEAN("Bool", BuiltinTypes.UINT1, "__bool_"),
    BYTE("(_ BitVec 8)", BuiltinTypes.SINT8, "__byte_"),
    CHARACTER("(_ BitVec 8)", BuiltinTypes.SINT8, "__char_"),
    SHORT("(_ BitVec 16)", BuiltinTypes.SINT16, "__short_"),
    INTEGER("(_ BitVec 32)", BuiltinTypes.SINT32, "__int_"),
    LONG("(_ BitVec 64)", BuiltinTypes.SINT64, "__long_"),
    FLOAT("(_ FloatingPoint 8 24)", BuiltinTypes.FLOAT, "__float_"),
    DOUBLE("(_ FloatingPoint 11 53)", BuiltinTypes.DOUBLE, "__double_"),
    STRING("String", BuiltinTypes.STRING, "__string_");

    private String smtlibType;
    private Type<?> jconstraintsType;
    private String dsePrefix;

    private TypeInfo(String smtlibType, Type<?> jconstraintsType, String dsePrefix) {
        this.smtlibType = smtlibType;
        this.jconstraintsType = jconstraintsType;
        this.dsePrefix = dsePrefix;
    }

    public String getSmtlibType() {
        return smtlibType;
    }

    public Type<?> getJconstraintsType() {
        return jconstraintsType;
    }

    public String getDsePrefix() {
        return dsePrefix;
    }
}