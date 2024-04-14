package cn.dxbtech.portbridge.server.test;

import cn.dxbtech.portbridge.commons.PersistenceUtil;
import cn.dxbtech.portbridge.server.info.port.AbstractPortInfoGetter;
import cn.dxbtech.portbridge.server.info.port.PortInfo;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

/**
 * Created by dxb on 2018/4/29.
 */
public class TestPortInfo {
    @Test
    public void name() throws Exception {
        List<PortInfo> portInformation = AbstractPortInfoGetter.get(null);
        Logger logger = LoggerFactory.getLogger(getClass());

        logger.info("{}", portInformation);

    }

    @Test
    public void judgeSystem() throws Exception {
        Properties properties = System.getProperties();
        System.getProperty("os.name");
    }

    @Test
    public void path() throws Exception {

        String filePath = PersistenceUtil.getFilePath("mapping.jso", "mapping.jso", "mapping.jso");
        System.out.println(filePath);

    }
}
