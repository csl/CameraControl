package com.camera;

import java.io.*;
import java.net.*;

import android.util.Log;
import android.widget.Toast;

public class SocketServer implements Runnable
{
	private int port;
	private ServerSocket sc;
	private CameraControl mysc;
	
	public boolean IsSync;

	public SocketServer(int port, CameraControl sc)throws IOException
	{
		this.port = port;
		this.sc = new ServerSocket(port);
		mysc = sc;
		IsSync = false;
	}
	
	public void run()
	{
		Socket con = null;
		while(true)
		{
			try
			{
				con = this.sc.accept();
				DataInputStream in = new DataInputStream(con.getInputStream());
				String str = in.readUTF();
				Log.v("vDEBUG: ", "vClient " + str);
				
				if (str.equals("getTimeStamp"))
				{
					DataOutputStream out = new DataOutputStream(con.getOutputStream());
					
					out.writeUTF(Long.toString(System.currentTimeMillis()/1000));
					out.writeUTF("END");
					out.flush();
				}
				else if (str.equals("NeedSync"))
				{
					long diff = mysc.SyncTimeStamp();

					if (diff == 0)
					{
						DataOutputStream out = new DataOutputStream(con.getOutputStream());
						
						out.writeUTF(Long.toString(System.currentTimeMillis()/1000));
						out.writeUTF("END");
						out.flush();
					}
				}
				else if (str.equals("Sync"))
				{
					IsSync = true;		
					DataOutputStream out = new DataOutputStream(con.getOutputStream());
					out.writeUTF(mysc.Imei);					
					out.writeUTF("END");
					out.flush();
				}
				else if (IsSync == true && str.equals("getTakePicture"))
				{
				    String count = in.readUTF();
				    mysc.PictureCount = Integer.valueOf(count);					    
				    Log.d("TAG", "getTakePicture " + mysc.PictureCount);
					DataOutputStream out = new DataOutputStream(con.getOutputStream());
					
					mysc.takePicture();
					out.writeUTF("OK");
					out.flush();
						
				}
				else if (IsSync == true && str.equals("getPicData"))
				{
					    Log.d("TAG", "getPicData");
					    String count = in.readUTF();
					    mysc.sendfile(con.getInetAddress().toString().substring(1), Integer.valueOf(count));				    
				}
				else if (IsSync == true && str.equals("reSetPicNumber"))
				{
					    Log.d("TAG", "reSetPicNumber");
					    String count = in.readUTF();
					    mysc.PictureCount = Integer.valueOf(count);					    
				}
				
				in.close();
				con.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
}
