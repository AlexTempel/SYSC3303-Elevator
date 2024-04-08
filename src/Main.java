import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) {
        int schedulerInfoPort = 20001;

        try {
            int elevator1PortNumber = 15001;
            ElevatorSubsystem elevator1Subsystem = new ElevatorSubsystem(elevator1PortNumber, schedulerInfoPort, InetAddress.getByName("localhost"));
            Thread elevator1Thread = new Thread(elevator1Subsystem);
            elevator1Thread.start();

            int elevator2PortNumber = 15002;
            ElevatorSubsystem elevator2Subsystem = new ElevatorSubsystem(elevator2PortNumber, schedulerInfoPort, InetAddress.getByName("localhost"));
            Thread elevator2Thread = new Thread(elevator2Subsystem);
            elevator2Thread.start();

            int elevator3PortNumber = 15003;
            ElevatorSubsystem elevator3Subsystem = new ElevatorSubsystem(elevator3PortNumber, schedulerInfoPort, InetAddress.getByName("localhost"));
            Thread elevator3Thread = new Thread(elevator3Subsystem);
            elevator3Thread.start();

            int elevator4PortNumber = 15004;
            ElevatorSubsystem elevator4Subsystem = new ElevatorSubsystem(elevator4PortNumber, schedulerInfoPort, InetAddress.getByName("localhost"));
            Thread elevator4Thread = new Thread(elevator4Subsystem);
            elevator4Thread.start();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}