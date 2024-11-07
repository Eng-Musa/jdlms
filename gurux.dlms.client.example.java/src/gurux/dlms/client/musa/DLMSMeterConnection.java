package gurux.dlms.client.musa;

import gurux.common.enums.TraceLevel;
import gurux.dlms.GXDLMSException;
import gurux.dlms.GXReplyData;
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

    private static final String IP_ADDRESS = "172.16.8.140";  // Set your meter's IP here
    private static final int PORT = 5258;  // Set the communication port here
    private static final String PASSWORD = "password";  // Set meter's password here
    private static final Authentication AUTH_LEVEL = Authentication.NONE;  // Adjust as per your meter settings

    public static void connectAndReadMeter() {
        GXNet connection = new GXNet(NetworkType.TCP, IP_ADDRESS, PORT);
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);

        try {
            // Modified client configuration
            client.setClientAddress(16);
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

    public static void main(String[] args) {
        connectAndReadMeter2();
    }


    public static void connectAndReadMeter1() {
        GXNet connection = new GXNet(NetworkType.TCP, IP_ADDRESS, PORT);
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);

        try {
            // Basic client configuration
            client.setClientAddress(16);
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
            client.setClientAddress(16);
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

            // Define registers to read with their OBIS codes
            String[][] obisToRead = {
                    {"0.0.1.0.0.255", "Clock"},
                    {"1-0:0.0.0", "Meter Serial Number"},  // Convert from display format
                    {"0.0.96.1.0.255", "Device ID 1"},     // Alternative location for serial number
                    {"0.0.96.1.1.255", "Device ID 2"},     // Alternative location for serial number
                    {"0.0.96.9.0.255", "Ambient Temperature"} // Common OBIS code for temperature

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
            case 27: return "W";
            case 28: return "VA";
            case 29: return "var";
            case 30: return "Wh";
            case 31: return "VAh";
            case 32: return "varh";
            case 33: return "A";
            case 35: return "V";
            case 44: return "Hz";
            case 62: return "°C";  // Temperature
            default: return "Unit(" + unit + ")";
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
