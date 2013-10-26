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
	static final int TIMEOUT = 3*1000; // ms
	static final int CAPTURE_TIMEOUT = 10*1000; // ms
	
	String address;
	int port;
	Socket sock;
	
	Timer keepalive;
			
	byte[] thumbbuf = new byte[65536];
	
	boolean invalid = true;
	
	boolean busy = false;
	
	public Camera(String address, int port) {
		this.address = address;
		this.port = port;
		
		sock = new Socket();
	}
	
	public void connect(final OnPostExecuteListener listener) {
		new AsyncTask<Void, Void, Long>() {

			@Override
			protected Long doInBackground(Void... arg0) {
				waitBusy();
				busy = true;
				
				try {
					sock.connect(new InetSocketAddress(address, port), TIMEOUT);
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
					System.err.println("Error: Rejected by Server.");
					return (long) 0;				
				}
				
				if(new String(cmd).equals("okay")) {
					keepalive = new Timer();
					keepalive.scheduleAtFixedRate(new TimerTask() {
						@Override
						public void run() {
							if(!busy) {
								Camera.this.sendCommand("aliv");
								if(!Camera.this.isValid())
									keepalive.cancel();
							}
						}			
					}, 0, TIMEOUT/2);
					
					invalid = false;
				} else {
					invalid = true;
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
				listener.onPostExecute();
				busy = false;
			}
		}.execute();
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
	
	
	public void disconnect() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				waitBusy();
				busy = true;
				if(keepalive != null) {
					keepalive.cancel();
					keepalive = null;
				}
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
//			disconnect();
			System.err.printf("Error: Sending Command '%s' failed.\n", cmd);
		}
		
	}
	
	private void recvAll(byte[] buffer, int len, int timeout) throws IOException {
		int read = 0;
		long lastrecv = System.currentTimeMillis();
		while(read < len) {
			int n = 0;
			
			if(sock.getInputStream().available() > 0) {
				n = sock.getInputStream().read(buffer, read, len-read);
			} else {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(System.currentTimeMillis() - lastrecv > timeout)
				throw new IOException("Failed to receive everything.");
			
			if(n > 0) {
				read += n;
				lastrecv = System.currentTimeMillis();
			}
		}
	}
	
	private int recvCommand_t(byte[] cmd, byte[] buffer, int max, int timeout) throws IOException {
		try {
			recvAll(cmd, 4, timeout);
		} catch (IOException e) {
			disconnect();			
			System.err.println("Error: Receiving Command failed.");
			return 0;
		}
		
		if(new String(cmd).equals("stmb")) { // stmb is the only data receiving command so far
			byte lenbuf[] = new byte[4];
			try {
				recvAll(lenbuf, 4, timeout);
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
				recvAll(buffer, len, timeout);
			} catch (IOException e) {
				disconnect();
				System.err.println("Error: Receiving Command Data failed.");
				return 0;
			}
			
			buffer[0] = 1;
			return len;
		}
		
		return 0;
	}
	
	private int recvCommand(byte[] cmd, byte[] buffer, int max) throws IOException {
		return recvCommand_t(cmd, buffer, max, TIMEOUT);
	}
		
	public void requestThumbnail(final OnPostExecuteListener listener) throws IOException {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				waitBusy();
				busy = true;
				
				sendCommand("thmb");
				byte[] cmd = new byte[4];
				int n = -1;
				
				try {
					Thread.sleep(2*1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				
				try {
					n = recvCommand_t(cmd, thumbbuf, thumbbuf.length, CAPTURE_TIMEOUT);
				} catch (IOException e) {
					disconnect();
					System.err.println("Error: Receiving Thumbnail failed.");					
				}
				
				if(!new String(cmd).equals("stmb"))
					return null;
				if(n == -1)
					System.err.println("Error: Thumbnail buffer too small!");
				
				return null;
			}
			
			@Override
			protected void onPostExecute(Void res) {
				busy = false;
				listener.onPostExecute();
			}
		}.execute();
	}
	
	public interface OnPostExecuteListener {
		public void onPostExecute();
	}
	
	public void requestCapture(final OnPostExecuteListener listener) {
		new AsyncTask<Void, Void, Void>() {
		@Override
		protected Void doInBackground(Void... params) {
			byte cmd[] = new byte[4];
			
			waitBusy();
			busy = true;
			
			sendCommand("capt");
			try {
				recvCommand_t(cmd, null, 0, CAPTURE_TIMEOUT);
			} catch (IOException e) {
				invalid = true;
				e.printStackTrace();
			}
			return null;
		}
		@Override
		protected void onPostExecute(Void res) {
			busy = false;
			listener.onPostExecute();
		}
		}.execute();
	}
	
	
	public byte[] getThumbnail() {
		return thumbbuf;
	}
	
}
