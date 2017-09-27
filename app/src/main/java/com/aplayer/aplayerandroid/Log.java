package com.aplayer.aplayerandroid;

public class Log {
	
	private static boolean openLog = false;
	
	public static void setOpenLog(boolean openLog){
		Log.openLog = openLog;
	}
	
	public static int i(String tag, String msg) {
		if(!openLog) return 1;
		
		return android.util.Log.i(tag, msg);
    }
	
	public static int i(String tag, String msg, Throwable tr) {
		if(!openLog) return 1;
		
        return android.util.Log.i(tag, msg,tr);
    }
	
	public static int e(String tag, String msg) {
		if(!openLog) return 1;
		
		return android.util.Log.e(tag, msg);
    }
	
	public static int e(String tag, String msg, Throwable tr) {
		if(!openLog) return 1;
		
        return android.util.Log.e(tag, msg,tr);
    }
	
	public static int v(String tag, String msg) {
		if(!openLog) return 1;
		
		return android.util.Log.v(tag, msg);
    }
	
	public static int v(String tag, String msg, Throwable tr) {
		if(!openLog) return 1;
		
        return android.util.Log.v(tag, msg,tr);
    }
}
