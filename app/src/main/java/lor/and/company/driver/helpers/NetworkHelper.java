package lor.and.company.driver.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;

public class NetworkHelper {
    Context context;
    NetworkListener listener;

    public NetworkHelper(Context context) {
        this.context = context;
    }

    public void registerNetworkCallback(NetworkListener networkListener)
    {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();

            connectivityManager.registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){
                                                                   @Override
                                                                   public void onAvailable(Network network) {
                                                                       networkListener.isNetworkConnected(true); // Global Static Variable
                                                                   }

                                                                   @Override
                                                                   public void onLost(Network network) {
                                                                       networkListener.isNetworkConnected(false); // Global Static Variable
                                                                   }
                                                               }

            );
            networkListener.isNetworkConnected(false);
        }catch (Exception e){
            networkListener.isNetworkConnected(false);
        }
    }

    public interface NetworkListener {
        void isNetworkConnected(Boolean connected);
    }
}
