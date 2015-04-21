package smarthome;

import java.io.FileNotFoundException;
import java.util.Arrays;

import sensors.EnergySensor;
import sensors.LocationSensor;
import sensors.TemperatureSensor;

/**
 * Entry point to run Sensors for SmartHome
 */
public class Sensor {
    private static final String TYPES[] = { "temperature", "energy", "location" };

    /**
     * Constructor of a Sensor
     * 
     * @param args
     *            command line arguments
     * @throws FileNotFoundException
     */
    public Sensor(String[] args) throws FileNotFoundException {
        switch (args[0]) {
        case "temperature":
            TemperatureSensor tempSensor = new TemperatureSensor(args[1]);
            tempSensor.main("TempSensor", args, "config.pub");
            break;
        case "energy":
            EnergySensor energySensor = new EnergySensor(args[1]);
            energySensor.main("EnergySensor", args, "config.pub");
            break;
        case "location":
            LocationSensor locationSensor = new LocationSensor(args[1]);
            locationSensor.main("LocationSensor", args, "config.pub");
            break;
        }
    }

    /**
     * Entry point to sensors
     * 
     * @param args
     *            command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("USAGE: Sensor.java type predefined-data-file");
            System.exit(1);
        }

        String type = args[0];
        if (!Arrays.asList(TYPES).contains(type)) {
            System.err.println("INVALID TYPE: " + type);
            System.exit(1);
        }

        try {
            new Sensor(args);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + args[1]);
            System.exit(1);
        }
    }

}