package com.theappineer.truckdemo.utils;

import android.os.AsyncTask;

import com.theappineer.truckdemo.ui.MyProgressDialog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRequestTask {

	public static String HttpPostRequest(String uri, String json) {
		HttpURLConnection urlConnection;
		String result = null;
		try {
			//Connect
			urlConnection = (HttpURLConnection) ((new URL(uri).openConnection()));
			urlConnection.setDoInput(true);
			urlConnection.connect();

			//Read
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

			String line = null;
			StringBuilder sb = new StringBuilder();

			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line);
			}

			bufferedReader.close();
			result = sb.toString();

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static class HttpAsyncTask extends AsyncTask<String, String, String> {

//		private MyProgressDialog myProgressDialog;
		private HttpResponseListener mListener;

		public HttpAsyncTask(MyProgressDialog dialog, HttpResponseListener listener) {
//			myProgressDialog = dialog;
			mListener = listener;
		}
		@Override
		protected void onPreExecute() {
//			if(myProgressDialog != null)
//				myProgressDialog.show();
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			String url = params[0];
			String parameters = params[1];
			String responceAllMenuRes = HttpRequestTask.HttpPostRequest(url, parameters);
			return responceAllMenuRes;
		}

		@Override
		protected void onPostExecute(String s) {
			if(mListener != null)
				mListener.onResponse(s);
//			if(myProgressDialog != null)
//				myProgressDialog.dismiss();
			super.onPostExecute(s);
		}
	}

	public interface HttpResponseListener {
		public void onResponse(String response);
	}

}
