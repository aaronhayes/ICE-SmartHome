package smarthome;

import java.util.ArrayList;
import java.util.HashMap;

import Ice.Current;
import IceStorm.AlreadySubscribed;
import IceStorm.BadQoS;
import csse4004.EMMPrx;
import csse4004.EMMPrxHelper;
import csse4004.ShutdownPrx;
import csse4004.ShutdownPrxHelper;
import csse4004.UIPrx;
import csse4004.UIPrxHelper;
import csse4004._EnergySensorDisp;
import csse4004._HMDisp;
import csse4004._LocationSensorDisp;
import csse4004._TempSensorDisp;
import csse4004._TempSensorWarningDisp;
import datastore.AdjustmentLogData;

/**
 * Home Manager for Smart Home.
 *
 */
public class HomeManager extends HomeManagerAbstract {
    
    private int activeUsers = 0;
    
    private int energy;
    private ArrayList<AdjustmentLogData> logData = new ArrayList<AdjustmentLogData>();
    
    private HashMap<String, String> userLocations = new HashMap<String, String>();
    
    private ShutdownPrx shutdownPrx;
    
    private IceStorm.TopicPrx locationTopic;
    private IceStorm.TopicPrx tempLogTopic;
    private IceStorm.TopicPrx tempWarningTopic;
    
    private Ice.ObjectPrx locationSubscriber = null;
    private Ice.ObjectPrx tempLogSubscriber = null;
    private Ice.ObjectPrx tempWarningSubscriber = null;
    
    private boolean adjustingTemperature = false;
    
    /**
     * Get the status of home occupants. 
     * @return true if all users are away, false otherwise.
     */
    private boolean areAllOccupantsAway() {
        return !userLocations.containsValue("home");
    }
    
    /**
     * Class for temperature adjustments to run in new Thread
     */
    public class TemperatureAdjustment extends Thread {
        
        public TemperatureAdjustment(int value, HashMap<String, String> users) {
            logData.add(new AdjustmentLogData(value, users));
        }
        
        public void run() {
            adjustingTemperature = true;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            adjustingTemperature = false;
        }
    }
    
    /**
     * Location Sensor ICEStorm Subscriber Class
     *
     */
    @SuppressWarnings("serial")
    public class LocationSensorI extends _LocationSensorDisp {
        
        @Override
        public void logLocation(String name, String location, Current __current) {
            if (adjustingTemperature) {
                return;
            }
            
            String loc = userLocations.put(name, location);
            
            if (location.equals(loc)) {
                return;
            }
            
            if (areAllOccupantsAway()) {
                // Subscribe to Warnings Only
                tempLogTopic.unsubscribe(tempLogSubscriber);
                
                if (tempWarningSubscriber == null) {
                    tempWarningSubscriber = getIceStormSubscriber(tempWarningTopic,
                            "SmartHouse.TempSensorWarning", new TempSensorWarningI());
                }
                
                try {
                    tempWarningTopic.subscribeAndGetPublisher(
                            new HashMap<String, String>(), tempWarningSubscriber);
                } catch (AlreadySubscribed e) {
                } catch (BadQoS e) {
                }

            } else {
                // Subscribe to Temperature Secondly
                tempWarningTopic.unsubscribe(tempWarningSubscriber);
                
                if (tempLogSubscriber == null) {
                    tempLogSubscriber = getIceStormSubscriber(tempLogTopic,
                            "SmartHouse.TempSensor", new TempSensorI());
                }
                
                try {
                    tempLogTopic.subscribeAndGetPublisher(
                            new HashMap<String, String>(), tempLogSubscriber);
                } catch (AlreadySubscribed e) {
                } catch (BadQoS e) {
                }
                
            }
        }
    }

    /**
     * Temperature Sensor ICEStorm Subscriber Class
     */
    @SuppressWarnings("serial")
    public class TempSensorI extends _TempSensorDisp {

        @Override
        public void logData(int value, Current __current) {
            if (adjustingTemperature) {
                return;
            }
            
            if (value != 22 && !areAllOccupantsAway()) {
                (new TemperatureAdjustment(value, userLocations)).start();
            }
        }
    }

