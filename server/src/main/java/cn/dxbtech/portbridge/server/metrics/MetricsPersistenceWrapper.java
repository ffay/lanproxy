package cn.dxbtech.portbridge.server.metrics;

import cn.dxbtech.portbridge.commons.JsonUtil;
import cn.dxbtech.portbridge.commons.PersistenceUtil;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class MetricsPersistenceWrapper implements Runnable {
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run() {
        logger.info(getClass().getSimpleName() + " started");
        MetricsCollector.getAndResetAllMetrics();
        try (Stream<Path> metrics = Files.list(Paths.get(PersistenceUtil.getFilePath(false, "metrics")))) {
            metrics.forEach(dir -> {
                try (Stream<Path> pathStream = Files.list(dir)) {
                    pathStream.forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            logger.info("{}", e.toString());
        }

        for (; ; ) {
            try {
                call();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void call() throws InterruptedException {
        List<Metrics> allMetrics = MetricsCollector.getAllMetrics();
        CountDownLatch countDownLatch = new CountDownLatch(allMetrics.size());
        logger.debug("call collect {}", allMetrics.size());
        for (Metrics metrics : allMetrics) {
            executorService.submit(new MetricsPersistence(metrics, countDownLatch));
        }
        // 阻塞 防止重复调度
        countDownLatch.await();
    }


    class MetricsPersistence implements Runnable {
        private final Metrics metrics;
        private final CountDownLatch countDownLatch;
        private final Path path;

        MetricsPersistence(Metrics metrics, CountDownLatch countDownLatch) {
            this.metrics = metrics;
            this.countDownLatch = countDownLatch;
            String filePath = PersistenceUtil.getFilePath(false, "metrics", metrics.getPort() + "", metrics.getTimestamp() + "");
            path = Paths.get(filePath);

        }

        private Metrics getLast() {
            Path parent = path.getParent();
            try (Stream<Path> pathStream = Files.list(parent)) {
                Optional<Path> reduce = pathStream.reduce((o1, o2) -> {
                    String fileName1 = o1.getFileName().toString();
                    String fileName2 = o2.getFileName().toString();
                    Long time1 = Long.valueOf(fileName1);
                    Long time2 = Long.valueOf(fileName2);
                    return time1 > time2 ? o1 : o2;
                });
                if (reduce.isPresent()) {
                    Path lastDataPath = reduce.get();
                    return JsonUtil.json2object(Files.readAllBytes(lastDataPath), new TypeToken<Metrics>() {
                    });
                }
                return null;
            } catch (IOException ignore) {
            }
            return null;
        }

        @Override
        public void run() {
            try {
                Metrics last = getLast();
                if (metrics.equals(last)) {
                    logger.debug("skip {}", metrics);
                    Files.deleteIfExists(path);
                    return;
                }
                PersistenceUtil.getFilePath("metrics", metrics.getPort() + "", metrics.getTimestamp() + "");
                logger.debug("Persistence {}", metrics);
                Files.write(path, JsonUtil.object2json(metrics).getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        }
    }
}
