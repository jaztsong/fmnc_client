package com.netscale.fmnc_client;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Timer;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class Curl_Thread extends Service{
	private Context context;
	private String URL = null;
	private Timer curl_timer;
	final private String Server_URL = "http://netscale03.crc.nd.edu/";
	private int successCount;
	private int totalCount;

	private String getUserPreference(String key) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		return sharedPreferences.getString(key, null);
	}
	
	private void saveUserPreferrence(String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Log.d("FMNC save file",key+" "+value);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}
	
	public Bitmap getBitmapFromURL(String src) {
	    try {
	        java.net.URL url = new java.net.URL(Server_URL + src);
	        HttpURLConnection connection = (HttpURLConnection) url
	                .openConnection();
	        connection.setDoInput(true);
	        connection.connect();
	        InputStream input = connection.getInputStream();
	        Bitmap myBitmap = BitmapFactory.decodeStream(input);
	        Log.d("Download Img",src + " get "+myBitmap.getByteCount()+" Bytes.");
	        return myBitmap;
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}
	public String getConnectivityInfo(){
		String results="?";
		ConnectivityManager conMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(conMgr == null)
		{
			Log.d("FMNC", "conMgr is null");
		}else{
			Log.d("FMNC", "conMgr is not null");
			NetworkInfo networkinfo=null;
			//Log.d("FMNC", "ConnectionType"+conMgr.getActiveNetworkInfo().getTypeName());
			networkinfo = conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			Log.d("FMNC", "ConnectionType"+networkinfo);
			if (networkinfo == null ||  !networkinfo.isConnected() ) {
				networkinfo = conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (networkinfo == null || !networkinfo.isConnected()  ){
					// notify user you are not online
					return "Offline";
				}else{
					// notify user you are online wifi
					Log.d("FMNC", "Before get SSID");
					return "wifi";
				}

			}
			else{
				// notify user you are online cellular
				Log.d("FMNC", "Before get cellular power");
				return "cellular";
			}
		}
		return results;
	}
	private String getAppendix(String type){
		TelephonyManager mngr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
		String uniq_id = null;    
		if(mngr.getDeviceId() == null){
			try {                                                                               
				Class<?> c = Class.forName("android.os.SystemProperties");        	 
				Method get = c.getMethod("get", String.class, String.class );                     
				uniq_id = (String)(   get.invoke(c, "ro.serialno", "unknown" )  );              
			}                                                                                 
			catch (Exception ignored)                                                         
			{                               
			}
		}else{
			uniq_id = mngr.getDeviceId().toString();    
		}

//		try {
//		    Thread.sleep(1000);                 //1000 milliseconds is one second.
//		} catch(InterruptedException ex) {
//		    Thread.currentThread().interrupt();
//		}
		saveUserPreferrence("Last_fetch", (new Date((new Timestamp(System.currentTimeMillis())).getTime())).toString());
		String results = "?app=Android?version="+MainActivity.AP_VERSION+"?reqID="+getUserPreference("Total")
				+"?imei="+uniq_id;
//		if (type == "cellular"){
//			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//			CellInfoGsm cellinfogsm = (CellInfoGsm)telephonyManager.getAllCellInfo().get(0);
//			CellSignalStrengthGsm cellSignalStrengthGsm = cellinfogsm.getCellSignalStrength();
//
//			results  += cellSignalStrengthGsm.getDbm();
//
//		}
		 if (type == "wifi"){
			results += "?type=WiFi";

			WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo connectionInfo = wifiManager.getConnectionInfo();

			if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
				results  += "?SSID="+connectionInfo.getSSID();
				results = results.replace("\"", "");
				results += "?BSSID="+connectionInfo.getMacAddress();
				if (android.os.Build.VERSION.SDK_INT>=20){
					results += "?Band="+connectionInfo.getFrequency();
				}
				
				results += "?RSSI="+connectionInfo.getRssi();

			}
			long s_t = System.currentTimeMillis();
			getBitmapFromURL("pic1.jpg");
			long e_t = System.currentTimeMillis();
			results += "?Throughput="+(e_t - s_t);
		}
		
				
		return results;

	}
	public float getBatteryLevel() {
	    Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	    int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	    int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

	    // Error checking that probably isn't needed but I added just in case.
	    if(level == -1 || scale == -1) {
	        return 50.0f;
	    }
	    Log.d("FMNC Battery",Float.toString(((float)level / (float)scale) * 100.0f));
	    return ((float)level / (float)scale) * 100.0f; 
	}

	public void start_curl() {
		//reset up Alarm 
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context,Curl_Thread.class);
		PendingIntent pi = PendingIntent.getService(context, 1001, i, 0);
		Log.d("FMNC","Alarm lanched Curl"+pi.toString());
		am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 1000*60*7, pi); // Millisec * Second * Minute
		
		
		if(getBatteryLevel() < 30.0){
			return;
		}
		
		final String type = getConnectivityInfo();
		if(type == "wifi"){
			URL = getUserPreference(type);
			if (URL == null){
				Log.d("FMNC","Not predefined url found");
				Intent intent = new Intent(this, Msg_of_The_Day.class);
				startService(intent);
			}else{
				Log.d("FMNC Curl","Start to curl.");
				Thread t = new Thread(new Runnable(){
					@Override
					public void run() {
						//URL = "http://netscale03.crc.nd.edu:443/tests/train?0?SliceSize=1200?PacketGap=1200?Length=100?Rmin=01?Rmax=15?Capacity=NA?Util=NA";
						final String url= URL + getAppendix(type);
						/// Method by using Volley
						RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

						// Request a string response from the provided URL.
						StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
						            new Response.Listener<String>() {
						    @Override
						    public void onResponse(String response) {
						    	successCount++;
						        totalCount++;
						        saveUserPreferrence("Total",Integer.toString(totalCount));
						        saveUserPreferrence("Success",Integer.toString(successCount));
						        Intent b = new Intent("FMNC_COUNT_UPDATE");
						        LocalBroadcastManager.getInstance(context).sendBroadcast(b);
						        //MainActivity.test.setText(successCount+"/"+totalCount);
						        
						    }
						}, new Response.ErrorListener() {

							@Override
							public void onErrorResponse(VolleyError arg0) {
								// TODO Auto-generated method stub
								totalCount++;
								saveUserPreferrence("Total",Integer.toString(totalCount));
								Intent b = new Intent("FMNC_COUNT_UPDATE");
								LocalBroadcastManager.getInstance(context).sendBroadcast(b);
								//MainActivity.test.setText(successCount+"/"+totalCount);
								
							}
						});
						// Add the request to the RequestQueue.
						queue.add(stringRequest);
						////Method by using HttpClient
//						// TODO Auto-generated method stub
//						// Creating HTTP client
//						HttpParams httpParameters = new BasicHttpParams();
//						// Set the timeout in milliseconds until a connection is established.
//						// The default value is zero, that means the timeout is not used. 
//						int timeoutConnection = 9000;
//						HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
//						// Set the default socket timeout (SO_TIMEOUT) 
//						// in milliseconds which is the timeout for waiting for data.
//						int timeoutSocket = 10000;
//						HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
//
//						DefaultHttpClient httpClient = new DefaultHttpClient();
//
//						HttpResponse response = null;
//						// Making HTTP Request
//						try {
//							//String t_url="http://netscale03.crc.nd.edu/tests/train?0?SliceSize=1200?PacketGap=1200?Length=100?Rmin=01?Rmax=15?Capacity=NA?Util=NA";
//							//t_url = "http://netscale03.crc.nd.edu/tests/base";
//							response = httpClient.execute(new HttpGet(url));
//							Log.d("FMNC link", url);
//							//httpClient.setParams(httpParameters);
//							//Intent b = new Intent("Render");
//							// writing response to log
//							//b.putExtra("Response",EntityUtils.toString(response.getEntity()));
//							//LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(b);
//							Log.d("Http Response:TRY", response.toString());
//						} catch (ClientProtocolException e) {
//							// writing exception to log
//							e.printStackTrace();
//							Log.e("Http Response:TRY", e.toString());
//							
//						} catch (IOException e) {
//							// writing exception to log
//							e.printStackTrace();
//
//						}finally {
//							httpClient.getConnectionManager().shutdown();
//						}
					}
				});
				t.start();
//				try {
//			        t.join();
//			    } catch (InterruptedException e) {
//			    	e.printStackTrace();
//			    }
				Handler h = new Handler(Looper.getMainLooper());
				h.post(new Runnable(){
					@Override
					public void run() {
						Toast.makeText(getApplicationContext(), "WGET.",
			                      Toast.LENGTH_SHORT).show();
					}
				});
				
			}
		}

	}
	@Override
	public void onCreate() {
		// The service is being created
		context = getApplicationContext();
		String tmp_string = getUserPreference("Total");
		if (tmp_string == null){
			totalCount = 0;
		}
		else{
			totalCount = Integer.parseInt(tmp_string);
		}
		tmp_string = getUserPreference("Success");
		if (tmp_string == null){
			successCount = 0;
		}
		else{
			successCount = Integer.parseInt(tmp_string);
		}
		
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()
        
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
		.setSmallIcon(R.drawable.ic_launcher)
		.setContentTitle("FMNC")
		.setContentText("Last time fetch:"+getLastTimeFetch());
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MainActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
						);
		mBuilder.setContentIntent(resultPendingIntent);


		// mId allows you to update the notification later on.
		mBuilder.setWhen(System.currentTimeMillis());
		final int ONGOING_NOTIFICATION_ID = 7;
		startForeground(ONGOING_NOTIFICATION_ID, mBuilder.build());
		start_curl();
		return START_STICKY;
	}
	private String getLastTimeFetch() {
		// TODO Auto-generated method stub
		String tmp_string = getUserPreference("Last_fetch");
		if (tmp_string == null){
			tmp_string = "N/A";
		}
		return tmp_string;
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void onDestroy() {
		// The service is no longer used and is being destroyed
	}
}
