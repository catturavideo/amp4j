package org.amp4j.amp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import java.io.ByteArrayOutputStream;

// import java.util.UnsupportedOperationException;

import java.io.UnsupportedEncodingException;

import java.lang.reflect.Field;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * small ordered key=>value mapping where the keys and values are both byte
 * arrays.
 */

public class AMPBox implements Map<byte[], byte[]> {
    private class Pair implements Map.Entry<byte[], byte[]> {
        Pair(byte[] k, byte[] v) {
            this.key = k;
            this.value = v;
        }
        byte[] key;
        byte[] value;

        public boolean equals(Object o) {
            if (o instanceof Pair) {
                Pair other = (Pair) o;
                return (Arrays.equals(other.key, this.key) &&
                        Arrays.equals(other.value, this.value));
            }
            return false;
        }

        public byte[] getKey() { return key; }
        public byte[] getValue() { return value; }

        public byte[] setValue(byte[] value) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
    }

    private ArrayList<Pair> pairs;

    public AMPBox() {
        pairs = new ArrayList<Pair>();
    }


    /* implementation of Map interface */
    public void clear () throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public Set<byte[]> keySet() {
        HashSet<byte[]> hs = new HashSet<byte[]>();
        for (Pair p: pairs) {
            hs.add(p.key);
        }
        return hs;
    }

    public Set<Map.Entry<byte[], byte[]>> entrySet() {
        HashSet<Map.Entry<byte[], byte[]>> hs =
            new HashSet<Map.Entry<byte[], byte[]>>();
        for (Pair p: pairs) {
            hs.add(p);
        }
        return hs;
    }

    public Collection<byte[]> values() {
        ArrayList<byte[]> v = new ArrayList<byte[]>();
        for (Pair p: pairs) {
            v.add(p.value);
        }
        return v;
    }

    public int size() {
        return pairs.size();
    }

    public boolean isEmpty() {
        return 0 == size();
    }

    public boolean equals (Object o) {
        if (!(o instanceof AMPBox)) {
            return false;
        }
        AMPBox other = (AMPBox) o;

        for (Pair p: pairs) {
            if (!Arrays.equals(other.get(p.key), p.value)) {
                return false;
            }
        }
        return true;
    }

    public byte[] put(byte[] key, byte[] value) {
        pairs.add(new Pair(key, value));
        return null;
    }

    public void putAll(Map<? extends byte[], ? extends byte[]> m) {
        for (Map.Entry<? extends byte[], ? extends byte[]> me: m.entrySet()) {
            put(me.getKey(), me.getValue());
        }
    }

    public byte[] remove(Object k) {
        byte[] key = (byte[]) k;
        for (int i = 0; i < pairs.size(); i++) {
            Pair p = pairs.get(i);
            if (Arrays.equals(p.key, key)) {
                pairs.remove(i);
                return p.value;
            }
        }
        return null;
    }

    /**
     * Convenience API because there is no byte literal syntax in java.
     */
    public void put(String key, String value) {
        put(asBytes(key), asBytes(value));
    }

    public void put(String key, byte[] value) {
        put(asBytes(key), value);
    }

    public static byte[] asBytes(String in) {
        return asBytes(in, "ISO-8859-1");
    }

    public static byte[] asBytes(String in, String encoding) {
        try {
            return in.getBytes(encoding);
        } catch (UnsupportedEncodingException uee) {
            throw new Error("JVMs are required to support encoding: " +encoding);
        }
    }

    public static String asString(byte[] in, String knownEncoding) {
        try {
            return new String(in, knownEncoding);
        } catch (UnsupportedEncodingException uee) {
            throw new Error("JVMs are required to support this encoding: " + knownEncoding);
        }
    }

    public static String asString(byte[] in) {
        return asString(in, "ISO-8859-1");
    }


    public byte[] get(byte[] key) {
        for(Pair p: pairs) {
            if (Arrays.equals(key, p.key)) {
                return p.value;
            }
        }
        return null;
    }

    public byte[] get(String key) {
        return get(key.getBytes());
    }

    public byte[] get(Object key) {
        if (key instanceof String) {
            return get((String)key);
        } else if (key instanceof byte[]) {
            return get((byte[])key);
        }
        return null;
    }

