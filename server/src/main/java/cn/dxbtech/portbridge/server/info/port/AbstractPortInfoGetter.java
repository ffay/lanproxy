package cn.dxbtech.portbridge.server.info.port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * 获取端口占用情况
 * <p>
 * <p>
 * Created by dxb on 2018/4/29.
 */
public abstract class AbstractPortInfoGetter {
    Logger logger = LoggerFactory.getLogger(getClass());

    public static List<PortInfo> get(String sort) throws IOException, InterruptedException {
        if (sort == null) {
            sort = "";
        }
        AbstractPortInfoGetter getter;
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            getter = new WindowsPortInfo();

        } else if (osName.contains("Linux")) {
            getter = new LinuxPortInfo();
        } else {
            throw new IllegalArgumentException("Unknown os type : " + osName);
        }
        return getter.getPortInformation(sort);
    }

    List<PortInfo> getPortInformation(String sort) throws IOException, InterruptedException {
        LinkedList<PortInfo> portInfos = new LinkedList<>();
        List<String> netstat = getNetstatOutput();
        List<String> psOutput = getPsOutput();

        for (PortInfoHandler portInfoHandler : getPortInfoHandlers()) {
            portInfoHandler.process(portInfos, netstat, psOutput);
        }

        switch (sort) {
            case "port":
                portInfos.sort((o1, o2) -> o1.getPort() - o2.getPort());
                break;

            default:
                //排序
                portInfos.sort((o1, o2) -> {
                    int i = o1.getName().compareTo(o2.getName());
                    if (i == 0) {
                        i = o1.getPort() - o2.getPort();
                    }
                    return i;
                });
                break;
        }

        return portInfos;
    }

    abstract List<PortInfoHandler> getPortInfoHandlers();

    abstract List<String> getNetstatOutput() throws IOException, InterruptedException;

    abstract List<String> getPsOutput() throws IOException, InterruptedException;

    abstract List<String> exec(String[] commands) throws IOException;

    String fromArray(String[] strings, int index) {
        if (strings.length > index) {
            return strings[index];
        }
        return "";
    }

    List<String> readString(InputStream inputStream) throws IOException {
        return readString(inputStream, Charset.forName("UTF-8"));
    }

    List<String> readString(InputStream inputStream, Charset charset) throws IOException {
        LinkedList<String> outputs = new LinkedList<>();
        try (InputStreamReader in = new InputStreamReader(inputStream);
             BufferedReader bufferedReader = new BufferedReader(in)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outputs.add(new String(line.getBytes(), charset));
            }
        }
        return outputs;
    }

    interface PortInfoHandler {
        void process(List<PortInfo> portInfoList, List<String> netstat, List<String> ps);
    }


}
