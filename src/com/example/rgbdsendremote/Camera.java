package com.example.rgbdsendremote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

import android.os.AsyncTask;

public class Camera {
	String address;
	int port;
	Socket sock;
	
	Timer keepalive;
	
	final int timeout = 3;
	
	long lastcommand = System.currentTimeMillis();
	
	byte[] thumbbuf = new byte[32768];
	
	boolean invalid = true;
	
	boolean busy = false;
	
	public Camera(String address, int port) {
		this.address = address;
		this.port = port;
		
		sock = new Socket();
	}
	
	public void connect() throws IOException {
		new ConnectTask().execute();
	}
	
	private void waitBusy() {
		while(busy) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e2) {
				e2.printStackTrace();
			}
		}		
	}
	
	private class ConnectTask extends AsyncTask<Void, Void, Long> {

		@Override
		protected Long doInBackground(Void... arg0) {
			waitBusy();
			busy = true;
			
			try {
				sock.connect(new InetSocketAddress(address, port), timeout*1000);
			} catch(IOException e) {
				invalid = true;
				return (long) 0;
			}
			
			sendCommand("subs");
			byte[] cmd = new byte[4];
			try {
				recvCommand(cmd, null, 0);
			} catch (IOException e1) {
				invalid = true;
				e1.printStackTrace();
				return (long) 0;				
			}
			
			if(new String(cmd).equals("okay")) {
				keepalive = new Timer();
				keepalive.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						Camera.this.sendCommand("aliv");
						if(!Camera.this.isValid())
							this.cancel();
					}			
				}, 0, timeout*1000/2);
				invalid = false;			
			} else {
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return (long) 0;				
			}
			
			return (long) 1;
		}
		
		@Override
		protected void onPostExecute(Long res) {
			busy = false;
		}
	}
	
	public void disconnect() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				waitBusy();
				busy = true;
				keepalive.cancel();
				sendCommand("quit");	
				invalid = true;
				try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				busy = false;
			}
		}.execute();
			
	}
	
	public boolean isValid() {
		return !invalid;
	}
	
	private void sendCommand(String cmd) {
		try {
			sock.getOutputStream().write(cmd.getBytes(), 0, 4);
		} catch (IOException e) {
			invalid = true;
			e.printStackTrace();
		}		
	}
	
	private void recvAll(byte[] buffer, int len) throws IOException {
		int read = 0;
		while(read < len) {
			int n = sock.getInputStream().read(buffer, read, len-read);
			if(n == -1 || System.currentTimeMillis() - lastcommand > timeout*1000)
				throw new IOException("Failed to receive everything.");
			
			read += n;
			
			if(n > 0)
				lastcommand = System.currentTimeMillis();
		}
	}
	
	private int recvCommand(byte[] cmd, byte[] buffer, int max) throws IOException {
		try {
			recvAll(cmd, 4);
		} catch (IOException e) {
			invalid = true;
			e.printStackTrace();
			return 0;
		}
		
		lastcommand = System.currentTimeMillis();
		
		if(new String(cmd).equals("stmb")) {
			byte lenbuf[] = new byte[4];
			try {
				recvAll(lenbuf, 4);
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
			ByteBuffer b = ByteBuffer.wrap(lenbuf);
			b.order(ByteOrder.BIG_ENDIAN);
			int len = b.getInt();
			
			if(len > max)
				return -1;
			
			try {
				recvAll(buffer, len);
			} catch (IOException e) {
				e.printStackTrace();
				throw e;
			}
			
			return len;
		}
		
		return 0;
	}
	
	private class RequestThumbnailTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			waitBusy();
			busy = true;
			
			sendCommand("thmb");
			byte[] cmd = new byte[4];
			int n = -1;
			
			do {		
				try {
					n = recvCommand(cmd, thumbbuf, thumbbuf.length);
				} catch (IOException e) {
					invalid = false;
					e.printStackTrace();
				}
				System.out.println(new String(cmd));
			} while(isValid() && n != -1 && new String(cmd).equals("aliv"));
			
			if(!new String(cmd).equals("stmb"))
				return null;
			if(n == -1)
				System.err.println("Thumbnail buffer too small!");
			
			return null;
		}
		
		@Override
		protected void onPostExecute(Void res) {
			busy = false;
		}
	}
	
	public void requestThumbnail() throws IOException {
		new RequestThumbnailTask().execute();
	}
	
	public void requestCapture() {
		new AsyncTask<Void, Void, Void>() {
		@Override
		protected Void doInBackground(Void... params) {
			byte cmd[] = new byte[4];
			
			waitBusy();
			busy = true;
			
			sendCommand("capt");
			try {
				recvCommand(cmd, null, 0);
			} catch (IOException e) {
				invalid = true;
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void res) {
			busy = false;
		}
		}.execute();
	}
	
	public void waitOk(){
		new AsyncTask<Void, Void, Long>() {
			@Override
			protected Long doInBackground(Void... params) {
				waitBusy();
				busy = true;
				byte[] cmd = new byte[4];

				try {
					recvCommand(cmd, null, 0);
				} catch (IOException e) {
					invalid = true;
					return (long) 0;
				}

				if(new String(cmd).equals("okay"))
					return (long) 1;

				return (long) 0;
			}
			
			@Override
			protected void onPostExecute(Long res) {
				busy = false;
			}
		}.execute();
		
	}
	
	public byte[] getThumbnail() {
		return thumbbuf;
	}
	
}
