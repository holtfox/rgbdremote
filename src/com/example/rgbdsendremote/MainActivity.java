package com.example.rgbdsendremote;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	LinearLayout mainLayout;
	LinearLayout imageLayout;
	ArrayList<ImageView> imageViews = new ArrayList<ImageView>();
	ArrayList<Camera> cameras = new ArrayList<Camera>();
	
	private class AddCamListener implements OnClickListener {
		public void onClick(View v) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			
			builder.setTitle(R.string.add_dialog_title);
					
			final EditText address = new EditText(MainActivity.this);
			address.setHint("example.com:1337");
			address.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			builder.setView(address);
						
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		            public void onClick(DialogInterface dialog, int id) {
		        	   
		        	    
		        	    int port = 11222;
		        	    String input = address.getText().toString();
		        	    String addrprt[] = input.split(":");
		        	    if(addrprt.length >= 2)
		        	    	port = Integer.parseInt(addrprt[1]);
		        	    
		        	    final Camera c = new Camera(addrprt[0], port);
		        	    
		        	    final ProgressDialog progr = new ProgressDialog(MainActivity.this);
						
		        	    progr.setCancelable(false);
						progr.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						progr.setMessage("Connecting");
						progr.show();	
						
						c.connect(new Camera.OnPostExecuteListener() {
							@Override
							public void onPostExecute() {
								if(c.isValid()) {
									ImageView iv = new ImageView(MainActivity.this);
									iv.setImageResource(getResources().getIdentifier("ic_menu_refresh", "drawable", "android"));
					        	    
									imageLayout.addView(iv, 0);
									imageViews.add(iv);
									
									cameras.add(c);
								} else {
									Toast t = Toast.makeText(MainActivity.this, "Connection failed.", Toast.LENGTH_SHORT);
									t.show();
								}
								progr.dismiss();
							}
						});
						
		            }
		        });
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
					public void onClick(DialogInterface dialog, int id) {
						
					}
				});
						
			
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	};
	
	private class ThumbnailListener implements OnClickListener {
		private class ThumbnailReceivedListener implements Camera.OnPostExecuteListener {
			Camera cam;
			ImageView iv;
			
			@Override
			public void onPostExecute() {
				iv.setImageBitmap(BitmapFactory.decodeByteArray(cam.getThumbnail(), 0, cam.getThumbnail().length));
			}
			
			ThumbnailReceivedListener(Camera cam, ImageView iv) {
				this.cam = cam;
				this.iv = iv;				
				
			}
		}
		@Override
		public void onClick(View v) {
			for(int i = 0; i < cameras.size(); i++) {
				if(!cameras.get(i).isValid()) {
					imageViews.get(i).setImageResource(getResources().getIdentifier("ic_menu_report_image", "drawable", "android"));
					continue;
				}
				
				try {
					cameras.get(i).requestThumbnail(new ThumbnailReceivedListener(cameras.get(i), imageViews.get(i)));
				} catch (IOException e) {
					imageViews.get(i).setImageResource(getResources().getIdentifier("ic_menu_report_image", "drawable", "android"));
					e.printStackTrace();
					continue;
				}				
			}
		}
		
	};
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        
        imageLayout = new LinearLayout(this);
        imageLayout.setOrientation(LinearLayout.VERTICAL);
        imageLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        
        LinearLayout buttons = new LinearLayout(this);
                
        Button bthumb = new Button(this);
        bthumb.setText("Thumbnail");
        bthumb.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        bthumb.setOnClickListener(new ThumbnailListener());
        
        Button bcapt = new Button(this);
        bcapt.setText("Capture");
        bcapt.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        bcapt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final ProgressDialog progr = new ProgressDialog(MainActivity.this);
				final int[] working = new int[1];
				working[0] = cameras.size();
				progr.setCancelable(false);
				progr.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progr.setMessage("Capturing");
				progr.show();				
								
				for(int i = 0; i < cameras.size(); i++) {
					if(!cameras.get(i).isValid())
						imageViews.get(i).setImageResource(getResources().getIdentifier("ic_menu_report_image", "drawable", "android"));
					
					
					cameras.get(i).requestCapture(new Camera.OnPostExecuteListener() {
						public void onPostExecute() {
							working[0]--;
							if(working[0] == 0)
								progr.dismiss();
						}
					});
				}				
			}
        });
        
        buttons.setGravity(Gravity.BOTTOM);
        
        buttons.addView(bthumb);
        buttons.addView(bcapt);
        
        ImageButton addnew = new ImageButton(this);
        addnew.setImageResource(getResources().getIdentifier("ic_menu_add", "drawable", "android"));
        addnew.setOnClickListener(new AddCamListener());
        imageLayout.addView(addnew);
                
        mainLayout.addView(imageLayout);
        mainLayout.addView(buttons);
        setContentView(mainLayout);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void onDestroy() {
    	for(int i = 0; i < cameras.size(); i++) {
    		if(cameras.get(i) != null && cameras.get(i).keepalive != null)
    			cameras.get(i).keepalive.cancel();
    	}
    	
    	imageViews.clear();
    	cameras.clear();
    	super.onDestroy();
    }
    
}
