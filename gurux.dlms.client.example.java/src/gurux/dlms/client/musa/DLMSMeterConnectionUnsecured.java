//package gurux.dlms.client.musa;
//
//import gurux.common.enums.TraceLevel;
//import gurux.dlms.GXDLMSClient;
//import gurux.dlms.GXDLMSException;
//import gurux.dlms.client.GXDLMSReader;
//import gurux.dlms.enums.Authentication;
//import gurux.dlms.objects.GXDLMSObject;
//import gurux.net.GXNet;
//import gurux.net.enums.NetworkType;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//
//public class DLMSMeterConnectionUnsecured {
//    private static final String IP_ADDRESS = "172.16.8.140";  // Set your meter's IP here
//    private static final int PORT = 5258;  // Set the communication port here
//    private static final String PASSWORD = "password";  // Set meter's password here
//    private static final Authentication AUTH_LEVEL = Authentication.NONE;  // Adjust as per your meter settings
//
//    public static void connectAndReadMeter() {
//        try {
//            // Resolve IP address
//            InetAddress inetAddress = InetAddress.getByName(IP_ADDRESS);
//            GXNet connection = new GXNet(NetworkType.TCP, inetAddress.toString(), PORT);
//
//            // Use GXDLMSClient for non-secure communication
//            GXDLMSClient client = new GXDLMSClient();
//
//            // Set authentication and password
//            client.setAuthentication(AUTH_LEVEL);
//            client.setPassword(PASSWORD.getBytes());
//
//            // Open the connection
//            connection.open();
//
//            // Initialize the client and reader
//            TraceLevel traceLevel = TraceLevel.VERBOSE;  // Example trace level, adjust as needed
//            String frameCounter = "frameCounterIdentifier";
//            // Use the appropriate constructor here for non-secure communication
//            GXDLMSReader reader = new GXDLMSReader(client, connection, traceLevel, frameCounter);
//            reader.initializeConnection();
//
//            // Read the active energy data using OBIS code
//            // Example OBIS for active energy
//            GXDLMSObject object = null;
//            for (GXDLMSObject obj : client.getObjects()) {
//                if (obj.getLogicalName().toString().equals("1.0.1.8.0.255")) {  // Example OBIS code for active energy
//                    object = obj;
//                    break;
//                }
//            }
//
//            if (object != null) {
//                Object result = reader.read(object, 2);
//                System.out.println("Active Energy: " + result);
//            } else {
//                System.out.println("Specified OBIS code not found in the meter.");
//            }
//
//            // Finalize communication and close connection
//            reader.close();
//            System.out.println("Connection closed successfully.");
//
//        } catch (UnknownHostException e) {
//            System.err.println("Error: Unable to resolve host: " + IP_ADDRESS);
//            e.printStackTrace();
//        } catch (GXDLMSException e) {
//            System.err.println("DLMS error: " + e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void main(String[] args) {
//        connectAndReadMeter();
//    }
//}
