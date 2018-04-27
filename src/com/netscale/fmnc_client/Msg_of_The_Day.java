package com.netscale.fmnc_client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

public class Msg_of_The_Day extends Service {
	private static final String HUB_URL = "http://www3.nd.edu/~lsong2/fmnc/config.xml";
	private static final String HUB_CACHE_FILENAME = "cachehub.xml";
	private Context context;

	private void downloadConfigFiles() {
		// Pull the files from the server down to the local filesystem
		try {

			// Get Hub.xml file
			URL url = new URL(HUB_URL);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(25000);

			byte[] cacheBuffer = new byte[8192];
			int bytesRead;
			FileOutputStream outCache = context.openFileOutput(
					HUB_CACHE_FILENAME, Context.MODE_PRIVATE);
			InputStream inputStream = connection.getInputStream();
			while ((bytesRead = inputStream.read(cacheBuffer)) > 0) {
				outCache.write(cacheBuffer, 0, bytesRead);
			}
			outCache.close();
			inputStream.close();
			connection.disconnect();

		} catch (Exception e) {
			// If we hit this catch, there was an error downloading the files
			Log.d("DEBUG_CONFIG",
					"Exception in downloading config file or signature from XMLHubConfigProvider");
			e.printStackTrace();
		}
	}
	private boolean parseXMLSettings(InputStream fileStream) {
		XPath xpath = XPathFactory.newInstance().newXPath();
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory
				.newInstance();

		try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document xmlDocument = builder.parse(fileStream);
			NodeList serverList = (NodeList) xpath.evaluate(
					"/fmncconfig/serverlist/server", xmlDocument,
					XPathConstants.NODESET);
			Log.d("FMNC","Start to parse the file");
			parseServers(serverList);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	private void parseServers(NodeList serverlist) throws Exception {
		Log.d("FMNC check list","List length--->"+serverlist.getLength());
		for (int i = 0; i < serverlist.getLength(); ++i) {
			String type = null;
			String host = null;
			String url = null;
			boolean isListed = true;

			if (serverlist.item(i).getAttributes().getNamedItem("listed") != null) {
				isListed = Boolean.parseBoolean(serverlist.item(i)
						.getAttributes().getNamedItem("listed").getNodeValue());
			}

			for (int j = 0; j < serverlist.item(i).getChildNodes().getLength(); ++j) {
				String propertyName = serverlist.item(i).getChildNodes()
						.item(j).getNodeName();
				String propertyText = serverlist.item(i).getChildNodes()
						.item(j).getTextContent();
				Log.d("FMNC check list",propertyText+"---"+propertyName);
				if (propertyName.equals("type")) {
					type = propertyText;
				} else if (propertyName.equals("host")) {
					host = propertyText;
				} else if (propertyName.equals("url")) {
					url = propertyText;
				}
			}
			Log.d("FMNC check file",type+" "+host+" "+url);
			if (type != null && host != null && url != null ) {
				ConfigServer(type, host, url);
			}
		}
	}
	public void ConfigServer(String type, String host,String url){
		saveUserPreferrence(type,"http://"+host+url);

	}
	private void saveUserPreferrence(String key, String value) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		Log.d("FMNC save file",key+" "+value);
		Editor editor = sharedPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

public void start_fetch(){
	Thread t = new Thread(new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			downloadConfigFiles();
			// Read files in from local filesystem and verify the signatures
			
			File hubCache = new File(context.getFilesDir(), HUB_CACHE_FILENAME);

			InputStream fileStream = null;

			// Ensure the files exist
			if (hubCache.exists()) {
				if ((Calendar.getInstance().get(Calendar.MILLISECOND) - hubCache
						.lastModified()) < 1000 * 60 * 60 * 24) {
					try {
						Log.w("XMLNode", "Hub file length" + hubCache.length());

						fileStream = context.openFileInput(HUB_CACHE_FILENAME);
						Log.d("FMNC","The filename is "+hubCache.getPath());
						parseXMLSettings(fileStream);


						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	});
	t.start();

	Handler h = new Handler(Looper.getMainLooper());
	h.post(new Runnable(){
		@Override
		public void run() {
			Toast.makeText(getApplicationContext(), "Fetch.",
                      Toast.LENGTH_SHORT).show();
		}
	});
	AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
	Intent i1 = new Intent(context,Msg_of_The_Day.class);
	PendingIntent pi1 = PendingIntent.getService(context, 1000, i1, 0);
	Log.d("FMNC","Alarm lanched MSG"+pi1.toString());
	am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 1000*60*60*24, pi1); // Millisec * Second * Minute
	
}
	

	@Override
	public void onCreate() {
		// The service is being created
		context = getApplicationContext();
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// The service is starting, due to a call to startService()
		Log.d("MSG_DAY","Start service");
		start_fetch();
		return 1;
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
