package dse.tracer.recorder.event;

import com.oracle.truffle.api.TruffleLogger;

public abstract class Event {
    protected TruffleLogger logger;

    public Event() {
        this.logger = TruffleLogger.getLogger("symbolictracer", getClass().getName());
    }

    public abstract String toString();
}
