package datastore;

import java.util.HashMap;
import java.util.Map;

/**
 * Data Storage of Temperature Adjustment Logs
 *
 */
public class AdjustmentLogData {
    private int temperature;
    private HashMap<String, String> users;

    /**
     * Basic Constructor of Adjustment Log
     * 
     * @param temperature
     *            temperature
     * @param users
     *            HashMap of Users and their location
     */
    public AdjustmentLogData(int temperature, HashMap<String, String> users) {
        this.temperature = temperature;
        this.users = users;
    }

    /**
     * Get Temperature Value of Log
     * 
     * @return temperature value
     */
    public int getTemperature() {
        return this.temperature;
    }

    /**
     * Convert HashMap of user locations to UI friendly String
     * 
     * @return String of Users At home
     */
    public String usersAtHomeSring() {
        int count = 0;
        StringBuilder sb = new StringBuilder();

        sb.append("At Home: ");

        for (Map.Entry<String, String> user : users.entrySet()) {
            String name = user.getKey();
            String location = user.getValue();
            if (location.equals("home")) {
                if (count++ > 0) {
                    sb.append(" and ");
                }
                sb.append(name);
            }
        }

        return sb.toString();
    }

    /**
     * Convert the log message into UI friendly format, as per specifications.
     * 
     * @param EOL
     *            End of Line Character to Use.
     * @return Properly formatted log message for UI.
     */
    public String toString(String EOL) {
        StringBuilder builder = new StringBuilder();
        builder.append("Air-conditoning adjusted.");
        builder.append(EOL);
        builder.append("Temperature: at ");
        builder.append(this.getTemperature());
        builder.append(" degrees");
        builder.append(EOL);
        builder.append(this.usersAtHomeSring());
        return builder.toString();
    }
}
