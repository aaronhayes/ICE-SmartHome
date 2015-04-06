package sensors;

import java.io.FileNotFoundException;
import java.io.IOException;

import csse4004.EnergySensorPrx;
import csse4004.EnergySensorPrxHelper;

/**
 * Energy Sensor
 */
public class EnergySensor extends SensorAbstract {

	public EnergySensor(String dataFile) throws FileNotFoundException {
		super(dataFile);
	}

	@Override
	public int run(String[] args) {
		Ice.ObjectPrx publisher = getPublisher("energy");
		
		EnergySensorPrx energySensor = EnergySensorPrxHelper
				.uncheckedCast(publisher);
		
		while (true) {
			try {
				String[] data = readData(true).split(",");
				int value = Integer.parseInt(data[0]);
				int time = Integer.parseInt(data[1]);
				for (int i = 0; i <= time; i++) {
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
		
		return 0;
	}

}
