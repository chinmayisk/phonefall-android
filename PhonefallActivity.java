


import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;


import android.util.Log;
import android.widget.Toast;

public class PhonefallActivity extends Activity implements SensorEventListener {

	private float mLastX, mLastY, mLastZ;
	float deltaX, deltaY ,deltaZ;
	float finalX, finalY ,finalZ;
	float gyroX,gyroY,gyroZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private Sensor mGyroscope, gsensor, msensor;
	private boolean gyroexists;
	private final float NOISE = (float) 4.5;
	  float[] gData = new float[3];           // Gravity or accelerometer
	    float[] mData = new float[3];           // Magnetometer
	    float[] orientation = new float[3];
	    float[] Rmat = new float[9];
	    float[] R2 = new float[9];
	    float[] Imat = new float[9];
	    boolean haveGrav = false;
	    boolean haveAccel = false;
	    boolean haveMag = false;
	int rolllatest;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phonefall);
		
		PackageManager pmanager= this.getPackageManager();
		mInitialized = false;
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
		gsensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		mSensorManager.registerListener(this, gsensor , SensorManager.SENSOR_DELAY_GAME);
		 msensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(this, msensor , SensorManager.SENSOR_DELAY_GAME);
		gyroexists= pmanager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
		if(gyroexists)
		{
			mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			mSensorManager.registerListener(this, mGyroscope , SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// can be safely ignored for this demo
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		 float[] data;
		synchronized (this) {
	        switch (event.sensor.getType()){
	            case Sensor.TYPE_ACCELEROMETER:
	                 accelerometeraction(event);
	                 if (haveGrav) break;    
	 	            gData[0] = event.values[0];
	 	            gData[1] = event.values[1];
	 	            gData[2] = event.values[2];
	 	           haveAccel = true;
	            break;
	        case Sensor.TYPE_GYROSCOPE:
	               gyroscopeaction(event);
	        break;
	        
	   
	          case Sensor.TYPE_GRAVITY:
	            gData[0] = event.values[0];
	            gData[1] = event.values[1];
	            gData[2] = event.values[2];
	            haveGrav = true;
	            break;
	     
	          case Sensor.TYPE_MAGNETIC_FIELD:
	            mData[0] = event.values[0];
	            mData[1] = event.values[1];
	            mData[2] = event.values[2];
	            haveMag = true;
	            break;
	          default:
	            return;
	        }
		}
		//Kalman filter
	        if ((haveGrav || haveAccel) && haveMag) {
	            SensorManager.getRotationMatrix(Rmat, Imat, gData, mData);
	            SensorManager.remapCoordinateSystem(Rmat,
	                    SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, R2);
	           
	            SensorManager.getOrientation(R2, orientation);
	            float incl = SensorManager.getInclination(Imat);
	          
	         
	         	rolllatest = (int)(orientation[2]*57.2957795);
	           
	    }
		
		
	}

	private void gyroscopeaction(SensorEvent event) {
		
		gyroX =  event.values[0];
		gyroY = event.values[1];
		gyroZ = event.values[2];
		
	}

	private void accelerometeraction(SensorEvent event) {
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
			
			mInitialized = true;
		} else {
			deltaX = Math.abs(mLastX - x);
			deltaY = Math.abs(mLastY - y);
			deltaZ = Math.abs(mLastZ - z);

			//Calculate Lower Threshold
			final float lowerThreshold = Math.round(Math.sqrt(Math.pow(deltaX, 2)+Math.pow(deltaY, 2)+Math.pow(deltaZ, 2)));

			//Lower Threshold
			if(lowerThreshold <= 6){

				//Lower Timeout
				new Timer().schedule(new TimerTask() {

					@Override
					public void run() {

						runOnUiThread(new Runnable() {

							public void run() 
							{
								//Calculate Upper Threshold
								final float upperThreshold = Math.round(Math.sqrt(Math.pow(deltaX, 2)+Math.pow(deltaY, 2)+Math.pow(deltaZ, 2)));
								if(upperThreshold >= 9){

									//Upper Timeout
									new Timer().schedule(new TimerTask() {

										@Override
										public void run() {
											runOnUiThread(new Runnable() {

												public void run() 
												{
													//Check Phone Orientation
													checkOrientation();

												}
											});
										}
									},1000);
								}
							}
						});
					}
				},500);
			}

			mLastX = x;
			mLastY = y;
			mLastZ = z;
			
			
			
			
		}
		
	}

	//Check orientation of Phone after Fall is detected
	protected void checkOrientation() {
		boolean phoneStatus = false;
		
	        // check for accelerometer values to exceed NOISE it is an indication for Phone fall 	
		 { 
				Calendar cal1 = Calendar.getInstance();
				int startTimer = cal1.get(Calendar.SECOND);
				int waitTimer = startTimer + 5;

		
				boolean checkStatus = false;
				int runningTimer = 0;
				for(runningTimer =0; runningTimer <= waitTimer || checkStatus == true;){
			
				if (deltaX > NOISE || deltaY > NOISE || deltaZ > NOISE){
					phoneStatus = false;
					checkStatus = true;
					break;
				}else{

					// using the roll (gyrometer fundamentals ) and setting treshold of fall between 0-15 degrees and 165-180 degrees . 
					if(rolllatest <= 15 || rolllatest >=165)
					{
						phoneStatus = true ;
					}
					
				
				}
				Calendar cal2 = Calendar.getInstance();
				runningTimer = cal2.get(Calendar.SECOND);
				}
			}
		
		
		if(phoneStatus == true){
			doAlert();
		}

	}

	//Function to alert system
	private void doAlert() {
		
	 Toast.makeText(getApplicationContext(), "Phone has fallen down !!!", Toast.LENGTH_SHORT).show();
	}

	@Override
	 protected void onStop() {
	    super.onStop();
	    mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
	    if(gyroexists)
	    mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		mSensorManager.unregisterListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
		mSensorManager.unregisterListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
	 }
	
	

	 

	

}
