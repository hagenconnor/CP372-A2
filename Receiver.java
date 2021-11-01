import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Receiver {
    public static void main(String[] args){
        String sender = args[0];
        int port_data = Integer.parseInt(args[1]);
        int port_ack = Integer.parseInt(args[2]);
        String dest = args[3];

        InetAddress address_ack = null;
        DatagramSocket socket_data = null;

        //Socket for receiving data -- Receiver receives data.
        try {
            address_ack = InetAddress.getByName(sender);
            socket_data = new DatagramSocket(port_data, address_ack);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnknownHostException s){
            s.printStackTrace();
        }

        //Receiver sends ack
        byte[] buf = new byte[1024];  
        DatagramPacket dp = new DatagramPacket(buf, 1024);  
        try {
            socket_data.receive(dp);
            String str = new String(dp.getData(), 0, dp.getLength());  
            System.out.println(str);
            
            String text_back = "ACK";
            dp = new DatagramPacket(text_back.getBytes(), text_back.length());
            dp.setAddress(address_ack);
            dp.setPort(port_ack);
            socket_data.send(dp);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }  

    }
}
