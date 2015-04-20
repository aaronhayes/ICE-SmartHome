package sensors;

import java.io.FileNotFoundException;
import java.io.IOException;

import csse4004.TempSensorPrx;
import csse4004.TempSensorPrxHelper;
import csse4004.TempSensorWarningPrx;
import csse4004.TempSensorWarningPrxHelper;

/**
 * Temperature Sensor
 */
public class TemperatureSensor extends SensorAbstract {
	
	/**
	 * Constructor for Temperature Sensor
	 * @param dataFile String of datafile location
	 * @throws FileNotFoundException
	 */
	public TemperatureSensor(String dataFile) throws FileNotFoundException {
		super(dataFile);
	}
	
	/**
	 * ICE main function
	 *  - Get Publisher on the temperature log and temperature alert topics
	 *  - Publish log reading every second
	 *  - Publish alert messages if required.
	 */
	@Override
	public int run(String[] args) {
		IceStorm.TopicPrx shutdown = getShutdownTopic();
		Ice.ObjectPrx shutdownObjPrx = getIceStormShutdownSubscriber(shutdown);
		
		IceStorm.TopicPrx log = getTopic("temperatureLog");
		IceStorm.TopicPrx alert = getTopic("temperatureAlert");
		
		Ice.ObjectPrx publisher  = log.getPublisher().ice_oneway();
		Ice.ObjectPrx alertPublisher = alert.getPublisher().ice_oneway();
		
		TempSensorPrx tempSensor = TempSensorPrxHelper.uncheckedCast(publisher);
		TempSensorWarningPrx tempSensorWarning = TempSensorWarningPrxHelper
				.uncheckedCast(alertPublisher);
		
		while (!communicator().isShutdown()) {
			try {
				String[] data = readData().split(",");
				int value = Integer.parseInt(data[0]);
				int time = Integer.parseInt(data[1]);
				for (int i = 0; i <= time; i++) {
					if (communicator().isShutdown()) {
						break;
					}
					tempSensor.logData(value);
					if (value < 15 || value > 28) {
						tempSensorWarning.temperatureAlert(value);
					}
					Thread.sleep(1000);
				}				
			} catch (IOException e) {
				e.printStackTrace();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}


		shutdown.unsubscribe(shutdownObjPrx);
		log.destroy();
		alert.destroy();
		
		communicator().destroy();
		return 0;
	}
}
