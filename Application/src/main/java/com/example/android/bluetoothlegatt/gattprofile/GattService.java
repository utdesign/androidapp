import android.content.res.Resources;

import com.example.android.bluetoothlegatt.R;

import java.util.HashMap;

/**
 * Created by quangta93 on 3/1/15.
 */
public class GattService {

    private static HashMap<String, String> services = new HashMap();

    public static final String GENERIC_ACCESS_PROFILE = "1800";
    public static final String GENERIC_ATTRIBUTE_PROFILE = "1801";

    static {
        services.put(GENERIC_ACCESS_PROFILE, Resources.getSystem().getString(R.string.generic_access_profile));
        services.put(GENERIC_ATTRIBUTE_PROFILE, Resources.getSystem().getString(R.string.generic_attribute_profile));
    }

    public static String lookup(String uuid, String defaultName) {
        String name = services.get(uuid);
        return name == null ? defaultName : name;
    }
}