    public boolean containsValue(Object v) {
        byte[] value = (byte[]) v;
        for (Pair p: pairs) {
            if (Arrays.equals(p.value, value)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsKey(Object value) {
        return null != get(value);
    }

    /**
     * Take the values encoded in this packet and map them into an arbitrary
     * Java object.  This method will fill out fields declared in the given
     * object's class which correspond to types defined in the AMP protocol:
     * integer, unicode string, raw bytes, boolean, float.
     */

    public void fillOut(Object o) {
        Class c = o.getClass();
        Field[] fields = c.getFields();

        try {
            for (Field f: fields) {
                byte[] toDecode = get(f.getName());
                Object decoded = getAndDecode(f.getName(), f.getType());
                if(null != decoded) {
                    f.set(o, decoded);
                }
            }
        } catch (IllegalAccessException iae) {
            /*
              This should be basically impossible to get; getFields should
              only give us public fields.
             */
        }
    }

    public Object getAndDecode(String key, Class t) {
        byte[] toDecode = this.get(key);
        if (null != toDecode)
        {
            if ((t == int.class) || (t == Integer.class))
            {
                return Integer.parseInt(asString(toDecode), 10);
            }
            else if (t == String.class)
            {
                return asString(toDecode, "UTF-8");
            }
            else if ((t == boolean.class) || (t == Boolean.class))
            {
                String s = asString(toDecode);
                if (s.equals("True"))
                {
                    return Boolean.TRUE;
                }
                else if (s.equals("False"))
                {
                    return Boolean.FALSE;
                }
                else
                {
                    throw new Error(String.format("Expected \"True\" or \"False\", not %s", s));
                }
            }
            else if (t == byte[].class)
            {
                return toDecode;
            }
            else if ((t == long.class) || (t == Long.class))
            {
                return Long.parseLong(asString(toDecode), 10);
            }
            else if (t == DateTime.class)
            {
                // Should look something like this:
                // 2012-01-23T12:34:56.054321-01:23
                //
                if (toDecode.length != 32)
                {
                    throw new Error("Expected DateTime argument to be 32 bytes in length.");
                }

                String s  = asString(toDecode);
                int year  = Integer.parseInt(s.substring(0, 4), 10);
                int month = Integer.parseInt(s.substring(5, 7), 10);
                int day   = Integer.parseInt(s.substring(8, 10), 10);

                int hour = Integer.parseInt(s.substring(11, 13), 10);
                int min  = Integer.parseInt(s.substring(14, 16), 10);
                int sec  = Integer.parseInt(s.substring(17, 19), 10);

                int sec_micro   = Integer.parseInt(s.substring(20, 26), 10);
                String tz_sign  = s.substring(26, 27);
                int tz_hour     = Integer.parseInt(s.substring(27, 29), 10);
                int tz_min      = Integer.parseInt(s.substring(30, 32), 10);

                if (tz_sign.equals("+"))
                {
                    // pass
                }
                else if (tz_sign.equals("-"))
                {
                    tz_hour *= -1;
                    tz_min *= -1;
                }
                else
                {
                    throw new Error("Bad timezone sign character: " + tz_sign);
                }

                DateTimeZone dtz = DateTimeZone.forOffsetHoursMinutes(tz_hour, tz_min);
                DateTime dt = new DateTime(year, month, day, hour, min, sec, (sec_micro / 1000), dtz);

                return dt;
            }
        }
        return null;
    }

    public void putAndEncode(String key, Object o) {
        Class t = o.getClass();
        byte[] value = null;
        if (t == Integer.class) {
            value = asBytes(((Integer)o).toString());
        } else if (t == String.class) {
            value = asBytes((String) o, "UTF-8");
        } else if (t == Boolean.class) {
            if (((Boolean)o).booleanValue()) {
                value = asBytes("True");
            } else {
                value = asBytes("False");
            }
        } else if (t == byte[].class) {
            value = (byte[]) o;
        }
        if (null != value) {
            put(asBytes(key), value);
        }
    }

    public void extractFrom(Object o) {
        Class c = o.getClass();
        Field[] fields = c.getFields();
        try {
            for (Field f: fields) {
                putAndEncode(f.getName(), f.get(o));
            }
        } catch (IllegalAccessException iae) {
            /*
              This should be basically impossible to get; getFields should
              only give us public fields.
             */
        }
    }

    public byte[] encode() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Pair p: pairs) {
            for (byte[] bp : new byte[][] {p.key, p.value}) {
                baos.write(bp.length / 0x100); // DIV
                baos.write(bp.length % 0x100); // MOD
                baos.write(bp, 0, bp.length);
            }
        }
        baos.write(0);
        baos.write(0);
        return baos.toByteArray();
    }
}
