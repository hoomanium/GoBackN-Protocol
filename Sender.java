/* 
 *
 * File:   Sender.java
 * Author: Hooman Hejazi
 * Course: CS 456 - Computer Networks
 * Assignment: A2 - Go-Back-N Protocol
 * Created on November 10, 2015, 9:42 AM
 *
 */
import java.io.*;
import java.net.*;
import java.io.PrintWriter;
import java.io.FileInputStream;

/* 
 * Class Description:
 * 
 *   The Sender class the reads data from the specified file and sends it using 
 *   the Go-Back-N protocol to the receiver via the network emulator. 
 *   The window size is set to N=10. After all contents of the file have been transmitted
 *   successfully to the receiver (and corresponding ACKs have been received),  
 *   the sender then sends an EOT packet to the receiver.  
 *   The sender closes its connection and exit only after it has received ACKs for 
 *   all  data packets it has sent and an EOT from the receiver. 
 *   
 *   If the sender has a packet to send, it first checks to see if the window is full, 
 *   that is, whether there are N outstanding, unacknowledged packets. 
 *   If the window is not full, the packet is sent and the appropriate variables are updated. 
 *   A timer is started if it was not done before. 
 *   The sender will use only a single timer that will be set for the oldest transmitted but not yet 
 *   acknowledged packet. If the window is full, the sender will try sending the packet later. 
 *   When the sender receives an acknowledgement packet with sequence number n, the ACK will be taken 
 *   to be a cumulative acknowledgement, indicating that all packets with a sequence number up to 
 *   and including n have been correctly received at the receiver. 
 *   If a timeout occurs, the sender resends all packets that have been previously sent but 
 *   that have not yet been acknowledged. 
 *   If an ACK is received but there are still additional transmitted but yet to be acknowledged 
 *   packets, the timer is restarted. If there are no outstanding packets, the timer is stopped. 
 *
 */

/*
 *
 * Contract:
 *
 * 1.   The Sender class takes four command line arguments:
 *      <host address of the network emulator> which is a String
 *      <UDP port number used by the emulator receive data from the sender> which is an Integer
 *      <UDP port number used by the sender to receive ACKs from the emulator> which is an Integer
 *      <name of the file to be transferred> which is a String;
 *    
 *      For example, the Receiver class can be run over the command line as follows:
 *      java Sender localhost 58110 58113 Test
 *
 * 2.   The Network Emulator and Receiver programm must be running before the Sender is run.
 *
 */

public class Sender {

    /* Global Constants */
    private static final int timeOut = 100;
    private static final Integer windowSize = 10;
    private static final Integer packetSize = 500;
    private static final Integer seqNumModulo = 32;
    private static final String seqLog = "seqnum.log";
    private static final String ackLog = "ack.log";

    /* Global Variables */
    private static String netEmuAddress = null;
    private static Integer netEmuDataPort = 0;
    private static Integer senderAckPort = 0;
    private static String fileName = null;
    private static String fileData = null;

    private static int sendBase = 0;
    private static int seqNumber = 0;
    private static int nextSeqNum = 0;
    private static int packetAckNum = 0;
    private static int lastAck = -1;
    
    private static long startTime = 0;
    private static boolean timerFlag = false;
    
    private static DatagramSocket senderSocket = null;

    private static packet packets[];

    /* Read the data from the supplied file and return a string containing files contents */
    public static String readFromFile(String fName) throws Exception {
        String content = null;
        File file = new File(fName);
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            char[] chars = new char[(int) file.length()];
            reader.read(chars);
            content = new String(chars);
            reader.close();
        } catch (IOException e) {
            System.err.println("ERROR: Unable to read the supplied file <" + fileName + ">");
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return content;
    }

