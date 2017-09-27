package com.aplayer.aplayerandroid;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aplayer.aplayerandroid.APlayerAndroid.OnExtIOListerner;

import android.os.Build;
import android.os.Environment;

import cn.droidlover.xdroid.demo.kit.DES;
import cn.droidlover.xdroid.demo.kit.MyBase64;

public class AHttp  implements OnExtIOListerner
{
	private static final String TAG 			  	 = "APlayerAndroid";
	private String 				 mUrlPath          	 = null;
	private long                 mCurPos          	 = 0;
	private long                 mFileSize         	 = 0;
	private CacheFile            mFileBuf          	 = null;
	private String               mCacheFileDir     	 = null;
	private InterHttp            mInterHttp          = null;
	private String               mCurCacheFileName 	 = "";
	private boolean              mUseCache         	 = true;
	private CacheFileDirManage   mCacheFileDirManage = null;
	private boolean              mAbort              = false;
	private boolean              mInterHttpStart     = false;

	private byte[] 				mDecryptKey       = null;
	private boolean             mUseDES           = true;

	private final int           mHeadEncrySize     = 1024;
	private final int           mSegmentSize       = 4096;
	private final int           mEncrySizePerTime  = 64;

	private long                mOffset            = 0;
	private String              mOrigUrl           = null;
	private DES 				mDes;

	private class CacheFileDirManage implements Runnable{
		private File dirFile;
		
		public CacheFileDirManage(String dirPath) {
			dirFile = new File(dirPath,"");
			new Thread(this).start();
		}
		
		public void run(){
			List<AFileInfo> listAFiles = new ArrayList<AFileInfo>();
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
			
			/*if(listAFiles != null){
				Collections.sort(listAFiles);
			}*/
			
			
			String curCacheFileDataName  = mCurCacheFileName + ".data";
			String curCacheFileRecName   = mCurCacheFileName + ".rec";
			
			long diffSize =  dirFileSize - (long)(usableSpace * 0.3);
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
					/*if(diffSize < 0){
						break;
					}*/
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
		private FileOutputStream  mDataFileOutputStream;
		private RandomAccessFile  mDataRandomAccessFile;
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
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
	        }
	        
	        if(!fileRec.exists()){
	        	try {
					fileRec.createNewFile();
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
	        }
	        
	        try {
	        	mDataFileOutputStream = new FileOutputStream(fileData,true);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
	        
	        try {
				mDataRandomAccessFile = new RandomAccessFile(fileData, "r");
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
			} catch (Exception e3) {
				e3.printStackTrace();
				return false;
			}
	        
	        int nodeSize = 0;
	        if(recFileLength >= 12){
				try {
					mFileSize = mRecRandomAccessFile.readLong();
					nodeSize  = mRecRandomAccessFile.readInt();
				}
				catch (Exception e1) {
					e1.printStackTrace();
					return false;
				}
	        }else{
	        	try {
	        		mRecRandomAccessFile.seek(0L);
	        		mRecRandomAccessFile.writeLong(0L);
					mRecRandomAccessFile.writeInt(0);
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
	        }
	        
	        try {
				if(nodeSize * 20  + mRecRandomAccessFile.getFilePointer() != mRecRandomAccessFile.length()){
					Log.e(TAG, "recfile size is not right");
					return false;
				}
			} catch (Exception e1) {
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
			
			if(mDataFileOutputStream == null || mRecRandomAccessFile == null){
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
						mDataFileOutputStream.write(buffer, byteOffset, mCurWriteBufNode.size);
						mWriteStreamPos += mCurWriteBufNode.size;
						hasNewNode = true;
					}
					fileOffset  = fileOffset - set;
					byteOffset  = byteOffset - (int)set;
					byteCount   = byteCount + (int)set;
				}
			}

			if(byteCount != 0){
				mDataFileOutputStream.write(buffer, byteOffset, byteCount);

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
			}
			
			return;
		}
		
		public int read(long fileOffset, byte[] buffer,int byteOffset,int byteCount) throws Exception{
			if(mDataRandomAccessFile == null){
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
					mDataRandomAccessFile.seek(filepos);
					return mDataRandomAccessFile.read(buffer, byteOffset, byteCount);
				}
			}
			return -1;
		}
		
