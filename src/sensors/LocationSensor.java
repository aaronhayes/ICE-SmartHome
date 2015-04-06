package sensors;

import java.io.FileNotFoundException;
import java.io.IOException;

import csse4004.LocationSensorPrx;
import csse4004.LocationSensorPrxHelper;

/**
 * Location Sensor
 *
 */
public class LocationSensor extends SensorAbstract {
	private String user;
	
	/**
	 * Constructor for Location Sensor
	 * @param dataFile
	 * @throws FileNotFoundException
	 */
	public LocationSensor(String dataFile) throws FileNotFoundException {
		super(dataFile);
		this.user = dataFile;	// use the filename as the user
	}

	/**
	 * ICE main function
	 *  - Get Publisher on the location topic
	 *  - Publish log reading every second
	 */
	@Override
	public int run(String[] args) {
		Ice.ObjectPrx publisher = getPublisher("location");
		
		LocationSensorPrx location = LocationSensorPrxHelper.uncheckedCast(publisher);
		
		while (true) {
			try {
				String[] data = readData().split(",");
				String value = data[0];
				int time = Integer.parseInt(data[1]);
				for (int i = 0; i <= time; i++) {
					location.logLocation(user, value);
					Thread.sleep(1000);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
		}
		
		return 0;
	}

}
