package smarthome;

import java.util.HashMap;

import Ice.ObjectPrx;
import IceStorm.BadQoS;

/**
 * Abstract for HomeManager to provide modularity and code simplicity.
 * 
 */
public abstract class HomeManagerAbstract extends Ice.Application {
    private final String PROXY = "TopicManager.Proxy";
    
    /**
     * Get ICEStorm Topic Prx from TopicName String
     * 
     * @param topicName
     *            Name of Topic
     * @return ICEStorm Topic Prx
     */
    protected IceStorm.TopicPrx getIceStormTopic(String topicName) {
        IceStorm.TopicManagerPrx manager = IceStorm.TopicManagerPrxHelper
                .uncheckedCast(communicator().propertyToProxy(PROXY));

        if (manager == null) {
            System.err.println("Invalid IceStorm Proxy");
            return null;
        }

        IceStorm.TopicPrx topic;
        try {
            topic = manager.retrieve(topicName);
        } catch (IceStorm.NoSuchTopic ex) {
            try {
                topic = manager.create(topicName);
            } catch (IceStorm.TopicExists e) {
                System.err.println("IceStorm Topic Error");
                return null;
            }
        }
        return topic;
    }

    /**
     * Get ICEStorm Subscriber. Using ice_oneway communications. 
     * - Create Adapter 
     * - Generate random ID for subscriber. 
     * - Active Adapter 
     * - Subscribe and Get Publisher for Topic.
     * 
     * @param topic
     *            IceStorm Topic Prx
     * @param adapterName
     *            String Name of ICEStorm Adapter
     * @param iceObject
     *            ICE Object to add to adapter
     * @return ICEStorm Subscriber of topic
     */
    protected Ice.ObjectPrx getIceStormSubscriber(IceStorm.TopicPrx topic,
            String adapterName, Ice.Object iceObject) {
        
        Ice.ObjectAdapter adapter = communicator().createObjectAdapter(adapterName);
        
        Ice.Identity id = new Ice.Identity(null, "");
        id.name = java.util.UUID.randomUUID().toString();

        ObjectPrx subscriber = adapter.add(iceObject, id);
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
}
