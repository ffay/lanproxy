package cn.dxbtech.portbridge.commons;

import java.io.File;
import java.io.IOException;

public class PersistenceUtil {
    public static String getFilePath(String... fileName) {
        return getFilePath(true, fileName);
    }

    public static String getFilePath(boolean create, String... fileName) {
        // 持久化存放在用户根目录下
        StringBuilder dataPath = new StringBuilder(String.join(File.separator, System.getProperty("user.home"), ".port-bridge"));
        for (String s : fileName) {
            dataPath.append(File.separator).append(s);
        }
        File file = new File(dataPath.toString());
        if (!file.getParentFile().isDirectory()) {
            file.getParentFile().mkdirs();
        }

        if (create && !file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return dataPath.toString();
    }
}
