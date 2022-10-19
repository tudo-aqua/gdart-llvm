package dse.tracer;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import com.oracle.truffle.api.TruffleLogger;

// TODO: avoid code duplication
public class Seeds {
    private static TruffleLogger logger = TruffleLogger.getLogger("symbolictracer", Seeds.class.getName());

    private static final Boolean DEFAULT_BOOLEAN = false;
    private static final Byte DEFAULT_BYTE = 0;
    private static final Character DEFAULT_CHARACTER = ' ';
    private static final Short DEFAULT_SHORT = 0;
    private static final Integer DEFAULT_INTEGER = 0;
    private static final Long DEFAULT_LONG = 0L;
    private static final Float DEFAULT_FLOAT = 0f;
    private static final Double DEFAULT_DOUBLE = 0d;
    private static final String DEFAULT_STRING = "";

    private Queue<Boolean> booleans = new LinkedList<>();
    private Queue<Byte> bytes = new LinkedList<>();
    private Queue<Character> characters = new LinkedList<>();
    private Queue<Short> shorts = new LinkedList<>();
    private Queue<Integer> integers = new LinkedList<>();
    private Queue<Long> longs = new LinkedList<>();
    private Queue<Float> floats = new LinkedList<>();
    private Queue<Double> doubles = new LinkedList<>();
    private Queue<String> strings = new LinkedList<>();

    public void add(Boolean seed) {
        booleans.add(seed);
    }

    public void add(Byte seed) {
        bytes.add(seed);
    }

    public void add(Character seed) {
        characters.add(seed);
    }

    public void add(Short seed) {
        shorts.add(seed);
    }

    public void add(Integer seed) {
        integers.add(seed);
    }

    public void add(Long seed) {
        longs.add(seed);
    }

    public void add(Float seed) {
        floats.add(seed);
    }

    public void add(Double seed) {
        doubles.add(seed);
    }

    public void add(String seed) {
        strings.add(seed);
    }

    public Boolean nextBoolean() {
        Boolean concreteValue = DEFAULT_BOOLEAN;
        try {
            concreteValue = booleans.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded boolean values; using default value");
        }
        return concreteValue;
    }

    public Byte nextByte() {
        Byte concreteValue = DEFAULT_BYTE;
        try {
            concreteValue = bytes.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded byte values; using default value");
        }
        return concreteValue;
    }

    public Character nextCharacter() {
        Character concreteValue = DEFAULT_CHARACTER;
        try {
            concreteValue = characters.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded character values; using default value");
        }
        return concreteValue;
    }

    public Short nextShort() {
        Short concreteValue = DEFAULT_SHORT;
        try {
            concreteValue = shorts.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded short values; using default value");
        }
        return concreteValue;
    }

    public Integer nextInteger() {
        Integer concreteValue = DEFAULT_INTEGER;
        try {
            concreteValue = integers.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded integer values; using default value");
        }
        return concreteValue;
    }

    public Long nextLong() {
        Long concreteValue = DEFAULT_LONG;
        try {
            concreteValue = longs.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded long values; using default value");
        }
        return concreteValue;
    }

    public Float nextFloat() {
        Float concreteValue = DEFAULT_FLOAT;
        try {
            concreteValue = floats.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded float values; using default value");
        }
        return concreteValue;
    }

    public Double nextDouble() {
        Double concreteValue = DEFAULT_DOUBLE;
        try {
            concreteValue = doubles.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded double values; using default value");
        }
        return concreteValue;
    }

    public String nextString() {
        String concreteValue = DEFAULT_STRING;
        try {
            concreteValue = strings.remove();
        } catch (NoSuchElementException exception) {
            logger.warning("not enough seeded string values; using default value");
        }
        return concreteValue;
    }
}
