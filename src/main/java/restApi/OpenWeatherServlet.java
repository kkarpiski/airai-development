package restApi;

import com.rabbitmq.client.Connection;
import org.json.JSONObject;
import rabbit.MQAgent;
import utility.FileReader;
import utility.UnixTimer;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class OpenWeatherServlet {
    private final static String apikey = FileReader.getOpenWeatherApikey();
    private final static String q1 = "https://api.openweathermap.org/data/2.5/onecall/timemachine?lat=";
    private final static String q2 = "&lon=";
    private final static String q3 = "&dt=";
    private final static String q4 = "&appid=";

    public OpenWeatherServlet() {
        System.out.println("OpenWeather Data Download");
        System.out.println("[ow] start");
        try {
            ArrayList<String> arr = utility.FileReader.readOpenWeather();
            Iterator<String> iter = arr.iterator();
            do {
                ArrayList<String> arr2 = new ArrayList<>();
                arr2.add(iter.next());
                arr2.add(iter.next());
                saveData(Objects.requireNonNull(openWeatherGet(arr2)), iter.next());
            } while (iter.hasNext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[ow] end");
    }

    private static JSONObject openWeatherGet(ArrayList<String> arr) {
        try {
            URL url = new URL(q1 + arr.get(0) + q2 + arr.get(1) + q3 + UnixTimer.getUnixTime() + q4 + apikey);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            return new JSONObject(String.valueOf(content));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, String> getData(JSONObject hourData, String id) {
        Map<String, String> map = new HashMap<>();
        map.put("wind_direction", String.valueOf(hourData.get("wind_deg")));
        map.put("wind_speed", String.valueOf(hourData.get("wind_speed")));
        map.put("weather", String.valueOf(hourData.getJSONArray("weather").getJSONObject(0).get("main")));
        map.put("time", String.valueOf(hourData.get("dt")));
        map.put("station", id);
        return map;
    }


    private static void saveData(JSONObject openWeatherResponse, String id) {
        Connection c = MQAgent.connectRabbitMQ();
        assert c != null;
        MQAgent.sendData(c, "ow", getData(openWeatherResponse.getJSONObject("current"), id));
        MQAgent.receiveData(c, "ow");
        Iterator<Object> iter = openWeatherResponse.getJSONArray("hourly").iterator();
        do {
            MQAgent.sendData(c, "ow", getData((JSONObject) iter.next(), id));
            MQAgent.receiveData(c, "ow");
        } while (iter.hasNext());
    }
}
