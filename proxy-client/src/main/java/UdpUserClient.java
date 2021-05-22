import lombok.SneakyThrows;
import org.fengfei.lanproxy.protocol.ProxyMessage;
import org.fengfei.lanproxy.protocol.UdpProxyMessageCodec;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class UdpUserClient {

    private static final AtomicLong userIdProducer = new AtomicLong(0);


    static byte[] udpPolePunchingInfo = new byte[128];

    static {
        for (int i = 0; i < 128; i++) {
            if ((i & 1) == 0) {
                udpPolePunchingInfo[i] = Byte.MAX_VALUE;
            } else {
                udpPolePunchingInfo[i] = Byte.MIN_VALUE;
            }
        }
    }

    private static boolean checkUdpPole(byte[] sendInfo, byte[] receiveInfo) {
        for (int i = 0; i < sendInfo.length; i++) {
            if (sendInfo[i] != receiveInfo[i]) return false;
        }
        return true;
    }


    public static void main(String[] args) throws IOException {
        final String centerServerIp = "127.0.0.1";
        final Integer centerServerPort = 7777;


        int locolUdpSocketPort = 7771;
        String clientKey = "82e3e7294cfc41b49cbc1a906646e050";
        String targetAddressInfo = "127.0.0.1:8888";
        Integer localTcpProxyPort = 5555;

        Scanner scanner = new Scanner(System.in);

        System.out.print("请输入本地Udp发包端口(默认为7771):");
        String portStr = scanner.next();
        if (portStr != null && portStr.trim().length() != 0) {
            locolUdpSocketPort = Integer.parseInt(portStr);
        }

        System.out.print("请输入本地Tcp代理端口(默认为5555):");
        String tcpProxyPortStr = scanner.next();
        if (tcpProxyPortStr != null && tcpProxyPortStr.trim().length() != 0) {
            localTcpProxyPort = Integer.parseInt(tcpProxyPortStr);
        }


        System.out.print("请输入ClientKey(eg: 82e3e7294cfc41b49cbc1a906646e050):");
        clientKey = scanner.next();
        System.out.print("请输入目标局域网服务地址(eg: 127.0.0.1:8888):");
        targetAddressInfo = scanner.next();

        System.out.println("start connect to center server");
        final DatagramSocket udpSocket = new DatagramSocket(locolUdpSocketPort);
        byte[] bytes = (clientKey + "-" + targetAddressInfo).getBytes();

        //connect to center server
        udpSocket.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress(centerServerIp, centerServerPort)));
        byte[] receivedPunchingInfo = new byte[128];

        //第一次从client端接收到的udp打洞信息
        final DatagramPacket receiveP = new DatagramPacket(receivedPunchingInfo, receivedPunchingInfo.length);
        udpSocket.receive(receiveP);
        udpSocket.send(new DatagramPacket(udpPolePunchingInfo, udpPolePunchingInfo.length, receiveP.getSocketAddress()));

        final InetAddress clientAddress = receiveP.getAddress();

        boolean connected = checkUdpPole(receivedPunchingInfo, udpPolePunchingInfo);
        if (connected) {
            System.out.println("connect success:" + clientAddress);
        }


        final int port = receiveP.getPort();
        //开一个线程用来维持心跳

        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    ProxyMessage requestMsg = new ProxyMessage(ProxyMessage.TYPE_HEARTBEAT, -1, null, null);
                    byte[] dataBytes = UdpProxyMessageCodec.encode(requestMsg);
                    udpSocket.send(new DatagramPacket(dataBytes, dataBytes.length, clientAddress, port));
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        }).start();

        final byte[] byteBuf = new byte[2048];

        final DatagramPacket dataReceiveP = new DatagramPacket(byteBuf, byteBuf.length);


        final ConcurrentHashMap<String, Socket> dataSocketMap = new ConcurrentHashMap<>();

        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    udpSocket.receive(dataReceiveP);
                    byte[] proxyMsgBytes = new byte[dataReceiveP.getLength()];
                    System.arraycopy(byteBuf, dataReceiveP.getOffset(), proxyMsgBytes, 0, dataReceiveP.getLength());
                    ProxyMessage msg = UdpProxyMessageCodec.decode(proxyMsgBytes);
                    if (msg.getType() == ProxyMessage.TYPE_HEARTBEAT) {
                        System.out.println("receive heartBeatInfo from " + receiveP.getAddress());
                        continue;
                    }

                    System.out.println(new String(msg.getData()));
                    Socket socket = dataSocketMap.get(msg.getUri());
                    if (socket.isConnected() && !socket.isClosed()) {
                        socket.getOutputStream().write(msg.getData());
                        socket.getOutputStream().flush();
                    }
                }
            }
        }).start();


        //开启一个SocketServer 用以接受真实访问数据 使用TCP接受
        ServerSocket realDateServerSocket = new ServerSocket(localTcpProxyPort);
        while (true) {
            final Socket realDateSocket = realDateServerSocket.accept();
            final String userId = newUserId();
            dataSocketMap.put(userId, realDateSocket);
            new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    byte[] byteBuf = new byte[2048];
                    int dataLen;
                    InputStream inputStream = realDateSocket.getInputStream();
                    while ((dataLen = inputStream.read(byteBuf)) > 0) {

                        byte[] realDataBytes = new byte[dataLen];
                        System.arraycopy(byteBuf, 0, realDataBytes, 0, dataLen);
                        //构造msg
                        ProxyMessage requestMsg = new ProxyMessage(ProxyMessage.P_TYPE_TRANSFER_UDP, dataLen, userId, realDataBytes);
                        byte[] dataBytes = UdpProxyMessageCodec.encode(requestMsg);
                        udpSocket.send(new DatagramPacket(dataBytes, dataBytes.length, clientAddress, port));
                        System.out.println("发送真实请求数据到:" + receiveP.getAddress());
                    }
                }
            }).start();
        }

    }

    private static String newUserId() {
        return String.valueOf(userIdProducer.incrementAndGet());
    }
}
