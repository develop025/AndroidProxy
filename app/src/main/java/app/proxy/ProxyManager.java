package app.proxy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Proxy;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class ProxyManager {
    private static final String TAG = "Proxy";

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static void setProxy(Context context, String proxyHost, String proxyPort) {
        // Set the proxy values
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", proxyPort);
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", proxyPort);

        // Use reflection to apply the new proxy values.
        try {
            // Get the application and APK classes.  Suppress the lint warning that reflection may not always work in the future and on all devices.
            Class applicationClass = Class.forName("android.app.Application");
            @SuppressLint("PrivateApi") Class loadedApkClass = Class.forName("android.app.LoadedApk");

            // Get the declared fields.  Suppress the lint warning that `mLoadedApk` cannot be resolved.
            @SuppressWarnings("JavaReflectionMemberAccess") Field mLoadedApkField = applicationClass.getDeclaredField("mLoadedApk");
            Field mReceiversField = loadedApkClass.getDeclaredField("mReceivers");

            // Allow the values to be changed.
            mLoadedApkField.setAccessible(true);
            mReceiversField.setAccessible(true);

            // Get the APK object.
            Object mLoadedApkObject = mLoadedApkField.get(context);

            // Get an array map of the receivers.
            ArrayMap receivers = (ArrayMap) mReceiversField.get(mLoadedApkObject);
            List<Object> values = Arrays.asList(receivers.values().toArray());
            // Set the proxy.
            for (int n = 0; n < values.size(); n++) {
                Object receiverMap = values.get(n);
                Object[] set = ((ArrayMap) receiverMap).keySet().toArray();
                for (int m = 0; m < set.length; m++) {
                    Object receiver = set[m];
//                }
//                for (Object receiver : ((ArrayMap) receiverMap).keySet().toArray()) {
                    // `Class<?>`, which is an `unbounded wildcard parameterized type`, must be used instead of `Class`, which is a `raw type`.  Otherwise, `receiveClass.getDeclaredMethod` is unhappy.
                    Class<?> receiverClass = receiver.getClass();

                    // Get the declared fields.
                    final Field[] declaredFieldArray = receiverClass.getDeclaredFields();

                    // Set the proxy for each field that is a `ProxyChangeListener`.
                    for (Field field : declaredFieldArray) {
                        if (field.getType().getName().contains("ProxyChangeListener")) {
                            Method onReceiveMethod = receiverClass.getDeclaredMethod("onReceive", Context.class, Intent.class);
                            Intent intent = new Intent(Proxy.PROXY_CHANGE_ACTION);
                            onReceiveMethod.invoke(receiver, context, intent);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
            Log.d(TAG, "Exception: " + exception);
        }
    }
}
