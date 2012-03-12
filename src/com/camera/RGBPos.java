package com.camera;

import java.util.ArrayList;

import android.graphics.Color;
import android.util.Log;

public class RGBPos
{
	private static final String TAG = "RgbMD";

	//Specific settings
	public static int mPixelThreshold = 25; //Difference in pixel (RGB)
	public static int mThreshold = 6000; //Number of different pixels (RGB)

	private static int[] mPrevious = null;
	private static int mPreviousWidth = 0;
	private static int mPreviousHeight = 0;
	
	private static int Range_rate = 5;

	private static int Width_limit = 0;
	private static int height_limit = 0;
	
	private static int length = 20;

	public int[] getPrevious() {
		return ((mPrevious!=null)?mPrevious.clone():null);
	}
	
    public static int  getAverageColor(int[][] gray, int x, int y, int w, int h)  
    {  
        int rs = gray[x][y]  
                        + (x == 0 ? 255 : gray[x - 1][y])  
                        + (x == 0 || y == 0 ? 255 : gray[x - 1][y - 1])  
                        + (x == 0 || y == h - 1 ? 255 : gray[x - 1][y + 1])  
                        + (y == 0 ? 255 : gray[x][y - 1])  
                        + (y == h - 1 ? 255 : gray[x][y + 1])  
                        + (x == w - 1 ? 255 : gray[x + 1][ y])  
                        + (x == w - 1 || y == 0 ? 255 : gray[x + 1][y - 1])  
                        + (x == w - 1 || y == h - 1 ? 255 : gray[x + 1][y + 1]);  
        return rs / 9;  
    }  	

