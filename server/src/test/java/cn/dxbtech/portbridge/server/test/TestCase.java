package cn.dxbtech.portbridge.server.test;

import cn.dxbtech.portbridge.protocol.ProxyMessage;
import cn.dxbtech.portbridge.server.config.ProxyConfig;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCase {
    @Test
    public void test1() throws Exception {
        Field[] fields = ProxyMessage.class.getFields();
        for (Field field : fields) {
            field.getName();
        }
    }

    @Test
    public void name() throws Exception {
        LoggerFactory.getLogger(getClass()).info(ProxyConfig.CONFIG_FILE);
    }


    @Test
    public void getProperties() throws Exception {
        Properties properties = System.getProperties();

        for (Map.Entry<Object, Object> objectObjectEntry : properties.entrySet()) {

            LoggerFactory.getLogger(getClass()).info("{}", objectObjectEntry);
        }

    }

    @Test
    public void x() {
        URL resource = getClass().getClassLoader()
                .getResource("META-INF/MANIFEST.MF");
        try {
            Manifest manifest = new Manifest(resource.openStream());
            Attributes attributes = manifest.getAttributes("Manifest-Version");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void regex() throws Exception {
        //"(tcp|tcp6|udp|udp6)\\s+\\d+\\s+(\\S+?):(\\d+)\\s+\\S+\\s+(LISTEN)?\\s*(\\d+)\\/(\\S)+\\s+"
        String regex = "(tcp|tcp6|udp|udp6)\\s+\\d+\\s+\\d+\\s+(\\S+?):(\\d+).+";
        Pattern pattern = Pattern.compile(regex);

        Matcher matcher = pattern.matcher("tcp        0      0 0.0.0.0:40006           0.0.0.0:*               LISTEN      29885/java          ");
        boolean matches = matcher.matches();
        LoggerFactory.getLogger(getClass()).info("" + matches);
    }
}
