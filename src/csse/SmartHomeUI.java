package csse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import Ice.Current;
import csse4004.HMPrx;
import csse4004.HMPrxHelper;
import csse4004._UIDisp;

/**
 * User Interface for Smart Home.
 *
 */
public class SmartHomeUI extends Ice.Application {

    /**
     * UI ICE implementation. Receive energy usage statistics.
     */
    @SuppressWarnings("serial")
    public class UII extends _UIDisp {

        /**
         * Receive RMI message from ICE about energy usage. Print Energy Warning
         * Message.
         * 
         * @param value
         *            Energy Usage Value
         */
        @Override
        public void highEnergyWarning(int value, Current __current) {
            System.out.println("Energy Usage Warning: "
                    + "Current electricity consumption is " + value);
            System.out.println("Please consider the environment before"
                    + " switching on any electrical appliance.");
            System.out.flush();
        }
    }

    private final static String EOL = System.getProperty("line.separator");

    /**
     * SmartHomeUI Class Constructor.
     */
    public SmartHomeUI() {

    }

    /**
     * Prints welcome message. Sends user name and port to HM.
     * 
     * @param hmPrx
     *            HomeManagerPrx Ice Object for RPC/RMI communications.
     * @param port
     *            int of the port the UI is running on
     * @throws IOException
     */
    private static void welcomeMessage(HMPrx hmPrx, int port)
            throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Welcome to the Smart Home Monitoring System");
        System.out.println("Please Enter your user name:");
        System.out.flush();

        try {
            String userName = in.readLine().trim();
            hmPrx.registerUser(userName, port);
        } catch (IOException e) {
            System.exit(1);
        }
    }

    /**
     * Print Menu to Console and accept commands from user.
     * 
     * @param hmPrx
     *            HomeManagerPrx Ice Object for RPC/RMI communications.
     * @throws IOException
     */
    private static void menu(HMPrx hmPrx) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Welcome to the Smart Home Monitoring System");
        System.out.println("Please select an option");
        System.out.println("1. View log - temperature adjustment");
        System.out.println("2. View media files");
        System.out.println("3. View disc tracks");
        System.out.println("E. Exit");
        System.out.flush();

        String option = in.readLine().trim();

        switch (option) {
        case "1": // View Temperature Log
            viewLog(hmPrx);
            break;
        case "2": // View Media Files
            viewMediaFiles(hmPrx);
            break;
        case "3": // View Disc Tracks
            System.out.println("Please enter the disc title:");
            System.out.flush();
            viewDiscTracks(hmPrx, in.readLine().trim());
            break;
        case "E": // Exit
            exit(hmPrx);
            break;
        default: // Invalid Command
            System.out.println("Invalid Command");
            System.out.flush();
            in.readLine();
            menu(hmPrx);
            break;
        }

    }

    /**
     * Make RPC/RMI call to HomeManger to view Temperature Log Print results to
     * console.
     * 
     * @param hmPrx
     *            HomeManagerPrx Ice Object for RPC/RMI communications.
     * @throws IOException
     */
    private static void viewLog(HMPrx hmPrx) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.err.println(EOL);
        String log = hmPrx.viewLog(EOL);
        System.out.println(log);
        System.out.flush();

        // Wait for enter
        in.readLine();
        menu(hmPrx);
    }

    /**
     * Make RPC/RMI call to HomeManger to view Media Files. Print results to
     * console.
     * 
     * @param hmPrx
     *            HomeManagerPrx Ice Object for RPC/RMI communications.
     * @throws IOException
     */
    private static void viewMediaFiles(HMPrx hmPrx) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        // System.out.println(hmPrx.getFiles());
        String files = hmPrx.viewMediaFiles(EOL);
        System.out.println(files);
        System.out.flush();

        // Wait for enter
        in.readLine();
        menu(hmPrx);
    }

    /**
     * Make RPC/RMI call to HomeManger to view Media Tracks of a Disc. Print
     * results to console.
     * 
     * @param hmPrx
     *            HomeManagerPrx Ice Object for RPC/RMI communications.
     * @param discTitle
     *            String of the Disc Title
     * @throws IOException
     */
    private static void viewDiscTracks(HMPrx hmPrx, String discTitle)
            throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        // System.out.println(hmPrx.getTracks(discTitle));
        String tracks = hmPrx.viewDiscTracks(discTitle, EOL);
        System.out.println(tracks);

        // Wait for enter
        in.readLine();
        menu(hmPrx);
    }

    /**
     * Send Shutdown command to HomeManager
     * 
     * @param hmPrx
     *            HomeManagerPrx Ice Object for RPC/RMI communications.
     */
    private static void exit(HMPrx hmPrx) {
        hmPrx.shutdown();
    }

    /**
     * Entry point into SmartHomeUI
     * 
     * @param args
     *            command line arguments (Should be none).
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("USAGE: SmartHomeUI.java");
            System.exit(1);
        }
        SmartHomeUI smartHomeUI = new SmartHomeUI();
        smartHomeUI.main("SmartHomeUI", args);
    }

    /**
     * Main Function for ICE functionality.
     */
    @Override
    public int run(String[] args) {

        Ice.ObjectAdapter hmListener;
        int port;

        try {
            hmListener = communicator().createObjectAdapterWithEndpoints("UI",
                    "tcp -h 127.0.0.1 -p 12003");
            port = 12003;
        } catch (Exception e) {
            try {
                hmListener = communicator().createObjectAdapterWithEndpoints(
                        "UI", "tcp -h 127.0.0.1 -p 12004");
                port = 12004;
            } catch (Exception ex) {
                communicator().destroy();
                System.err.println("Too many User Interface instances");
                return 1;
            }
        }

        hmListener.add(new UII(), communicator().stringToIdentity("ui"));
        hmListener.activate();

        Ice.ObjectPrx obj = communicator().stringToProxy(
                "hm:tcp -h 127.0.0.1 -p 12001");
        HMPrx hmPrx = HMPrxHelper.uncheckedCast(obj);

        shutdownOnInterrupt();

        try {
            welcomeMessage(hmPrx, port);
            menu(hmPrx);
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }

        communicator().destroy();
        return 0;
    }
}