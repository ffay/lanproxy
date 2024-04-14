package cn.dxbtech.portbridge.server.info.port;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxPortInfo extends AbstractPortInfoGetter {
    @Override
    List<String> getNetstatOutput() throws IOException, InterruptedException {
        return exec(new String[]{"sh", "-c", "netstat -nlp"});
    }

    @Override
    List<String> getPsOutput() throws IOException, InterruptedException {
        return null;
    }

    @Override
    List<String> exec(String[] commands) throws IOException {
        Process exec = Runtime.getRuntime().exec(commands);
        try {
            InputStream inputStream = exec.getInputStream();
            InputStream errorStream = exec.getErrorStream();
            List<String> in = readString(inputStream);
            List<String> error = readString(errorStream);
            if (!error.isEmpty()) {
                logger.warn("out put {}", error);
            }
            return in;
        } finally {
            exec.destroy();
        }
    }

    @Override
    List<PortInfoHandler> getPortInfoHandlers() {
        ArrayList<PortInfoHandler> portInfoHandlers = new ArrayList<>();


        portInfoHandlers.add((portInfoList, netstat, ps) -> {
            Pattern pattern = Pattern.compile("(tcp|tcp6|udp|udp6)\\s+\\d+\\s+\\d+\\s+(\\S+?):(\\d+)\\s+\\S+\\s+(LISTEN)?\\s*(\\d+)\\/(.+)");
            for (String line : netstat) {

                Matcher matcher = pattern.matcher(line);
                if (!matcher.matches()) {
                    continue;
                }

                String type = matcher.group(1);
                String host = matcher.group(2);
                String port = matcher.group(3);
                String pid = matcher.group(5);
                String process = matcher.group(6).trim();

                PortInfo portInfo = new PortInfo();
                portInfo.setName(process);
                portInfo.setInformation(process);
                portInfo.setHost(host);
                portInfo.setPid(pid);
                portInfo.setPort(Integer.valueOf(port));
                portInfo.setType(type);

                portInfoList.add(portInfo);
            }
        });


        portInfoHandlers.add((portInfoList, netstat, ps) -> {
            Map<String, String> pidPsName = new HashMap<>();
            try {
                List<String> jps = exec(new String[]{"sh", "-c", "jps -l"});
                for (String process : jps) {
                    String[] split = process.split("\\s+");
                    if (split.length == 2) {
                        pidPsName.put(split[0], split[1]);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (PortInfo portInfo : portInfoList) {
                if (pidPsName.containsKey(portInfo.getPid())) {
                    String psName = pidPsName.get(portInfo.getPid());
                    portInfo.setName(String.join(": ", portInfo.getName(), psName));
                }
            }

        });

        return portInfoHandlers;
    }
}
