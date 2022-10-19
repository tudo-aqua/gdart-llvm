package dse.tracer.recorder.event;

public class ErrorEvent extends Event {
    private String error;

    public ErrorEvent(String error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "[ERROR] " + error;
    }
}
