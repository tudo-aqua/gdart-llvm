package dse.tracer;

import java.io.PrintStream;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.instrumentation.Instrumenter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter;
import com.oracle.truffle.api.instrumentation.SourceSectionFilter.SourcePredicate;
import com.oracle.truffle.api.instrumentation.TruffleInstrument;
import com.oracle.truffle.api.instrumentation.TruffleInstrument.Registration;
import com.oracle.truffle.api.source.Source;

import dse.tracer.recorder.Recorder;

@Registration(id = SymbolicTracer.ID, name = "symbolic tracer", version = "1.0", services = SymbolicTracer.class)
public final class SymbolicTracer extends TruffleInstrument {
    @Option(name = "run", help = "Run symbolic tracer", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<Boolean> enabledOption = new OptionKey<>(false);

    // define concolic seed value tracer arguments
    @Option(name = "concolic.bools", help = "Concolic booolean seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> booleanSeedsOption = new OptionKey<>("");
    
    @Option(name = "concolic.bytes", help = "Concolic byte seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> byteSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.chars", help = "Concolic char seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> charSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.shorts", help = "Concolic short seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> shortSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.ints", help = "Concolic integer seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> integerSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.longs", help = "Concolic long seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> longSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.floats", help = "Concolic float seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> floatSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.doubles", help = "Concolic double seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> doubleSeedsOption = new OptionKey<>("");

    @Option(name = "concolic.strings", help = "Concolic string seed values", category = OptionCategory.USER, stability = OptionStability.STABLE)
    static final OptionKey<String> stringSeedsOption = new OptionKey<>("");

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return new SymbolicTracerOptionDescriptors();
    }

    public static final String ID = "symbolictracer";
    private TruffleLogger logger;

    private Seeds seeds;
    private Recorder recorder;

    @Override
    protected void onCreate(final Env env) {
        logger = env.getLogger(getClass().getName());
        logger.info("onCreate()");
        env.registerService(this);
        
        seeds = new Seeds();
        recorder = new Recorder(seeds, env);

        // parse seed values from arguments
        final OptionValues optionValues = env.getOptions();
        logger.info("options: " + optionValues);
        
        // TODO: avoid code duplication
        String booleanSeedsString = booleanSeedsOption.getValue(optionValues);
        if (!booleanSeedsString.isEmpty()) {
            for (String seedString : booleanSeedsString.split(",")) seeds.add(Boolean.valueOf(seedString));
        }

        String byteSeedsString = byteSeedsOption.getValue(optionValues);
        if (!byteSeedsString.isEmpty()) {
            for (String seedString : byteSeedsString.split(",")) seeds.add(Byte.valueOf(seedString));
        }

        String charSeedsString = charSeedsOption.getValue(optionValues);
        if (!charSeedsString.isEmpty()) {
            for (String seedString : charSeedsString.split(",")) seeds.add(seedString.charAt(0));
        }

        String shortSeedsString = shortSeedsOption.getValue(optionValues);
        if (!shortSeedsString.isEmpty()) {
            for (String seedString : shortSeedsString.split(",")) seeds.add(Short.valueOf(seedString));
        }

        String integerSeedsString = integerSeedsOption.getValue(optionValues);
        if (!integerSeedsString.isEmpty()) {
            for (String seedString : integerSeedsString.split(",")) seeds.add(Integer.valueOf(seedString));
        }

        String longSeedsString = longSeedsOption.getValue(optionValues);
        if (!longSeedsString.isEmpty()) {
            for (String seedString : longSeedsString.split(",")) seeds.add(Long.valueOf(seedString));
        }

        String floatSeedsString = floatSeedsOption.getValue(optionValues);
        if (!floatSeedsString.isEmpty()) {
            for (String seedString : floatSeedsString.split(",")) seeds.add(Float.valueOf(seedString));
        }

        String doubleSeedsString = doubleSeedsOption.getValue(optionValues);
        if (!doubleSeedsString.isEmpty()) {
            for (String seedString : doubleSeedsString.split(",")) seeds.add(Double.valueOf(seedString));
        }

        String stringSeedsString = stringSeedsOption.getValue(optionValues);
        if (!doubleSeedsString.isEmpty()) {
            // TODO: what about seed strings containing a comma? -> escaped by dse?
            for (String seedString : stringSeedsString.split(",")) seeds.add(seedString);
        }

        class FilterSourcePredicate implements SourcePredicate {
            @Override
            public boolean test(Source source) {
                // ignore external c source files
                return source.getName().endsWith(".ll");
            }
        }

        SourceSectionFilter.Builder sourceEventFilterBuilder = SourceSectionFilter.newBuilder();
        SourceSectionFilter sourceEventFilter = sourceEventFilterBuilder.includeInternal(true).sourceIs(new FilterSourcePredicate()).build();
        
        Instrumenter instrumenter = env.getInstrumenter();
        instrumenter.attachExecutionEventFactory(sourceEventFilter, new Listener(recorder));
    }

    @Override
    protected void onDispose(Env env) {
        logger.info("onDispose()");

        // print trace
        //new PrintStream(env.out()).println(recorder.getOutput());

        new PrintStream(env.out()).println("[ENDOFTRACE]");
    }
}
