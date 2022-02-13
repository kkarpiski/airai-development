package restApi;

import com.rabbitmq.client.Connection;
import org.json.JSONArray;
import org.json.JSONObject;
import rabbit.MQAgent;
import utility.FileReader;
import utility.UnixTimer;

import javax.json.Json;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

/**
 * Created by kkarp.
 */
public class OpenWeatherPollutionServlet {

    private final static String apikey = FileReader.getOpenWeatherApikey();
    private final static String q1 = "https://api.openweathermap.org/data/2.5/air_pollution?lat=";
    private final static String q2 = "&lon=";
    private final static String q3 = "&dt=";
    private final static String q4 = "&appid=";

    public OpenWeatherPollutionServlet() {

        System.out.println("OpenWeather Data Download");
        System.out.println("[owpoll] start");
        try {
            ArrayList<String> arrpoll = utility.FileReader.readOpenWeatherPollution();
            Iterator<String> iter = arrpoll.iterator();
            do {
                ArrayList<String> arrPoll2 = new ArrayList<>();
                arrPoll2.add(iter.next());
                arrPoll2.add(iter.next());
                saveData(Objects.requireNonNull(openWeatherPollutionGet(arrPoll2)), iter.next());
            } while (iter.hasNext());
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("[owpoll] end");
    }

    private static JSONObject openWeatherPollutionGet(ArrayList<String> arrPollution) {
        try {
            URL url = new URL(q1 + arrPollution.get(0) + q2 + arrPollution.get(1) + q3 + UnixTimer.getUnixTime() + q4 + apikey);
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

    private static Map<String, String> getPollutionData(JSONObject openWeatherPollutionResponse, String id) {

        Map<String, String> map = new HashMap<>();

            /*map.put("no", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(1).get("no")));
            map.put("no2", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(2).get("no2")));
            map.put("o3", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(3).get("o3")));
            map.put("so2", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(4).get("so2")));
            map.put("pm2_5", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(5).get("pm2_5")));
            map.put("pm10", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(6).get("pm10")));
            map.put("nh3", String.valueOf(hourDataPollution.getJSONArray("components").getJSONObject(7).get("nh3")));
            map.put("time", String.valueOf(hourDataPollution.getJSONArray("list").getJSONObject(0).get("dt")));
            map.put("station", id);*/


        JSONArray array = openWeatherPollutionResponse.getJSONArray("list");
        for (int i = 0; i < array.length(); i++) {
            JSONObject row = array.getJSONObject(i);

            for (int o = 0; o < row.length(); o++) {
                JSONObject obj = row.getJSONObject("components");

                Iterator stationKeys = obj.keys();
                while (stationKeys.hasNext()) {
                    String stationkey = (String) stationKeys.next();
                    Object stationValue = obj.get(stationkey);
                    System.out.println(stationkey + " : " + stationValue);

                    map.put(stationkey, String.valueOf(stationKeys));
                }
            }
        }
        return map;
    }

        private static void saveData (JSONObject openWeatherPollutionResponse, String id){
            Connection c = MQAgent.connectRabbitMQ();
            assert c != null;

            MQAgent.sendData(c, "ow", getPollutionData(openWeatherPollutionResponse, id)); //poll or not?
            MQAgent.receiveData(c, "ow");
 /*           Iterator<Object> iter = openWeatherPollutionResponse.getJSONArray("hourly").iterator();
            do {
                MQAgent.sendData(c, "ow", getPollutionData((JSONObject) iter.next(), id));
                MQAgent.receiveData(c, "ow");
            } while (iter.hasNext());*/  //do odkomentowania w momencie pobierania danych historycznych
        }

    }