    /* Write the data string to the file supplied by fileName, with two options:
     * Option 0: write to file without End-Of-Line charachter 
     * Option 1: write to file with End-Of-Line charachter 
     */
    public static void writeToFile(String fileName, String data, int option) throws Exception {

        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
            // write to file without End-Of-Line charachter
            if (option == 0) {
                out.print(data);
            } // write to file with End-Of-Line charachter 
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

    /* Convert a data string into a series of UDP packets, assigning a sequence number to each packet */
    public static packet[] packetGenerator(String content) throws Exception {
        /* Calculate the total number of packets to be transmitted */
        int numPackets = (int) Math.ceil((double) content.length() / (double) packetSize);

        packet packets[] = new packet[numPackets];
        packet temp_packet;

        int i = 0;
        while (i < numPackets - 1) {
            temp_packet
                    = packet.createPacket(i % seqNumModulo, content.substring(i * packetSize, (i + 1) * packetSize));
            packets[i] = temp_packet;
            i++;
        }

        /* Last packet containing the last 500 bytes or less of the file */
        temp_packet
                = packet.createPacket(i % seqNumModulo, content.substring(i * packetSize, content.length()));
        packets[i] = temp_packet;

        return packets;
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

    public static void receivePacket() throws Exception {
        byte[] receiveData = new byte[1024];
        int newSendBase = 0;
        try {
            DatagramPacket receivePacket
                    = new DatagramPacket(receiveData, receiveData.length);

            senderSocket.receive(receivePacket);
            packet receivedPacket = packet.parseUDPdata(receivePacket.getData());

            /* Ignore any Socket Timeout Exception as timeouts are handeled elsewhere */
            /* Set the ACK number to the sequence number of the received packet*/
            packetAckNum = receivedPacket.getSeqNum();

            if (packetAckNum != lastAck) {
                if (packetAckNum < sendBase) {
                    //newSendBase = seqNumModulo - sendBase;
                    newSendBase = sendBase % seqNumModulo;
                    sendBase = (sendBase + newSendBase + packetAckNum + 1);
                } else {
                    sendBase = (packetAckNum + 1);
                }
                lastAck = packetAckNum;
            }
            /* Stop the timer */
            if ((sendBase % seqNumModulo) == (nextSeqNum % seqNumModulo)) {
                timerFlag = false;
                startTime = 0;
            } /* Initiate the timer using system timer */ else {
                timerFlag = true;
                startTime = System.currentTimeMillis();
            }
            /* Write ACK numbers into the log file */

            writeToFile(ackLog, Integer.toString(packetAckNum), 1);
        } catch (SocketTimeoutException e) {
        } catch (IOException e) {
            System.err.println("ERROR: Unable to write to " + ackLog);
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("ERROR: Something went wrong while receiving a packet");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void timeOutRetransmission(long currentTime) throws Exception {
        long elapsedTime = currentTime - startTime;

        if (timerFlag && startTime != 0) {
            /* In the event of time out, restart the timer and retransmit N packets */
            if (elapsedTime >= timeOut) {
                startTime = System.currentTimeMillis();
                timerFlag = true;
                for (int i = sendBase; i < nextSeqNum; i++) {
                    packetSend(packets[i], netEmuAddress, netEmuDataPort, senderSocket);
                    try {
                        writeToFile(seqLog, Integer.toString(packets[i].getSeqNum()), 1);
                    } catch (IOException e) {
                        System.err.println("ERROR: Unable to write to " + seqLog);
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {

        /* Validate input arguments */
        if (args.length != 4) {
            System.err.println("ERROR: Sender class requires exactly four arguments as follow:");
            System.err.println("       <host address of the network emulator>");
            System.err.println("       <UDP port number used by the emulator to receive data from the sender>");
            System.err.println("       <UDP port number used by the sender to receive ACKs from the emulator>");
            System.err.println("       <name of the file to be transferred>.");
            System.exit(1);
        }

        try {
            netEmuAddress = args[0];
        } catch (Exception e) {
            System.err.println("ERROR: Invalid input argument for  <host address of the network emulator>.");
            System.exit(1);
        }

        try {
            netEmuDataPort = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.err.println("ERROR: Invalid input argument for <UDP port number used by the emulator to receive data from the sender>.");
            System.exit(1);
        }

        try {
            senderAckPort = Integer.parseInt(args[2]);
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

        if (netEmuDataPort > 65535 || senderAckPort > 65535) {
            System.err.println("ERROR: Out of range port number.");
            System.exit(1);
        }

        System.out.println("Starting to read file: " + fileName);
        /* Read data from the specified file into a byte array */
        String content = readFromFile(fileName);
        /* Store the file content into an array of packets */
        packets = packetGenerator(content);

        try {
            senderSocket = new DatagramSocket(senderAckPort);
        } catch (Exception e) {
            System.err.println("ERROR: Unable to create senderSocket.");
            e.printStackTrace();
            System.exit(1);
        }

        senderSocket.setSoTimeout(timeOut);
        while (true) {
            timeOutRetransmission(System.currentTimeMillis());

            /* Window is not full, so more packets can be transmitted */
            if ((nextSeqNum < (sendBase + windowSize)) && (nextSeqNum < packets.length)) {
                packetSend(packets[nextSeqNum], netEmuAddress, netEmuDataPort, senderSocket);
                try {
                    writeToFile(seqLog, Integer.toString(packets[nextSeqNum].getSeqNum()), 1);
                } catch (Exception e) {
                    System.err.println("ERROR: Unable to write to " + seqLog);
                    e.printStackTrace();
                    System.exit(1);
                }

                if (sendBase == nextSeqNum) {
                    startTime = System.currentTimeMillis();
                    timerFlag = true;
                }
                nextSeqNum++;
            }

            receivePacket();
            
            /* All packets have been transmitted, send an EOT packet and close the connection */
            if (packets.length == sendBase) {
                packetSend(packet.createEOT(nextSeqNum), netEmuAddress, netEmuDataPort, senderSocket);
                senderSocket.close();
                return;
            }
        }

    }
}
