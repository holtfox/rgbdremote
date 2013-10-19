package com.example.rgbdsendremote;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Timer;
import java.util.TimerTask;

public class Camera {
	String address;
	int port;
	Socket sock;
	
	Timer keepalive;
	
	final int timeout = 3;
	
	long lastcommand = System.currentTimeMillis();
	
	byte[] thumbbuf = new byte[32768];
	
	boolean invalid = true;
	
	public Camera(String address, int port) {
		this.address = address;
		this.port = port;
		
		sock = new Socket();
	}
	
	public void connect() throws IOException {
		try {
			sock.connect(new InetSocketAddress(address, port), timeout*1000);
		} catch(IOException e) {
			invalid = true;
			e.printStackTrace();
			throw e;
		}
		
		sendCommand("subs");
		byte[] cmd = new byte[4];
		recvCommand(cmd, null, 0);
		
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
			throw new IOException("Rejected by Server.");
		}		
	}
	
	public void disconnect() {
		keepalive.cancel();
		sendCommand("quit");	
		invalid = true;
		try {
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isValid() {
		return !invalid;
	}
	
	public void sendCommand(String cmd) {
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
	
	public int recvCommand(byte[] cmd, byte[] buffer, int max) throws IOException {
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
	
	public void requestThumbnail() throws IOException {
		sendCommand("thmb");
		byte[] cmd = new byte[4];
		int n;
		
		do {		
			n = recvCommand(cmd, thumbbuf, thumbbuf.length);
			System.out.println(new String(cmd));
		} while(isValid() && n != -1 && new String(cmd).equals("aliv"));
		
		if(!new String(cmd).equals("stmb"))
			return;
		if(n == -1)
			throw new IOException("Thumbnail buffer too small!");
	}
	
	public void requestCapture() {
		sendCommand("capt");
	}
	
	public boolean waitOk(){
		byte[] cmd = new byte[4];
		
		try {
			recvCommand(cmd, null, 0);
		} catch (IOException e) {
			return false;
		}
				
		if(new String(cmd).equals("okay"))
			return true;
		
		return false;
	}
	
	public byte[] getThumbnail() {
		return thumbbuf;
	}
	
}