    /**
     * Temperature Warning ICEStorm Subscriber Class
     */
    @SuppressWarnings("serial")
    public class TempSensorWarningI extends _TempSensorWarningDisp {

        @Override
        public void temperatureAlert(int value, Current __current) {
            if (adjustingTemperature) {
                return;
            }
            (new TemperatureAdjustment(value, userLocations)).start();
        }
    }

    /**
     * Energy Sensor ICEStorm Subscriber Class
     */
    @SuppressWarnings("serial")
    public class EnergySensorI extends _EnergySensorDisp {
        private UIPrx user1;
        private UIPrx user2;

        public EnergySensorI(UIPrx uiPrx, UIPrx uiPrx2) {
            super();
            this.user1 = uiPrx;
            this.user2 = uiPrx2;
        }

        /**
         * Received an energy usage, alert the UI if value is different and
         * above the 4000W limit. Check for connection refused exception in case
         * socket broken.
         * 
         * @param value
         *            Current Energy Usage Value
         */
        @Override
        public void energyAlert(int value, Current __current) {
            if (energy != value && value > 4000) {
                try {
                    user1.highEnergyWarning(value);
                } catch (Ice.ConnectionRefusedException e) {
                    // Ignore
                }
                try {
                    user2.highEnergyWarning(value);
                } catch (Ice.ConnectionRefusedException e) {
                    // Ignore
                }
            }
            energy = value;
        }
    }

    /**
     * ICE RPC/RMI listener to allow User Interface to communicate with the Home
     * Manager.
     */
    @SuppressWarnings("serial")
    public class HMI extends _HMDisp {
        private EMMPrx emmPrx;

        /**
         * Constructor for HMI
         * 
         * @param emmPrx
         *            EMMPrx Ice Object for RPC/RPI communication with EMM.
         */
        public HMI(EMMPrx emmPrx) {
            super();
            this.emmPrx = emmPrx;
        }

        /**
         * Shutdown request from User. 
         * Send Shutdown notice to sensors/emm.
         * 
         */
        @Override
        public void shutdown(Current __current) {
            if (--activeUsers > 0) {
                return;
            }
            
            try {
                this.emmPrx.shutdown();
            } catch (Ice.ConnectionRefusedException e) {
                // Ignore
            }
            shutdownPrx.shutdownRequest();
            communicator().shutdown();
        }

        /**
         * viewLog request from User
         * 
         * @param eol
         *            String end of line Character of User Interface System.
         * @return String of current log or log empty message.
         */
        @Override
        public String viewLog(String eol, Current __current) {
            if (logData.isEmpty()) {
                return "Log of temperature adjustment is empty";
            }
            
            return logData.get(logData.size() - 1).toString(eol);
        }

        /**
         * viewMediaFiles request from User. Collect information about All media
         * on EMM. Notify user of data.
         * 
         * @param eol
         *            String end of line Character of User Interface System.
         * @return String of All media files or no files found message.
         */
        @Override
        public String viewMediaFiles(String eol, Current __current) {
            try {
                String data = this.emmPrx.getFiles();
                if (data.length() == 0) {
                    return "No media files were found" + eol;
                }
                String[] files = data.split(",");

                StringBuilder builder = new StringBuilder();
                for (String file : files) {
                    String title = this.emmPrx.getTitle(file);
                    String disc = this.emmPrx.getDisc(file);
                    builder.append(file);
                    builder.append(", ");
                    builder.append(title);
                    builder.append(", ");
                    builder.append(disc);
                    builder.append(eol);
                }
                return builder.toString();
            } catch (Ice.ConnectionRefusedException e) {
                return "No media files were found" + eol;
            }
        }

