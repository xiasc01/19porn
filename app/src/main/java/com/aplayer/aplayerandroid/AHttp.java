package com.aplayer.aplayerandroid;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Environment;
import android.util.Log;

import cn.droidlover.xdroid.demo.kit.DES;
import cn.droidlover.xdroid.demo.kit.MyBase64;

public class AHttp
{
	private static final String TAG 			  = "APlayerAndroid";
	private InputStream         mInputStream      = null;
	private HttpURLConnection   mHttpConnection   = null;
	private String 				mUrlPath          = null;
	private long                mCurPos           = 0;
	private long                mCurHttpPos       = 0;
	private long                mFileSize         = 0;
	private CacheFile           mFileBuf          = null;
	private String              mCacheFileDir     = null;
	private String              mCurCacheFileName = "";
	private boolean             mUseCache         = true;
	private static  int    		mObjid            = 0;
	private byte[] 				mDecryptKey       = null;
	private boolean             mUseDES           = true;
	
	private final int           mHeadEncrySize     = 1024;
	private final int           mSegmentSize       = 4096;
	private final int           mEncrySizePerTime  = 64;

	private long                mOffset            = 0;
	private String              mOrigUrl           = null;
	private DES                 mDes;
	
	
	private class CacheFileDirManage implements Runnable{
		private File dirFile;
		private List<AFileInfo> listAFiles = new ArrayList<AFileInfo>();
		
		public CacheFileDirManage(String dirPath) {
			dirFile = new File(dirPath,"");
			new Thread(this).start();
		}
		
		public void run(){
			long usableSpace = dirFile.getUsableSpace();
			
			long dirFileSize = 0;
			File[] files = dirFile.listFiles();
			if(files != null){
				for(int i = 0;i < files.length;i++){
					AFileInfo aFileInfo = new AFileInfo();
					aFileInfo.file = files[i];
					aFileInfo.modifyTime = files[i].lastModified();
					listAFiles.add(aFileInfo);
					
					dirFileSize += files[i].length();
				}
			}
			
			if(listAFiles != null){
				Collections.sort(listAFiles);
			}
			
			
			String curCacheFileDataName  = mCurCacheFileName + ".data";
			String curCacheFileRecName   = mCurCacheFileName + ".rec";
			
			long diffSize =  dirFileSize - (long)(usableSpace * 0.1);
			if(diffSize > 0){
				for(int i = 0;i < listAFiles.size();i++){
					AFileInfo aFileInfo = listAFiles.get(i);
					String aFileName = aFileInfo.file.getName();
					
					if(aFileName.compareTo(curCacheFileRecName) == 0 || aFileName.compareTo(curCacheFileDataName) == 0){
						Log.e(TAG, aFileName + " is using,can not delete");
						continue;
					}
				
					diffSize -= aFileInfo.file.length();
					aFileInfo.file.delete();
					if(diffSize < 0){
						break;
					}
				}
			}
		}
		
		private class AFileInfo  implements Comparable{
			File file;
			long modifyTime;
			public int compareTo(Object o){
				AFileInfo fileInfo = (AFileInfo)o;
				return (int)(this.modifyTime - fileInfo.modifyTime);
			}
		}
	}
	
	private class CacheFile{
		
		private class BufNode implements Comparable{
			public long filePos; 
			public long startPos;
			public int  size;
			public long  recFilePos;
			public int compareTo(Object o){
				BufNode bufNode = (BufNode)o;
				return (int)(this.startPos - bufNode.startPos);
			}
		}
		
		private FileInputStream   mFileInputStream;
		private FileOutputStream  mFileOutputStream;
		private RandomAccessFile  mRandomAccessFile;
		private RandomAccessFile  mRecRandomAccessFile;
		
		private List<BufNode>     mListBufNodes;
		private long              mReadStreamPos;
		private long              mWriteStreamPos;
		private byte[]            mMemoryBuf;
		private static final int  mMemoryBufSize = 2 * 1024 * 1024;
		private BufNode           mCurWriteBufNode;
		private long              mFileSize = 0;
		
