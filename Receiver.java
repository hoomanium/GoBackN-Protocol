/* 
 *
 * File:   Receiver.java
 * Author: Hooman Hejazi
 * Created on November 10, 2015, 9:42 AM
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.io.PrintWriter;

/* 
 * Class Description:
 * 
 *   The Receiver class receives the packets sent by the Sender class via the Network Emulator. 
 *   Upon receiving the packets, it checks the sequence number of the packet. 
 *   If the sequence number is the one that it is expecting, it sends an ACK packet back to the 
 *   sender with the sequence number equal to the sequence number of the received packet. 
 *   In all other cases, it discards the received packet and resends an ACK packet for the most 
 *   recently received inorder packet;    
 *   After  the  receiver  has  received  all  data  packets  and  an  EOT  from  the  sender,  
 *   it  sends  an  EOT  packet then exit. 
 *   The  receiver  program  generates  a  log  file, namely  arrival.log
 *   The file arrival.log contains the sequence numbers of all the data packets that 
 *   the receiver receives during the entire period of transmission, in form of a single number per line. 
 *
 */

/*
 *
 * Contract:
 *
 * 1.   The Receiver class takes four command line arguments:
 *      <host address of the network emulator> which is a String
 *      <UDP port number used by the emulator to receive ACKs from the receiver> which is an Integer
 *      <UDP port number used by the receiver to receive data from the emulator> which is an Integer
 *      <name of the file into which the received data is written> which is a String;
 *    
 *      For example, the Receiver class can be run over the command line as follows:
 *      java Receiver localhost 57112 57111 OutputFile
 *
 * 2.   The Network Emulator  must be running before the Receiver is run.
 *
 */

public class Receiver {

    /* Global Constants */
    private static final int timeOut = 100;
    private static final Integer windowSize = 10;
    private static final Integer packetSize = 500;
    private static final Integer seqNumModulo = 32;
    private static final String logFile = "arrival.log";

    /* Global Variables */
    private static String netEmuAddress = null;
    private static Integer netEmuACKPort = 0;
    private static Integer receiverPort = 0;
    private static String fileName = null;

    private static Integer expectedSeqNum = 0;
    private static Integer packetSeqNum = 0;
    private static Integer previousPacket = -1;
    private static packet ackPacket = null;

    private static DatagramSocket receiverSocket = null;

    /* Write the data string to the file supplied by fileName, with two options:
     * Option 0: write to the file without an End-Of-Line charachter 
     * Option 1: write to the file with an End-Of-Line charachter 
     */
    public static void writeToFile(String fileName, String data, int option) throws Exception {

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
            /* Write to the file without an End-Of-Line charachter */
            if (option == 0) {
                out.print(data);
            } 
            /* Write to the file with an End-Of-Line charachter */
            else {
                out.println(data);
            }
            out.close();
        } catch (Exception e) {
            System.err.println("ERROR: Unable to write to the file <" + fileName + ">");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* Send UDP packet to the specified hostAddress and port number via the given datagramsocket */
    public static void packetSend(packet p, String hostAddress, Integer port, DatagramSocket socket) throws Exception {
        byte[] sendData = p.getUDPdata();
        InetAddress IPAddress = null;
        /* Extract the IP address of the Network Emulator from hostAddress */
        try {
            IPAddress = InetAddress.getByName(netEmuAddress);
        } catch (Exception e) {
            System.err.println("ERROR: Unable to obtain the IP address of the Network Emulator.");
            e.printStackTrace();
            System.exit(1);
        }
        
        DatagramPacket sendPacket
                = new DatagramPacket(sendData, sendData.length, IPAddress, port);

        try {
            socket.send(sendPacket);
        } catch (Exception e) {
            System.err.println("ERROR: Unable to send the UDP Datagram.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) throws Exception {
       
        /* Validate input arguments */
        if (args.length != 4) {
            System.err.println("ERROR: Receiver class requires exactly four arguments as follow:");
            System.err.println("       <host address of the network emulator>");
            System.err.println("       <UDP port number used by the emulator to receive ACKs from the receiver>");
            System.err.println("       <UDP port number used by the receiver to receive data from the emulator>");
            System.err.println("       <name of the file into which the received data is written>.");
            System.exit(1);
        }
        try {
            netEmuAddress = args[0];
        } catch (Exception e) {
            System.err.println("ERROR: Invalid input argument for  <host address of the network emulator>.");
            System.exit(1);
        }
        try {
            netEmuACKPort = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("ERROR: Invalid input argument for <UDP port number used by the emulator to receive data from the sender>.");
            System.exit(1);
        }
        try {
            receiverPort = Integer.parseInt(args[2]);
        } catch (Exception e) {
            System.err.println("ERROR: Invalid input argument for <UDP port number used by the sender to receive ACKs from the emulator>.");
            System.exit(1);
        }
        try {
            fileName = args[3];
        } catch (Exception e) {
            System.err.println("ERROR: Invalid input argument for  <name of the file to be transferred>.");
            System.exit(1);
        }
        if (netEmuACKPort > 65535 || receiverPort > 65535) {
            System.err.println("ERROR: Out of range port number.");
            System.exit(1);
        }
        /* Create a receiverSocket with the supplied receiverPort */
        try {
            receiverSocket = new DatagramSocket(receiverPort);
        } catch (Exception e) {
            System.err.println("ERROR: Unable to create receiverSocket.");
            e.printStackTrace();
            System.exit(1);
        }

        while (true) {
            byte[] receiveData = new byte[1024];
            
            DatagramPacket receivePacket
                    = new DatagramPacket(receiveData, receiveData.length);

            receiverSocket.receive(receivePacket);
            
            /* Parse the contents of the received packet and store it into a temporary  */
            packet receiveTemp = packet.parseUDPdata(receivePacket.getData());
            
            /* A data packet (packet type 1) has been received */
            if (receiveTemp.getType() == 1) {
                
                /* Write the sequence number of arriving packets into the logFile */
                writeToFile(logFile, Integer.toString(receiveTemp.getSeqNum()), 1);
                
                if (receiveTemp.getSeqNum() == expectedSeqNum) {
                    previousPacket = receiveTemp.getSeqNum();
                    ackPacket = packet.createACK(previousPacket);
                    packetSend(ackPacket, netEmuAddress, netEmuACKPort, receiverSocket);
                    writeToFile(fileName, (new String(receiveTemp.getData())), 0);
                    expectedSeqNum = (expectedSeqNum + 1) % seqNumModulo;
                } else if (previousPacket > -1) {
                    ackPacket = packet.createACK(previousPacket);
                    packetSend(ackPacket, netEmuAddress, netEmuACKPort, receiverSocket);
                }
                
            /* An EOT packet (packet type 2) has been received */
            } else if (receiveTemp.getType() == 2) {
                /* Create an EOT packet */
                ackPacket = packet.createEOT(previousPacket);
                
                /* Wait for 1 second to account for future sender packets
                   and ensure all ACKs have been delivered */
                Thread.sleep(1000);
                
                packetSend(ackPacket, netEmuAddress, netEmuACKPort, receiverSocket);
                receiverSocket.close();
                return;
            }
        }
    }

}
