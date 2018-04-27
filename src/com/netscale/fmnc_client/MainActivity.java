package com.netscale.fmnc_client;


import java.util.Timer;
import java.util.TimerTask;



import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Msg_of_The_Day daily_msg;
	private Curl_Thread curl;
	public static TextView test;
	private Timer config_timer;
	private TimerTask config_task;
	private TimerTask curl_task;
	final Handler handler = new Handler();
	public static final String AP_VERSION = "3";
	
	
    private BroadcastReceiver mUpdateMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
        	test.setText(getTally());
        }
    };
	private String getTally() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		return sharedPreferences.getString("Success", null) + "/" + sharedPreferences.getString("Total", null);
	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Button bt= (Button) findViewById(R.id.button1);
		setContentView(R.layout.activity_main);
		test = (TextView) findViewById(R.id.textView3);
		//SetAlarms(getApplicationContext());
		SetAlarms(getApplicationContext());
		LocalBroadcastManager.getInstance(this).registerReceiver(
                mUpdateMessageReceiver, new IntentFilter("FMNC_COUNT_UPDATE"));
		
		test.setText(getTally());

	}
	public void SetAlarms(Context context)
	{
		
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context,Curl_Thread.class);
		PendingIntent pi = PendingIntent.getService(context, 1001, i, 0);
		Log.d("FMNC","Alarm lanched Curl"+pi.toString());
		am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 1000*10, pi); // Millisec * Second * Minute
		
		Intent i1 = new Intent(context,Msg_of_The_Day.class);
		PendingIntent pi1 = PendingIntent.getService(context, 1000, i1, 0);
		Log.d("FMNC","Alarm lanched MSG"+pi1.toString());
		am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+ 1000*1, pi1); // Millisec * Second * Minute
	}
	public void CancelAlarms(Context context)
	{
		Intent intent = new Intent(context,Curl_Thread.class);
		PendingIntent sender = PendingIntent.getService(context, 1001, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		try {
			alarmManager.cancel(sender);
			Log.d("FMNC","Alarm Canceled Curl"+sender.toString());
		} catch (Exception e) {
			Log.e("FMNC", "AlarmManager Curl was not canceled. " + e.toString());
		}

		Intent i1 = new Intent(context,Msg_of_The_Day.class);
		PendingIntent pi1 = PendingIntent.getService(context, 1000, i1, 0);

		try {
			alarmManager.cancel(pi1);
			Log.d("FMNC","Alarm Canceled MSG"+pi1.toString());
		} catch (Exception e) {
			Log.e("FMNC", "AlarmManager MSG Day was not canceled. " + e.toString());
		}
	}
	public void Start_Auto(View v){
		SetAlarms(getApplicationContext());
	}
	public void Cancel_Auto(View v){
		CancelAlarms(getApplicationContext());
	}
	public void Test(View v){
		Intent intent = new Intent(this, Curl_Thread.class);
		startService(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
