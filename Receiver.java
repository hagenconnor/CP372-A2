import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Receiver {
    static int port_ack;
    static boolean tested = false;
    public static void main(String[] args){
        String sender = args[0];
        int port_data = Integer.parseInt(args[1]);
        port_ack = Integer.parseInt(args[2]);
        String dest = args[3];

        try{
            DatagramSocket socket = new DatagramSocket(port_data);
            DatagramSocket socket_ack = new DatagramSocket();
                        
            System.out.println("Creating file");
            File f = new File (dest); // Creating the file
            FileOutputStream outToFile = new FileOutputStream(f); // Creating the stream through which we write the file content
            
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
        
        while (true) {
            byte[] message = new byte[1024]; // Where the data from the received datagram is stored
            byte[] fileByteArray = new byte[1021]; // Where we store the data to be writen to the file

            // Receive packet and retrieve the data
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.receive(receivedPacket);
            message = receivedPacket.getData(); // Data to be written to the file

            // Get port and address for sending acknowledgment
            InetAddress address = receivedPacket.getAddress();
            //int port = receivedPacket.getPort();

            // Retrieve sequence number
            sequenceNumber = ((message[0] & 0xff) << 8) + (message[1] & 0xff);
            // Check if we reached last datagram (end of file)
            flag = (message[2] & 0xff) == 1;
            
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
                break;
            }
        }
    }    
    
    private static void sendAck(int foundLast, DatagramSocket socket, InetAddress address, int port) throws IOException {
        // send acknowledgement
        byte[] ackPacket = new byte[2];
        ackPacket[0] = (byte) (foundLast >> 8);
        ackPacket[1] = (byte) (foundLast);
        // the datagram packet to be sent
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement);
        System.out.println("Sent ack: Sequence Number = " + foundLast);
    }
}
