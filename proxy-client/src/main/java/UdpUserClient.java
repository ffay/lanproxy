
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class UdpUserClient {


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


    public static void main(String[] args) throws IOException, InterruptedException {
        final DatagramSocket udpSocket = new DatagramSocket(7771);
        byte[] bytes = ("82e3e7294cfc41b49cbc1a906646e050-127.0.0.1:8888").getBytes();
        udpSocket.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress("127.0.0.1", 7777)));
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
                    byte[] heartBeatInfo = new byte[]{Byte.MAX_VALUE, Byte.MIN_VALUE};
                    udpSocket.send(new DatagramPacket(heartBeatInfo, heartBeatInfo.length, clientAddress, port));
                    TimeUnit.SECONDS.sleep(5);
                }
            }
        }).start();

        byte[] byteBuf = new byte[2048];

        final DatagramPacket dataReceiveP = new DatagramPacket(byteBuf, byteBuf.length);


        final ConcurrentHashMap<String, OutputStream> map = new ConcurrentHashMap<>();

        new Thread(new Runnable() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    udpSocket.receive(dataReceiveP);
                    if (dataReceiveP.getData()[0] == Byte.MAX_VALUE && dataReceiveP.getData()[1] == Byte.MIN_VALUE) {
                        System.out.println("receive heartBeatInfo from " + receiveP.getAddress());
                        continue;
                    }
                    map.get("1").write(dataReceiveP.getData());
                    System.out.println(new String(dataReceiveP.getData()));
                }
            }
        }).start();


        //开启一个SocketServer 用以接受真实访问数据 使用TCP接受
        ServerSocket realDateServerSocket = new ServerSocket(5555);
        while (true) {
            final Socket realDateSocket = realDateServerSocket.accept();
            new Thread(new Runnable() {
                @SneakyThrows
                @Override
                public void run() {
                    byte[] sendDataBytes = new byte[2048];
                    int i = 0;
                    InputStream inputStream = realDateSocket.getInputStream();
                    OutputStream outputStream = realDateSocket.getOutputStream();
                    String hostAddress = realDateSocket.getInetAddress().getHostAddress();
                    map.put("1", outputStream);
                    while (inputStream.read(sendDataBytes) > 0) {
                        udpSocket.send(new DatagramPacket(sendDataBytes, sendDataBytes.length, clientAddress, port));
                        System.out.println("发送真实请求数据到:" + receiveP.getAddress());
                    }
                }
            }).start();
        }

    }
}
