import javax.swing.*;
import javax.xml.crypto.Data;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Sender {

    static DatagramSocket socket_ack;
    static DatagramSocket socket_data;
    public static void main(String args[]){
        JFrame frame = new JFrame("BoardClient");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000,150);
        
        //Connection panel.
        JPanel connection = new JPanel();
        JButton alive_button = new JButton("Is Alive?");
        JTextField server_ip = new JTextField();
        server_ip.setPreferredSize( new Dimension( 200, 24 ) );
        JLabel ip_label = new JLabel("IP Address: ");
        JTextField ACK_port = new JTextField();
        ACK_port.setPreferredSize( new Dimension( 200, 24 ) );
        JLabel port_label = new JLabel("ACK Port: ");
        JTextField data_port = new JTextField();
        data_port.setPreferredSize( new Dimension( 200, 24 ) );
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

        alive_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed (ActionEvent e){
                String receiver_ip = server_ip.getText().toString();
                int receiver_ackPort = Integer.parseInt(ACK_port.getText());
                int receiver_dataPort = Integer.parseInt(data_port.getText());
                Boolean status = test_connection(receiver_dataPort, receiver_ackPort, receiver_ip);
                if (status){
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

        private static Boolean test_connection(int port_data, int port_ack, String ip){
            Boolean connected = true;
            InetAddress address_ack = null;
            try {
                address_ack = InetAddress.getByName(ip);
                socket_ack = new DatagramSocket(port_ack, address_ack);
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




            return connected;
        }
}