		public CacheFile(){
			mWriteStreamPos = 0;
			mReadStreamPos  = 0;
			mListBufNodes = new ArrayList<BufNode>();
		}
		
		public boolean open(String filePath){	
			String filePathData = filePath + ".data";
			String filePathRec  = filePath + ".rec";
			
			File fileData = new File(filePathData);
			File fileRec  = new File(filePathRec);
			
			if(fileData.exists() && !fileRec.exists()){
				if(!fileData.delete()){
					return false;
				}
			}
			if(!fileData.exists() && fileRec.exists()){
				if(!fileRec.delete()){
					return false;
				}
			}
			
	        if(!fileData.exists()){
	        	try {
					fileData.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
	        }
	        
	        if(!fileRec.exists()){
	        	try {
					fileRec.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
	        }
	        
	        try {
	        	mFileOutputStream = new FileOutputStream(fileData,true);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
	        
	        try {
				mRandomAccessFile = new RandomAccessFile(fileData, "r");
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
	        
	        try {
				mRecRandomAccessFile = new RandomAccessFile(fileRec, "rw");
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
	        
	        int recFileLength = 0;
	        try {
				recFileLength = (int) mRecRandomAccessFile.length();
			} catch (IOException e3) {
				e3.printStackTrace();
				return false;
			}
	        
	        int nodeSize = 0;
	        if(recFileLength >= 12){
				try {
					mFileSize = mRecRandomAccessFile.readLong();
					nodeSize  = mRecRandomAccessFile.readInt();
				}catch(EOFException e2){
					e2.printStackTrace();
					mFileSize = 0;
					nodeSize = 0;
				} 
				catch (IOException e1) {
					e1.printStackTrace();
					return false;
				}
	        }else{
	        	try {
	        		mRecRandomAccessFile.seek(0L);
	        		mRecRandomAccessFile.writeLong(0L);
					mRecRandomAccessFile.writeInt(0);
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
	        }
	        
	        try {
				if(nodeSize * 20  + mRecRandomAccessFile.getFilePointer() != mRecRandomAccessFile.length()){
					Log.e(TAG, "recfile size is not right");
					return false;
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
	        for(int i = 0;i < nodeSize;i++){
	        	BufNode bufNode = new BufNode();
	        	try {
	        		bufNode.recFilePos = mRecRandomAccessFile.getFilePointer();
	        		bufNode.filePos    = mRecRandomAccessFile.readLong();
					bufNode.startPos   = mRecRandomAccessFile.readLong();
					bufNode.size       = mRecRandomAccessFile.readInt();
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
	        	mListBufNodes.add(bufNode);
	        }
	        
	        if(mListBufNodes.size() > 0){
	        	Collections.sort(mListBufNodes);
	        }
	        
	        Log.i(TAG, "AbufNode open nodesize = " + mListBufNodes.size());
	        
	        mWriteStreamPos = fileData.length();
	        for(int i = 0;i < mListBufNodes.size();i++){
	        	BufNode bufNode = mListBufNodes.get(i);
	        	if(bufNode.filePos + bufNode.size == mWriteStreamPos){
	        		Log.i(TAG,"find mCurWriteBufNode filePos = " + bufNode.filePos +" mWriteStreamPos = " + mWriteStreamPos);
	        		mCurWriteBufNode = bufNode;
	        		break;
	        	}
	        }
	        
	        if(mCurWriteBufNode == null){
	        	/*if(mListBufNodes.size() > 0){
	        		Log.i(TAG,"listbufnodes is not empty,but curnodebuf is not finded");
	        		return false;
	        	}*/
	        	mCurWriteBufNode = new BufNode();
				mCurWriteBufNode.filePos    = mWriteStreamPos;
				mCurWriteBufNode.startPos   = 0;
				mCurWriteBufNode.size       = 0;
				try {
					mCurWriteBufNode.recFilePos = (int)mRecRandomAccessFile.getFilePointer();
					mRecRandomAccessFile.writeLong(mCurWriteBufNode.filePos);
					mRecRandomAccessFile.writeLong(mCurWriteBufNode.startPos);
					mRecRandomAccessFile.writeInt(mCurWriteBufNode.size);
					mRecRandomAccessFile.seek(8L);
					mRecRandomAccessFile.writeInt(mListBufNodes.size() + 1);
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				}
				mListBufNodes.add(mCurWriteBufNode);
	        }
	        
			return true;
		}
		
		public long    getFileSize(){
			return mFileSize;
		}
		
		public void setFileSize(long fileSize){
			mFileSize = fileSize;
			if(mRecRandomAccessFile != null){
				try {
					mRecRandomAccessFile.seek(0L);
					mRecRandomAccessFile.writeLong(mFileSize);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void write(long fileOffset, byte[] buffer,int byteOffset,int byteCount) throws Exception{	
			boolean hasNewNode = false;
			
			if(mFileOutputStream == null || mRecRandomAccessFile == null){
				return;
			}
			
			for(int i = 0;i < mListBufNodes.size();i++){
				BufNode bufNode = mListBufNodes.get(i);
				long sst  = fileOffset - bufNode.startPos;
				long eet  = (fileOffset + byteCount) - (bufNode.startPos + bufNode.size);
				long set  = fileOffset - (bufNode.startPos + bufNode.size);
				long est  = (fileOffset + byteCount) -bufNode.startPos;
				
				if(sst < 0 && eet < 0 && est > 0){
					byteCount = (int)sst * -1;	
					break;
				}
				if(sst > 0 && eet > 0 && set < 0){
					fileOffset  = fileOffset - set;
					byteOffset  = byteOffset - (int)set;
					byteCount   = byteCount + (int)set;
				}
				
				if(sst >= 0 && eet <= 0){
					byteCount = 0;
					break;
				}
				
				if(sst <= 0 && eet >= 0){
					if(sst != 0){
						mCurWriteBufNode = new BufNode();
						mCurWriteBufNode.filePos  = mWriteStreamPos;
						mCurWriteBufNode.startPos = fileOffset;
						mCurWriteBufNode.size     = (int)sst * -1;
						mListBufNodes.add(mCurWriteBufNode);
						mFileOutputStream.write(buffer, byteOffset, mCurWriteBufNode.size);
						mWriteStreamPos += mCurWriteBufNode.size;
						hasNewNode = true;
					}
					fileOffset  = fileOffset - set;
					byteOffset  = byteOffset - (int)set;
					byteCount   = byteCount + (int)set;
				}
			}

			if(byteCount != 0){
				mFileOutputStream.write(buffer, byteOffset, byteCount);

				if((mCurWriteBufNode.startPos + mCurWriteBufNode.size ==  fileOffset) &&
						(mWriteStreamPos == mCurWriteBufNode.filePos + mCurWriteBufNode.size)){
					mCurWriteBufNode.size += byteCount;
					mRecRandomAccessFile.seek(mCurWriteBufNode.recFilePos + 16L);
					mRecRandomAccessFile.writeInt(mCurWriteBufNode.size);
				}else{
					mCurWriteBufNode = new BufNode();
					mCurWriteBufNode.filePos  = mWriteStreamPos;
					mCurWriteBufNode.startPos = fileOffset;
					mCurWriteBufNode.size     = byteCount;

					{
						long length = mRecRandomAccessFile.length();
						mRecRandomAccessFile.seek(length);
						mCurWriteBufNode.recFilePos = (int)length;
						mRecRandomAccessFile.writeLong(mCurWriteBufNode.filePos);
						mRecRandomAccessFile.writeLong(mCurWriteBufNode.startPos);
						mRecRandomAccessFile.writeInt(mCurWriteBufNode.size);
						mRecRandomAccessFile.seek(8L);
						mRecRandomAccessFile.writeInt(mListBufNodes.size() + 1);
					}

					mListBufNodes.add(mCurWriteBufNode);
					hasNewNode = true;
				}

				mWriteStreamPos += byteCount;
			}

			if(hasNewNode){
				hasNewNode = false;
				Collections.sort(mListBufNodes);
				for(int i = 0;i < mListBufNodes.size();i++){
					BufNode bufNode = mListBufNodes.get(i);
				}
			}
			
			return;
		}
		
		public int read(long fileOffset, byte[] buffer,int byteOffset,int byteCount) throws Exception{
			if(mRandomAccessFile == null){
				return -1;
			}
			
			for(int i = 0;i < mListBufNodes.size();i++){
				BufNode bufNode = mListBufNodes.get(i);
				long sst  = fileOffset - bufNode.startPos;
				long eet  = (fileOffset + byteCount) - (bufNode.startPos + bufNode.size);
				long set  = fileOffset - (bufNode.startPos + bufNode.size);
				long est  = (fileOffset + byteCount) -bufNode.startPos;
				
				if(sst >= 0 && set < 0){
					int canReadByte = (int)(0 - set);
					if(canReadByte < byteCount){
						byteCount = canReadByte;
					}
					long filepos = bufNode.filePos;
					filepos += sst;
					mRandomAccessFile.seek(filepos);
					return mRandomAccessFile.read(buffer, byteOffset, byteCount);
				}
			}
			return -1;
		}
		
		public boolean close(){	
			try {
				if(mFileOutputStream != null){
					mFileOutputStream.close();
					mFileOutputStream = null;
				}
				
				if(mRecRandomAccessFile != null){
					mRecRandomAccessFile.close();
					mRecRandomAccessFile = null;
				}
				
				if(mRandomAccessFile != null){
					mRandomAccessFile.close();
					mRandomAccessFile = null;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return true;
		}
	}
	
	public AHttp(){
		mUseDES  = false;
		mObjid++;
	}
	
	public int open(String urlPath){
		Log.i(TAG, "Ahttp " + urlPath);

		mOrigUrl = urlPath;
		mOffset  = getPosFromUrl(mOrigUrl);

		init();

		String decryptKey = getKeyFromUrl(mOrigUrl);
		if(decryptKey != null){
			mDecryptKey = MyBase64.decode(decryptKey.getBytes());
			if(mDecryptKey != null){
				mUseDES 	= true;
			}
		}

		if(mUseDES){
			mDes = new DES();
			mDes.setKey(mDecryptKey);
			//native_dec_set_key(mDecryptKey, mObjid);
		}
		
		mUrlPath  = getReallyUrl(mOrigUrl);
		if(mUrlPath == null){
			mUrlPath = mOrigUrl;
		}

		if(!openBufFile()){
			return -1;
		}
		
		if(mFileBuf != null && mFileBuf.getFileSize() > 0){
			return 1;
		}
		
		
		if(!openHttpFile(mCurPos)){
			return -1;
		}
		
		
		if(getFileSize() > 0){
			mFileBuf.setFileSize(getFileSize());
			return 1;
		}
		
		return -1;
	}
	
	public int close(){
		closeBufFile();
		closeHttpFile();
		return 1;
	}

	private String getReallyUrl(String url){
		String pattern = "^(.+)&pos=\\d+";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(url);
		if(m.find()){
			String reallyUrl = m.group(1);
			return reallyUrl;
		}
		return  null;
	}

	private Long getPosFromUrl(String url){
		String pattern = "&pos=(\\d+)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(url);
		if(m.find()){
			String pos = m.group(1);
			return  Long.parseLong(pos);
		}
		return  0L;
	}

	private Long getSizeFromUrl(String url){
		String pattern = "&size=(\\d+)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(url);
		if(m.find()){
			String size = m.group(1);
			return  Long.parseLong(size);
		}
		return  0L;
	}

	private String getKeyFromUrl(String url){
		String pattern = "&key=(.+)&md5=";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(url);
		if(m.find()){
			String key = m.group(1);
			return key;
		}
		return  null;
	}

	private String getMd5FromUrl(String url){
		String pattern = "&md5=(.+)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(url);
		if(m.find()){
			String md5 = m.group(1);
			return md5;
		}
		return  null;
	}

	private int fileBufRead(long desPos,int size,ByteBuffer bbfout){
		
		byte[] tempBuf = new byte[size];
		
		int readByte 	= -1;
		try {
			readByte  = mFileBuf.read(desPos,tempBuf, 0, size);
		} catch (Exception e) {
			e.printStackTrace();
			mUseCache = false;
			return -1;
		}
		
		if(-1 != readByte){
			Log.i(TAG, "Ahttp file buf read readbyte = " + readByte);
			
			if(mUseDES){
				int desReadByte = desDecrypt(tempBuf, desPos, readByte);
				
				int offset = (int)(mCurPos - desPos);
				desReadByte -= offset;
				
				if(desReadByte > 0){	
					bbfout.put(tempBuf,offset, desReadByte);
				}
				readByte =  desReadByte;
			}else{
				bbfout.put(tempBuf, 0, readByte);
			}
			
			if(readByte == 0){
				Log.e(TAG, "ahttp readByte == 0");
			}
			return readByte;
		}
		
		return readByte;
	}
	
	private int httpRead(long desPos,int size,ByteBuffer out){
		
		byte[] tempBuf = new byte[size];
		
		int readByte = httpReadInner(tempBuf,desPos,size);
		
		if(readByte != -1){
			if(mUseCache){
				try {
					mFileBuf.write(desPos,tempBuf, 0, readByte);
				} catch (Exception e) {
					e.printStackTrace();
					mUseCache = false;
				}
			}
			
			if(mUseDES){
				int desReadByte = desDecrypt(tempBuf, desPos, readByte);
				
				int offset = (int)(mCurPos - desPos);
				desReadByte -= offset;
				
				if(desReadByte > 0){
					out.put(tempBuf,offset, desReadByte);
				}
				readByte =  desReadByte;
			}else{
				out.put(tempBuf, 0, readByte);
			}
		}
		
		return readByte;
	}
	
	private int httpReadInner(byte[] buffer,long pos,int size){
		
		if(pos != mCurHttpPos){
			seekHttpFile(pos);
			mCurHttpPos = pos;
		}
		
		
		if(mInputStream == null){
			if(!openHttpFile(mCurHttpPos)){
				return -1;
			}
		}
		
		int readByte = -1;
		try {
			readByte = mInputStream.read(buffer, 0, size);
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
		mCurHttpPos += readByte;
		return readByte;
	}
	
	public int read(Object bufOut){
		
		Log.i(TAG,"ahttp read mCurPos = " + mCurPos + " mCurHttpPos = " + mCurHttpPos);
		
		if(bufOut == null){
			return -1;
		}
		
		ByteBuffer bbfout = ((ByteBuffer) bufOut);
		bbfout.position(0);
		int size = bbfout.limit();
		
		if(size == 0){
			Log.e(TAG, "ahttp read size == 0");
			return -1;
		}
		
		long desPos = mCurPos;
		
		if(mUseDES){
			desPos = desAdjustStreamPos(mCurPos);
			size = size / 8 * 8;
			if(size == 0){
				Log.e(TAG, "ahttp read size == 0");
			}
		} 
		
		if(mUseCache && mFileBuf != null){
			int readByte = this.fileBufRead(desPos, size, bbfout);
			if(readByte > 0){
				mCurPos += readByte;
				return readByte;
			}
		}
		
		int readByte = httpRead(desPos, size, bbfout);
		
		Log.i(TAG,"ahttp read readByte = " + readByte);
		
		mCurPos += readByte;
		return readByte;
	}
	
	public long seek(long offset, int whence){
		Log.i(TAG, "Ahttp seek offset = " + offset + "whence = " + whence);
		 if(0x10000 == whence){
			getFileSize();
			return mFileSize;
		}
		
		long pos = 0;
		if(0 == whence){
			pos = offset;
		}else if(1 == whence){
			pos = mCurPos + offset;
		}else if(2 == whence){
			getFileSize();
			pos = mFileSize - offset;
		}else{
			Log.e(TAG, "Ahttp seek whence = " + whence);
			return -1;
		}
		mCurPos = pos;
		return 1;
	}
	
	public int setCacheFileDir(String cacheFileDir){
		File file = new File(cacheFileDir);
		if(!file.exists()){
			try {
				file.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
		}
		
		if(!file.isDirectory()){
			return -1;
		}
		
		mCacheFileDir = cacheFileDir;
		CacheFileDirManage cacheFileDirManage = new CacheFileDirManage(cacheFileDir);
		return 1;
	}
	
	private void init(){
		mInputStream      = null;
		mHttpConnection   = null;
		mUrlPath          = null;
		mCurPos           = 0;
		mCurHttpPos       = 0;
		mFileSize         = 0;
		mFileBuf          = new CacheFile();
		mCurCacheFileName = "";
		mUseCache         = true;
	}
	
	private long getFileSize(){
		if(mFileSize == 0){
			mFileSize = getSizeFromUrl(mOrigUrl);
		}

		if(mFileSize == 0 && mFileBuf != null){
			mFileSize = mFileBuf.getFileSize();
		}
		
		if(mFileSize == 0 && mHttpConnection != null){
			mFileSize = (long)mHttpConnection.getContentLength();
		}
		
		return mFileSize;
	}
	
	private boolean seekHttpFile(long pos){
		Log.i(TAG,"seekHttpFile pos = " + pos);
		
		if(!closeHttpFile()){
			return false;
		}
		
		return openHttpFile(pos);
	}
	
	private boolean closeHttpFile(){
		try {
			if(mInputStream != null){
				mInputStream.close();
				mInputStream = null;
			}
				
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		if(mHttpConnection != null){
			mHttpConnection.disconnect();
			mHttpConnection = null;
		}
		
		return true;
	}
	
	private boolean openHttpFile(long pos){
		Log.i(TAG,"openHttpFile pos = " + pos);
		mCurHttpPos = pos;
		
		URL url = null;
		try {
			url = new URL(mUrlPath);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		
		try {
			mHttpConnection = (HttpURLConnection)url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} 
		
		mHttpConnection.setDoInput(true); //允许输入流，即允许下载  
		mHttpConnection.setRequestProperty("Accept", "*/*");

		pos += mOffset;
		long pos2 = mOffset + getFileSize();
		String range = "bytes=" + pos + "-" + pos2;

		try {
			mHttpConnection.setRequestProperty("Range", range);
			mHttpConnection.setRequestMethod("GET");
			mInputStream = mHttpConnection.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		/*try {
			mInputStream = mHttpConnection.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}*/
		Log.i(TAG,"openHttpFile success");
		return true;
	}
	
	private boolean openBufFile(){
		String filePathMd5 = getMd5FromUrl(mOrigUrl);
		if(filePathMd5 == null){
			filePathMd5 = stringToMD5(mUrlPath);
		}
		
		if(mCacheFileDir == null){
			mCacheFileDir = Environment.getExternalStorageDirectory().toString() + "/ahttp/";
			File file = new File(mCacheFileDir);
			if(!file.exists()){
				try {
					file.mkdirs();
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}
			
			if(!file.isDirectory()){
				return false;
			}
			
			CacheFileDirManage cacheFileDirManage = new CacheFileDirManage(mCacheFileDir);
		}
		
		mCurCacheFileName = filePathMd5;
		
		filePathMd5 = mCacheFileDir + filePathMd5;
		
		if(mFileBuf != null){
			return mFileBuf.open(filePathMd5);
		}
		return false;
	}
	
	private boolean closeBufFile(){
		if(mFileBuf != null){
			boolean ret = mFileBuf.close();
			mFileBuf  = null;
			return ret;
		}
		return false;	
	}
	
	private String stringToMD5(String string) {
		byte[] hash;

		try {
			hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		StringBuilder hex = new StringBuilder(hash.length * 2);
		for (byte b : hash) {
			if ((b & 0xFF) < 0x10)
				hex.append("0");
			hex.append(Integer.toHexString(b & 0xFF));
		}

		return hex.toString();
	}

	private long  desAdjustStreamPos(long streamPos){
		long desPos  = streamPos;
		
		if(streamPos < mHeadEncrySize){
			desPos  = streamPos / 8 * 8;
		}else{
			if((streamPos - mHeadEncrySize) % mSegmentSize < mEncrySizePerTime){
				desPos  = streamPos / 8 * 8;
			}
		}
		
		return desPos;
	}
	
	private int  desDecrypt(byte[] data,long streamPos,int size){
		long  pos 	 			          = streamPos;
		long  posEnd 			          = streamPos + size;
		long  decryptSize 		          = 0;
		int   desReadByte 		          = size;
		
		Log.i(TAG, "desDecrypt streampos = " + streamPos + " size = " + size);
		
		while(pos - posEnd < 0){
			if(pos < mHeadEncrySize){
				if(pos % 8 != 0){
					Log.e(TAG, "decrypt is error pos % 8 != 0");
					return -1;
				}
				
				if(posEnd - pos < 8){
					desReadByte -= (posEnd - pos);
					break;
				}
				
				if(posEnd - pos  < mHeadEncrySize){
					decryptSize = (posEnd - pos) / 8 * 8;
				}else{
					decryptSize = mHeadEncrySize - pos;
				}

				byte[] in 	= new byte[(int)decryptSize];
				byte[] out  = new byte[(int)decryptSize];
				System.arraycopy(data,(int)(pos -streamPos),in,0,(int)decryptSize);
				mDes.decrypt(in,out);
				System.arraycopy(out,0,data,(int)(pos -streamPos),(int)decryptSize);

				pos += decryptSize;
				
			}else{
				if((pos - mHeadEncrySize) % mSegmentSize < mEncrySizePerTime){
					
					if(pos % 8 != 0){
						Log.e(TAG, "decrypt is error pos % 8 != 0");
						return -1;
					}
					
					long enSegEndPos = (pos - mHeadEncrySize) / mSegmentSize * mSegmentSize + mHeadEncrySize + mEncrySizePerTime;
					if(posEnd > enSegEndPos){
						decryptSize = enSegEndPos - pos;
					}else{
						decryptSize = (posEnd - pos) / 8 * 8;
						if(decryptSize < 8){
							desReadByte -= (posEnd - pos);
							break;
						}
					}

					byte[] in 	= new byte[(int)decryptSize];
					byte[] out  = new byte[(int)decryptSize];
					System.arraycopy(data,(int)(pos -streamPos),in,0,(int)decryptSize);
					mDes.decrypt(in,out);
					System.arraycopy(out,0,data,(int)(pos -streamPos),(int)decryptSize);

					pos += decryptSize;
				}else{
					long segEndPos =  (pos - mHeadEncrySize) / mSegmentSize * mSegmentSize + mHeadEncrySize + mSegmentSize;
					
					if(posEnd > segEndPos){
						pos = segEndPos;
					}else{
						pos = posEnd;
					}
				}
			}		
		}
		Log.i(TAG, "desDecrypt streampos = " + streamPos + " size = " + size);
		return desReadByte;
	}

	private native int    native_dec_decrypt(ByteBuffer in,ByteBuffer out,int objid);
	private native int    native_dec_set_key(ByteBuffer code,int objid);
}