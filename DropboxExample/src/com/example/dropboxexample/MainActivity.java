package com.example.dropboxexample;

import java.io.IOException;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxFileSystem.PathListener;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxPath.InvalidPathException;

public class MainActivity extends Activity implements PathListener {
	protected static final String PATH_VERSION = "version.json";

	protected DbxAccountManager accMan;
	
	protected static final int REQUEST_LINK=0;

	private static final String APP_SECRET = "myAppSecret";
	private static final String APP_KEY = "myAppKey";

	protected boolean updatedByMobileClient=false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		 accMan = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		new Thread(new Runnable(){

			@Override
			public void run() {
				if(accMan.hasLinkedAccount())
				{
					try
					{
						DbxFileSystem dbxFs = DbxFileSystem.forAccount(accMan.getLinkedAccount());
						dbxFs.syncNowAndWait();
					}
					catch(Exception e)
					{
						
					}
				}
				
				final Activity activity=MainActivity.this;
				
				activity.runOnUiThread(new Runnable(){

					@Override
					public void run() {

						updateVersionLabel();
						updateButtonStates();
						setupListener();
					}});
			}}).start();
		
	}

	private void setupListener() {
		try
		{
			if(accMan.hasLinkedAccount())
			{
				DbxFileSystem dbxFs = DbxFileSystem.forAccount(accMan.getLinkedAccount());
				dbxFs.addPathListener(this, new DbxPath(PATH_VERSION),DbxFileSystem.PathListener.Mode.PATH_OR_CHILD);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private void updateButtonStates() {
		boolean linked=accMan.hasLinkedAccount();
		
		Button loginButton=(Button)findViewById(R.id.button_login);
		loginButton.setEnabled(!linked);
		
		Button logoutButton=(Button)findViewById(R.id.button_logout);
		logoutButton.setEnabled(linked);
		
		Button uploadButton=(Button)findViewById(R.id.button_upload);
		uploadButton.setEnabled(linked);
	}

	public void login(View view)
	{
		accMan.startLink(this, REQUEST_LINK);
	}
	
	public void logout(View view)
	{
		accMan.unlink();
		updateButtonStates();
	}
	
	public void upload(View view) 
	{
		int version=0;
		try
		{
			version=1+getCurrentVersion();
			
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(accMan.getLinkedAccount());
			
			DbxPath path = new DbxPath(PATH_VERSION);
			DbxFile testFile=null;
			
			updatedByMobileClient=true;
			if(dbxFs.exists(path))
			{
				testFile=dbxFs.open(path);
			}
			else
			{
				testFile = dbxFs.create(path);
			}
			
		    try {
		        testFile.writeString("{\"version\":"+version+"}");
		    } catch (IOException e) {
		    	Toast.makeText(this, "An error occurred while uploading the file", Toast.LENGTH_LONG).show();
			} finally {
				if(testFile!=null)
					testFile.close();
		    }
		}
		catch(Unauthorized exc)
		{
			Toast.makeText(this, "You\'re not authorized to perform this action", Toast.LENGTH_LONG).show();
		} catch (InvalidPathException e1) {
			e1.printStackTrace();
		} catch (DbxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		updateVersionLabel(version);
	}
	
	protected int getCurrentVersion()
	{
		if(accMan.hasLinkedAccount())
		{
			try
			{
				DbxFileSystem dbxFs = DbxFileSystem.forAccount(accMan.getLinkedAccount());
				DbxFile testFile = dbxFs.open(new DbxPath(PATH_VERSION));
			    try {
			        String jsonString=testFile.readString();
			        JSONObject json=new JSONObject(jsonString);
			        
			        return json.getInt("version");
			    } catch (Exception e) {
			    	Toast.makeText(this, "An error occurred while uploading the file", Toast.LENGTH_LONG).show();
				}  finally {
			        testFile.close();
			    }
			}
			catch(Unauthorized exc)
			{
				Toast.makeText(this, "You\'re not authorized to read the version file", Toast.LENGTH_LONG).show();
			} catch (InvalidPathException e1) {
				e1.printStackTrace();
			} catch (DbxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			return 1;
		}
		else return 1;
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LINK) {
            if (resultCode == Activity.RESULT_OK) {
            	setupListener();
            	updateButtonStates();
            } else {
               Toast.makeText(this, "Sorry, i couldn\'t link to Dropbox",Toast.LENGTH_LONG).show();
            }            
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

	@Override
	public void onPathChange(DbxFileSystem arg0, DbxPath arg1, Mode arg2) {
		try
		{
			if(updatedByMobileClient)
			{
				updatedByMobileClient=false;
				return;
			}
			
			if(arg1.getName().equals(PATH_VERSION))
			{
				Log.i("VERSION","synching");
				arg0.syncNowAndWait();
				
				MainActivity.this.runOnUiThread(new Runnable(){

					@Override
					public void run() {
						updateVersionLabel();
					}});
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	protected void updateVersionLabel() {
		int version=getCurrentVersion();
		updateVersionLabel(version);
	}
	
	protected void updateVersionLabel(int version) {
		TextView label=(TextView)findViewById(R.id.label_version);
		label.setText(getString(R.string.label_version)+" "+version);
	}
}
