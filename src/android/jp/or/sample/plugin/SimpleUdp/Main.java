package jp.or.sample.plugin.SimpleUdp;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import java.util.HashMap;

public class Main extends CordovaPlugin {
	public static String TAG = "SimpleUdp.Main";
	private Activity activity;
	private static final int DEFAULT_RECEIVE_BUFFER_SIZE = 1024;
	private HashMap<Integer, DatagramSocket> sockets = new HashMap<>();

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView)
	{
		Log.d(TAG, "[Plugin] initialize called");
		super.initialize(cordova, webView);

		activity = cordova.getActivity();
	}

	@Override
	public void onResume(boolean multitasking)
	{
		Log.d(TAG, "[Plugin] onResume called");
		super.onResume(multitasking);
	}

	@Override
	public void onPause(boolean multitasking)
	{
		Log.d(TAG, "[Plugin] onPause called");
		super.onPause(multitasking);
	}

	@Override
	public void onNewIntent(Intent intent)
	{
		Log.d(TAG, "[Plugin] onNewIntent called");
		super.onNewIntent(intent);
	}

	@Override
	public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException
	{
		Log.d(TAG, "[Plugin] execute called");
		if( action.equals("receiving") ){
			try {
				final CallbackContext callback = callbackContext;
				final int localRecvPort = args.getInt(0);
				
				if( sockets.containsKey(localRecvPort) ){
					DatagramSocket t = sockets.get(localRecvPort);
					t.close();
					sockets.remove(localRecvPort);
				}
				final DatagramSocket udpReceive = new DatagramSocket(localRecvPort);
				sockets.put(localRecvPort, udpReceive);
				
				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
							while(true){
								byte[] buff = new byte[DEFAULT_RECEIVE_BUFFER_SIZE];
								DatagramPacket packet = new DatagramPacket(buff, buff.length);
								udpReceive.setSoTimeout(0);
								udpReceive.receive(packet);
								Log.d(TAG, "received");
							
								JSONObject message = new JSONObject();
								int len = packet.getLength();
								String payload = new String(buff, 0, len);
								message.put("payload", payload);
								byte[] address = packet.getAddress().getAddress();
								message.put("ipaddress", (((long)(address[0] & 0x00ff)) << 24) | (((long)(address[1] & 0x00ff)) << 16) | (((long)(address[2] & 0x00ff)) << 8) | (((long)(address[3] & 0x00ff)) << 0) );
								message.put("port", packet.getPort());
								
								final PluginResult result = new PluginResult(PluginResult.Status.OK, message);
								result.setKeepCallback(true);
								callback.sendPluginResult(result);
							}
						} catch (Exception ex) {
							callbackContext.error(ex.getMessage());
							if( udpReceive != null )
								udpReceive.close();
						}
					}
				});
			
			
			}catch(Exception ex){
				Log.d(TAG, ex.getMessage());
				callbackContext.error("Invalid arg0(int)");
				return false;
			}


		}else
		if( action.equals("send") ){
			try{
				String message = args.getString(0);
				String host = args.getString(1);
				int port = args.getInt(2);

				cordova.getThreadPool().execute(new Runnable() {
					@Override
					public void run() {
						try {
							DatagramSocket udpSend = new DatagramSocket();
							byte[] buff = message.getBytes();
							InetAddress inetAddress = InetAddress.getByName(host);
							DatagramPacket packet = new DatagramPacket(buff, buff.length, inetAddress, port);
							udpSend.send(packet);
							Log.d(TAG, "sended");
							udpSend.close();
							
							callbackContext.success();
						} catch (Exception ex) {
							callbackContext.error(ex.getMessage());
						}
					}
				});
				callbackContext.success();
			}catch(Exception ex){
				Log.d(TAG, ex.getMessage());
				callbackContext.error("Invalid arg0(String), arg1(String), arg2(int)");
				return false;
			}
		}else {
			String message = "Unknown action : (" + action + ") " + args.getString(0);
			Log.d(TAG, message);
			callbackContext.error(message);
			return false;
		}

		return true;
	}
}