        /**
         * viewDiscTracks Request from User. 
         *  Get information about tracks on the disc.
         * 
         * @param eol
         *            String end of line Character of User Interface System.
         * @param discTitle
         *            String title of Disc (Album Title)
         * @return list of tracks on disc in order or disc not found message.
         */
        @Override
        public String viewDiscTracks(String discTitle, String eol,
                Current __current) {
            try {
                String data = this.emmPrx.getTracks(discTitle);
                if (data.length() == 0) {
                    return "The disc " + discTitle
                            + " was not found in the media collection" + eol;
                }
                String[] tracks = data.split(",");

                StringBuilder builder = new StringBuilder();
                for (String track : tracks) {
                    builder.append(track);
                    builder.append(eol);
                }
                return builder.toString();
            } catch (Ice.ConnectionRefusedException e) {
                return "The disc " + discTitle
                        + " was not found in the media collection" + eol;
            }
        }

        /**
         * Register a user as a UI instance. Can support a maximum of 2 users.
         * 
         * @param username
         *            Name of the user
         * @param port
         *            port number user is using
         */
        @Override
        public void registerUser(String username, int port, Current __current) {
            activeUsers++;
        }
    }

    /**
     * Entry point into Home Manager.
     * 
     * @param args
     *            Command line arguments (should be none).
     */
    public static void main(String[] args) {
        if (args.length != 0) {
            System.err.println("USAGE: HomeManager.java");
            System.exit(1);
        }
        HomeManager hm = new HomeManager();
        int status = hm.main("HomeManager", args, "config.sub");
        System.exit(status);
    }

    /**
     * ICE main function 
     * - Subscribe to Topics 
     * - Setup Listener for UI to communicate to 
     * - Setup Communication to UI and EMM 
     * - Wait for shutdown 
     * - Unsubscribe
     * 
     * @param args
     *            ICE arguments
     */
    @Override
    public int run(String[] args) {

        Ice.ObjectPrx obj = communicator().stringToProxy(
                "emm:tcp -h 127.0.0.1 -p 12002");
        EMMPrx emmPrx = EMMPrxHelper.uncheckedCast(obj);

        Ice.ObjectPrx uiObj = communicator().stringToProxy(
                "ui:tcp -h 127.0.0.1 -p 12003");
        UIPrx uiPrx = UIPrxHelper.uncheckedCast(uiObj);

        Ice.ObjectPrx uiObj2 = communicator().stringToProxy(
                "ui:tcp -h 127.0.0.1 -p 12004");
        UIPrx uiPrx2 = UIPrxHelper.uncheckedCast(uiObj2);

        IceStorm.TopicPrx shutdownTopic = getIceStormTopic("shutdown");
        IceStorm.TopicPrx energyTopic = getIceStormTopic("energy");
        
        
        this.tempLogTopic = getIceStormTopic("temperatureLog");
        this.tempWarningTopic = getIceStormTopic("temperatureAlert");
        this.locationTopic = getIceStormTopic("location");

        Ice.ObjectPrx shutdownPublisher = shutdownTopic.getPublisher()
                .ice_oneway();

        this.shutdownPrx = ShutdownPrxHelper.uncheckedCast(shutdownPublisher);

        Ice.ObjectPrx energySubscriber = getIceStormSubscriber(energyTopic,
                "SmartHouse.EnergySensor", new EnergySensorI(uiPrx, uiPrx2));

        this.locationSubscriber = getIceStormSubscriber(locationTopic,
                "SmartHouse.LocationSensor", new LocationSensorI());

        Ice.ObjectAdapter uiListener = communicator()
                .createObjectAdapterWithEndpoints("HM", "tcp -h 127.0.0.1 -p 12001");

        uiListener.add(new HMI(emmPrx), communicator().stringToIdentity("hm"));
        uiListener.activate();

        shutdownOnInterrupt();
        communicator().waitForShutdown();

        // Unsubscribe from topics
        energyTopic.unsubscribe(energySubscriber);
        this.tempLogTopic.unsubscribe(this.tempLogSubscriber);
        this.tempWarningTopic.unsubscribe(this.tempWarningSubscriber);
        this.locationTopic.unsubscribe(this.locationSubscriber);

        // Give Sensors a chance to receive shutdown message
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        shutdownTopic.destroy();
        communicator().destroy();
        return 0;
    }
}