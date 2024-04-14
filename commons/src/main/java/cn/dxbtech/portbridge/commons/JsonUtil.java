package cn.dxbtech.portbridge.commons;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * JSON与POJO转换工具类.
 *
 * @author fengfei
 *
 */
public class JsonUtil {
    private static Gson gson = new Gson();

    @SuppressWarnings("unchecked")
    public static <T> T json2object(String json, TypeToken<T> typeToken) {
        try {
            return (T) gson.fromJson(json, typeToken.getType());
        } catch (Exception ignore) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T json2object(byte[] json, TypeToken<T> typeToken) {
        try {
            return (T) gson.fromJson(new String(json), typeToken.getType());
        } catch (Exception ignore) {
        }
        return null;
    }

    /**
     * java对象转为json对象
     */
    public static String object2json(Object obj) {
        return gson.toJson(obj);
    }

}