module csse4004 {
	interface EMM {
    	string getTitle(string fileName);
    	string getDisc(string fileName);
    	string getTracks(string disc);
    	string getFiles();
    	void shutdown();
	};
	
	interface HM {
		string viewLog(string eol);
		string viewMediaFiles(string eol);
		string viewDiscTracks(string discTitle, string eol);
		void shutdown();
	};
	
	interface UI {
		void highEnergyWarning(int value);
	};
	
	interface TempSensor {
		void logData(int value);
	};
	
	interface TempSensorWarning {
		void temperatureAlert(int value);
	};
	
	interface EnergySensor {
		void energyAlert(int value);
	};
	
	interface LocationSensor {
		void logLocation(string name, string location);
	};
	
	interface Shutdown {
		void shutdownRequest();
	};
};