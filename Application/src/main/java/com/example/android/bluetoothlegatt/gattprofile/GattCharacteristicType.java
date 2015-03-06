import android.content.res.Resources;

import com.example.android.bluetoothlegatt.R;

import java.util.HashMap;

/**
 * Created by quangta93 on 3/1/15.
 */
public class GattCharacteristicType {

    private static HashMap<String, String> characteristicTypes = new HashMap();

    public static final String DEVICE_NAME = "2A00";
    public static final String APPEARANCE = "2A01";
    public static final String PERIPHERAL_PRIVACY_FLAG = "2A02";
    public static final String RECONNECTION_ADDRESS = "2A03";
    public static final String PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS = "2A04";
    public static final String SERVICE_CHANGED = "2A05";

    static {
        characteristicTypes.put(DEVICE_NAME, Resources.getSystem().getString(R.string.device_name));
        characteristicTypes.put(APPEARANCE, Resources.getSystem().getString(R.string.appearance));
        characteristicTypes.put(PERIPHERAL_PRIVACY_FLAG, Resources.getSystem().getString(R.string.peripheral_privacy_flag));
        characteristicTypes.put(RECONNECTION_ADDRESS, Resources.getSystem().getString(R.string.reconnection_address));
        characteristicTypes.put(PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, Resources.getSystem().getString(R.string.peripheral_preferred_connection_parameters));
        characteristicTypes.put(SERVICE_CHANGED, Resources.getSystem().getString(R.string.service_changed));
    }

    public static String lookup(String uuid, String defaultName) {
        String name = characteristicTypes.get(uuid);
        return name == null ? defaultName : name;
    }
}
