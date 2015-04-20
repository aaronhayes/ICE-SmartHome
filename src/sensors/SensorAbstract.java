package sensors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import Ice.Current;
import Ice.ObjectPrx;
import IceStorm.BadQoS;
import IceStorm.TopicExists;
import csse4004._ShutdownDisp;

/**
 * Abstract for sensors
 */
public abstract class SensorAbstract extends Ice.Application {
    private RandomAccessFile file;
    private String fileName;
    private final String PROXY = "TopicManager.Proxy";

    /**
     * Constructor.
     * 
     * @param dataFile
     *            name of file (String)
     * @throws FileNotFoundException
     */
    public SensorAbstract(String dataFile) throws FileNotFoundException {
        this.file = new RandomAccessFile(dataFile, "r");
        this.fileName = dataFile;
    }

    /**
     * Get File Name
     * 
     * @return String of the File Name
     */
    protected String getFileName() {
        return this.fileName;
    }

    /**
     * Read a line from the File
     * 
     * @return Next line from the file
     * @throws IOException
     */
    private String getLine() throws IOException {
        return this.file.readLine();
    }

    /**
     * Read data from predefined data file.
     * 
     * @return Next line from the file.
     * @throws IOException
     */
    protected String readData() throws IOException {
        String line = getLine();
        if (line == null) {
            this.file.seek(0);
            line = getLine();
        }

        return line;
    }

    /**
     * Get the IceStorm.TopcPrx object of a topic.
     * 
     * @param topicName
     *            String of the topic to publish
     * @return IceStorm.TopicPrx topic publisher.
     */
    protected IceStorm.TopicPrx getTopic(String topicName) {
        IceStorm.TopicManagerPrx manager = IceStorm.TopicManagerPrxHelper
                .checkedCast(communicator().propertyToProxy(PROXY));

        IceStorm.TopicPrx topic = null;

        if (manager == null) {
            System.err.println("Invalid IceStorm Proxy.");
            System.exit(1);
        }

        try {
            topic = manager.retrieve(topicName);
        } catch (IceStorm.NoSuchTopic e) {
            try {
                topic = manager.create(topicName);
            } catch (TopicExists e1) {
                e1.printStackTrace();
                System.exit(1);
            }
        }

        return topic;
    }

    /**
     * Get Shutdown Topic from ICEStorm
     * 
     * @return IceStorm Shutdown TopicPrx
     */
    protected IceStorm.TopicPrx getShutdownTopic() {
        return getTopic("shutdown");
    }

    /**
     * Get ICEStorm Subscriber. Using ice_oneway communications. 
     * - Create Adapter 
     * - Generate random ID for subscriber. 
     * - Active Adapter 
     * - Subscribe and Get Publisher for Shutdown Topic.
     * 
     * @param topic
     *            ICEStorm topic Prx
     * @return ICEStorm Subscriber of Shutdown Topic
     */
    protected Ice.ObjectPrx getIceStormShutdownSubscriber(
            IceStorm.TopicPrx topic) {
        Ice.ObjectAdapter adapter = communicator()
                .createObjectAdapterWithEndpoints("SmartHouse.Shutdown",
                        "tcp:udp");

        Ice.Identity id = new Ice.Identity(null, "");
        id.name = java.util.UUID.randomUUID().toString();

        ObjectPrx subscriber = adapter.add(new ShutdownI(), id);
        adapter.activate();
        subscriber.ice_oneway();

        try {
            topic.subscribeAndGetPublisher(new HashMap<String, String>(), 
                    subscriber);
        } catch (IceStorm.AlreadySubscribed e) {
            return subscriber;
        } catch (BadQoS e) {
            e.printStackTrace();
        }

        return subscriber;
    }

    /**
     * Shutdown ICEStorm Subscriber Class
     */
    @SuppressWarnings("serial")
    public class ShutdownI extends _ShutdownDisp {
        @Override
        public void shutdownRequest(Current __current) {
            communicator().shutdown();
        }
    }
}
