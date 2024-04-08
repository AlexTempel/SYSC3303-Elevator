import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class ElevatorSubsystem implements Runnable {
    private final int elevator_id;
    private final ElevatorDoors doors;
    private final DatagramSocket socket;
    private int ElevatorInfoSocketID;
    private int current_floor;
    private InetAddress schedulerAddress;
    private int schedulerPort = 20002;
    private ElevatorState state;
    private int numPeople = 0;
    private ArrayList<Request> allReqList = new ArrayList<Request>();
    private Boolean upwards = null;
    //private Request currentRequest = null;
    private ArrayList<Request> currReqList = new ArrayList<Request>();

    /**
     * Contruct the ElevatorSubsystem Object
     * @param id integer identifier of the elevator, also the recieve port
     */
    public ElevatorSubsystem(int id, int elevatorInfoPort, InetAddress schedulerAddr) throws SocketException, UnknownHostException {
        this.elevator_id = id;
        this.doors = new ElevatorDoors();
        this.socket = new DatagramSocket(id);
        this.current_floor = 1; //start the Elevator at the ground floor
        this.state = ElevatorState.WAITING;
        this.ElevatorInfoSocketID = elevatorInfoPort;
        this.schedulerAddress = schedulerAddr;

    }

    @Override
    public void run() {
        System.out.println("Starting Elevator");
        state = ElevatorSubsystem.ElevatorState.WAITING;
        System.out.printf("Elevator %d Current State: %s\n", elevator_id, state);
        while (state != ElevatorSubsystem.ElevatorState.BROKEN) {
            // Obtain formatted request data
            if (state != ElevatorState.WAITING) {
                state = ElevatorSubsystem.ElevatorState.WAITING;
                System.out.printf("Elevator %d Current State: %s\n", elevator_id, state);
            }
            try {
                if(currReqList.isEmpty()){
                    if(upwards == null){
                        upwards = true;
                    }else{
                        upwards = !upwards;
                    }
                }

                // Check for Reqs from the scheduler
                getRequests();

                // Continue servicing if a req came in
                if (!allReqList.isEmpty()) {
                    pickRequest(allReqList);

                    // Move only if there is a current req
                    if (!currReqList.isEmpty()) {
                        moveElevator();
                    }
                }
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Collect all the requests waiting on the socket and add to an arraylist
     */
    public void getRequests(){

        DatagramPacket receivePacket = new DatagramPacket(new byte[1024], 1024);

        //receive all the pending requests
        try {
            socket.setSoTimeout(10);
            socket.receive(receivePacket);

        } catch (IOException e) {
            return;
        }

        // Add request to list
        System.out.println("Elevator " + elevator_id + " received request from scheduler");
        allReqList.add(Request.parsePacket(receivePacket));
        getRequests();
    }

    /**
     * Determine which of the Requests to serve
     */
    public void pickRequest(ArrayList<Request> reqList) {
        if (reqList.isEmpty() || currReqList.size() == 5) {
            return;
        } else {
            // Loop through all requests to see the appropriate one to service
            Request tempReq = null;
            int removeIndex = -1;

            for (int i = 0; i < reqList.size(); i++) {
                // Check only for upwards requests
                if (upwards) {
                    // Assign a request to temp req only if it is correct direction
                    if (tempReq == null) {
                        if (reqList.get(i).getStartingFloor() - current_floor >= 0 && reqList.get(i).getDestinationFloor() > reqList.get(i).getStartingFloor()){
                            tempReq = reqList.get(i);
                            removeIndex = i;
                        }
                    }else{
                        // Compare the temp req with the next one in the list
                        int currentDiff = tempReq.getStartingFloor() - current_floor;
                        int newDiff = reqList.get(i).getStartingFloor() - current_floor;

                        // Replace if it is closer and upwards
                        if(newDiff < currentDiff && newDiff > 0 && reqList.get(i).getDestinationFloor() > reqList.get(i).getStartingFloor()){
                            tempReq = reqList.get(i);
                            removeIndex = i;
                        }
                    }
                }else{ //assume downwards
                    // Assign a request to temp req only if it is correct direction
                    if (tempReq == null) {
                        if (reqList.get(i).getStartingFloor() - current_floor <= 0 && reqList.get(i).getDestinationFloor() < reqList.get(i).getStartingFloor()){
                            tempReq = reqList.get(i);
                            removeIndex = i;
                        }
                    }else{
                        // Compare the temp req with the next one in the list
                        int currentDiff = tempReq.getStartingFloor() - current_floor;
                        int newDiff = reqList.get(i).getStartingFloor() - current_floor;

                        // Replace if it is closer and downwards
                        if(newDiff > currentDiff && newDiff < 0 && reqList.get(i).getDestinationFloor() < reqList.get(i).getStartingFloor()){
                            tempReq = reqList.get(i);
                            removeIndex = i;
                        }
                    }
                }
            }
            if (tempReq != null) {
                currReqList.add(tempReq);
            }
            if (removeIndex != -1){
                allReqList.remove(removeIndex);
            }
        }
    }

    /**
     * Move the Elevator one floor and see if anyone is getting off/on
     */
    public void moveElevator() throws InterruptedException, IOException {

        // Max height or ground floor, change direction
        if (current_floor == 22){
            upwards = false;
        }
        if (current_floor == 1){
            upwards = true;
        }

        // Chance to permanently Break
        int breakChance = (int) (Math.random() * 400);
        //System.out.println("This is break chance " + breakChance);
        if (breakChance == 144){
            state = ElevatorSubsystem.ElevatorState.BROKEN;
            System.out.printf("Elevator %d Current State: %s\n",elevator_id, state);
            updateScheduler(true);
            throw new InterruptedException("Elevator is broken");

        }

        // See if anyone is getting on or off
        int numUnloading = 0;
        int numLoading = 0;

        ArrayList<Request> removalObjects = new ArrayList<>();
        for (Request request : currReqList) {
            if (current_floor == request.getStartingFloor()) {
                numLoading  += 1;
            } else if (current_floor == request.getDestinationFloor()){

                numUnloading += 1;

                // Remove the finished request
                request.complete();
                sendConfirmation(request);

                // Take out of current req list
                removalObjects.add(request);

            }
        }

        // Remove the completed trips
        for(Request r: removalObjects){
            currReqList.remove(r);
        }

        System.out.println("Elevator " + elevator_id + " is on floor " + current_floor);

        // Let them on or off
        if (numLoading != 0 || numUnloading != 0){
            System.out.println("Eleavtor " + elevator_id + " is Letting " + numLoading + " People on and " + numUnloading + " People off.");
            Thread.sleep(4000); //Simulate Loading time
            cycleDoors();
            numPeople = numPeople + numLoading - numUnloading;
        }

        System.out.println("Here is how many people are on Elevator " + elevator_id + ": " + numPeople);

        // Move if we have direction and request
        state = ElevatorSubsystem.ElevatorState.MOVING;
        System.out.printf("Elevator %d Current State: %s\n",elevator_id, state);

        if(upwards && !currReqList.isEmpty()){
            System.out.println("Elevator " + elevator_id + " is going up");
            current_floor += 1;
        }else if (!upwards && !currReqList.isEmpty()){
            System.out.println("Elevator " + elevator_id + " is going down");
            current_floor -= 1;
        }

        updateScheduler(false);
        Thread.sleep(11000);
    }

    /**
     * For testing
     * @return
     */
    public ArrayList<Request> getCurrReqList(){
        return currReqList;
    }

    /**
     * For testing
     * @return
     */
    public ArrayList<Request> getAllReqList(){
        return allReqList;
    }

    /**
     * For Testing
     */
    public void setUpwards(){
        upwards = true;
    }

    /**
     * Simulate opening and closing the doors
     * @throws InterruptedException
     */
    private void cycleDoors() throws InterruptedException, IOException {
        boolean isJammed;
        state = ElevatorSubsystem.ElevatorState.DOORS_OPEN;
        System.out.printf("Elevator %d Current State: %s\n",elevator_id, state);
        isJammed = doors.toggleDoors();

        while(isJammed){
            System.out.printf("Elevator " + elevator_id + " doors are jammed! Wait for repair...\n");

            // Tell Scheduler elevator is broken
            updateScheduler(true);
            Thread.sleep(10000);

            // Try again to open doors
            isJammed = doors.toggleDoors();
        }

        updateScheduler(false);

        state = ElevatorSubsystem.ElevatorState.LOADING;
        System.out.printf("Elevator %d Current State: %s\n",elevator_id, state);
        Thread.sleep(2000);

        isJammed = doors.toggleDoors();
        while(isJammed){
            System.out.printf("Elevator " + elevator_id + " doors are jammed! Wait for repair...\n");

            // Tell Scheduler elevator is broken
            updateScheduler(true);
            Thread.sleep(10000);

            // Try again to open doors
            isJammed = doors.toggleDoors();
        }

        state = ElevatorSubsystem.ElevatorState.DOORS_CLOSE;
        System.out.printf("Elevator %d Current State: %s\n",elevator_id, state);
    }

    /**
     * Converts Request to string, sends packet to Scheduelr
     * @param confirmation Request with the completed attribute true
     * @throws IOException
     */
    public void sendConfirmation(Request confirmation) throws IOException {
        String message = confirmation.convertToPacketMessage();
        DatagramPacket sendPacket = new DatagramPacket(message.getBytes(StandardCharsets.UTF_8), message.getBytes().length);
        socket.connect(schedulerAddress, schedulerPort);
        socket.send(sendPacket);
        socket.disconnect();

        System.out.println("Elevator " + elevator_id + " sent complete request");
    }

    /**
     * Send the info packet to the scheduler after every floor
     * @throws IOException
     */
    public void updateScheduler(boolean broken) throws IOException {

        // Skip this is the scheduler has not sent anything
        if (schedulerAddress == null){
            return;
        }

        //create new info packet
        ElevatorInfo info = new ElevatorInfo(current_floor, numPeople, upwards, broken);
        DatagramPacket infoPacket = info.convertToPacket();
        socket.connect(schedulerAddress, ElevatorInfoSocketID);
        socket.send(infoPacket);
        socket.disconnect();
    }

    /**
     * Socket Cleanup
     */
    public void closeSocket(){
        socket.close();
    }

    /**
     * For Testing - set an address and port for scheduler
     * @throws UnknownHostException
     */
    public void setSchedulerAddress() throws UnknownHostException {
        schedulerAddress = InetAddress.getByName("localhost");
        schedulerPort = 156;
    }

    public void setAllReqList(ArrayList<Request> list){
        allReqList = list;
    }

    public enum ElevatorState {

        WAITING,
        MOVING,
        DOORS_OPEN,
        DOORS_CLOSE,
        LOADING,
        BROKEN

    }
}
