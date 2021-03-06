package com.camera;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.net.ftp.FTPClient;

import com.camera.RGBPos.Node;

public class CameraControl extends Activity implements SurfaceHolder.Callback, LocationListener
{
	private static String TAG = "CameraControl";
	private SocketServer ss = null;

	private static RGBPos detector = null;

    private static float nowX = 0.0f;
    private static float nowY = 0.0f;
	
    private int mode = 0;
    private int msg_index = 0;
    private String sfilename = "";
	
	public boolean Ready = false;
	private Camera mCamera01;
	public String  Imei;

	//information
    double latitude;
    double longitude;
    double latDegree;
    double lonDegree;
    double latMinute;
    double lonMinute;
    double latSecond;
    double lonSecond;    
	
	public SensorManager mSensorManager = null; 
	private float mCameraOrientation;
	
	public String IPAdrress;
	public String ftp_account;
	public String ftp_password;

	private AlertDialog.Builder builder;
	private static final int MENU_TAKE_PICTURE = Menu.FIRST;
	private static final int MENU_FTP_SETUP = Menu.FIRST + 1;
	private static final int MENU_EXIT = Menu.FIRST + 2;

	//Take Picture of Number
	public int PictureCount;
	  
	private float fBearing; 
	private SurfaceView mSurfaceView01;
	private SurfaceHolder mSurfaceHolder01;

	private static volatile AtomicBoolean processing = new AtomicBoolean(false);
    private static final AtomicBoolean computing = new AtomicBoolean(false); 
	  
	private boolean bIfPreview = false;
	private String strCaptureFilePath = Environment.getExternalStorageDirectory() + "/camera/";
	