		public boolean close(){	
			try {
				if(mDataFileOutputStream != null){
					mDataFileOutputStream.close();
					mDataFileOutputStream = null;
				}
				
				if(mRecRandomAccessFile != null){
					mRecRandomAccessFile.close();
					mRecRandomAccessFile = null;
				}
				
				if(mDataRandomAccessFile != null){
					mDataRandomAccessFile.close();
					mDataRandomAccessFile = null;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return true;
		}
	}

	private class InterHttp extends  Thread{
		private boolean mReading = true;
		private InputStream         mInputStream      = null;
		private HttpURLConnection   mHttpConnection   = null;
		private String              mUrlPath          = null;
		private byte[]              mBuffer           = null;
		private static  final  int  BUFFERSIZE       = 4 * 1024 * 1024;
		private static  final  int  HTTPREADSIZE     = 64 * 1024;
		private ReentrantLock       mLock 			  = new ReentrantLock();
		private long                mStartPos         = 0;
		private int                 mCurrentSize      = 0;
		private long                mSeekHttpPos   	  = -1;
		private boolean             mIsHttpOpen       = false;
		private Object              mLockObject       = new Object();


		public InterHttp(){
			mBuffer = new byte[BUFFERSIZE];
		}

		@Override
		public void run() {
			int errorCnt = 0;
			int status  = 0;
			while (mReading && errorCnt < 5)
			{
				if(mSeekHttpPos != -1){
					Log.i(TAG,"InterHttp:: seek http pos = " + mSeekHttpPos);
					if(!closeHttpFile() || !openHttpFile(mUrlPath,mSeekHttpPos)){
						errorCnt++;
						Log.e(TAG,"InterHttp:: closeHttpFile or openHttpFile is fail");
						try { Thread.sleep(1000); } catch (Exception e) {}
						continue;
					}
					Log.i(TAG,"InterHttp:: seek over");
					synchronized (mLockObject){
						mStartPos = mSeekHttpPos;
						mCurrentSize = 0;
					}
					mSeekHttpPos = -1;
				}

				if(mInputStream == null){
					long pos = (mSeekHttpPos == -1) ? 0 : mSeekHttpPos;
					if(!openHttpFile(mUrlPath,pos)){
						errorCnt++;
						Log.e(TAG,"InterHttp:: openHttpFile is fail");
						try { Thread.sleep(1000); } catch (Exception e) {}
						continue;
					}
					synchronized (mLockObject){
						mStartPos = pos;
						mCurrentSize = 0;
					}
					mSeekHttpPos = -1;
				}

				if(mInputStream != null){
					synchronized (mLockObject){
						int leftSize =  BUFFERSIZE - mCurrentSize;
						int readByte = (HTTPREADSIZE < leftSize) ? HTTPREADSIZE : leftSize;
						status = 0;
						try {
							if(readByte > 0){
								Log.i(TAG,"InterHttp::  mCurrentSize = " + mCurrentSize + " readByte = " + readByte);
								readByte = mInputStream.read(mBuffer,mCurrentSize,readByte);
								if(readByte == 0){
									status = 1;
								}
								if(readByte == -1){
									Log.e(TAG,"InterHttp:: http readByte == -1");
									status = 2;
								}
							}else{
								status = 1;
							}
						} catch (IOException e) {
							Log.e(TAG,"InterHttp:: http read is fail");
							e.printStackTrace();
							status = 2;
							readByte = 0;
						}

						if(readByte > 0){
							mCurrentSize += readByte;
						}
					}

					if(status == 0) {
						errorCnt = 0;
						//try { Thread.sleep(0, 1000);} catch (Exception e) {}
					}else if(status == 1){
						try { Thread.sleep(100);} catch (Exception e) {}
					}else if(status == 2){
						//try { Thread.sleep(10 * 1000);} catch (Exception e) {}
						mSeekHttpPos = mStartPos;
						//errorCnt++;
					}

				}else{
					try { Thread.sleep(20); } catch (Exception e) {}
				}
			}
			mReading = false;
		}

		public void abort(boolean isAbort){
			mAbort = isAbort;
		}

		public boolean open(String urlPath){
			mUrlPath = urlPath;
			return openHttpFile(mUrlPath,0);
		}

		public boolean close(){
			mReading = false;
			mAbort   = true;
			try {
				this.join(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return closeHttpFile();
		}

		public int read(byte[] buf,long pos,int size){
			Log.i(TAG,"InterHttp read enter pos " + pos);

			while (true){
				int readByte = -1;
				synchronized (mLockObject){
					Log.i(TAG,"startPos = " + mStartPos + " currentSize = " + mCurrentSize + " pos = " + pos);
					if(mCurrentSize > 0){
						if(pos < mStartPos ){
							mSeekHttpPos = pos;
							Log.i(TAG,"InterHttp:: pos < mStartPos pos = " + pos + " mStartPos = " + mStartPos);
						}else{
							long offset    = pos - mStartPos;
							long validSize = mCurrentSize - offset;
							if(validSize <= 0){
								if(offset >= BUFFERSIZE){
									mSeekHttpPos = pos;
									Log.i(TAG,"InterHttp:: offset > BUFFERSIZE");
								}
							}else{
								readByte  = (size <= (int)validSize) ? size : (int)validSize;
								System.arraycopy(mBuffer,(int)offset,buf,0,readByte);

								if((pos + size) > (mStartPos + BUFFERSIZE) && mCurrentSize > (BUFFERSIZE / 2)){
									System.arraycopy(mBuffer,BUFFERSIZE / 2,mBuffer,0,BUFFERSIZE / 2);
									mCurrentSize -= (BUFFERSIZE / 2);
									mStartPos    += (BUFFERSIZE / 2);
								}
							}
						}
					}
				}

				//mLock.unlock();
				if(readByte > -1 || !mReading || mAbort){
					if(!mReading){
						Log.e(TAG,"Ahttp read return for mReading == false");
					}

					if(mAbort){
						Log.e(TAG,"Ahttp read return for abort");
					}
					return  readByte;
				}

				try { Thread.sleep(100); } catch (Exception e) {}
			}
		}

		public long getFileLength(){
			while (!mIsHttpOpen){
				if(mAbort || !mReading){
					return 0;
				}
				try { Thread.sleep(100); } catch (Exception e) {}
			}

			if(mHttpConnection != null){
				String length = mHttpConnection.getHeaderField("Content-Length");
				if(length != null){
					mFileSize  = Long.parseLong(length);
					Log.i(TAG,"InterHttp getFileLength FileSize = " + mFileSize);
					return mFileSize;
				}
			}
			return  0;
		}

		private boolean openHttpFile(String urlPath,long pos){
			URL url = null;
			try {
				url = new URL(urlPath);
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}


			try {
				mHttpConnection = (HttpURLConnection)url.openConnection();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}


			mHttpConnection.setDoInput(true); //允许输入流，即允许下载
			mHttpConnection.setRequestProperty("Accept", "*/*");

			pos += mOffset;
			long pos2 = mOffset + getFileSize();
			String range = "bytes=" + pos + "-" + pos2;

			mHttpConnection.setRequestProperty("Range", range);
			mHttpConnection.setRequestProperty("Accept-Encoding", "identity");
			mHttpConnection.setDefaultUseCaches(false);
			mHttpConnection.setUseCaches(false);
			//mHttpConnection.setRequestProperty("Connection", "close");

			try {
				mHttpConnection.setRequestMethod("GET");
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			try {
				mInputStream = mHttpConnection.getInputStream();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			if(mInputStream == null){
				return  false;
			}

			mIsHttpOpen = true;
			return true;
		}

		private boolean closeHttpFile(){
			mIsHttpOpen = false;
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

	}

	public AHttp(){
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

		if(mInterHttp != null){
			if(mInterHttp.open(mUrlPath)){
				mInterHttp.start();
				mInterHttpStart = true;
			}else{
				Log.e(TAG,"Ahttp open fail");
				return  -1;
			}
		}
		
		if(getFileSize() > 0){
			mFileBuf.setFileSize(getFileSize());
			return 1;
		}
		
		return -1;
	}
	
	public int close(String ret){
		Log.i(TAG,"ahttp close ret = " + ret);
		closeBufFile();
		if(mInterHttp != null){
			mInterHttp.close();
			mInterHttp = null;
		}
		
		if(!ret.equals(APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_CLOSE) && !ret.equals(APlayerAndroid.PlayCompleteRet.PLAYRE_RESULT_COMPLETE)){
			Log.e(TAG,"ahttp delete cache file ret = " + ret);
			this.deleteCache(mUrlPath);
		}
		return 1;
	}

	public int abort(boolean isAbort){
		if(mInterHttp != null){
			mInterHttp.abort(isAbort);
		}
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
		//mCacheFileDirManage = new CacheFileDirManage(cacheFileDir);
		return 1;
	}

	public int read(ByteBuffer bbfout){
		if(bbfout == null){
			return -1;
		}

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
				return -1;
			}
		}

		byte[] tempBuf = new byte[size];

		int readByte = read(desPos,size,tempBuf);
		if(readByte > 0){
			int desReadByte = readByte;
			if(mUseDES){
				desReadByte = desDecrypt(tempBuf, desPos, readByte);
			}


			int offset = (int)(mCurPos - desPos);
			desReadByte -= offset;
			if(desReadByte > 0){
				bbfout.put(tempBuf,offset, desReadByte);
				mCurPos += desReadByte;
			}
			return  desReadByte;
		}

		return  -1;
	}

	public int read(long desPos,int size,byte[] buf){
		int readByte = 0;
		if(mUseCache && mFileBuf != null){
			try {
				readByte  = mFileBuf.read(desPos,buf, 0, size);
			} catch (Exception e) {
				e.printStackTrace();
				mUseCache = false;
				return -1;
			}

			if(-1 != readByte){
				Log.i(TAG, "Ahttp file buf read readbyte = " + readByte);
				return readByte;
			}
		}

		if(!mInterHttpStart){
			Log.i(TAG,"Ahttp::read InterHttp start");
			mInterHttpStart = true;
			mInterHttp.open(mUrlPath);
			mInterHttp.start();
		}

		if(desPos >= getFileSize()){
			return  0;
		}

		readByte = mInterHttp.read(buf, desPos, size);
		if(readByte != -1){
			Log.i(TAG, "Ahttp mInterHttp read readByte = " + readByte + " pos = " + desPos);
			if(mUseCache){
				try {
					mFileBuf.write(desPos,buf, 0, readByte);
				} catch (Exception e) {
					e.printStackTrace();
					mUseCache = false;
				}
			}
			return readByte;
		}
		Log.e(TAG, "Ahttp mInterHttp read readbyte fail ");
		return -1;
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
		return  mCurPos = pos;
	//	return 1;
	}
	
	public boolean deleteCache(String urlPath){
		String filePathMd5 = stringToMD5(urlPath);
		if(filePathMd5 == null){
			return false;
		}
		
		if(mCacheFileDir == null){
			mCacheFileDir = Environment.getExternalStorageDirectory().toString() + "/ahttp/";
		}
		
		File fileDir = new File(mCacheFileDir);
		if(!fileDir.isDirectory() || !fileDir.exists()){
			return false;
		}
		
		filePathMd5 = mCacheFileDir + filePathMd5 + ".data";
		File file = new File(filePathMd5);
		if(file.exists()){
			file.delete();
		}
		
		return true;
	}
	
	private void init(){
		mUrlPath          = null;
		mCurPos           = 0;
		mFileSize         = 0;
		mFileBuf          = new CacheFile();
		mInterHttp        = new InterHttp();
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

		/*if(mFileSize == 0 && mHttpConnection != null){

			String length = mHttpConnection.getHeaderField("Content-Length");
			mFileSize  = Long.parseLong(length);
		}*/
		if(mFileSize == 0 && mInterHttp != null){
			mFileSize = mInterHttp.getFileLength();
		}
		
		return mFileSize;
	}
	
	/*private boolean seekHttpFile(long pos){
		if(!closeHttpFile()){
			return false;
		}
		
		return openHttpFile(pos);
	}*/
	
	/*private boolean closeHttpFile(){
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
	}*/
	
	/*private boolean openHttpFile(long pos){
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
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}


		mHttpConnection.setDoInput(true); //允许输入流，即允许下载
		
		String range = "bytes=" + pos + "-";
		mHttpConnection.setRequestProperty("Range", range);
		mHttpConnection .setRequestProperty("Accept-Encoding", "identity");
		
		try {
			mHttpConnection.setRequestMethod("GET");
		} catch (ProtocolException e) {
			e.printStackTrace();
			return false;
		}
		
		try {
			mInputStream = mHttpConnection.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}*/
	
	private boolean openBufFile(){
		
		String filePathMd5 = stringToMD5(mOrigUrl);
		if(filePathMd5 == null){
			return false;
		}
		
		if(mCacheFileDir == null){
			mCacheFileDir = Environment.getExternalStorageDirectory().toString() + "/ahttp/";
		}
		
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
		
		mCacheFileDirManage = new CacheFileDirManage(mCacheFileDir);
		
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
		//Log.i(TAG, "desDecrypt streampos = " + streamPos + " size = " + size);
		return desReadByte;
	}
}