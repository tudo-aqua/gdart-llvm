package dse.tracer.recorder.event;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.smtlibUtility.smtconverter.SMTLibExportWrapper;
import gov.nasa.jpf.constraints.solvers.dontknow.DontKnowSolver;

public class AssumptionEvent extends Event {
    private Expression<?> assumption;
    private boolean satisfied;

    public AssumptionEvent(Expression<?> assumption, boolean satisfied) {
        this.assumption = assumption;
        this.satisfied = satisfied;
    }

    @Override
    public String toString() {
        // export assumption expression to SMTLIB
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(outputStream, false);
        SMTLibExportWrapper exporter = (new SMTLibExportWrapper(new DontKnowSolver(), printStream));
        SolverContext exporterContext = exporter.createContext();
        exporterContext.add((Expression<Boolean>)assumption);

        // extract assertion from SMTLIB output lines
        String[] outputLines = outputStream.toString().split("\\R");
        String assertion = outputLines[outputLines.length - 1];

        return "[ASSUMPTION] " + assertion + " // sat=" + (satisfied ? "true" : "false");
    }
}
