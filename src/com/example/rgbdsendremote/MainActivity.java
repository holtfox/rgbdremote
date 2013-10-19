package com.example.rgbdsendremote;

import java.io.IOException;
import java.util.ArrayList;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
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
		        	    ImageView iv = new ImageView(MainActivity.this);
		            	iv.setImageResource(getResources().getIdentifier("ic_menu_add", "drawable", "android"));
		        	    
		        	    imageLayout.addView(iv, 0);
		        	    imageViews.add(iv);
		        	    
		        	    int port = 11222;
		        	    String input = address.getText().toString();
		        	    String addrprt[] = input.split(":");
		        	    if(addrprt.length >= 2)
		        	    	port = Integer.parseInt(addrprt[1]);
		        	    
		        	    cameras.add(new Camera(addrprt[0], port));
		        	    try {
							cameras.get(cameras.size()-1).connect();
						} catch (IOException e) {
							e.printStackTrace();
						}
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
			for(int i = 0; i < cameras.size(); i++) {
				if(!cameras.get(i).isValid()) {
					imageViews.get(i).setImageResource(getResources().getIdentifier("ic_menu_report_image", "drawable", "android"));
					continue;
				}
				
				try {
					cameras.get(i).requestThumbnail();
				} catch (IOException e) {
					imageViews.get(i).setImageResource(getResources().getIdentifier("ic_menu_report_image", "drawable", "android"));
					e.printStackTrace();
					continue;
				}
				imageViews.get(i).setImageBitmap(BitmapFactory.decodeByteArray(cameras.get(i).getThumbnail(), 0, cameras.size()));
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
				for(int i = 0; i < cameras.size(); i++) {
					cameras.get(i).requestCapture();
				}
				
				for(int i = 0; i < cameras.size(); i++) {
					cameras.get(i).waitOk();
				}
			}
        });
        
        buttons.setGravity(Gravity.BOTTOM);
        
//        buttons.addView(bthumb);
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
    
}
