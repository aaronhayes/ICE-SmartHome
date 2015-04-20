package csse;

import java.util.HashMap;

import Ice.Current;
import Ice.ObjectPrx;
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

/**
 * Home Manager for Smart Home.
 *
 */
public class HomeManager extends Ice.Application {
	private final String PROXY = "TopicManager.Proxy";
	
	private int temperature;
	private boolean logSet = false;
	private int energy;
	private HashMap<String, String> userLocations = new HashMap<String, String>();
	private ShutdownPrx shutdownPrx;
	
	/**
	 * Location Sensor ICEStorm Subscriber Class
	 *
	 */
	@SuppressWarnings("serial")
	public class LocationSensorI extends _LocationSensorDisp {

		@Override
		public void logLocation(String name, String location, Current __current) {
			userLocations.put(name, location);
		}
	}
	
	/**
	 * Temperature Sensor ICEStorm Subscriber Class
	 */
	@SuppressWarnings("serial")
	public class TempSensorI extends _TempSensorDisp {

		@Override
		public void logData(int value, Current __current) {
			logSet = true;
			temperature = value;
		}
	}
	
	/**
	 * Temperature Warning ICEStorm Subscriber Class
	 */
	@SuppressWarnings("serial")
	public class TempSensorWarningI extends _TempSensorWarningDisp {

