import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Receiver {
    static int port_ack;
    static boolean tested = false;
    static InetAddress sender;
    public static void main(String[] args){
        try {
            sender = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int port_data = Integer.parseInt(args[1]);
        port_ack = Integer.parseInt(args[2]);
        String dest = args[3];

        try{
            DatagramSocket socket = new DatagramSocket(port_data);
            DatagramSocket socket_ack = new DatagramSocket();
                        
            System.out.println("Creating file");
            File f = new File (dest); // Creating the file
            FileOutputStream outToFile = new FileOutputStream(f); // Creating the stream through which we write the file content
            waitForTest(socket, socket_ack);
            receiveFile(outToFile, socket, socket_ack, port_ack); // Receiving the file
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }  
        
    }

    private static void receiveFile(FileOutputStream outToFile, DatagramSocket socket, DatagramSocket socket_ack, int port_ack) throws IOException {
        System.out.println("Receiving file");
        boolean flag; // Have we reached end of file
        int sequenceNumber = 0; // Order of sequences
        int foundLast = 1; // The las sequence found
        boolean transmitting = false;
        long start = 0;
        
        while (true) {
            byte[] message = new byte[1024]; // Where the data from the received datagram is stored
            byte[] fileByteArray = new byte[1021]; // Where we store the data to be writen to the file

            // Receive packet and retrieve the data
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.receive(receivedPacket);
            if (!transmitting){
                start = System.currentTimeMillis();
                transmitting = true;
            }
            message = receivedPacket.getData(); // Data to be written to the file

            // Get port and address for sending acknowledgment
            InetAddress address = sender;
            //InetAddress address = receivedPacket.getAddress();
            //int port = receivedPacket.getPort();

            // Retrieve sequence number
            sequenceNumber = message[0];
            //sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            // Check if we reached last datagram (end of file)
            flag = (message[1] & 0xff) == 1;
            
            // If sequence number is the last seen + 1, then it is correct
            // We get the data from the message and write the ack that it has been received correctly
            if (foundLast == 0){
                if (sequenceNumber == 1) {

                    // set the last sequence number to be the one we just received
                    foundLast = sequenceNumber;
    
                    // Retrieve data from message
                    System.arraycopy(message, 3, fileByteArray, 0, 1021);
    
                    // Write the retrieved data to the file and print received data sequence number
                    outToFile.write(fileByteArray);
                    System.out.println("Received: Sequence number:" + foundLast);
    
                    // Send acknowledgement
                    sendAck(foundLast, socket_ack, address, port_ack);
                } else {
                    System.out.println("Expected sequence number: " + 0 + " but received " + sequenceNumber + ". DISCARDING");
                    // Re send the acknowledgement
                    foundLast = 0;
                    sendAck(foundLast, socket_ack, address, port_ack);
                    
                }
            } else{
                if (sequenceNumber == 0) {
                    // set the last sequence number to be the one we just received
                    foundLast = sequenceNumber;
    
                    // Retrieve data from message
                    System.arraycopy(message, 3, fileByteArray, 0, 1021);
    
                    // Write the retrieved data to the file and print received data sequence number
                    outToFile.write(fileByteArray);
                    System.out.println("Received: Sequence number:" + foundLast);
    
                    // Send acknowledgement
                    sendAck(foundLast, socket, address, port_ack);
                } else {
                    System.out.println("Expected sequence number: " + 1 + " but received " + sequenceNumber + ". DISCARDING");
                    // Re send the acknowledgement
                    foundLast = 1;
                    sendAck(foundLast, socket, address, port_ack);
                }
            }
            
            // Check for last datagram
            if (flag) {
                outToFile.close();
                transmitting = false;
                long finish = System.currentTimeMillis();
                long timeElapsed = finish - start;
                long elapsed_seconds = (timeElapsed / 1000);
                System.out.println("Total-transmission time (ms): " + timeElapsed);
                System.out.println("Total-transmission time (s): " + elapsed_seconds);
                break;
            }
        }
    }    
    
    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ackPacket = new byte[1];
        //ackPacket[0] = (byte) (foundLast >> 8);
        ackPacket[0] = (byte) (foundLast);
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + foundLast);
    }
    private static void waitForTest(DatagramSocket socket, DatagramSocket socket_ack){
        byte[] buf = new byte[1024];  
        DatagramPacket dp = new DatagramPacket(buf, 1024);  
        try {
            socket.receive(dp);
            String str = new String(dp.getData(), 0, dp.getLength());  
            System.out.println(str);
    
            String text_back = "ACK";
            dp = new DatagramPacket(text_back.getBytes(), text_back.length());
            dp.setAddress(sender);
            dp.setPort(port_ack);
            socket_ack.send(dp);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
}}
