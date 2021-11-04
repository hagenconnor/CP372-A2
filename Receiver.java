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
        //Get address of sender from args.
        try {
            sender = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        int port_data = Integer.parseInt(args[1]); //Data port for receiving file.
        port_ack = Integer.parseInt(args[2]); //Acknowledgement port -- for sending acks.
        String dest = args[3]; //Destination of local file -- stores the received file.

        try{
            //Set up sockets.
            DatagramSocket socket = new DatagramSocket(port_data);
            DatagramSocket socket_ack = new DatagramSocket();
                        
            System.out.println("Creating local file...");
            //Create file to destination.
            File f = new File (dest); 
            FileOutputStream outToFile = new FileOutputStream(f);
            System.out.println("Waiting for socket test...");
            waitForTest(socket, socket_ack);
            receiveFile(outToFile, socket, socket_ack, port_ack);
        }catch(Exception ex){
            ex.printStackTrace();
            System.exit(1);
        }  
        
    }
    //Private method for receiving a file via a DatagramSocket.
    private static void receiveFile(FileOutputStream outToFile, DatagramSocket socket, DatagramSocket socket_ack, int port_ack) throws IOException {
        System.out.println("Waiting to receive file...");
        boolean flag; // Have we reached end of file
        int sequenceNumber = 0; // Order of sequences
        int foundLast = 1; // The last sequence found
        boolean transmitting = false; //Boolean for packet loss simulation.
        long start = 0; //Timer
        
        while (true) {
            byte[] message = new byte[1024]; // Where the data from the received datagram is stored
            byte[] fileByteArray = new byte[1022]; // Where we store the data to be writen to the file

            // Receive packet and retrieve the data
            DatagramPacket receivedPacket = new DatagramPacket(message, message.length);
            socket.receive(receivedPacket);
            if (!transmitting){
                start = System.currentTimeMillis();
                transmitting = true;
            }
            message = receivedPacket.getData();
            InetAddress address = sender;

            // Retrieve sequence number
            sequenceNumber = message[0];
            // Check if we reached last datagram (end of file)
            flag = (message[1] & 0xff) == 1;
            
            if (foundLast == 0){
                if (sequenceNumber == 1) {

                    // set the last sequence number to be the one we just received
                    foundLast = sequenceNumber;
    
                    // Retrieve data from message
                    System.arraycopy(message, 2, fileByteArray, 0, 1022);
    
                    // Write the retrieved data to the file and print received data sequence number
                    outToFile.write(fileByteArray);
                    System.out.println("Received: Sequence number:" + foundLast);
    
                    // Send acknowledgement
                    sendAck(foundLast, socket_ack, address, port_ack);
                } else {
                    System.out.println("Expected sequence number: " + 1 + " but received " + sequenceNumber + ". Discarding...");
                    // Re send the acknowledgement
                    foundLast = 0;
                    sendAck(foundLast, socket_ack, address, port_ack);
                    
                }
            } else{
                if (sequenceNumber == 0) {
                    foundLast = sequenceNumber;

                    // Retrieve data from message
                    System.arraycopy(message, 2, fileByteArray, 0, 1022);
    
                    // Write retrieved data to the file.
                    outToFile.write(fileByteArray);
                    System.out.println("Received: Sequence number:" + foundLast);
    
                    // Send ack
                    sendAck(foundLast, socket, address, port_ack);
                } else {
                    System.out.println("Expected sequence number: " + 0 + " but received " + sequenceNumber + ". Discarding...");
                    //Wrong sequence number. Re send the ack
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
        //Send ack.
        byte[] ackPacket = new byte[1];
        ackPacket[0] = (byte) (foundLast); //Create ack of size 1.
        DatagramPacket acknowledgement = new DatagramPacket(ackPacket, ackPacket.length, address, port);
        socket.send(acknowledgement); //Send acknowledgement.
        System.out.println("Sent ack: Sequence Number: " + foundLast);
    }

    //Private method to test data and acknowledgement sockets.
    //Must pass test before the receiver can receive data. 
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
