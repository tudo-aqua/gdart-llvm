package dse.tracer.recorder.event;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.smtlibUtility.smtconverter.SMTLibExportWrapper;
import gov.nasa.jpf.constraints.solvers.dontknow.DontKnowSolver;

public class DecisionEvent extends Event {
    private Expression<?> condition;
    private int branchId;

    public DecisionEvent(Expression<?> condition, int branchId) {
        this.condition = condition;
        this.branchId = branchId;
    }

    @Override
    public String toString() {
        // export condition expression to SMTLIB
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream, false);
        SMTLibExportWrapper exporter = (new SMTLibExportWrapper(new DontKnowSolver(), printStream));
        SolverContext exporterContext = exporter.createContext();
        exporterContext.add((Expression<Boolean>)condition);

        // extract assertion from SMTLIB output lines
        String[] outputLines = outputStream.toString().split("\\R");
        String assertion = outputLines[outputLines.length - 1];

        return "[DECISION] " + assertion + " // branchCount=2, branchId=" + branchId;
    }
}
