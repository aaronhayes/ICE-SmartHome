package sensors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import IceStorm.TopicExists;

/**
 * Abstract for sensors
 */
public abstract class SensorAbstract extends Ice.Application {
	private RandomAccessFile file;
	private String fileName;
	private final String PROXY = "TopicManager.Proxy";
	
	/**
	 * Constructor.
	 * @param dataFile name of file (String)
	 * @throws FileNotFoundException
	 */
	public SensorAbstract(String dataFile) throws FileNotFoundException {
		this.file = new RandomAccessFile(dataFile, "r");
		this.fileName = dataFile;
	}
	
	/**
	 * Get File Name
	 * @return String of the File Name
	 */
	protected String getFileName() {
		return this.fileName;
	}

	/**
	 * Read a line from the File
	 * @return Next line from the file
	 * @throws IOException
	 */
	private String getLine() throws IOException {
		return this.file.readLine();
	}
	
	/**
	 * Read data from predefined data file.
	 * 
	 * @param repeat boolean to indicate if the file should return
	 * 	to the beginning once EOF is reached.
	 * @return Next line from the file.
	 * @throws IOException
	 */
	protected String readData(boolean repeat) throws IOException {
		String line = getLine();
		if (line == null && repeat == true) {
			this.file.seek(0);
			line = getLine();
		}
		
		return line;
	}
	
	/**
	 * Get the Ice.ObjectPrx for the publisher of a topic.
	 * @param topicName String of the topic to publish
	 * @return Ice.ObjectPrx publisher.
	 */
	protected Ice.ObjectPrx getPublisher(String topicName) {
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
		
		return topic.getPublisher().ice_oneway();
	}
}
