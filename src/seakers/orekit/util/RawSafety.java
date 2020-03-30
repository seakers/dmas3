package seakers.orekit.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

public class RawSafety {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Iterator<T> castType(Iterator it) {
        return (Iterator<T>) it;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> Collection<T> castType(Collection v) {
        return (Collection<T>) v;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V> HashMap<K, V> castHashMap(Object v) {
        return ( HashMap<K, V>) v;
    }

}
