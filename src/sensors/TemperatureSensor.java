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
	public TemperatureSensor(String dataFile) throws FileNotFoundException {
		super(dataFile);
	}
	
	@Override
	public int run(String[] args) {
		Ice.ObjectPrx publisher  = getPublisher("temperatureLog");
		Ice.ObjectPrx alertPublisher = getPublisher("temperatureAlert");
		
		TempSensorPrx tempSensor = TempSensorPrxHelper
				.uncheckedCast(publisher);
		
		TempSensorWarningPrx tempSensorWarning = TempSensorWarningPrxHelper
				.uncheckedCast(alertPublisher);
		
		while (true) {
			try {
				String[] data = readData(true).split(",");
				int value = Integer.parseInt(data[0]);
				int time = Integer.parseInt(data[1]);
				for (int i = 0; i <= time; i++) {
					tempSensor.logData(value);
					if (value == 0) {
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

		return 0;
	}

}
