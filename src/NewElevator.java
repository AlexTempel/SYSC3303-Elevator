import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class NewElevator implements Runnable {
    private final int requestPort;
    private final int infoPort;
    private final DatagramSocket elevatorSocket;
    private InetAddress schedulerAddress;
    private final ArrayList<Request> requestList;
    private final ArrayList<Request> currentRequestList;
    private int floor;
    private boolean upwards;
    private int numberOfPassengers;
    private boolean broken;

    NewElevator(int requestPort, int infoPort) throws SocketException {
        this.requestPort = requestPort;
        this.elevatorSocket = new DatagramSocket(requestPort);
        this.infoPort = infoPort;
        requestList = new ArrayList<>();
        currentRequestList = new ArrayList<>();
        floor = 1;
        upwards = true;
        numberOfPassengers = 0;
        broken = false;
    }

    public void run() {
        try {
            checkForRequests();
            elevatorSocket.setSoTimeout(10);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkForRequests() throws IOException {
        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);
        try {
            elevatorSocket.receive(receivePacket);
        } catch (SocketTimeoutException e ) {
            return;
        }
        if (schedulerAddress == null) {
            schedulerAddress = receivePacket.getAddress();
            requestList.add(Request.parsePacket(receivePacket));
            return;
        }
        requestList.add(Request.parsePacket(receivePacket));
        checkForRequests();
    }

}