	private TextView tip;
	private EditText ip;
	private TextView tacc;
	private EditText acc;
	private TextView tpwd;
	private EditText pwd;
	
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.mcamera);
        
        Thread.setDefaultUncaughtExceptionHandler(handler);
        
        
    	IPAdrress = "192.168.173.1";
    	ftp_account = "test";
    	ftp_password = "test";

        //Checking Status
        if (CheckInternet(3))
         {
            String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            
            if(provider != null)
            {
                Log.v(TAG, " Location providers: " + provider);

                //Start searching for location and update the location text when update available
                //startFetchingLocation();
                //Open Server Socket
                try {
                	ss = new SocketServer(12121, this);
            		Thread socket_thread = new Thread(ss);
            		socket_thread.start();

            		// Launch GPS scan
             	    LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
             	    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 100.0f, this);
            		
        		 } catch (IOException e) {
        			e.printStackTrace();
        	     }
		  		 catch (Exception e) {
		  			e.printStackTrace();
		  	     }
                
                
                builder = new AlertDialog.Builder(this);
                builder.setMessage("shooting in handheld mode?");
                builder.setCancelable(false);
	               
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id)
	                  {
                    	 mode = 1;
                         showNotification();

                 		 // create a File object for the parent directory
                 		 File wallpaperDirectory = new File(strCaptureFilePath);
                 		 if (!wallpaperDirectory.exists()){
                     		 wallpaperDirectory.mkdirs();
                 		 }
                 		    
                 		 //get phone imei
                 		Imei = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId();
               		 	PictureCount = 0;
               		 	detector = new RGBPos();
          	            
                   	    ss.IsSync = true;
	                  }
	            });
	               
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id)
	                    {
                    	 mode = 2;
                         showNotification();

                 		 // create a File object for the parent directory
                 		 File wallpaperDirectory = new File(strCaptureFilePath);
                 		 if (!wallpaperDirectory.exists()){
                     		 wallpaperDirectory.mkdirs();
                 		 }
                 		    
                 		 //get phone imei
                 		Imei = ((TelephonyManager) getSystemService(TELEPHONY_SERVICE)).getDeviceId();
               		 	PictureCount = 0;
               		 	detector = new RGBPos();
          	            
                        String ipaddr = getLocalIpAddress();
                        openMessageDialog(ipaddr);
                    	 
	                    }
	                });
	                
                AlertDialog alert = builder.create();
	            alert.show();
	            SwitchCamera();
            }
            else
              {
            	openOptionsDialog("GPS not ready");
            }        	
        }
        else
        {
        	openOptionsDialog("must check Internet");
        }    
    }

    
   //creating a Handler
    private Thread.UncaughtExceptionHandler handler=
            new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable ex) 
            {
    	        PendingIntent intent = PendingIntent.getActivity(CameraControl.this, 0,
    	                new Intent(getIntent()), getIntent().getFlags());
    	
    	        AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
    	        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);

    	        android.os.Process.killProcess(android.os.Process.myPid());           
    	        
            }
        };

    
    
    public void onDestroy()
    {
        super.onDestroy();
        Log.i(TAG,"onDestroy");
    }
    
	public boolean onCreateOptionsMenu(Menu menu)
	{
	    super.onCreateOptionsMenu(menu);

	    if (mode == 1)
	    {
		    menu.add(0 , MENU_TAKE_PICTURE, 1 , "Take").setAlphabeticShortcut('E');
		    menu.add(0 , MENU_FTP_SETUP, 1 , "FTP Setup").setAlphabeticShortcut('E');
	    }
	    menu.add(0 , MENU_EXIT, 1 , "Exit").setIcon(R.drawable.exit)
	    .setAlphabeticShortcut('E');
	    
	    return true;  
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		    switch (item.getItemId())
		    { 		      
	          case MENU_TAKE_PICTURE:
	        	  takePicture();
	        	  break ;
	          case MENU_FTP_SETUP:
	        	  AlertDialog.Builder alert = new AlertDialog.Builder(this);

	              alert.setTitle("FTP SETUP");
	              alert.setMessage("Please input IPAddress, Account, Password...");
	              
	              ScrollView sv = new ScrollView(this);
	              LinearLayout ll = new LinearLayout(this);
	              ll.setOrientation(LinearLayout.VERTICAL);
	              sv.addView(ll);

	              tip = new TextView(this);
	              tip.setText("IP: ");
	              ip = new EditText(this);
	              ip.setText(IPAdrress);
	              ll.addView(tip);
	              ll.addView(ip);

	              tacc = new TextView(this);
	              tacc.setText("account: ");
	              acc = new EditText(this);
	              acc.setText(ftp_account);
	              ll.addView(tacc);
	              ll.addView(acc);
	              
	              tpwd = new TextView(this);
	              tpwd.setText("password: ");
	              pwd = new EditText(this);
	              pwd.setText(ftp_password);
	              
	              ll.addView(tpwd);
	              ll.addView(pwd);
	              
	              // Set an EditText view to get user input
	              alert.setView(sv);
	              
	              alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	              public void onClick(DialogInterface dialog, int whichButton)
	              {
		                if (IPAdrress.equals("") || ftp_account.equals("") || ftp_password.equals(""))
		                {
		                    openOptionsDialog("null");
		                    return;
		                }

		                IPAdrress = ip.getText().toString();
		                ftp_account = acc.getText().toString();
		                ftp_password = pwd.getText().toString();
	              }});
	              	              
	              alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	                  public void onClick(DialogInterface dialog, int whichButton)
	                  {
	                  }
	                });
	            
	                alert.show(); 	              
	        	  
	        	  break ;
	          case MENU_EXIT:
	        	  delenot();
	              android.os.Process.killProcess(android.os.Process.myPid());           
	        	  finish();
	        	  break ;
		    }
		      return true ;
	}

    
    public void SwitchCamera()
    {
		 if(!checkSDCard())
		 {
			 openOptionsDialog("no SDCard");
		 }
		 else
		 {
			 mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

			 if (mSensorManager != null) {
		        if(mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI)){
		        }else{
		         }
		        mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
			 } else Log.d(TAG, "No sensors!");

			 DisplayMetrics dm = new DisplayMetrics();
			 getWindowManager().getDefaultDisplay().getMetrics(dm);

			  Log.i(TAG,"SwtichCamera");
			    
			  mSurfaceView01 = (SurfaceView) findViewById(R.id.mSurfaceView1);
			  mSurfaceHolder01 = mSurfaceView01.getHolder();
			  mSurfaceHolder01.addCallback(this);
			    
			  //mSurfaceHolder01.setFixedSize(320, 240);
			    
			  mSurfaceHolder01.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
			    
			    
			  bIfPreview = false;
		 }
    	
    }
    
    public String getLocalIpAddress() {
    	  try {
    	    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); )
    	    {
    	        NetworkInterface intf = en.nextElement();
    	          for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) 
    	          {
    	              InetAddress inetAddress = enumIpAddr.nextElement();
    	              if (!inetAddress.isLoopbackAddress()) {
    	                  return inetAddress.getHostAddress().toString();
    	              }
    	          }
    	    }
    	  }
    	  catch (SocketException ex) {
    	      Log.e("", ex.toString());
    	  }

    	  return null;
    }
    
    public long SyncTimeStamp()
    {
    	long timestamp_before, timestamp_after;
        try {
        		timestamp_before = System.currentTimeMillis()/1000;
        		android.provider.Settings.System.putInt(getContentResolver(), android.provider.Settings.System.AUTO_TIME, 1);
        		timestamp_after = System.currentTimeMillis()/1000;
                return timestamp_after - timestamp_before;
        } catch (Exception e) {
                Log.i("Err. get my IP failed.", e.toString());
        }
        return -1;
    }
    
    private boolean CheckInternet(int retry)
    {
    	boolean has = false;
    	for (int i=0; i<=retry; i++)
    	{
    		has = HaveInternet();
    		if (has == true) break;    		
    	}
    	
		return has;
    }
    
    private boolean HaveInternet()
    {
	     boolean result = false;
	     
	     ConnectivityManager connManager = (ConnectivityManager) 
	                                getSystemService(Context.CONNECTIVITY_SERVICE); 
	      
	     NetworkInfo info = connManager.getActiveNetworkInfo();
	     
	     if (info == null || !info.isConnected())
	     {
	    	 result = false;
	     }
	     else 
	     {
		     if (!info.isAvailable())
		     {
		    	 result =false;
		     }
		     else
		     {
		    	 result = true;
		     }
     }
    
     return result;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
	    String url = "";
	
	    if (keyCode == KeyEvent.KEYCODE_BACK)
	    {
                builder.setMessage("Are you exit?");
                builder.setCancelable(false);
	               
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id)
	                    {
	                     delenot();
	                     finish();
	                    }
	                });
	               
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                     public void onClick(DialogInterface dialog, int id)
	                    {
	                    }
	                });
	                
	              AlertDialog alert = builder.create();
	              alert.show();
	              
	              return false;
	    }
	    else if (keyCode == KeyEvent.KEYCODE_MENU)
	    {
	    	return super.onKeyDown(keyCode, event);
	    }
	
	    return super.onKeyDown(keyCode, event);
     
    }
    
	protected void showNotification() 
	{
        CharSequence from ="CameraControl";
        CharSequence message ="running";

		//Intent intent = new Intent(this, rWebView.class);
        Intent intent = this.getIntent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		Notification notif = new Notification(R.drawable.icon , "CameraControl",  System.currentTimeMillis());
		
		notif.setLatestEventInfo(this, from, message, contentIntent);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(R.string.app_name, notif);

    }

	void delenot() 
	{
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(R.string.app_name);
    }    
	
	
	private SensorEventListener mSensorListener = new SensorEventListener() 
	{
		               private static final int matrix_size = 16;
		               float[] R = new float[matrix_size];
		               float[] outR = new float[matrix_size];
		               float[] I = new float[matrix_size];
		               float[] values = new float[3];
		               float[] mags = null;
		               float[] accels = null;
		               
		               public void onAccuracyChanged(Sensor sensor, int accuracy) {
		                       //Log.d (TAG, Thread.currentThread().getId()+"  onAccuracyChanged......");
		                 }
		               
		               public void onSensorChanged(SensorEvent event) 
		               {
		            	   nowX = event.values[0];
		            	   nowY = event.values[1];
		            	   
		                   if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return;
		                       switch (event.sensor.getType()) {
		                           case Sensor.TYPE_MAGNETIC_FIELD:
		                               mags = event.values;
		                               break;
		                       case Sensor.TYPE_ACCELEROMETER:
		                               accels = event.values;
		                               break;
		                   }
		                       
		                   if (mags != null && accels != null)
		                   {
		                           SensorManager.getRotationMatrix(R, I, accels, mags);
		                           // Correct if screen is in Landscape
		                           SensorManager.remapCoordinateSystem(R, SensorManager.AXIS_X, SensorManager.AXIS_Z, outR);
		                           SensorManager.getOrientation(outR, values);
		                           // Convert radian to degree
		                           fBearing =values[0];
		  
		                           // Convert radian to degree
		                           mCameraOrientation =values[2];

		                      		//mSensorManager.unregisterListener(mSensorListener);
		                           mags = null;
		                           accels = null;
		                   }
               }
      }; 
	    
	  private void initCamera()
	  {
	    if(!bIfPreview)
	    {
	      //mCamera01.release();
	 	  mCamera01 = Camera.open();
	    }    
	    
	    if (mCamera01 != null && !bIfPreview)
	    {
	      Log.i(TAG, "inside the camera");
	      
	      Camera.Parameters parameters = mCamera01.getParameters();
	      parameters.setPictureFormat(PixelFormat.JPEG);
	      
	      //parameters.setPreviewSize(360, 240);
	      //parameters.setPictureSize(360, 240);
	      
	      mCamera01.setParameters(parameters);

	      try
	      {
		         try{
		        	 
			 			fBearing =(float) (fBearing * 180 / 3.14);// 57.32=180 / 3.14
			 			if (fBearing < 0) fBearing +=360;
			 			int value =(int)( mCameraOrientation*180/3.14) + 90;

			 			//openMessageDialog(Integer.toString(value));
			 			int angle = roundOrientation(value);
			 			Log.d("angle", Integer.toString(angle));
			 			
			 			try{
			 				mCamera01.setDisplayOrientation(angle);
			 			}
			 			catch (Exception X)
			 			{
			 			} 		 
			 		}catch (Exception X)
			 		{
			 		}	 
			    	  mCamera01.setPreviewDisplay(mSurfaceHolder01);
			    	  mCamera01.startPreview();
	      }
	      catch (Exception X)
	      {  
	    	  mCamera01.release();
	    	  mCamera01 = null;            
	      }
	      
	      mCamera01.startPreview();
	      bIfPreview = true;
	    }
	  }

	  public void takePicture() 
	  {
	      if(checkSDCard())
	      {
	    	   mSensorManager.unregisterListener(mSensorListener);

	           try{
	    			fBearing =(float) (fBearing*180/3.14);
	    			if (fBearing < 0) fBearing +=360;
	    			int value =(int)( mCameraOrientation*180/3.14) + 90;
	    			int angle = roundOrientation(value);
	    			try{
	    				mCamera01.setDisplayOrientation(angle);
	    			}
	    			catch (Exception X)
	    			{
	    				
	    			} 		 
	    		}catch (Exception X)
	    		{
	    		}	     	   
	    	  
	    	    if (mCamera01 != null && bIfPreview) 
	    	    {
	    	      mCamera01.takePicture(shutterCallback, rawCallback, jpegCallback);
	    	      bIfPreview = false;
	    	    }
	      }
	  }
	  
	  private void resetCamera()
	  {
	    if (mCamera01 != null)
	    {
	        if (mSensorManager != null) {
	            if(mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI)){
	            	//openMessageDialog(" sensors enable!");
	            }else{
	            	//openMessageDialog(" sensors disable!");
	            }
	            mSensorManager.registerListener(mSensorListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
	         } else Log.d(TAG, "No sensors!");
	    	
	        mCamera01.stopPreview();
	        mCamera01.release();
	        mCamera01 = null;
	        bIfPreview = false;
	    }
	  }
	   
	  private ShutterCallback shutterCallback = new ShutterCallback() 
	  { 
	    public void onShutter() 
	    { 
	      // Shutter has closed 
	    } 
	  }; 
	   
	  private PictureCallback rawCallback = new PictureCallback() 
	  { 
	    public void onPictureTaken(byte[] data, Camera _camera) 
	    {
	    } 
	  }; 
	  
		private static final class DetectionThread extends Thread 
		{
			private byte[] data;
			private int width;
			private int height;
			private String path;
			private int[] pix;
			
			public DetectionThread(byte[] data,Bitmap bm, int width, int height, String path) 
			{
				this.data = data;
				this.width = width;
				this.height = height;
				this.path = path;
		        pix = new int[width * height];
		        bm.getPixels(pix, 0, width, 0, 0, width, height);
			}

			@Override
		    public void run() 
			{
				if (!processing.compareAndSet(false, true)) return;

				Log.d(TAG, "detect running...");
				try {
		        	//Previous frame
		        	int[] pre = null;
					
					//Current frame (with changes)
					long bConversion = System.currentTimeMillis();
					int[] img = null;
					img = ImageProcessing.decodeYUV420SPtoRGB(data, width, height);
					long aConversion = System.currentTimeMillis();

					Log.d(TAG, "Converstion="+(aConversion-bConversion));
					
					//Current frame (without changes)
					int[] org = null;
					if (img!=null) org = img.clone();

					ArrayList<Node> allgravity = detector.detect(img, pix, width, height);

					String info = "";
					
					for (int i=0; i<allgravity.size(); i++)
					{
						info = info + allgravity.get(i).getX() + "," + allgravity.get(i).getY() + "|";						
					}
										
		            ExifInterface exif = new ExifInterface(path);
		            exif.setAttribute(ExifInterface.TAG_MODEL , info);
		            exif.saveAttributes();
					
					
		        } catch (Exception e) {
		            e.printStackTrace();
		        } finally {
		            processing.set(false);
		        }
		        
				processing.set(false);
		    }
		};

	  public void sendfile(String Host, int count)
	  {
		    //Send Picture file
		    int port = 12124;
		    Log.d("TAG", Host);					    
		    File file = new File(strCaptureFilePath + Imei + "-" + count + ".jpg");
    		int length = (int) file.length();

		    if (file.exists())
		    {
		    	try {
		    		Socket skt = new Socket(Host, port); 
	 
	              PrintStream printStream = 
	                    new PrintStream(skt.getOutputStream()); 

	              //printStream.println(file.getName()); 
	              BufferedInputStream inputStream = 
	                    new BufferedInputStream( new FileInputStream(file) ); 
	            
	              int bufferSize = 1024;
	              byte[] buffer = new byte[bufferSize];
	                
	              while((length = inputStream.read(buffer)) != -1)
	                {
	                printStream.write(buffer, 0, length);
	                }
	 
	              printStream.flush();
	              printStream.close();
	              inputStream.close(); 
	 
	              skt.close();
	              
	              //delete
	              delFile(file.getName());
	              
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    	
		    }

	  }

	  private PictureCallback jpegCallback = new PictureCallback() 
	  {
	    public void onPictureTaken(byte[] _data, Camera _camera)
	    {
	      // TODO Handle JPEG image data
	     //finding pos
         Camera.Size size = _camera.getParameters().getPreviewSize();
	     String pathfile = strCaptureFilePath + Imei + "-" + PictureCount + ".jpg";    	    
	    	
	      
	      Bitmap bm = BitmapFactory.decodeByteArray(_data, 0, _data.length); 

	     //thread for position
		 DetectionThread thread = new DetectionThread(_data, bm, size.width, size.height, pathfile);
		 thread.start();
	      
	      PictureCount++;
	      
	      File myCaptureFile = new File(pathfile);
	      
	      try
	      {
	        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(myCaptureFile));
	        bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
	        
	        bos.flush();	        
	        bos.close();

            ExifInterface exif = new ExifInterface(pathfile);
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, Integer.toString((int)( mCameraOrientation*180/3.14) + 90));
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "" + latDegree +"/1," + latMinute + "/1," + latSecond + "/1");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, ""+ lonDegree + "/1, " + lonMinute + "/1," + lonSecond + "/1");      
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, String.valueOf(System.currentTimeMillis()/1000));      
            exif.saveAttributes();
	      
            if (mode == 1)
            {
            	openAskDialog("Do you want upload to server?", 1, Imei + "-" + PictureCount + ".jpg");
            }
            else
            {
		        resetCamera();        
		        initCamera();
            }
	      }
	      catch (Exception e)
	      {
	        Log.e(TAG, e.getMessage());
	      }
	    }
	  };
	  
	  private void delFile(String strFileName)
	  {
	    try
	    {
	      File myFile = new File(strFileName);
	      if(myFile.exists())
	      {
	        myFile.delete();
	      }
	    }
	    catch (Exception e)
	    {
	      Log.e(TAG, e.toString());
	      e.printStackTrace();
	    }
	  }
	  
	  public void mMakeTextToast(String str, boolean isLong)
	  {
	    if(isLong==true)
	    {
	      Toast.makeText(CameraControl.this, str, Toast.LENGTH_LONG).show();
	    }
	    else
	    {
	      Toast.makeText(CameraControl.this, str, Toast.LENGTH_SHORT).show();
	    }
	  }
	  
	  private boolean checkSDCard()
	  {
	    if(android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
	    {
	      return true;
	    }
	    else
	    {
	      return false;
	    }
	  }
	  
	  @Override
	  public void surfaceChanged(SurfaceHolder surfaceholder, int format, int w, int h)
	  {
	    // TODO Auto-generated method stub
	    Log.i(TAG, "Surface Changed");
	    
	    if(bIfPreview){
	    	mCamera01.stopPreview();
	        bIfPreview = false;
	   	 }
	    
	   	 try {
	   		 
	         try{
	        	 
	 			fBearing =(float) (fBearing * 180 / 3.14);// 57.32=180 / 3.14
	 			if (fBearing < 0) fBearing +=360;
	 			int value =(int)( mCameraOrientation*180/3.14) + 90;

	 			//openMessageDialog(Integer.toString(value));
	 			int angle = roundOrientation(value);
	 			Log.d("angle", Integer.toString(angle));
	 			
	 			try{
	 				mCamera01.setDisplayOrientation(angle);
	 			}
	 			catch (Exception X)
	 			{
	 				
	 			} 		 
	 		}catch (Exception X)
	 		{
	 		}	 
	    	  mCamera01.setPreviewDisplay(surfaceholder);
	    	  mCamera01.startPreview();
	    	  bIfPreview = true;
	    	  
	   	 } catch (IOException e) {
	    	  // TODO Auto-generated catch block
	    	  e.printStackTrace();
	    }
	  }
	  
	  @Override
	  public void surfaceCreated(SurfaceHolder surfaceholder)
	  {
	    // TODO Auto-generated method stub
		  	Log.i(TAG, "surfaceCreated");
	    	//mCamera01 = Camera.open();
		  	resetCamera(); 
		  	initCamera();
	   }
	  
	  @Override
	  public void surfaceDestroyed(SurfaceHolder surfaceholder)
	  {
	    // TODO Auto-generated method stub
	    try
	    {
	    	mSensorManager.unregisterListener(mSensorListener); 
	    	mCamera01.stopPreview();
	    	mCamera01.release();
	    	mCamera01 = null;
	    	bIfPreview = false;
	       //(strCaptureFilePath);
	    }
	    catch(Exception e)
	    {
	      e.printStackTrace();
	    }
	    Log.i(TAG, "Surface Destroyed");
	  }
	  
	  private int roundOrientation(int orientationInput) 
	  {
		  int orientation = orientationInput;
		  int retVal;

		  if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) 
		  {
			  orientation = 0;
		  }
		  orientation = orientation % 360;
		  
		  if (orientation < 45) {
			  retVal = 0;
		  } else if (orientation <135) {//90+45
			  retVal = 90;
		  } else if (orientation < 225) {//180+45
			  retVal = 180;
		  } else if (orientation < 315) {//270+45
			  retVal = 270;
		  } else {
			  retVal = 0;
		  }
		  // Log.d(TAG,"roundOrientation o:"+orientationInput + " " + retVal);
		   return retVal;
		} 
	  
	    @Override
	    public void onLocationChanged(Location location) {
	        if (location != null) {           
	            // Get latitude and longitude from the GPS
	            latitude = location.getLatitude();
	            longitude = location.getLongitude();
	            
	            // Get Latitude in Degree/Minute/Second
	            LatLonConvert latConvert = new LatLonConvert(latitude);          
	            latDegree = latConvert.getDegree();
	            latMinute = latConvert.getMinute();
	            latSecond = latConvert.getSecond();
	            
	            // Get Longitude in Degree/Minute/Second
	            LatLonConvert lonConvert = new LatLonConvert(longitude);          
	            lonDegree = lonConvert.getDegree();
	            lonMinute = lonConvert.getMinute();
	            lonSecond = lonConvert.getSecond();           
	        }  
	    }
	    
	    @Override
	    public void onProviderDisabled(String provider) {
	        // TODO Auto-generated method stub
	      
	    }

	    @Override
	    public void onProviderEnabled(String provider) {
	        // TODO Auto-generated method stub
	      
	    }

	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras) {
	        // TODO Auto-generated method stub
	      
	    }  
	    
	    public void showmessage(String str)
	    {
			Toast.makeText(CameraControl.this, str, Toast.LENGTH_SHORT).show();
	    }

	
    private void openOptionsDialog(String info)
	{
	    new AlertDialog.Builder(this)
	    .setTitle("initial error")
	    .setMessage(info)
	    .setPositiveButton("OK",
	        new DialogInterface.OnClickListener()
	        {
	         public void onClick(DialogInterface dialoginterface, int i)
	         {
	            	finish();
	         }
	        }
	        )
	    .show();
	}

    public class upload extends Thread
    {

    String filename;
    int removep;    

    upload(String mfilename,int rp)
    {
    	filename = mfilename;
    	removep = rp;
    }

    public void run()
    {
	    FTPClient client = new FTPClient();
	    FileInputStream fis = null;
	
	    try {
	    client.connect(IPAdrress);
	    client.login(ftp_account, ftp_password);
	
	
	    if (client.isConnected() == true)
	    {
	    //
	    // Create an InputStream of the file to be uploaded
	    //
	    Log.i("TAG", filename);
	
	    try {
	    Thread.sleep(2000);
	    }
	    catch(InterruptedException e) {
	    }

	    //PictureCount
	    boolean mkDir = client.makeDirectory(Integer.toString(PictureCount));
        client.changeWorkingDirectory(Integer.toString(PictureCount));

	    fis = new FileInputStream(strCaptureFilePath + filename);
	
	    //
	    // Store file to server
	    //
	    client.storeFile(filename, fis);
	    client.logout();
	    
	    if (removep == 1)
	    {
	    	Log.i(TAG, "remove picture");
	    	File file = new File(strCaptureFilePath + filename);
	    	boolean deleted = file.delete();
	    }
	    
	    }
	    } catch (IOException e) {
	    e.printStackTrace();
	    } finally {
	
	    try {
	    if (fis != null) {
	    fis.close();
	    }
	    client.disconnect();
	    } catch (IOException e) {
	    e.printStackTrace();
	    }
	    }
	    }
    }    
    
	
    private void openMessageDialog(String info)
	{
	    new AlertDialog.Builder(this)
	    .setTitle("message")
	    .setMessage(info)
	    .setPositiveButton("OK",
	        new DialogInterface.OnClickListener()
	        {
	         public void onClick(DialogInterface dialoginterface, int i)
	         {
	        	 if (ss !=  null)
	        	 {
		        	 if (ss.IsSync == false)
		        	 {
			        	 openSyncMessageDialog("not sync");
		        	 }
	        	 }
	        	 else
		        	 openSyncMessageDialog("not sync");
	         }
	        }
	        )
	    .show();
	}

    private void openSyncMessageDialog(String info)
	{
	    new AlertDialog.Builder(this)
	    .setTitle("message")
	    .setMessage(info)
	    .setPositiveButton("OK",
	        new DialogInterface.OnClickListener()
	        {
	         public void onClick(DialogInterface dialoginterface, int i)
	         {
	        	 if (ss != null)
	        	 {
		        	 if (ss.IsSync == false)
		        	 {
		        		 openSyncMessageDialog("must sync");
		        	 }
	        	 }
	        	 else
	        		 openSyncMessageDialog("must sync");	        		 
	         }
	        }
	        )
	    .show();
	}
    
    private void openAskDialog(String upload_msg, int index, String fn) 
    {
    	msg_index = index;
    	sfilename = fn;
    	
        new AlertDialog.Builder(this)
          .setTitle(upload_msg)
          .setMessage("Yes or No")
          .setNegativeButton("No",
        		  
              new DialogInterface.OnClickListener() {
              
                public void onClick(DialogInterface dialoginterface, int i) 
                {
                  	if (msg_index == 1)
                	{
                  		resetCamera();        
        		        initCamera();           		
                	}                	
                  	else if (msg_index == 2)
                	{
	              		Thread t = new upload(sfilename, 2);
	              		t.start();            		
	    		        resetCamera();        
	    		        initCamera();
                	}
                }
          }
          )
       
          .setPositiveButton("Yes",
              new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialoginterface, int i) 
              {
              	if (msg_index == 1)
            	{
                    openAskDialog("Do you delete this pciture in phone?", 2, sfilename);             		
            	}
            	else if (msg_index == 2)
            	{
              		Thread t = new upload(sfilename, 1);
              		t.start();

    		        resetCamera();        
    		        initCamera();
            	}
              }
              
          }
          )
          
          .show();
      }
    
}