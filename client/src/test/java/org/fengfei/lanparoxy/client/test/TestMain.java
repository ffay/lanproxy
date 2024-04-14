package org.fengfei.lanparoxy.client.test;

import cn.dxbtech.portbridge.client.ProxyClientContainer;
import cn.dxbtech.portbridge.commons.container.Container;
import cn.dxbtech.portbridge.commons.container.ContainerHelper;

import java.util.Arrays;

public class TestMain {

    public static void main(String[] args) {
        ContainerHelper.start(Arrays.asList(new Container[] { new ProxyClientContainer() }));
    }

}
