package gurux.dlms.client.musa;

import gurux.common.enums.TraceLevel;
import gurux.dlms.GXDLMSException;
import gurux.dlms.GXReplyData;
import gurux.dlms.client.GXDLMSReader;
import gurux.dlms.client.GXDLMSSecureClient2;
import gurux.dlms.enums.Authentication;
import gurux.dlms.enums.InterfaceType;
import gurux.dlms.objects.GXDLMSObject;
import gurux.dlms.objects.GXDLMSObjectCollection;
import gurux.net.GXNet;
import gurux.net.enums.NetworkType;

import java.net.UnknownHostException;

public class DLMSMeterConnection {

    private static final String IP_ADDRESS = "172.16.8.140";  // Set your meter's IP here
    private static final int PORT = 5258;  // Set the communication port here
    private static final String PASSWORD = "password";  // Set meter's password here
    private static final Authentication AUTH_LEVEL = Authentication.NONE;  // Adjust as per your meter settings

    public static void connectAndReadMeter() {
        GXNet connection = new GXNet(NetworkType.TCP, IP_ADDRESS, PORT);
        GXDLMSSecureClient2 client = new GXDLMSSecureClient2(true);

        try {
            // Set client and server addresses as required by your meter.
            client.setClientAddress(16); // Typical public client address; adjust as needed
            client.setServerAddress(1);  // Set to the meter’s actual server address if known
            client.setInterfaceType(InterfaceType.HDLC);

            // Configure authentication and password
            client.setAuthentication(AUTH_LEVEL);
            client.setPassword(PASSWORD.getBytes());

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

            // Search for active energy OBIS code
            GXDLMSObject activeEnergyObject = null;
            for (GXDLMSObject obj : objects) {
                if ("1.0.1.7.0.255".equals(obj.getLogicalName())) {  // Match exact OBIS code format
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
        connectAndReadMeter();
    }
}
