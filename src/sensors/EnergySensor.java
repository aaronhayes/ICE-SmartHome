package sensors;

import java.io.FileNotFoundException;
import java.io.IOException;

import csse4004.EnergySensorPrx;
import csse4004.EnergySensorPrxHelper;

/**
 * Energy Sensor
 */
public class EnergySensor extends SensorAbstract {

	/**
	 * Constructor for Energy Sensor
	 * @param dataFile String of datafile location
	 * @throws FileNotFoundException
	 */
	public EnergySensor(String dataFile) throws FileNotFoundException {
		super(dataFile);
	}

	/**
	 * ICE main function
	 *  - Get Publisher on the energy topic
	 *  - Publish energy reading every second
	 */
	@Override
	public int run(String[] args) {
		IceStorm.TopicPrx shutdown = getShutdownTopic();
		Ice.ObjectPrx shutdownObjPrx = getIceStormShutdownSubscriber(shutdown);
		
		
		IceStorm.TopicPrx topic = getTopic("energy");
		Ice.ObjectPrx publisher = topic.getPublisher().ice_oneway();
		
		EnergySensorPrx energySensor = EnergySensorPrxHelper
				.uncheckedCast(publisher);
		
		while (!communicator().isShutdown()) {
			try {
				String[] data = readData().split(",");
				int value = Integer.parseInt(data[0]);
				int time = Integer.parseInt(data[1]);
				for (int i = 0; i <= time; i++) {
					if (communicator().isShutdown()) {
						break;
					}
					energySensor.energyAlert(value);
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		shutdown.unsubscribe(shutdownObjPrx);
		topic.destroy();
		communicator().destroy();
		return 0;
	}

}
