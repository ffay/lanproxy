package org.fengfei.lanproxy.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * 类型转换工具类.
 *
 * @author fengfei
 *
 */
public class LangUtil {

    private static Logger logger = LoggerFactory.getLogger(LangUtil.class);

    public static Boolean parseBoolean(Object value) {
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                return Boolean.valueOf((String) value);
            }
        }
        return null;
    }

    public static boolean parseBoolean(Object value, boolean defaultValue) {
        if (value != null) {
            if (value instanceof Boolean) {
                return (Boolean) value;
            } else if (value instanceof String) {
                try {
                    return Boolean.valueOf((String) value);
                } catch (Exception e) {
                    logger.warn("parse boolean value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }

    /**
     * @Title: parseInt
     * @Description: Int解析方法，可传入Integer或String值
     * @param value Integer或String值
     * @return Integer 返回类型
     */
    public static Integer parseInt(Object value) {
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                return Integer.valueOf((String) value);
            }
        }
        return null;
    }

    public static Integer parseInt(Object value, Integer defaultValue) {
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                try {
                    return Integer.valueOf((String) value);
                } catch (Exception e) {
                    logger.warn("parse Integer value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }

    /***
     *
     * @Title: parseLong
     * @Description: long解析方法，可传入Long或String值
     * @param value Integer或String值
     * @param @return
     * @return Long 返回类型
     */
    public static Long parseLong(Object value) {
        if (value != null) {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof String) {
                return Long.valueOf((String) value);
            }
        }
        return null;
    }

    public static Long parseLong(Object value, Long defaultValue) {
        if (value != null) {
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof String) {
                try {
                    return Long.valueOf((String) value);
                } catch (NumberFormatException e) {
                    logger.warn("parse Long value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }

    /**
     * @Title: parseDouble
     * @Description: Double解析方法，可传入Double或String值
     * @param value Double或String值
     * @return Double 返回类型
     */
    public static Double parseDouble(Object value) {
        if (value != null) {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof String) {
                return Double.valueOf((String) value);
            }
        }
        return null;
    }

    /**
     * @Title: parseDouble
     * @Description: Double解析方法，可传入Double或String值
     * @param value Double或String值
     * @return Double 返回类型
     */
    public static Double parseDouble(Object value, Double defaultValue) {
        if (value != null) {
            if (value instanceof Double) {
                return (Double) value;
            } else if (value instanceof String) {
                try {
                    return Double.valueOf((String) value);
                } catch (NumberFormatException e) {
                    logger.warn("parse Double value({}) failed.", value);
                }
            }
        }
        return defaultValue;
    }
}