		@Override
		public void temperatureAlert(int value, Current __current) {
			System.err.println("WARNING: TEMPERATURE IS: " + value);
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
		 * Received an energy usage, alert the UI if value is different 
		 * 	and above the 4000W limit.
		 * 	Check for connection refused exception in case socket broken.
		 * @param value Current Energy Usage Value
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
	 * ICE RPC/RMI listener to allow User Interface to communicate
	 * 	with the Home Manager.
	 */
	@SuppressWarnings("serial")
	public class HMI extends _HMDisp {
		private EMMPrx emmPrx;
		
		/**
		 * Constructor for HMI
		 * @param emmPrx EMMPrx Ice Object for RPC/RPI communication with EMM.
		 */
		public HMI(EMMPrx emmPrx) {
			super();
			this.emmPrx = emmPrx;
		}

		/**
		 * Shutdown request from User.
		 * 	Send Shutdown notice to sensors/emm.
		 */
		@Override
		public void shutdown(Current __current) {
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
		 * @param eol String end of line Character of User Interface System.
		 * @return String of current log or log empty message.
		 */
		@Override
		public String viewLog(String eol, Current __current) {
			if (!logSet) {
				return "Log of temperature adjustment is empty";
			}
			
			StringBuilder builder = new StringBuilder();
			builder.append("Air-conditoning adjusted.");
			builder.append(eol);
			builder.append("Temperature: at ");
			builder.append(temperature);
			builder.append(" degrees");
			builder.append(eol);
			builder.append("At Home: ");
			//TODO AT HOme shit
			return builder.toString();
		}

		/**
		 * viewMediaFiles request from User.
		 * 	Collect information about All media on EMM.
		 * 	Notify user of data.
		 * @param eol String end of line Character of User Interface System.
		 * @return String of All media files or no files found message.
		 */
		@Override
		public String viewMediaFiles(String eol, Current __current) {
			try {
				String[] files = this.emmPrx.getFiles().split(",");
				if (files.length == 0) {
					return "No media files were found";
				}
				StringBuilder builder = new StringBuilder();
				for (String file: files) {
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
				return "No media files were found";
			}
		}

		/**
		 * viewDiscTracks Request from User.
		 *  Get information about tracks on the disc.
		 * @param eol String end of line Character of User Interface System.
		 * @param discTitle String title of Disc (Album Title)
		 * @return list of tracks on disc in order or disc not found message.
		 */
		@Override
		public String viewDiscTracks(String discTitle, String eol, Current __current) {
			try {
				String[] tracks = this.emmPrx.getTracks(discTitle).split(",");
				StringBuilder builder = new StringBuilder();
				for (String track: tracks) {
					builder.append(track);
					builder.append(eol);
				}
				return builder.toString();
			} catch (Ice.ConnectionRefusedException e) {
				return "The disc " + discTitle 
						+ " was not found in the media collection"; 
			}
		}
	}
	
	/**
	 * Get ICEStorm Topic Prx from TopicName String
	 * @param topicName Name of Topic
	 * @return ICEStorm Topic Prx
	 */
	private IceStorm.TopicPrx getIceStormTopic(String topicName) {
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
	 * 	- Create Adapter
	 * 	- Generate random ID for subscriber.
	 * 	- Active Adapter
	 * 	- Subscribe and Get Publisher for Topic.
	 * @param topic IceStorm Topic Prx
	 * @param adapterName String Name of ICEStorm Adapter
	 * @param iceObject ICE Object to add to adapter
	 * @return ICEStorm Subscriber of topic
	 */
	private Ice.ObjectPrx getIceStormSubscriber(IceStorm.TopicPrx topic, 
			String adapterName, Ice.Object iceObject) {
		Ice.ObjectAdapter adapter = communicator().createObjectAdapter(adapterName);
		
		Ice.Identity id = new Ice.Identity(null, "");
		id.name = java.util.UUID.randomUUID().toString();
		
		ObjectPrx subscriber = adapter.add(iceObject, id);
		adapter.activate();
		subscriber.ice_oneway();
		
		try {
			topic.subscribeAndGetPublisher(new HashMap<String, String>(), subscriber);
		} catch (IceStorm.AlreadySubscribed e) {
			return subscriber;
		} catch (BadQoS e) {
			e.printStackTrace();
		}
		
		return subscriber;
	}
	
	/**
	 * Entry point into Home Manager.
	 * @param args Command line arguments (should be none).
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
	 * 	- Subscribe to Topics
	 *  - Setup Listener for UI to communicate to
	 *  - Setup Communication to UI and EMM
	 *  - Wait for shutdown
	 *  - Unsubscribe
	 *  @param args ICE arguments
	 */
	@Override
	public int run(String[] args) {
		
		Ice.ObjectPrx obj = communicator().stringToProxy("emm:tcp -h 127.0.0.1 -p 12002");
		EMMPrx emmPrx = EMMPrxHelper.uncheckedCast(obj);
		
		Ice.ObjectPrx uiObj = communicator().stringToProxy("ui:tcp -h 127.0.0.1 -p 12003");
		UIPrx uiPrx = UIPrxHelper.uncheckedCast(uiObj);
		
		Ice.ObjectPrx uiObj2 = communicator().stringToProxy("ui:tcp -h 127.0.0.1 -p 12004");
		UIPrx uiPrx2 = UIPrxHelper.uncheckedCast(uiObj2);
		
		IceStorm.TopicPrx shutdownTopic = getIceStormTopic("shutdown");
		IceStorm.TopicPrx tempLogTopic = getIceStormTopic("temperatureLog");
		IceStorm.TopicPrx tempAlertTopic = getIceStormTopic("temperatureAlert");
		IceStorm.TopicPrx energyTopic = getIceStormTopic("energy");
		IceStorm.TopicPrx locationTopic = getIceStormTopic("location");
		
		Ice.ObjectPrx shutdownPublisher = shutdownTopic.getPublisher().ice_oneway();
		this.shutdownPrx = ShutdownPrxHelper.uncheckedCast(shutdownPublisher);
		
		Ice.ObjectPrx tempLogSubscriber = getIceStormSubscriber(tempLogTopic, 
				"SmartHouse.TempSensor", new TempSensorI());
		
		Ice.ObjectPrx tempAlertSubscriber = getIceStormSubscriber(tempAlertTopic, 
				"SmartHouse.TempSensorWarning", new TempSensorWarningI());
		
		Ice.ObjectPrx energySubscriber = getIceStormSubscriber(energyTopic, 
				"SmartHouse.EnergySensor", new EnergySensorI(uiPrx, uiPrx2));
		
		Ice.ObjectPrx locationSubscriber = getIceStormSubscriber(locationTopic, 
				"SmartHouse.LocationSensor", new LocationSensorI());
		
		Ice.ObjectAdapter uiListener = communicator()
				.createObjectAdapterWithEndpoints("HM", "tcp -h 127.0.0.1 -p 12001");
		
		uiListener.add(new HMI(emmPrx), communicator().stringToIdentity("hm"));
		uiListener.activate();
		
		shutdownOnInterrupt();
		communicator().waitForShutdown();
		
		tempLogTopic.unsubscribe(tempLogSubscriber);
		tempAlertTopic.unsubscribe(tempAlertSubscriber);
		energyTopic.unsubscribe(energySubscriber);
		locationTopic.unsubscribe(locationSubscriber);
		
		// Give Sensors a chance to shutdown
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