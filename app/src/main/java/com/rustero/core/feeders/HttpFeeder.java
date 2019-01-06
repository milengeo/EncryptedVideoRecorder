package com.rustero.core.feeders;


import android.util.Log;

import com.rustero.Errors;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import javax.net.ssl.HttpsURLConnection;


public class HttpFeeder extends Feeder {

	private URL mUrl;
	private HttpURLConnection mWebConnection;
	private HttpsURLConnection mSslConnection;
	private ReadableByteChannel mChannel;
	private InputStream mStream;
	private String mRange;
	private int mHttpCode;




	protected void dlog(String aLine) { Log.d("HttpFeeder", aLine); }



	// * doers


	public boolean isSlow() {
		return true;
	}


	public boolean isSsl() {

		if (mPath.contains("https://"))
			return true;
		else
			return false;
	}



	private HttpURLConnection connection() {
		if (null != mSslConnection)
			return mSslConnection;
		else
			return mWebConnection;
	}



	public void detach() {
		try {
			if (null != mStream) {
				mStream.close();
				mStream = null;
			}
			if (null != mSslConnection) {
				mSslConnection.disconnect();
				mSslConnection = null;
			}
			if (null != mWebConnection) {
				mWebConnection.disconnect();
				mWebConnection = null;
			}
		} catch (Exception ex) {}
	}





	private boolean connect(long aFrom, long aUpto) {
		try {
			mUrl = new URL(mPath);
		} catch (Exception ex) {}

		mRange = "bytes=" + aFrom + "-";
		if (aUpto > 0)
			mRange += aUpto;

		if (isSsl())
			connectSsl(aFrom, aUpto);
		else
			connectWeb(aFrom, aUpto);

		if (mHttpCode == HttpURLConnection.HTTP_PARTIAL) {
			long length = -1;
			String hdr = connection().getHeaderField("Content-Range");
			long from = -1;
			long upto = -1;
			try {
				String[] s_length = hdr.split("/");
				length = Long.parseLong(s_length[1]);
				String[] s_bytes = s_length[0].split(" ");
				String[] s_range = s_bytes[1].split("-");
				from = Long.parseLong(s_range[0]);
				upto = Long.parseLong(s_range[1]);
			} catch (Exception ex) {}

			if (from != aFrom)
				mOutcome = Errors.PARTIAL_REQUESTS;
			if (aUpto > 0  &&  upto != aUpto)
				mOutcome = Errors.PARTIAL_REQUESTS;

		} else if (mHttpCode == HttpURLConnection.HTTP_OK) {
			mOutcome = Errors.NOT_DIRECT_URL;

		} else if (mHttpCode >= 300) {
			mOutcome = Errors.CONNECT_URL;
		}

//			Map<String, List<String>> hdrs = connection().getHeaderFields();
//			Set<String> hdrKeys = hdrs.keySet();
//			for (String k : hdrKeys)
//				App.log("  Key: " + k + "  Value: " + hdrs.get(k));

		if (mOutcome < 0) {
			mRequest = 0;
			return false;
		} else {
			return true;
		}
	}



	private void connectSsl(long aFrom, long aUpto) {
		try {
			mSslConnection = (HttpsURLConnection) mUrl.openConnection();
			mSslConnection.setRequestProperty("Range", mRange);
			mHttpCode = mSslConnection.getResponseCode();
		} catch (Exception ex) {
			mOutcome = Errors.PARTIAL_REQUESTS;
		}
	}



	private void connectWeb(long aFrom, long aUpto) {
		try {
			mWebConnection = (HttpURLConnection) mUrl.openConnection();
			mWebConnection.setUseCaches(false);
			mWebConnection.setRequestProperty("Range", mRange);
			mHttpCode = mWebConnection.getResponseCode();
		} catch (Exception ex) {
			mOutcome = Errors.PARTIAL_REQUESTS;
		}
	}






	public void perform() {
		try {
			try {
				long upto = 0;
				if (mAsked > 0)
					upto = mLocus+mAsked-1;
				if (!connect(mLocus, upto)) return;
				mStream = connection().getInputStream();
				mChannel = Channels.newChannel(mStream);
			} catch (Exception ex) {
			}

			while (!mQuit2) {
				if (mCancel) {
					break;
				}

				if (mOutcome != 0) {
					Thread.sleep(1);  // outcome is not used
					continue;
				}

				if (completed()) {
					break;
				}

				mBuffer.clear();
				if (mAsked > 0) {
					long left = mAsked - mSofar;
					if (left < mRoom)
						mBuffer.limit((int) left);
				}


//simu
//				if (mLocus > 1000000  &&  mLocus < 2000000) {
//					while (!mQuit2) {
//						Thread.sleep(1);  // outcome is not used
//					}
//				}


				int count = mChannel.read(mBuffer);
				if (count < 0) {
					if (0 == mTotal)
						mTotal = mLocus;
					break;  // end of file
				} else if (count > 0) {
					mSofar += count;
					mLocus += count;
				}
				mOutcome = count;
			}

			connection().disconnect();
			mSslConnection = null;
			mWebConnection = null;
		} catch (Exception ex) {
			dlog("ex: " + ex.getMessage());
			mOutcome = Errors.CONNECT_URL;
		}

		mRequest = 0;
	}

}
