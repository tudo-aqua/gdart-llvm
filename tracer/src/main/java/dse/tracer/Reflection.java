package dse.tracer;

import java.lang.reflect.Field;

import com.oracle.truffle.api.TruffleLogger;

public class Reflection {
    private static TruffleLogger logger = TruffleLogger.getLogger("symbolictracer", Seeds.class.getName());

    public static Object readField(Object object, Class<?> objectClass, String fieldName) {
        Object value = null;
        try {
            Field field = objectClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            value = field.get(object);
        } catch (NoSuchFieldException exception) {
            logger.severe(exception.toString());
        } catch (IllegalAccessException exception) {
            logger.severe(exception.toString());
        }
        return value;
    }
}