package pickme.smartmozoexample;

/**
 * Created by Hoa on 2/25/2016.
 */
public class ConfigSQL
{
    //Address of our scripts of the CRUD
    public static final String URL_ADD_BS="http://hoa.do/merlin/updateBluestone.php";
    public static final String URL_GET_ALL_BS = "http://hoa.do/merlin/getAllBluestones.php";
    public static final String URL_GET_BS = "http://hoa.do/merlin/getBluestone.php?mac=";

    public static final String URL_ADD_DEVICE="http://hoa.do/merlin/updateDevice.php";


    //Keys that will be used to send the request to php scripts
    public static final String KEY_BS_ID = "id";
    public static final String KEY_BS_MAC = "mac";
    public static final String KEY_BS_FIRMWARE = "firmware";
    public static final String KEY_BS_BATTERY = "battery";
    public static final String KEY_BS_TIME_ALIVE = "time_alive";
    public static final String KEY_BS_LAST_PICKUP = "last_pickup";
    public static final String KEY_BS_PICKUPS = "number_of_pickups";

    //Keys for devices
    public static final String KEY_DEVICE_UUID = "uuid";
    public static final String KEY_DEVICE_TYPE = "type";
    public static final String KEY_DEVICE_MODEL = "model";
    public static final String KEY_DEVICE_VERSION = "version";

    //JSON Tags
    public static final String TAG_JSON_ARRAY="result";
    public static final String TAG_ID = "id";
    public static final String TAG_MAC = "mac";
    public static final String TAG_FIRMWARE = "firmware";
    public static final String TAG_BATTERY = "battery";
    public static final String TAG_TIME_ALIVE = "time alive";
    public static final String TAG_LAST_PICKUP = "last pickup";
    public static final String TAG_PICKUPS = "number of pickups";

    //employee id to pass with intent
    public static final String BS_ID = "bs_id";
}
