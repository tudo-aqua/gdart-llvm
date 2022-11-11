package dse.tracer.recorder.event;

public class ResultWitnessEvent extends Event {

    private final String caller;
    private final int line;
    private final String callee;
    private final String result;

    public ResultWitnessEvent(String caller, int line, String callee, String result) {
        this.caller = caller;
        this.line = line;
        this.callee = callee;
        this.result = result;
    }

    @Override
    public String toString() {
        return "[RESULTWITNESS] " + caller + " : " + line + " : " + callee + " : " + result;
    }

}
