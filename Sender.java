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
    static Boolean connection_tested = false;
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
        JTextArea results = new JTextArea();
        results.setPreferredSize( new Dimension(200,24) );
        results.setLineWrap(true);
        results.setEditable(false);
    
        
        //Input panel.
        JPanel input_panel = new JPanel();
        JLabel timeout_label = new JLabel("Timeout: ");
        JTextField timeout = new JTextField();
        timeout.setPreferredSize( new Dimension( 200, 24 ) );
        JButton send_button = new JButton("SEND");
        JCheckBox reliable_toggle = new JCheckBox("Reliable");


        send_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e){
                if (connection_tested){ //Fix this.
                    JOptionPane.showMessageDialog(frame, "Please test your connection before sending a file.");
                } else{
                    try{
                        socket_data = new DatagramSocket();

                        socket_ack = new DatagramSocket(Integer.parseInt(ACK_port.getText()), dest_address);

                        File f_name = new File(file_name.getText().toString());
                        byte[] fileByteArray = readFileToByteArray(f_name);
                        InetAddress add = InetAddress.getByName(server_ip.getText().toString());
                        int port = Integer.parseInt(data_port.getText());
                        sendFile(socket_data, socket_ack, fileByteArray, add, port);
                    } catch (Exception v){
                        v.printStackTrace();
                    }
                    //Add code here for sending file.

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

        private static void sendFile(DatagramSocket socket, DatagramSocket socket_ack, byte[] fileByteArray, InetAddress address, int port) throws IOException {
            System.out.println("Sending file");
            int sequenceNumber = 1;
            boolean flag; //Determine if EOT has been reached.
            int ackSequence = 0; 
    
            for (int i = 0; i < fileByteArray.length; i = i + 1021) {
                if (sequenceNumber == 0){
                    sequenceNumber = 1;
                } else{
                    sequenceNumber = 0;
                }

                byte[] message = new byte[1024];
                //message[0] = (byte) (sequenceNumber >> 8);
                message[0] = (byte) (sequenceNumber); //Sequence number.
    
                if ((i + 1021) >= fileByteArray.length) {
                    flag = true;
                    message[1] = (byte) (1); // Embed flag indicating EOT.
                } else {
                    flag = false;
                    message[1] = (byte) (0); // No EOT flag yet.
                }
    
                if (!flag) {
                    System.arraycopy(fileByteArray, i, message, 3, 1021);
                } else {
                    //Last datagram.
                    System.arraycopy(fileByteArray, i, message, 3, fileByteArray.length - i);
                }
    
                DatagramPacket sendPacket = new DatagramPacket(message, message.length, address, port);
                System.out.println("Destination address: " + address);
                System.out.println("Destination port: " + port);
                socket.send(sendPacket); // Sending the data
                System.out.println("Sent: Sequence number = " + sequenceNumber);
    
                boolean ackRec;
    
                while (true) {
                    byte[] ack = new byte[2]; // Create another packet for datagram ACK
                    DatagramPacket ackpacket = new DatagramPacket(ack, ack.length);
    
                    try {
                        socket_ack.setSoTimeout(50);
                        socket_ack.receive(ackpacket);
                        ackSequence = (ack[0]);
                        ackRec = true;
                    } catch (SocketTimeoutException e) {
                        //ACK not received.
                        System.out.println("Socket timed out waiting for ack");
                        ackRec = false; 
                    }
    
                    // If the package was received correctly next packet can be sent
                    if ((ackSequence == sequenceNumber) && (ackRec)) {
                        System.out.println("Ack received: Sequence Number = " + ackSequence);
                        break;
                    } // Package was not received, so we resend it
                    else {
                        socket.send(sendPacket);
                        System.out.println("Resending: Sequence Number = " + sequenceNumber);
                    }
                }
            }
        }

        private static byte[] readFileToByteArray(File file) {
            FileInputStream fis = null;
            // Creating a byte array using the length of the file
            // file.length returns long which is cast to int
            byte[] bArray = new byte[(int) file.length()];
            try {
                fis = new FileInputStream(file);
                fis.read(bArray);
                fis.close();
    
            } catch (IOException ioExp) {
                ioExp.printStackTrace();
            }
            return bArray;
        }
}
