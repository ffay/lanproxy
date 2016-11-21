package org.fengfei.lanproxy.common.container;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 容器启动工具类.
 *
 * @author fengfei
 *
 */
public class ContainerHelper {

    private static Logger logger = LoggerFactory.getLogger(ContainerHelper.class);

    private static volatile boolean running = true;

    private static List<Container> cachedContainers;

    public static void start(List<Container> containers) {

        cachedContainers = containers;

        // 启动所有容器
        startContainers();

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {

                synchronized (ContainerHelper.class) {

                    // 停止所有容器.
                    stopContainers();
                    running = false;
                    ContainerHelper.class.notify();
                }
            }
        });

        synchronized (ContainerHelper.class) {
            while (running) {
                try {
                    ContainerHelper.class.wait();
                } catch (Throwable e) {
                }
            }
        }
    }

    private static void startContainers() {
        for (Container container : cachedContainers) {
            logger.info("starting container [{}]", container.getClass().getName());
            container.start();
            logger.info("container [{}] started", container.getClass().getName());
        }
    }

    private static void stopContainers() {
        for (Container container : cachedContainers) {
            logger.info("stopping container [{}]", container.getClass().getName());
            try {
                container.stop();
                logger.info("container [{}] stopped", container.getClass().getName());
            } catch (Exception ex) {
                logger.warn("container stopped with error", ex);
            }
        }
    }
}
