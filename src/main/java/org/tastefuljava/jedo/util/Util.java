package org.tastefuljava.jedo.util;

public class Util {
    // private constructor to prevent instanciation
    private Util() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static <T extends Enum<T>> T toEnum(Class<T> clazz, String value) {
        if (Util.isBlank(value)) {
            return null;
        }
        return Enum.valueOf(clazz, value.toUpperCase().replace('-', '_'));
    }
}
