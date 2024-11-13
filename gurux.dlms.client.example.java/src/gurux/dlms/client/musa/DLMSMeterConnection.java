package gurux.dlms.client.musa;

import gurux.common.enums.TraceLevel;
import gurux.dlms.GXDLMSException;
import gurux.dlms.client.GXDLMSReader;
import gurux.dlms.client.GXDLMSSecureClient2;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.Conformance;
import gurux.dlms.enums.InterfaceType;
import gurux.dlms.enums.ObjectType;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSObjectCollection;
import gurux.net.GXNet;
import gurux.net.enums.NetworkType;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DLMSMeterConnection {

    private static final String IP_ADDRESS = "172.16.8.248";  // Set your meter's IP here
    private static final int PORT = 5258;  // Set the communication port here
    private static final String PASSWORD = "UwsaOZy3";
//    NFVGELYt UwsaOZy3
    private static final Authentication AUTH_LEVEL = Authentication.LOW;  // Adjust as per your meter settings

    public static void connectAndReadMeter() {
        GXNet connection = new GXNet(NetworkType.TCP, IP_ADDRESS, PORT);
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);

        try {
            // Modified client configuration
            client.setClientAddress(1);
            client.setServerAddress(1);
            client.setInterfaceType(InterfaceType.HDLC);
            client.setAuthentication(AUTH_LEVEL);
            client.setPassword(PASSWORD.getBytes());

            // Add necessary conformance
            client.getProposedConformance().add(Conformance.GENERAL_PROTECTION);
            client.getProposedConformance().add(Conformance.SELECTIVE_ACCESS);
            client.getProposedConformance().add(Conformance.GET);

            // Set trace level for detailed debugging information
            TraceLevel traceLevel = TraceLevel.VERBOSE;

            // Initialize GXDLMSReader
            String frameCounter = "frameCounterIdentifier"; // Example frame counter
            GXDLMSReader reader = new GXDLMSReader(client, connection, traceLevel, frameCounter);


            // Open connection to the meter
            connection.open();

            // Initialize the connection to retrieve association view.
            System.out.println("Initializing connection...");
            reader.initializeConnection();

            // Get Association View explicitly
            System.out.println("Getting Association View...");
            reader.getAssociationView();


            // Retrieve objects from the meter
            System.out.println("Retrieving objects from the meter...");
            GXDLMSObjectCollection objects = client.getObjects();

            // Check if objects are retrieved
            if (objects == null || objects.isEmpty()) {
                System.out.println("No objects found in the meter.");
                return;
            }

            // Display all objects retrieved
            System.out.println("Objects retrieved from the meter:");
            for (GXDLMSObject obj : objects) {
                System.out.println("Object: " + obj.getLogicalName());
            }

            // Try reading the power register with the Microstar OBIS code format
            System.out.println("Reading power register...");
            GXDLMSObject powerObject = client.getObjects().findByLN(ObjectType.REGISTER, "0.0.0.2.0.255");
            System.out.println("PowerObject" + powerObject);

            if (powerObject != null) {
                Object result = reader.read(powerObject, 2);  // Attribute 2 typically contains the value
                System.out.println("Power Value: " + result);
            } else {
                System.out.println("Power register not found. Dumping all available objects:");
                for (GXDLMSObject obj : client.getObjects()) {
                    System.out.println("Found object: " + obj.getLogicalName() + " Type: " + obj.getObjectType());
                }
            }


            System.out.println("Debug - Conformance: " + client.getNegotiatedConformance());
            System.out.println("Debug - Authentication level: " + client.getAuthentication());
            System.out.println("Debug - Number of objects: " + client.getObjects().size());


            // Search for active energy OBIS code
            GXDLMSObject activeEnergyObject = null;
            for (GXDLMSObject obj : objects) {
                if ("1.0.0.0.1.255".equals(obj.getLogicalName())) {  // Match exact OBIS code format
                    activeEnergyObject = obj;
                    break;
                }
            }

            if (activeEnergyObject != null) {
                Object result = reader.read(activeEnergyObject, 2);
                System.out.println("Active Energy: " + result);
            } else {
                System.out.println("Specified OBIS code not found in the meter.");
            }

        } catch (UnknownHostException e) {
            System.err.println("Error: Unable to resolve host: " + IP_ADDRESS);
            e.printStackTrace();
        } catch (GXDLMSException e) {
            System.err.println("DLMS communication error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // Close connections safely in finally block
                System.out.println("Closing connection...");
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        connectAndReadMeter();
    }

    public static void connectAndReadMeter1() {
        GXNet connection = new GXNet(NetworkType.TCP, IP_ADDRESS, PORT);
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);

        try {
            // Basic client configuration
            client.setClientAddress(1);
            client.setServerAddress(1);
            client.setInterfaceType(InterfaceType.HDLC);
            client.setAuthentication(AUTH_LEVEL);
            client.setPassword(PASSWORD.getBytes());

            // Add necessary conformance
            client.getProposedConformance().add(Conformance.GENERAL_PROTECTION);
            client.getProposedConformance().add(Conformance.SELECTIVE_ACCESS);
            client.getProposedConformance().add(Conformance.GET);

            TraceLevel traceLevel = TraceLevel.VERBOSE;
            GXDLMSReader reader = new GXDLMSReader(client, connection, traceLevel, null);

            // Open connection and initialize
            connection.open();
            System.out.println("Initializing connection...");
            reader.initializeConnection();
            reader.getAssociationView();

            // Read specific registers
            System.out.println("\nReading meter registers:");

            // Common OBIS codes for electrical measurements
            String[][] obisToRead = {
                    {"0.0.1.0.0.255", "Clock"}
            };

            for (String[] obisCode : obisToRead) {
                try {
                    GXDLMSObject obj = client.getObjects().findByLN(ObjectType.NONE, obisCode[0]);
                    if (obj != null) {
                        System.out.println("\nReading " + obisCode[1] + " (" + obisCode[0] + ")");
                        Object result = reader.read(obj, 2);  // Attribute 2 typically contains the value
                        System.out.println("Value: " + result);

                        // Print additional object information
                        System.out.println("Object Type: " + obj.getObjectType());
                        System.out.println("Description: " + obj.getDescription());
                    }
                } catch (Exception e) {
                    System.out.println("Error reading " + obisCode[1] + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    System.out.println("\nClosing connection...");
                    connection.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    public static void connectAndReadMeter2() {
        GXNet connection = new GXNet(NetworkType.TCP, IP_ADDRESS, PORT);
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);

        try {
            // Basic client configuration
            client.setClientAddress(1);
            client.setServerAddress(1);
            client.setInterfaceType(InterfaceType.HDLC);
            client.setAuthentication(AUTH_LEVEL);
            client.setPassword(PASSWORD.getBytes());

            // Add necessary conformance
            client.getProposedConformance().add(Conformance.GENERAL_PROTECTION);
            client.getProposedConformance().add(Conformance.SELECTIVE_ACCESS);
            client.getProposedConformance().add(Conformance.GET);
            client.getProposedConformance().add(Conformance.BLOCK_TRANSFER_WITH_GET_OR_READ);
            client.getProposedConformance().add(Conformance.ACTION);
            client.getProposedConformance().add(Conformance.BLOCK_TRANSFER_WITH_SET_OR_WRITE);
            client.getProposedConformance().add(Conformance.SET);


            TraceLevel traceLevel = TraceLevel.VERBOSE;
            String frameCounter = "frameCounterIdentifier";
            GXDLMSReader reader = new GXDLMSReader(client, connection, traceLevel, frameCounter);

            // Open connection and initialize
            connection.open();
            System.out.println("Initializing connection...");
            reader.initializeConnection();
            reader.getAssociationView();

            // Read specific registers
            System.out.println("\nReading meter registers:");

            // Define registers to read with their OBIS codes
//            String[][] obisToRead = {
//                    {"0.0.0.2.0.255", "Active Firmware Identifier"},
//                    {"0.0.0.2.1.255", "Active Firmware Signature"},
//                    {"0.0.0.2.8.255", "Active Firmware Version"},
//                    {"0.0.1.0.0.255", "Current Date and Time (Clock)"},
//                    {"0.0.40.0.0.255", "Current Association (Logical Name)"},
//                    {"0.0.41.0.0.255", "SAP Assignment (Service Access Point)"},
//                    {"0.0.42.0.0.255", "COSEM Logical Device Name"},
//                    {"0.0.96.1.0.255", "Device ID 1 (Meter Serial Number)"},
//                    {"0.0.96.1.1.255", "Device ID 2 (Manufacturer ID)"},
//                    {"0.0.96.1.3.255", "Device ID 3 (Model Number)"},
//                    {"0.0.96.1.4.255", "Device ID 4 (Version Number)"},
//                    {"0.0.96.1.5.255", "Device ID 5 (Additional Info)"},
//                    {"0.0.96.1.6.255", "Device ID 6 (Meter Type)"},
//                    {"0.0.96.1.8.255", "Device ID 8 (Meter Configuration)"},
//                    {"0.1.96.1.1.255", "Profile Status (Current Profile)"},
//                    {"1.0.0.0.0.255", "Metering Point ID 1 (Primary Metering Point)"},
//                    {"1.0.0.0.1.255", "Metering Point ID 2 (Secondary Metering Point)"},
//                    {"1.0.0.0.2.255", "Metering Point ID 3 (Tertiary Metering Point)"},
//                    {"1.0.0.0.3.255", "Metering Point ID 4 (Quaternary Metering Point)"},
//
//                    // Demand Registers
//                    {"1.0.1.4.0.255", "Active Power Demand (Last 15 min)"},
//                    {"1.0.2.4.0.255", "Reactive Power Demand (Last 15 min)"},
//                    {"1.0.3.4.0.255", "Apparent Power Demand (Last 15 min)"},
//                    {"1.0.4.4.0.255", "Peak Demand (Last 30 min)"},
//                    {"1.0.9.4.0.255", "Maximum Demand (Last 12 months)"},
//                    {"1.0.10.4.0.255", "Demand Reset Time"},
//                    {"1.0.15.4.0.255", "Demand Interval Duration"},
//
//                    // Extended Registers
//                    {"1.0.1.6.0.255", "Voltage L1 (Phase 1)"},
//                    {"1.0.1.6.1.255", "Voltage L2 (Phase 2)"},
//                    {"1.0.1.6.2.255", "Voltage L3 (Phase 3)"},
//                    {"1.0.1.6.3.255", "Current L1 (Phase 1)"},
//                    {"1.0.1.6.4.255", "Current L2 (Phase 2)"},
//                    {"1.0.2.6.0.255", "Current L3 (Phase 3)"},
//                    {"1.0.2.6.1.255", "Power Factor L1 (Phase 1)"},
//                    {"1.0.2.6.2.255", "Power Factor L2 (Phase 2)"},
//                    {"1.0.2.6.3.255", "Power Factor L3 (Phase 3)"},
//                    {"1.0.2.6.4.255", "Frequency"},
//                    {"1.0.3.6.0.255", "Total Active Energy (Import)"},
//                    {"1.0.3.6.1.255", "Total Active Energy (Export)"},
//                    {"1.0.3.6. 2.255", "Total Reactive Energy (Import)"},
//                    {"1.0.3.6.3.255", "Total Reactive Energy (Export)"},
//                    {"1.0.3.6.4.255", "Total Apparent Energy"},
//                    {"1.0.4.6.0.255", "Active Energy (Import, Tariff 1)"},
//                    {"1.0.4.6.1.255", "Active Energy (Export, Tariff 1)"},
//                    {"1.0.4.6.2.255", "Active Energy (Import, Tariff 2)"},
//                    {"1.0.4.6.3.255", "Active Energy (Export, Tariff 2)"},
//                    {"1.0.4.6.4.255", "Reactive Energy (Import, Tariff 1)"},
//                    {"1.0.9.6.0.255", "Reactive Energy (Export, Tariff 1)"},
//                    {"1.0.9.6.1.255", "Reactive Energy (Import, Tariff 2)"},
//
//                    // Additional Registers
//                    {"1.0.10.6.0.255", "Total Active Energy (Import, Last Month)"},
//                    {"1.0.10.6.1.255", "Total Active Energy (Export, Last Month)"},
//                    {"1.0.10.6.2.255", "Total Reactive Energy (Import, Last Month)"},
//                    {"1.0.10.6.3.255", "Total Reactive Energy (Export, Last Month)"},
//                    {"1.0.10.6.4.255", "Total Apparent Energy (Last Month)"},
//                    {"1.0.15.6.0.255", "Total Active Energy (Import, Last Year)"},
//                    {"1.0.15.6.1.255", "Total Active Energy (Export, Last Year)"},
//                    {"1.0.15.6.2.255", "Total Reactive Energy (Import, Last Year)"},
//                    {"1.0.15.6.3.255", "Total Reactive Energy (Export, Last Year)"},
//                    {"1.0.15.6.4.255", "Total Apparent Energy (Last Year)"},
//
//                    // Register for Monitoring
//                    {"1.0.11.31.0.255", "Monitoring Register 1"},
//                    {"1.0.11.35.0.255", "Monitoring Register 2"},
//                    {"1.0.12.31.0.255", "Monitoring Register 3"},
//                    {"1.0.12.35.0.255", "Monitoring Register 4"},
//                    {"1.0.13.31.0.255", "Monitoring Register 5"},
//                    {"1.0.14.35.0.255", "Monitoring Register 6"},
//                    {"1.0.15.35.0.255", "Monitoring Register 7"},
//
//                    // Profile Generic
//                    {"1.0.98.1.0.255", "Profile Generic 1"},
//                    {"1.0.98.2.0.255", "Profile Generic 2"},
//                    {"1.0.99.1.1.255", "Profile Generic 3"},
//                    {"1.0.99.98.1.255", "Profile Generic 4"},
//
//                    // Additional Registers
//                    {"1.0.128.11.4.255", "Additional Register 1"},
//                    {"1.0.189.40.0.255", "Additional Register 2"},
//                    {"1.0.240.140.0.255", "Additional Register 3"},
//                    {"1.0.240.141.0.255", "Additional Register 4"},
//
//                    // Additional OBIS Codes
//                    {"1.0.1.8.0.255", "Active Power (Import, Tariff 1)"},
//                    {"1.0.1.8.1.255", "Active Power (Export, Tariff 1)"},
//                    {"1.0.1.8.2.255", "Active Power (Import, Tariff 2)"},
//                    {"1.0.1.8.3.255", "Active Power (Export, Tariff 2)"},
//                    {"1.0.2.8.0.255", "Reactive Power (Import, Tariff 1)"},
//                    {"1.0.2.8.1.255", "Reactive Power (Export, Tariff 1)"},
//                    {"1.0.2.8.2.255", "Reactive Power (Import, Tariff 2)"},
//
//                    {"1.0.2.8.3.255", "Reactive Power (Export, Tariff 2)"},
//                    {"1.0.3.8.0.255", "Apparent Power (Import, Tariff 1)"},
//                    {"1.0.3.8.1.255", "Apparent Power (Export, Tariff 1)"},
//                    {"1.0.3.8.2.255", "Apparent Power (Import, Tariff 2)"},
//                    {"1.0.3.8.3.255", "Apparent Power (Export, Tariff 2)"},
//                    {"1.0.4.8.0.255", "Active Energy (Import, Tariff 1, Last Month)"},
//                    {"1.0.4.8.1.255", "Active Energy (Export, Tariff 1, Last Month)"},
//                    {"1.0.4.8.2.255", "Active Energy (Import, Tariff 2, Last Month)"},
//                    {"1.0.4.8.3.255", "Active Energy (Export, Tariff 2, Last Month)"},
//                    {"1.0.5.8.0.255", "Reactive Energy (Import, Tariff 1, Last Month)"},
//                    {"1.0.5.8.1.255", "Reactive Energy (Export, Tariff 1, Last Month)"},
//                    {"1.0.5.8.2.255", "Reactive Energy (Import, Tariff 2, Last Month)"},
//                    {"1.0.5.8.3.255", "Reactive Energy (Export, Tariff 2, Last Month)"},
//                    {"1.0.6.8.0.255", "Total Active Energy (Import, Last Month)"},
//                    {"1.0.6.8.1.255", "Total Active Energy (Export, Last Month)"},
//                    {"1.0.6.8.2.255", "Total Reactive Energy (Import, Last Month)"},
//                    {"1.0.6.8.3.255", "Total Reactive Energy (Export, Last Month)"},
//                    {"1.0.7.8.0.255", "Total Active Energy (Import, Last Year)"},
//                    {"1.0.7.8.1.255", "Total Active Energy (Export, Last Year)"},
//                    {"1.0.7.8.2.255", "Total Reactive Energy (Import, Last Year)"},
//                    {"1.0.7.8.3.255", "Total Reactive Energy (Export, Last Year)"},
//                    {"1.0.8.8.0.255", "Total Apparent Energy (Last Month)"},
//                    {"1.0.8.8.1.255", "Total Apparent Energy (Last Year)"},
//                    {"1.0.9.8.0.255", "Total Active Energy (Import, Current Year)"},
//                    {"1.0.9.8.1.255", "Total Active Energy (Export, Current Year)"},
//                    {"1.0.9.8.2.255", "Total Reactive Energy (Import, Current Year)"},
//                    {"1.0.9.8.3.255", "Total Reactive Energy (Export, Current Year)"},
//                    {"1.0.10.8.0.255", "Total Apparent Energy (Current Year)"},
//                    {"1.0.11.8.0.255", "Total Active Energy (Import, Current Month)"},
//                    {"1.0.11.8.1.255", "Total Active Energy (Export, Current Month)"},
//                    {"1.0.11.8.2.255", "Total Reactive Energy (Import, Current Month)"},
//                    {"1.0.11.8.3.255", "Total Reactive Energy (Export, Current Month)"},
//                    {"1.0.12.8.0.255", "Total Apparent Energy (Current Month)"},
//                    {"1.0.13.8.0.255", "Total Active Energy (Import, Last 24 Hours)"},
//                    {"1.0.13.8.1.255", "Total Active Energy (Export, Last 24 Hours)"},
//                    {"1.0.13.8.2.255", "Total Reactive Energy (Import, Last 24 Hours)"},
//                    {"1.0.13.8.3.255", "Total Reactive Energy (Export, Last 24 Hours)"},
//                    {"1.0.14.8.0.255", "Total Apparent Energy (Last 24 Hours)"},
//                    {"1.0.15.8.0.255", "Total Active Energy (Import, Last 7 Days)"},
//                    {"1.0.15.8.1.255", "Total Active Energy (Export, Last 7 Days)"},
//                    {"1.0.15.8.2.255", "Total Reactive Energy (Import, Last 7 Days)"},
//                    {"1.0.15.8.3.255", "Total Reactive Energy (Export, Last 7 Days)"},
//                    {"1.0.16.8.0.255", "Total Apparent Energy (Last 7 Days)"},
//                    {"1.0.17.8.0.255", "Total Active Energy (Import, Last 30 Days)"},
//                    {"1.0.17.8.1.255", "Total Active Energy (Export, Last 30 Days)"},
//                    {"1.0.17.8.2.255", "Total Reactive Energy (Import, Last 30 Days)"},
//                    {"1.0.17.8.3.255", "Total Reactive Energy (Export, Last 30 Days)"},
//                    {"1.0.18.8.0.255", "Total Apparent Energy (Last 30 Days)"},
//                    {"1.0.19.8.0.255", "Total Active Energy (Import, Last 60 Days)"},
//                    {"1.0.19.8.1.255", "Total Active Energy (Export, Last 60 Days)"},
//                    {"1.0.19.8.2.255", "Total Reactive Energy (Import, Last 60 Days)"},
//                    {"1.0.19.8.3.255", "Total Reactive Energy (Export, Last 60 Days)"},
//                    {"1.0.20.8.0.255", "Total Apparent Energy (Last 60 Days)"},
//                    {"1.0.21.8.0.255", "Total Active Energy (Import, Last 90 Days)"},
//                    {"1.0.21.8.1.255", "Total Active Energy (Export, Last 90 Days)"},
//                    {"1.0.21.8.2.255", "Total Reactive Energy (Import, Last 90 Days)"},
//                    {"1.0.21.8.3.255", "Total Reactive Energy (Export, Last 90 Days)"},
//                    {"1.0.22.8.0.255", "Total Apparent Energy (Last 90 Days)"},
//                    {"1.0.23.8.0.255", "Total Active Energy (Import, Last 120 Days)"},
//                    {"1.0.23.8.1.255", "Total Active Energy (Export, Last 120 Days)"},
//                    {"1.0.23.8.2.255", "Total Reactive Energy (Import, Last 120 Days)"},
//                    {"1.0.23.8.3.255", "Total Reactive Energy (Export, Last 120 Days)"},
//                    {"1.0.24.8.0.255", "Total Apparent Energy (Last 120 Days)"},
//                    {"1.0.25.8.0.255", "Total Active Energy (Import, Last 150 Days)"},
//                    {"1.0.25.8.1.255", "Total Active Energy (Export, Last 150 Days)"},
//                    {"1.0.25.8.2.255", "Total Reactive Energy (Import, Last 150 Days)"},
//                    {"1.0.25.8.3.255", "Total Reactive Energy (Export, Last 150 Days)"},
//                    {"1.0.26.8.0.255", "Total Apparent Energy (Last 150 Days)"},
//                    {"1.0.27.8.0.255", "Total Active Energy (Import, Last 180 Days)"},
//                    {"1.0.27.8.1.255", "Total Active Energy (Export, Last 180 Days)"},
//                    {"1.0.27.8.2.255", "Total Reactive Energy (Import, Last 180 Days)"},
//                    {"1.0.27.8.3.255", "Total Reactive Energy (Export, Last 180 Days)"},
//                    {"1.0.28.8.0.255", "Total Apparent Energy (Last 180 Days)"},
//                    {"1.0.29.8.0.255", "Total Active Energy (Import, Last 210 Days)"},
//                    {"1.0.29.8.1.255", "Total Active Energy (Export, Last 210 Days)"},
//                    {"1.0 .29.8.2.255", "Total Reactive Energy (Import, Last 210 Days)"},
//                    {"1.0.29.8.3.255", "Total Reactive Energy (Export, Last 210 Days)"},
//                    {"1.0.30.8.0.255", "Total Apparent Energy (Last 210 Days)"},
//                    {"1.0.31.8.0.255", "Total Active Energy (Import, Last 240 Days)"},
//                    {"1.0.31.8.1.255", "Total Active Energy (Export, Last 240 Days)"},
//                    {"1.0.31.8.2.255", "Total Reactive Energy (Import, Last 240 Days)"},
//                    {"1.0.31.8.3.255", "Total Reactive Energy (Export, Last 240 Days)"},
//                    {"1.0.32.8.0.255", "Total Apparent Energy (Last 240 Days)"},
//                    {"1.0.33.8.0.255", "Total Active Energy (Import, Last 270 Days)"},
//                    {"1.0.33.8.1.255", "Total Active Energy (Export, Last 270 Days)"},
//                    {"1.0.33.8.2.255", "Total Reactive Energy (Import, Last 270 Days)"},
//                    {"1.0.33.8.3.255", "Total Reactive Energy (Export, Last 270 Days)"},
//                    {"1.0.34.8.0.255", "Total Apparent Energy (Last 270 Days)"},
//                    {"1.0.35.8.0.255", "Total Active Energy (Import, Last 300 Days)"},
//                    {"1.0.35.8.1.255", "Total Active Energy (Export, Last 300 Days)"},
//                    {"1.0.35.8.2.255", "Total Reactive Energy (Import, Last 300 Days)"},
//                    {"1.0.35.8.3.255", "Total Reactive Energy (Export, Last 300 Days)"},
//                    {"1.0.36.8.0.255", "Total Apparent Energy (Last 300 Days)"},
//                    {"1.0.37.8.0.255", "Total Active Energy (Import, Last 330 Days)"},
//                    {"1.0.37.8.1.255", "Total Active Energy (Export, Last 330 Days)"},
//                    {"1.0.37.8.2.255", "Total Reactive Energy (Import, Last 330 Days)"},
//                    {"1.0.37.8.3.255", "Total Reactive Energy (Export, Last 330 Days)"},
//                    {"1.0.38.8.0.255", "Total Apparent Energy (Last 330 Days)"},
//                    {"1.0.39.8.0.255", "Total Active Energy (Import, Last 360 Days)"},
//                    {"1.0.39.8.1.255", "Total Active Energy (Export, Last 360 Days)"},
//                    {"1.0.39.8.2.255", "Total Reactive Energy (Import, Last 360 Days)"},
//                    {"1.0.39.8.3.255", "Total Reactive Energy (Export, Last 360 Days)"},
//                    {"1.0.40.8.0.255", "Total Apparent Energy (Last 360 Days)"}
//            };

            String[][] obisToRead = {
                    {"1-0:0.9.2", "Current Date"},
                    {"1-0:0.9.1", "Current Time"},

                    
                    {"1-0:0.0.0", "Meter Serial Number"},
                    {"0-0:96.1.2", "HDLC Address"},
                    {"1-0:96.1.3", "Device Model"},
                    {"0-0:96.1.4", "Device Manufacturer"},
                    {"0-0:42.0.0", "Logical Device Name"},
                    {"0-0:240.46.0", "SGC"},
                    {"0-0:240.48.0", "TI"},
                    {"0-0:240.49.0", "KRN"},
                    {"0-0:0.2.1", "Firmware Version"},
                    {"0-0:96.1.1", "Software Version"},
                    {"0-0:96.1.5", "Hardware Version"},
                    {"0-0:96.1.6", "Hardware PCB Version"},
                    {"0-0:0.2.8", "Firmware Signature"},
                    {"0-0:96.2.0", "Number of Programming"},
                    {"0-0:240.44.0*1", "Event Log - Programming Events(History 1)"},
                    {"0-0:240.43.0", "Programmer ID"},
                    {"0-0:96.2.1", "Time of Last Programming"},
                    {"0-0:96.2.5", "Date of Last Calibration"},
                    {"0-0:96.2.12", "Date of Last Clock Synchronization"},
                    {"0-0:96.2.13", "Date of Last Firmware Activation"},
                    {"0-0:96.14.0", "Currently Active Tariff"},
                    {"1-0:0.1.0", "Billing Period Counter"},
                    {"1-0:0.1.2*1", "Last Reset Date(History 1)"},
                    {"1-0:0.1.3", "Daily Snapshot Count"},
                    {"1-0:0.1.5*1", "Last Daily Snapshot Date(History 1)"},
                    {"0-0:97.97.0", "Error Code"},
                    {"0-0:97.97.7", "Tamper Status"},
                    {"0-0:96.80.29", "Breaker Status"},
                    {"0-0:96.90.16", "Disconnect Event Count"},
                    {"0-0:240.45.0*1", "Event Log - Disconnect(History 1)"},
                    {"0-0:96.7.0", "Number of Power Failures - All Phases"},
                    {"0-0:96.12.1", "Number of Optical Port Communications"},
                    {"0-0:96.8.0", "Total Operation Duration"},
                    {"0-0:96.90.22", "Monthly Operation Duration"},
                    {"0-0:96.90.18", "Total Sleep Duration"},
                    {"1-0:0.6.0", "Rated Voltage"},
                    {"1-0:0.6.1", "Basic Current"},
                    {"1-0:0.6.2", "Rated Frequency"},
                    {"1-0:0.6.3", "Maximum Current"},
                    {"1-0:0.6.129", "Starting Current"},
                    {"1-0:0.6.130", "Active Accuracy"},
                    {"1-0:0.6.131", "Reactive Accuracy"},
                    {"1-0:0.3.0", "Metrological LED Output Constant - Active Energy (imp/kWh)"},
                    {"1-0:0.3.1", "Metrological LED Output Constant - Reactive Energy (imp/kvarh)"},
                    {"0-0:1.0.0", "Clock"},
                    {"0-0:96.9.0", "Ambient Temperature"},
                    {"0-0:96.6.3", "Battery Voltage"},
                    {"0-0:96.6.0", "Battery Use Duration"},
                    {"0-0:96.90.19", "Wake Up on Battery Duration"},
                    {"0-0:96.90.20", "Wake Up on Battery Count"},
                    {"0-0:96.52.2*2", "LoRa Operation Step"},
                    {"1-0:1.8.0", "Import Active Energy"},
                    {"1-0:1.8.1", "Import Active Energy - Rate 1"},
                    {"1-0:1.8.2", "Import Active Energy - Rate 2"},
                    {"1-0:1.8.3", "Import Active Energy - Rate 3"},
                    {"1-0:1.8.4", "Import Active Energy - Rate 4"},
                    {"1-0:2.8.0", "Export Active Energy"},
                    {"1-0:2.8.1", "Export Active Energy - Rate 1"},
                    {"1-0:2.8.2", "Export Active Energy - Rate 2"},
                    {"1-0:2.8.3", "Export Active Energy - Rate 3"},
                    {"1-0:2.8.4", "Export Active Energy - Rate 4"},
                    {"1-0:3.8.0", "Import Reactive Energy"},
                    {"1-0:3.8.1", "Import Reactive Energy - Rate 1"},
                    {"1-0:3.8.2", "Import Reactive Energy - Rate 2"},
                    {"1-0:3.8.3", "Import Reactive Energy - Rate 3"},
                    {"1-0:3.8.4", "Import Reactive Energy - Rate 4"},
                    {"1-0:4.8.0", "Export Reactive Energy"},
                    {"1-0:4.8.1", "Export Reactive Energy - Rate 1"},
                    {"1-0:4.8.2", "Export Reactive Energy - Rate 2"},
                    {"1-0:4.8.3", "Export Reactive Energy - Rate 3"},
                    {"1-0:4.8.4", "Export Reactive Energy - Rate 4"},
                    {"1-0:9.8.0", "Import Apparent Energy"},
                    {"1-0:9.8.1", "Import Apparent Energy - Rate 1"},
                    {"1-0:9.8.2", "Import Apparent Energy - Rate 2"},
                    {"1-0:9.8.3", "Import Apparent Energy - Rate 3"},
                    {"1-0:9.8.4", "Import Apparent Energy - Rate 4"},
                    {"1-0:10.8.0", "Export Apparent Energy"},
                    {"1-0:10.8.1", "Export Apparent Energy - Rate 1"},
                    {"1-0:10.8.2", "Export Apparent Energy - Rate 2"},
                    {"1-0:10.8.3", "Export Apparent Energy - Rate 3"},
                    {"1-0:10.8.4", "Export Apparent Energy - Rate 4"},
                    {"1-0:15.8.0", "Total Active Energy"},
                    {"1-0:15.8.1", "Total Active Energy - Rate 1"},
                    {"1-0:15.8.2", "Total Active Energy - Rate 2"},
                    {"1-0:15.8.3", "Total Active Energy - Rate 3"},
                    {"1-0:15.8.4", "Total Active Energy - Rate 4"},
                    {"1-0:128.8.0", "Total Reactive Energy"},
                    {"1-0:128.8.1", "Total Reactive Energy - Rate 1"},
                    {"1-0:128.8.2", "Total Reactive Energy - Rate 2"},
                    {"1-0:128.8.3", "Total Reactive Energy - Rate 3"},
                    {"1-0:128.8.4", "Total Reactive Energy - Rate 4"},
                    {"1-0:129.8.0", "Total Apparent Energy"},
                    {"1-0:129.8.1", "Total Apparent Energy - Rate 1"},
                    {"1-0:129.8.2", "Total Apparent Energy - Rate 2"},
                    {"1-0:129.8.3", "Total Apparent Energy - Rate 3"},
                    {"1-0:129.8.4", "Total Apparent Energy - Rate 4"},
                    {"1-0:1.6.0", "Import Active Maximum Demand"},
                    {"1-0:1.6.1", "Import Active Maximum Demand- Rate 1"},
                    {"1-0:1.6.2", "Import Active Maximum Demand- Rate 2"},
                    {"1-0:1.6.3", "Import Active Maximum Demand- Rate 3"},
                    {"1-0:1.6.4", "Import Active Maximum Demand- Rate 4"},
                    {"1-0:2.6.0", "Export Active Maximum Demand"},
                    {"1-0:2.6.1", "Export Active Maximum Demand- Rate 1"},
                    {"1-0:2.6.2", "Export Active Maximum Demand- Rate 2"},
                    {"1-0:2.6.3", "Export Active Maximum Demand- Rate 3"},
                    {"1-0:2.6.4", "Export Active Maximum Demand- Rate 4"}
            };

            for (String[] obisCode : obisToRead) {
                try {
                    // Add delay between reads
                    Thread.sleep(1000); // 1 second delay

                    // Convert OBIS code if needed
                    String logicalName = obisCode[0].contains("-") ?
                            convertToLogicalName(obisCode[0]) : obisCode[0];

                    System.out.println("\nReading " + obisCode[1] + " (" + logicalName + ")");

                    GXDLMSObject obj = client.getObjects().findByLN(ObjectType.NONE, logicalName);
                    if (obj != null) {
                        // Read both value and scaler if available
                        Object value = reader.read(obj, 2);  // Value in attribute 2
                        System.out.println("Raw Value: " + value);

                        try {
                            // Try to read scaler (attribute 3) for numeric values
                            Object scalerUnit = reader.read(obj, 3);
                            if (scalerUnit instanceof Object[]) {
                                Object[] su = (Object[]) scalerUnit;
                                if (su.length >= 2 && su[0] instanceof Number) {
                                    int scaler = ((Number) su[0]).intValue();
                                    int unit = ((Number) su[1]).intValue();
                                    if (value instanceof Number) {
                                        double scaledValue = ((Number) value).doubleValue()
                                                * Math.pow(10, scaler);
                                        System.out.println("Scaled Value: " + scaledValue
                                                + " " + getUnitString(unit));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Ignore scaler reading errors - not all objects have scalers
                        }

                        // Print additional object information
                        System.out.println("Object Type: " + obj.getObjectType());
                        System.out.println("Description: " + obj.getDescription());
                        System.out.println("--------------------------------------------------------------------");
                    } else {
                        System.out.println("Object not found for OBIS code: " + logicalName);
                    }
                } catch (Exception e) {
                    System.out.println("Error reading " + obisCode[1] + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    System.out.println("\nClosing connection...");
                    connection.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Helper method to convert from display format to logical name format
    private static String convertToLogicalName(String displayFormat) {
        return displayFormat.replace("-", ".").replace(":", ".") + ".255";
    }

    // Helper method to get unit string
    private static String getUnitString(int unit) {
        switch (unit) {
            case 27:
                return "W";
            case 28:
                return "VA";
            case 29:
                return "var";
            case 30:
                return "Wh";
            case 31:
                return "VAh";
            case 32:
                return "varh";
            case 33:
                return "A";
            case 35:
                return "V";
            case 44:
                return "Hz";
            case 62:
                return "°C";  // Temperature
            default:
                return "Unit(" + unit + ")";
        }
    }

    // Helper method to format date/time values
    private static String formatDateTime(Object value) {
        if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format((Date) value);
        }
        return String.valueOf(value);
    }
}
