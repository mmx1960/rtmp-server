package xyz.ycgame.rtmpserver.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * @author mmx1960
 */
public class JsonUtils {

    private static final ObjectMapper OM = new ObjectMapper();

    private JsonUtils() {
    }
    
    static {
        OM.configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true);
        OM.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);
        OM.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static <T> T parse(String json, Class<T> tClass) {
        try {
            return OM.readValue(json, tClass);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toJsonString(Object ob) {
        try {
            return OM.writeValueAsString(ob);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map objectToMap(Object object) {
        return OM.convertValue(object, Map.class);
    }

    public static <T> T mapToObject(Map map, Class<T> tClass) {
        return OM.convertValue(map, tClass);
    }
}
