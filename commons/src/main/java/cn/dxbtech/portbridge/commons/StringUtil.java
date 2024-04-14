package cn.dxbtech.portbridge.commons;

public class StringUtil {
    public static boolean isEmpty(String s) {
        return s == null || "".equals(s);
    }

    public static boolean isNotEmpty(String s) {
        return !isEmpty(s);
    }

    public static boolean equals(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }

        return s1.equals(s2);
    }

    public static String trim(String arg) {
        if (arg != null) {
            return arg.trim();
        }
        return arg;
    }
}
