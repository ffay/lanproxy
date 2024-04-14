package cn.dxbtech.portbridge.client;

import org.junit.Test;

public class ClientTest {

    @Test
    public void run() throws Exception {
        ProxyClientContainer.main(new String[]{
                "-s", "115.159.196.253",
                "-p", "4901",
//                "-k", "e6b7332cdc06460b905d94673a6e38bd",

        });
    }

    @Test
    public void runLocal() throws Exception {
        ProxyClientContainer.main(new String[]{
                "-s", "localhost",
                "-p", "4900",
                "-k", "490716549bfabfcc",
        });
    }

    @Test
    public void testHelp() throws Exception {
        ProxyClientContainer.main(new String[]{"-h"});
    }
}
