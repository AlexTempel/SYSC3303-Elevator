SYSC 3303 Iteration 5 Group 8

Elevator System 

ElevatorDoor.java: file for the elevator doors, is used for checking if the doors are open or closed, and for opening and closing the door.

ElevatorInfo.java: file for holding the infromation for the elevator, stores the floor number the number of passengers and if the elevator is going upwards or is broken.

ElevatorSubsystem.java: main subsystem for the elevator, contain all the main functionality of the system.

ElevatorSubsystemTest.java: test class for the elevator subsystem, uses unit test to test the functionality of the, requests, for moving the elevator, and for testing a
  Elevator failure 

Request.java
Data structure for the information about requests that are sent between components. It is designed to be converted in packets.
convertToPacketMessage() converts the object to a format for a packet.
parsePacket() converts a packet containing a message created by convertToPacketMessage() into a Request object.
parseString() converts a message created by convertToPacketMessage() into a Request object.
