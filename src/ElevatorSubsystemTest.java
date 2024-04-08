import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ElevatorSubsystemTest {

    @Test
    void getRequests() throws IOException {

        // Create elevator object and request
        ElevatorSubsystem testElevator = new ElevatorSubsystem(19505, 101, InetAddress.getByName("localhost"));
        testElevator.setSchedulerAddress();

        // Load a bunch of request packets on the elevator socket
        Request r1 = new Request(1, 4, 10);
        Request r2 = new Request(2, 3, 10);
        Request r3 = new Request(3, 2, 10);

        //Create a dummy socket
        String message = r1.convertToPacketMessage();
        String message2 = r2.convertToPacketMessage();
        String message3 = r3.convertToPacketMessage();
        InetAddress elevatorAddress = InetAddress.getByName("127.0.0.1");
        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.getBytes().length, elevatorAddress,19505);
        DatagramPacket sendPacket2 = new DatagramPacket(message2.getBytes(StandardCharsets.UTF_8), message2.getBytes().length, elevatorAddress,19505);
        DatagramPacket sendPacket3 = new DatagramPacket(message3.getBytes(StandardCharsets.UTF_8), message3.getBytes().length, elevatorAddress,19505);
        DatagramSocket mySocket = new DatagramSocket(19506);

        // Send to Elevator socket
        mySocket.send(sendPacket);
        mySocket.send(sendPacket2);
        mySocket.send(sendPacket3);

        testElevator.getRequests();

        ArrayList<Request> list1 = testElevator.getAllReqList();

        for(int i = 0; i < list1.size(); i++){
            int id = list1.get(i).getRequestID();
            System.out.println("id for element " + i + ": " + id);
        }

        testElevator.closeSocket();
        mySocket.close();

    }

    @Test
    void pickRequest() throws SocketException, UnknownHostException {

        ElevatorSubsystem e = new ElevatorSubsystem(1, 5, InetAddress.getByName("localhost"));

        Request r1 = new Request(1, 4, 10);
        Request r2 = new Request(2, 3, 10);
        Request r3 = new Request(3, 2, 10);
        Request r4 = new Request(4, 5, 10);
        Request r5 = new Request(5, 7, 10);
        Request r6 = new Request(6, 6, 10);

        ArrayList<Request> list = new ArrayList<Request>();

        list.add(r1);
        list.add(r2);
        list.add(r3);
        list.add(r4);
        list.add(r5);
        list.add(r6);

        e.setAllReqList(list);
        e.setUpwards();
        e.pickRequest(list);
        ArrayList<Request> newList = e.getCurrReqList();

        for(int i = 0; i < newList.size(); i++){
            int id = newList.get(i).getRequestID();
            System.out.println("id for element " + i + ": " + id);
        }

        e.closeSocket();
        assertEquals(newList.get(0).getRequestID(),3);

    }

    @Test
    void moveElevator() throws IOException, InterruptedException {
        ElevatorSubsystem e1 = new ElevatorSubsystem(    19507, 19509, InetAddress.getByName("localhost"));

        DatagramSocket newSocket = new DatagramSocket(19509);

        Request r1 = new Request(1, 1, 2);
        Request r2 = new Request(2, 3, 10);
        Request r3 = new Request(3, 2, 10);
        Request r4 = new Request(4, 5, 10);
        Request r5 = new Request(5, 7, 10);
        Request r6 = new Request(6, 6, 10);

        ArrayList<Request> list = new ArrayList<Request>();

        list.add(r1);
        list.add(r2);
        list.add(r3);
        list.add(r4);
        list.add(r5);
        list.add(r6);

        e1.setAllReqList(list);
        e1.setUpwards();
        e1.pickRequest(list);

        //set address for scheduler
        e1.setSchedulerAddress();

        e1.moveElevator(); // Start on floor 1 go to 2
        e1.moveElevator(); // Actions at floor 2

        e1.closeSocket();
        newSocket.close();
    }

    @Test
    void moveElevatorUntilFailure() throws IOException{
        ElevatorSubsystem e1 = new ElevatorSubsystem(    19510, 19511, InetAddress.getByName("localhost"));

        DatagramSocket newSocket = new DatagramSocket(19511);

        e1.setUpwards();

        //set address for scheduler
        e1.setSchedulerAddress();

        int i = 0;
        while(i < 4000){
            try {
                e1.moveElevator();
            } catch (InterruptedException e) {
                assertTrue(true);
                break;
            }
        }

        e1.closeSocket();
        newSocket.close();
    }
}