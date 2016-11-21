package org.fengfei.lanproxy.server.test;

import java.util.Arrays;

import org.fengfei.lanproxy.common.container.Container;
import org.fengfei.lanproxy.common.container.ContainerHelper;
import org.fengfei.lanproxy.server.ProxyServerContainer;

public class ServerMainTest {

    public static void main(String[] args) {
        ContainerHelper.start(Arrays.asList(new Container[] { new ProxyServerContainer() }));
    }

}