    public Node gravity(ArrayList<Node> node_array)
    {
     int n=node_array.size();
     double area = 0;
     Node center = new Node(0,0);
     center.x = 0;
     center.y = 0;

     
     for (int i = 0; i < n-1; i++)
     {
      //Node p =  node_array.get(i);
      area += (node_array.get(i).x * node_array.get(i+1).y - node_array.get(i+1).x * node_array.get(i).y)/2;
      center.x += (node_array.get(i).x*node_array.get(i+1).y - node_array.get(i+1).x*node_array.get(i).y) * (node_array.get(i).x + node_array.get(i+1).x);
      center.y += (node_array.get(i).x*node_array.get(i+1).y - node_array.get(i+1).x*node_array.get(i).y) * (node_array.get(i).y + node_array.get(i+1).y);
     }

     area += (node_array.get(n-1).x*node_array.get(0).y - node_array.get(0).x*node_array.get(n-1).y)/2;
     center.x += (node_array.get(n-1).x*node_array.get(0).y - node_array.get(0).x*node_array.get(n-1).y) * (node_array.get(n-1).x + node_array.get(0).x);
     center.y += (node_array.get(n-1).x*node_array.get(0).y - node_array.get(0).x*node_array.get(n-1).y) * (node_array.get(n-1).y + node_array.get(0).y);

     center.x /= 6*area;
     center.y /= 6*area;

     return center;
    }
    
    
	public ArrayList<Node> detect(int[] rgb, int width, int height) {
		if (rgb==null) throw new NullPointerException();
		
		int[] original = rgb.clone();
		int gray[][] = new int[width][height];
		int grayimg[][] = new int[width][height];
		
		ArrayList<Node> allgravity = new ArrayList<Node>();

		for (int i = 0, ij=0; i < height; i++) {
			
			if (i < height_limit || i > height - height_limit) continue;
			
			for (int j = 0; j < width; j++, ij++) 
			{
				if (j < Width_limit || j > width - Width_limit) continue;
				
				int rpix = (0xff & ((int)rgb[ij]));
				int gpix = (0xff00 & ((int)rgb[ij]));
				int bpix = (0xff0000 & ((int)rgb[ij]));
				
				//Catch any pixels that are out of range
				if (rpix < 0) rpix = 0;
				if (rpix > 255) rpix = 255;
				//Catch any pixels that are out of range
				if (gpix < 0) gpix = 0;
				if (gpix > 255) gpix = 255;
				//Catch any pixels that are out of range
				if (bpix < 0) bpix = 0;
				if (bpix > 255) bpix = 255;
				
				int top = (rpix + gpix + bpix)/3;
				
				gray[i][j] = top; 
				
			}		
		}
		
		long bDetection = System.currentTimeMillis();
        int SW = 16;
		for (int x = 0; x < width; x++) {  
            for (int y = 0; y < height; y++) {  
                if(getAverageColor(gray, x, y, width, height)>SW){  
                	grayimg[x][y] = 255;
                }else{  
                	grayimg[x][y] = 0;
                }  
            }
        }  		
		
		int maxp = 46;
		int offset = 10;

		//Find Target
		ArrayList<Target> rtarget = new ArrayList<Target>();
		int oldx = -1, oldy = -1;
		for (int x=0; x < width; x++) 
		{  
            for (int y=0; y < height; y++) 
            {
    			int match = 0;
	            for (int c=x; c < (length+x); c++) 
	            {
	                for (int z=y; z < (length+y); z++) 
	                {
	                	if (grayimg[c][z] == 0)
	                	{
	                		match++;
	                	}
	                }
	            }
	            
	            if ((match <= maxp && (maxp >= maxp - offset)) || (match >= maxp &&(maxp <= maxp + offset)))
	            {
	            	if ((oldx == -1 && oldy == -1) || (oldx - x > length) || (oldx - y > length))
	            	{
	            		Target ntarget = new Target(x, y);
	            		rtarget.add(ntarget);
	            	}
	            }
	            oldx = x;
    			oldy = y;
            }
        }  		
		
		//Target, find point
		for (int i=0; i<rtarget.size(); i++)
		{
			ArrayList<Node> rnode = new ArrayList<Node>();
			Target now_node = rtarget.get(i);
			
			int start = now_node.getX();
			int end = (now_node.getX()+length);
			int xypoint = now_node.getY();
			
			//(x,y) -> (x+length,y)
			for (int j=start; j<end; j++)
			{
            	if (grayimg[j][xypoint-1] == 255)
            	{
            		if (grayimg[j-1][xypoint] == 255)
            		{
	            		Node trnode = new Node(j, xypoint);
	            		rnode.add(trnode);
            		}
            	}
            	else
            	{
            		int nx=j;
            		int ny=xypoint-2;
            		if (ny >= 0)
            		{
	            		if (grayimg[nx][ny] == 255)
	            		{
	                		if (grayimg[nx-1][ny] == 255)
	                		{
	    	            		Node trnode = new Node(nx, xypoint-1);
	    	            		rnode.add(trnode);
	                		}
	            		}
            		}
            	}
			}

			//(x,y) -> (x,y+length)
			start = now_node.getY();
			end = (now_node.getY()+length);
			xypoint = now_node.getX() + 1;
			
			for (int j=start; j<end; j++)
			{
            	if (grayimg[xypoint-1][j] == 255)
            	{
            		if (grayimg[xypoint][j+1] == 255)
            		{
	            		Node trnode = new Node(xypoint, j);
	            		rnode.add(trnode);
            		}
            	}
            	else
            	{
            		int nx=xypoint-2;
            		int ny=j;
            		if (nx >= 0)
            		{
	            		if (grayimg[nx][ny] == 255)
	            		{
	                		if (grayimg[nx][ny+1] == 255)
	                		{
	    	            		Node trnode = new Node(xypoint-1, ny);
	    	            		rnode.add(trnode);
	                		}
	            		}
            		}
            	}
			}
			
			//(x+length, y) -> (x+length,y+length)
			start = now_node.getY();
			end = (now_node.getY()+length);
			xypoint = now_node.getX() + length-1;
			
			for (int j=start; j<end; j++)
			{
            	if (grayimg[xypoint+1][j] == 255)
            	{
            		if (grayimg[xypoint][j+1] == 255)
            		{
	            		Node trnode = new Node(xypoint, j);
	            		rnode.add(trnode);
            		}
            	}
            	else
            	{
            		int nx=xypoint+2;
            		int ny=j;
            		if (nx >= 0)
            		{
	            		if (grayimg[nx][ny] == 255)
	            		{
	                		if (grayimg[nx][ny+1] == 255)
	                		{
	    	            		Node trnode = new Node(xypoint+1, ny);
	    	            		rnode.add(trnode);
	                		}
	            		}
            		}
            	}
			}
			
			//(x, y+length) -> (x+length,y+length)
			start = now_node.getX();
			end = (now_node.getX()+length) - 1;
			xypoint = now_node.getY() + length - 1;
			
			for (int j=start; j<end; j++)
			{
            	if (grayimg[j][xypoint+1] == 255)
            	{
            		if (grayimg[j+1][xypoint] == 255)
            		{
	            		Node trnode = new Node(j, xypoint);
	            		rnode.add(trnode);
            		}
            	}
            	else
            	{
            		int nx=j;
            		int ny=xypoint+2;
            		if (ny >= 0)
            		{
	            		if (grayimg[nx][ny] == 255)
	            		{
	                		if (grayimg[nx][ny+1] == 255)
	                		{
	    	            		Node trnode = new Node(nx, xypoint+1);
	    	            		rnode.add(trnode);
	                		}
	            		}
            		}
            	}
			}
			
			allgravity.add(gravity(rnode));
		}
		
		long aDetection = System.currentTimeMillis();
		
		Log.d(TAG, "Detection "+(aDetection-bDetection));
		
		return allgravity;
	}
	
	class Target
	{
		private int x;
		private int y;
		
		Target(int cx,int cy)
		{
			x = cx;
			y = cy;
		}
		
		int getX()
		{
			return x;
		}

		int getY()
		{
			return y;
		}
		
	}

	class Node
	{
		private int x;
		private int y;
		
		Node(int cx,int cy)
		{
			x = cx;
			y = cy;
		}
		
		int getX()
		{
			return x;
		}

		int getY()
		{
			return y;
		}
	}	
}
