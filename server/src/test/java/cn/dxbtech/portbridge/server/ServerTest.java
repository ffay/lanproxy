package cn.dxbtech.portbridge.server;


import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class ServerTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Path configFile;


    @Before
    public void setUp() throws Exception {
        System.setProperty("web.pages", "C:\\Users\\dxb\\Documents\\Code\\IdeaProjects\\port-bridge2\\server\\src\\main\\resources\\config-web-2");
    }

    @Test
    public void run() throws Exception {
        ProxyServerContainer.main(new String[]{});
    }

    @Test
    public void testHelp() throws Exception {
        ProxyServerContainer.main(new String[]{"-h"});
    }
}
