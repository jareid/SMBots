package org.smokinmils.bot;

/**
 * Provides a pair of objects.
 * @author Jamie
 */
public class Pair {
    /**The first object in the pair. */
    private Object key;
    
    /**The second object in the pair .*/
    private Object value;

    /**
     * Constructor.
     * 
     * @param k The first object in the pair.
     * @param v The second object in the pair.
     */
    public Pair(final Object k, final Object v) {
        setKey(k);
        setValue(v);
    }

    /**
     * @return the key
     */
    public final Object getKey() {
        return key;
    }

    /**
     * @param k the key to set
     */
    public final void setKey(final Object k) {
        key = k;
    }

    /**
     * @return the value
     */
    public final Object getValue() {
        return value;
    }

    /**
     * @param v the value to set
     */
    public final void setValue(final Object v) {
        value = v;
    }
}
