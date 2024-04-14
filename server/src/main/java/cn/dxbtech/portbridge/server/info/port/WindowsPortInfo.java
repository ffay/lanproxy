package cn.dxbtech.portbridge.server.info.port;

import io.netty.util.internal.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WindowsPortInfo extends AbstractPortInfoGetter {

    @Override
    List<String> getNetstatOutput() throws IOException, InterruptedException {
        return exec(new String[]{"cmd.exe", "/c", "netstat", "/n", "/a", "/o"});
    }

    @Override
    List<String> getPsOutput() throws IOException, InterruptedException {
        return exec(new String[]{"cmd", "/c", "tasklist"});
    }

    @Override
    List<String> exec(String[] commands) throws IOException {
        Process exec = Runtime.getRuntime().exec(commands);
        try {
            InputStream inputStream = exec.getInputStream();
            InputStream errorStream = exec.getErrorStream();
            List<String> in = readString(inputStream, Charset.forName("GBK"));
            List<String> error = readString(errorStream, Charset.forName("GBK"));
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
        LinkedList<PortInfoHandler> portInfoHandlers = new LinkedList<>();

        portInfoHandlers.add((portInfoList, netstat, ps) -> {
            for (String s : netstat) {
                if (StringUtil.isNullOrEmpty(s)) {
                    continue;
                }

                s = s.trim();

                String[] split = s.split("\\s+");


                if (split.length != 5 && split.length != 4) {
                    logger.debug("split array length not 5 " + s);
                    continue;
                }

                String type = fromArray(split, 0);
                String localAddress = fromArray(split, 1);
                int portIndex = localAddress.lastIndexOf(":");

                if (portIndex < 0) {
                    logger.debug("not valid port info: " + s);
                    continue;
                }

                String pid = fromArray(split, split.length - 1);

                if (pid.equals("0")) {
                    //进程已经释放了端口
                    continue;
                }

                try {
                    Integer.valueOf(pid);
                } catch (Exception e) {
                    // not valid pid
                    continue;
                }

                PortInfo portInfo = new PortInfo();

                portInfo.setHost(localAddress.substring(0, portIndex));
                portInfo.setPort(Integer.valueOf(localAddress.substring(portIndex + 1)));

                portInfo.setType(type);
                portInfo.setPid(pid);

                if (portInfoList.contains(portInfo)) {
                    continue;
                }
                portInfoList.add(portInfo);
            }
        });


        portInfoHandlers.add((portInfoList, netstat, ps) -> {

            Map<String, String> processNameMap = new HashMap<>(ps.size());
            Map<String, String> processMemoryMap = new HashMap<>(ps.size());

            for (String process : ps) {

                if (StringUtil.isNullOrEmpty(process)) {
                    continue;
                }
                process = process.trim();

//                String[] split = process.split("\\s+");


                Pattern pattern = Pattern.compile("([\\s\\S]+\\S?)\\s+(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+");

                Matcher matcher = pattern.matcher(process);
                if (!matcher.find()) {
                    logger.warn("not match " + process);
                    continue;
                }


                String pid = matcher.group(2);
                try {
                    Integer.valueOf(pid);
                } catch (Exception e) {
                    // not valid pid
                    continue;
                }

                String processName = matcher.group(1);
                if (processName != null) {
                    processName = processName.trim();
                }
                processNameMap.put(pid, processName);
                processMemoryMap.put(pid, matcher.group(5));
            }


            for (PortInfo portInfo : portInfoList) {
                if (processNameMap.containsKey(portInfo.getPid())) {
                    portInfo.setName(processNameMap.get(portInfo.getPid()));
                }
                if (processMemoryMap.containsKey(portInfo.getPid())) {
                    portInfo.setInformation(processMemoryMap.get(portInfo.getPid()) + " KB");
                }
            }
        });

        portInfoHandlers.add((portInfoList, netstat, ps) -> {

            Map<String, String> pidPsName = new HashMap<>();
            try {
                List<String> jps = exec(new String[]{"cmd", "/c", "jps", "-l"});
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
