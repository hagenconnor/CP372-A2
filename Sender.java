import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Sender {

    static DatagramSocket socket_ack;
    static DatagramSocket socket_data;
    static InetAddress dest_address;
    static int socketTimeout;
    static int packet_count = 0;
    static JTextArea results;
    static Boolean connection_tested = false;
    static Boolean reliable = true;
    public static void main(String args[]){
        JFrame frame = new JFrame("Sender");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000,150);
        
        //Connection panel.
        JPanel connection = new JPanel();
        JButton alive_button = new JButton("Is Alive?");
        JTextField server_ip = new JTextField();
        server_ip.setPreferredSize(new Dimension( 200, 24 ));
        JLabel ip_label = new JLabel("IP Address: ");
        JTextField ACK_port = new JTextField();
        ACK_port.setPreferredSize(new Dimension( 200, 24 ));
        JLabel port_label = new JLabel("ACK Port: ");
        JTextField data_port = new JTextField();
        data_port.setPreferredSize(new Dimension( 200, 24 ));
        JLabel data_port_label = new JLabel("Data Port: ");
    
        //Transfer panel.
        JPanel transfer = new JPanel();
        JLabel file_label = new JLabel("File to transfer: ");
        JTextArea file_name = new JTextArea();
        file_name.setPreferredSize( new Dimension(200,24));
        JLabel results_label = new JLabel("Current sent in-order packets: ");
        results = new JTextArea();
        results.setPreferredSize( new Dimension(200,24) );
        results.setLineWrap(true);
        results.setEditable(false);
    
        
        //Input panel.
        JPanel input_panel = new JPanel();
        JLabel timeout_label = new JLabel("Timeout: ");
        JTextField timeout = new JTextField();
        timeout.setPreferredSize( new Dimension( 200, 24 ) );
        JButton send_button = new JButton("SEND");
        JCheckBox reliable_toggle = new JCheckBox("Reliable", true);

        

        send_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e){
                if (!connection_tested){
                    JOptionPane.showMessageDialog(frame, "Please test your connection before sending a file.");
                } else{
                    try{
                        DatagramSocket socket = new DatagramSocket();
                        String receiverAddress = server_ip.getText().toString();
                        int port_data = Integer.parseInt(data_port.getText());
                        dest_address = InetAddress.getByName(receiverAddress);

                        int port_ack = Integer.parseInt(ACK_port.getText());
                        DatagramSocket socket_ack = new DatagramSocket(port_ack);
                        socketTimeout = Integer.parseInt(timeout.getText());


                        File file = new File(file_name.getText().toString());
                        byte[] fileArray = readFileToByteArray(file);
                        sendFile(reliable, socket, socket_ack, fileArray, dest_address, port_data);
                        JOptionPane.showMessageDialog(frame, "File sent.");
                        connection_tested = false;
                        socket.close();
                        socket_ack.close();
                    } catch (Exception v){
                        v.printStackTrace();
                    }
                }
            }
        });

        reliable_toggle.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
               if (e.getStateChange() == ItemEvent.DESELECTED){
                reliable = false;
               } else{
                reliable = true;
               }
            }
         });

         alive_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e){
                String receiver_ip = server_ip.getText().toString();
                int receiver_ackPort = Integer.parseInt(ACK_port.getText());
                int receiver_dataPort = Integer.parseInt(data_port.getText());
                Boolean status = test_connection(receiver_dataPort, receiver_ackPort, receiver_ip);
                if (status){
                    connection_tested = true;
                    JOptionPane.showMessageDialog(frame, "Connected successfully. You may now send a file.");
                }
                else{
                    JOptionPane.showMessageDialog(frame, "Unable to find host. Please try again.");
                }
        
            }
        });
    
    
    
        //Connection panel.
        connection.add(alive_button);
        connection.add(ip_label);
        connection.add(server_ip);
        connection.add(port_label);
        connection.add(ACK_port);
        connection.add(data_port_label);
        connection.add(data_port);
    
        //Results panel
        transfer.add(file_label);
        transfer.add(file_name);
        transfer.add(results_label);
        transfer.add(results);
    
        //Command panel
        input_panel.add(timeout_label);
        input_panel.add(timeout);
        input_panel.add(send_button);
        input_panel.add(reliable_toggle);
        
        //Add panels to frame.
        frame.getContentPane().add(BorderLayout.NORTH,connection);
        frame.getContentPane().add(BorderLayout.CENTER,transfer);
        frame.getContentPane().add(BorderLayout.SOUTH, input_panel);
    
        frame.setVisible(true);
        
        }

        private static void sendFile(Boolean reliable, DatagramSocket socket_data, DatagramSocket socket_ack, byte[] fileByteArray, InetAddress address, int port) throws IOException {
            int sequenceNumber = 0; //Sequence number, alternates between 0 and 1.
            boolean EOTflag; //Flag for end of transmission.
            int ackSequence = 0; //Used for checking ack against sequence number.
            int dropPacket = 0; //Counter used for unreliable simulation.
            packet_count = 0;
    
            for (int i = 0; i < fileByteArray.length; i = i + 1022) {
                //Increase packet count and simulation counters.
                packet_count+=1;
                dropPacket += 1;
                

                // Create message of size 1024 bytes - first 2 bytes are for sequence number and EOT flag.
                byte[] message = new byte[1024];
                message[0] = (byte) (sequenceNumber); //Add sequence number.
                
                //Check if reached end of file.
                if ((i + 1022) >= fileByteArray.length) {
                    EOTflag = true;
                    message[1] = (byte) (1);
                } else {
                    EOTflag = false;
                    message[1] = (byte) (0);
                }
    
                if (!EOTflag) {
                    System.arraycopy(fileByteArray, i, message, 3, 1021);
                } else {
                    System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
                    System.out.println("-----Last Transmission.-----");
                }

                DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port); 

                if ((!reliable) & dropPacket != 10){
                    //Unreliable transmission simulation -- drop every 10th packet.
                    socket_data.send(sendPacket);
                    System.out.println("Sent: Sequence number: " + sequenceNumber);
                } else if (!(reliable) & dropPacket == 10){
                    //Unreliable transmission simulation -- drop every 10th packet.
                    dropPacket = 0;
                    System.out.println("Dropped: Sequence number: " + sequenceNumber);
                } else if (reliable){
                    //Reliable transmission -- don't simulate packet loss.
                    socket_data.send(sendPacket);
                    System.out.println("Sent: Sequence number: " + sequenceNumber);

                }

                boolean ACKreceived;
    
                while (true) {
                    // Create a packet for datagram ack
                    byte[] ack = new byte[2]; 
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
    
                    try {
                        //Timeout for ACK; set by GUI.
                        socket_ack.setSoTimeout(socketTimeout);
                        socket_ack.receive(ackPacket); //Receive on ack socket.
                        //Extract sequence number.
                        ackSequence = ack[0];
                        ACKreceived = true;
                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket timed out. No ack received.");
                        ACKreceived = false;
                    }
    
                    //If all is correct, break to send next packet, otherwise resend.
                    if ((ackSequence == sequenceNumber) && (ACKreceived)) {
                        System.out.println("Ack received. Sequence Number: " + ackSequence);
                        break;
                    }
                    else {
                        packet_count+=1;
                        socket_data.send(sendPacket); //Resend packet via data socket.
                        System.out.println("Resending packet: Sequence Number: " + sequenceNumber);
                    }
                }
                results.setText(Integer.toString(packet_count));
                //Alternate sequence number.
                if (sequenceNumber == 0){
                    sequenceNumber = 1;
                } else{
                    sequenceNumber = 0;
                }
            }
        }
        //Private helper method to read a file into a byte array for sending datagrams.
        private static byte[] readFileToByteArray(File file) {
            FileInputStream fis = null;
            byte[] array = new byte[(int) file.length()];
            try {
                fis = new FileInputStream(file);
                fis.read(array);
                fis.close();
    
            } catch (IOException e) {
                e.printStackTrace();
            }
            return array;
        }

//Method to test if a host is alive. Sends a ping to the receiver and waits for an acknowledgement.
 private static Boolean test_connection(int port_data, int port_ack, String ip){
    Boolean connected = true;
    InetAddress address_ack = null;
    try {
        address_ack = InetAddress.getByName(ip);
        socket_ack = new DatagramSocket(port_ack);
        socket_ack.setSoTimeout(10000);
    } catch (UnknownHostException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        connected = false;
    } catch (SocketException s){
        s.printStackTrace();
        connected = false;
    }

    String test_string = "Test connection to receiver.";
    DatagramPacket dp = new DatagramPacket(test_string.getBytes(), test_string.length());
    dp.setAddress(address_ack);
    dp.setPort(port_data);

    try {
        socket_ack.send(dp);
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }  
    try {
        byte[] buf = new byte[1024];  
        dp = new DatagramPacket(buf, 1024);  
        socket_ack.receive(dp);
        String str = new String(dp.getData(), 0, dp.getLength());  
        System.out.println(str);
        if (str.equals("ACK")){
            connected = true;
            socket_ack.close();
        }
        else{
            connected = false;
        }
    } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        connected = false;
    }  

    return connected;
}
}
