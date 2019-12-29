package org.tastefuljava.jedo.conversion;

import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.tastefuljava.jedo.JedoException;

public abstract class Converter<S,T> {
    private static final Logger LOG
            = Logger.getLogger(Converter.class.getName());
    public static final Converter IDENTITY;

    private static Map<Class<?>,Map<Class<?>,Converter>> CONVERTERS
            = new HashMap<>();

    public static <SS,TT> TT convert(SS value, Class<TT> targetType) {
        if (value == null) {
            return null;
        }
        Converter conv = getConverter(value.getClass(), targetType);
        if (conv == null) {
            boolean tstring = String.class == targetType;
            if (tstring) {
                TT result = (TT)value.toString();
                return result;
            }
            throw new JedoException(
                    "No conversion from " + value.getClass().getName()
                    + " to " + targetType.getName());
        }
        @SuppressWarnings("unchecked")
        TT result = (TT)conv.convert(value);
        return result;
    }

    public abstract T convert(S value);

    public static <S,T> void register(Class<S> from, Class<T> to,
            Converter<S,T> conv) {
        Map<Class<?>,Converter> map = CONVERTERS.get(to);
        if (map == null) {
            map = new HashMap<>();
            CONVERTERS.put(to, map);
        }
        map.put(from, conv);
    }

    private static <S,T> Converter<S,T> getConverter(
            Class<S> from, Class<T> targetType) {
        List<LinkedConverter> list = new ArrayList<>();
        int pos = 0;
        Class<?> to = targetType;
        LinkedConverter cur = null;
        while (!match(from, to)) {
            Map<Class<?>,Converter> map = CONVERTERS.get(to);
            if (map != null) {
                for (Map.Entry<Class<?>,Converter> e: map.entrySet()) {
                    Class<?> f = e.getKey();
                    Converter c = e.getValue();
                    boolean found = false;
                    for (LinkedConverter lc: list) {
                        found = match(f, lc.from);
                        if (found) {
                            break;
                        }
                    }
                    if (!found) {
                        @SuppressWarnings("unchecked")
                        LinkedConverter lc = new LinkedConverter(f, c, cur);
                        list.add(lc);
                    }
                }
            }
            if (pos >= list.size()) {
                return null;
            }
            cur = list.get(pos++);
            to = cur.from;
        }
        @SuppressWarnings("unchecked")
        Converter<S,T> result = cur == null
                ? IDENTITY
                : cur.link == null
                      ? cur.conv : cur;
        return result;
    }

    @SuppressWarnings("unchecked")
    private static boolean match(Class<?> from, Class<?> to) {
        return to.isAssignableFrom(from);
    }

    private static class LinkedConverter<S,T> extends Converter<S,T> {
        private final Class<S> from;
        private final Converter<S,Object> conv;
        private final LinkedConverter<Object,T> link;

        LinkedConverter(Class<S> from, Converter<S,Object> conv,
                LinkedConverter<Object,T> link) {
            this.from = from;
            this.conv = conv;
            this.link = link;
        }

        @Override
        public T convert(S value) {
            Object obj = conv.convert(value);
            @SuppressWarnings("unchecked")
            T result = link == null ? (T)obj : link.convert(obj);
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private static <S,T> Converter<S,T> identity(Class<S> cs, Class<T> ct) {
        return IDENTITY;
    }

    private static <S,T> void registerId(Class<S> cs, Class<T> ct) {
        register(cs, ct, identity(cs, ct));
    }

    static {
        IDENTITY = new Converter() {
            @Override
            public Object convert(Object value) {
                return value;
            }
        };
        registerId(byte.class, Byte.class);
        registerId(Byte.class, byte.class);
        registerId(short.class, Short.class);
        registerId(Short.class, short.class);
        registerId(int.class, Integer.class);
        registerId(Integer.class, int.class);
        registerId(long.class, Long.class);
        registerId(Long.class, long.class);
        registerId(float.class, Float.class);
        registerId(Float.class, float.class);
        registerId(double.class, Double.class);
        registerId(Double.class, double.class);
        register(Timestamp.class, Date.class, new Converter<Timestamp,Date>() {
            @Override
            public Date convert(Timestamp value) {
                return value == null ? null : new Date(value.getTime());
            }
        });
        register(Time.class, Date.class, new Converter<Time,Date>() {
            @Override
            public Date convert(Time value) {
                return value == null ? null : new Date(value.getTime());
            }
        });
        register(java.sql.Date.class, Date.class,
                new Converter<java.sql.Date,Date>() {
            @Override
            public Date convert(java.sql.Date value) {
                return value == null ? null : new Date(value.getTime());
            }
        });
        register(Date.class, Timestamp.class, new Converter<Date,Timestamp>() {
            @Override
            public Timestamp convert(Date value) {
                return value == null ? null : new Timestamp(value.getTime());
            }
        });
        register(Date.class, Time.class, new Converter<Date,Time>() {
            @Override
            public Time convert(Date value) {
                return value == null ? null : new Time(value.getTime());
            }
        });
        register(Date.class, java.sql.Date.class,
                new Converter<Date,java.sql.Date>() {
            @Override
            public java.sql.Date convert(Date value) {
                return value == null ? null
                        : new java.sql.Date(value.getTime());
            }
        });
        register(Byte.class, Short.class, new Converter<Byte,Short>() {
            @Override
            public Short convert(Byte value) {
                return value == null ? null : value.shortValue();
            }
        });
        register(Short.class, Integer.class, new Converter<Short,Integer>() {
            @Override
            public Integer convert(Short value) {
                return value == null ? null : value.intValue();
            }
        });
        register(Integer.class, Long.class, new Converter<Integer,Long>() {
            @Override
            public Long convert(Integer value) {
                return value == null ? null : value.longValue();
            }
        });
        register(Integer.class, Float.class, new Converter<Integer,Float>() {
            @Override
            public Float convert(Integer value) {
                return value == null ? null : value.floatValue();
            }
        });
        register(Long.class, Double.class, new Converter<Long,Double>() {
            @Override
            public Double convert(Long value) {
                return value == null ? null : value.doubleValue();
            }
        });
        register(Float.class, Double.class, new Converter<Float,Double>() {
            @Override
            public Double convert(Float value) {
                return value == null ? null : value.doubleValue();
            }
        });
        register(Clob.class, String.class, new Converter<Clob,String>() {
            @Override
            public String convert(Clob value) {
                try {
                    if (value == null) {
                        return null;
                    }
                    long length = value.length();
                    if (length == 0) {
                        return "";
                    } else if (length > Integer.MAX_VALUE) {
                        throw new JedoException("Text too long to be converter"
                                + " into a String");
                    }
                    return value.getSubString(1, (int)length);
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                    throw new JedoException(ex.getMessage());
                }
            }
        });
    }
}
