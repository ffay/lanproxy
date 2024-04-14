package cn.dxbtech.portbridge.commons.container;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 容器启动工具类.
 *
 * @author fengfei
 */
public class ContainerHelper {

    private static Logger logger = LoggerFactory.getLogger(ContainerHelper.class);

    private static volatile boolean running = true;

    private static List<Container> cachedContainers;

    public static void start(List<Container> containers) {

        cachedContainers = containers;

        // 启动所有容器
        startContainers();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            synchronized (ContainerHelper.class) {

                // 停止所有容器.
                stopContainers();
                running = false;
                ContainerHelper.class.notify();
            }
        }));

        synchronized (ContainerHelper.class) {
            while (running) {
                try {
                    ContainerHelper.class.wait();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static void startContainers() {
        for (Container container : cachedContainers) {
            logger.info("starting container [{}]", container.getClass().getSimpleName());
            container.start();
            logger.info("container [{}] started", container.getClass().getSimpleName());
        }
    }

    private static void stopContainers() {
        for (Container container : cachedContainers) {
            logger.info("stopping container [{}]", container.getClass().getSimpleName());
            try {
                container.stop();
                logger.info("container [{}] stopped", container.getClass().getSimpleName());
            } catch (Exception ex) {
                logger.warn("container stopped with error", ex);
            }
        }
    }
}
