import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class UdpUserClient {
    public static void main(String[] args) throws IOException, InterruptedException {
        DatagramSocket socket = new DatagramSocket(7771);
        byte[] bytes = ("82e3e7294cfc41b49cbc1a906646e050").getBytes();
        socket.send(new DatagramPacket(bytes, bytes.length, new InetSocketAddress("127.0.0.1", 7777)));
        byte[] buf = new byte[1024];
        DatagramPacket receiveP = new DatagramPacket(buf, 1024);
        socket.receive(receiveP);
        String s = new String(receiveP.getData());
        System.out.println(s);

        InetAddress address = receiveP.getAddress();
        int port = receiveP.getPort();
        while (true) {
            bytes = ("hi 现在是" + System.currentTimeMillis()).getBytes();
            socket.send(new DatagramPacket(bytes, bytes.length, address, port));
            socket.receive(receiveP);
            System.out.println(new String(receiveP.getData()));
            TimeUnit.SECONDS.sleep(2);
        }
//        ServerSocket serverSocket = new ServerSocket(123);
//        Socket accept = serverSocket.accept();
//        InputStream inputStream = accept.getInputStream();
//        bytes = new byte[1024];
//        while (inputStream.read(bytes) != -1) {
//            socket.send(new DatagramPacket(bytes, bytes.length, address, port));
//            socket.receive(receiveP);
//            System.out.println(new String(receiveP.getData()));
//        }
    }
}
