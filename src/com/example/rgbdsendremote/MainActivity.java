package com.example.rgbdsendremote;

import java.util.ArrayList;
import java.util.Locale;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class MainActivity extends Activity {
	LinearLayout mainLayout;
	LinearLayout cameraLayout;
	ArrayList<CameraView> cameraViews = new ArrayList<CameraView>();
	
	public void addCamera(String input) {
		int port = 11222;
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
				final CameraView cv = new CameraView(MainActivity.this, c);
				cv.setLongClickable(true);									
				cv.setOnLongClickListener(new OnLongClickListener() {
					public boolean onLongClick(final View arg0) {
						AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
						builder.setMessage("Delete this Sensor?");
						builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				            public void onClick(DialogInterface dialog, int id) {
				            	cameraViews.remove(cv);
								((ViewManager)cv.getParent()).removeView(cv);
				            }
				        });
												
						AlertDialog dialog = builder.create();
						dialog.show();
						return false;
					}
		    	});
				
				cameraLayout.addView(cv, 0);
				cameraViews.add(cv);
				progr.dismiss();
			}
		});
	}
	
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
		            	addCamera(address.getText().toString());   	    
						
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
		@Override
		public void onClick(View v) {
			for(int i = 0; i < cameraViews.size(); i++) {
				CameraView cv = cameraViews.get(i);
				
				cv.requestThumbnail();			
			}
		}
		
	};
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        
        cameraLayout = new LinearLayout(this);
        cameraLayout.setOrientation(LinearLayout.VERTICAL);
        cameraLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        
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
				working[0] = cameraViews.size();
				progr.setCancelable(false);
								
				progr.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progr.setMessage("Capturing");
				progr.show();				
				
				System.out.println(cameraViews.size());
				for(int i = 0; i < cameraViews.size(); i++) {
					cameraViews.get(i).requestCapture(new Camera.OnPostExecuteListener() {
						public void onPostExecute() {
							working[0]--;
							if(working[0] <= 0)
								progr.dismiss();
						}
					});
				}
				if(working[0] == 0)
					progr.dismiss();
			}
        });
        
        buttons.setGravity(Gravity.BOTTOM);
        
        buttons.addView(bthumb);
        buttons.addView(bcapt);
        
        ImageButton addnew = new ImageButton(this);
        addnew.setImageResource(getResources().getIdentifier("ic_menu_add", "drawable", "android"));
        addnew.setOnClickListener(new AddCamListener());
        cameraLayout.addView(addnew);

        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));
        sv.addView(cameraLayout);
        mainLayout.addView(sv);
        mainLayout.addView(buttons);
        setContentView(mainLayout);
        
        int i = 0;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String key = String.format(Locale.ENGLISH, "camera%02d", i);
        while(prefs.contains(key)) {
        	addCamera(prefs.getString(key, "192.168.1.235:11222"));
        	i++;
        	key = String.format(Locale.ENGLISH, "camera%02d", i);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    public void onDestroy() {
    	SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
    	prefs.clear();
    	for(int i = 0; i < cameraViews.size(); i++) {
    		if(cameraViews.get(i) != null) {
    			Camera c = cameraViews.get(i).camera; 
    			prefs.putString(String.format(Locale.ENGLISH, "camera%02d", i), String.format(Locale.ENGLISH, "%s:%d", c.address, c.port)); 
    			System.out.println(String.format(Locale.ENGLISH, "%s:%d", c.address, c.port));
    		}
    			
    	}
    	prefs.commit();
    	cameraViews.clear();
    	super.onDestroy();
    }
    
}
