package com.example.driveexample;

import java.io.IOException;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.Insert;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	protected static final String ACCOUNT_TOKEN = "ACCOUNT_TOKEN";
	protected static final String ACCOUNT_NAME = "ACCOUNT_NAME";
	public static final int RESULT_PICK_ACCOUNT=0;
	private static final int REQUEST_AUTHORIZATION = 1;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
		
		refreshButtonState();
	}

	private void refreshButtonState() {
		boolean hasAccount=getAccountToken()!=null;
		Button loginButton=(Button)findViewById(R.id.button_login);
		loginButton.setEnabled(!hasAccount);
		
		Button uploadButton=(Button)findViewById(R.id.button_upload);
		uploadButton.setEnabled(hasAccount);
		
		Button logoutButton=(Button)findViewById(R.id.button_logout);
		logoutButton.setEnabled(hasAccount);
	}

	private String getAccountToken() {
		SharedPreferences prefMan = PreferenceManager.getDefaultSharedPreferences(this);
		return prefMan.getString(ACCOUNT_TOKEN, null);
	}

	public void logmein(View view)
	{
		Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
		         true, null, null, null, null);
		startActivityForResult(intent, RESULT_PICK_ACCOUNT);
	}
	
	public void uploadFile(View view) throws IOException
	{
		String token=getAccountToken();
		if(token!=null)
		{
			new Thread(new Runnable(){

				@Override
				public void run() {
					try
					{
						GoogleAccountCredential credential=null;
						String accountName=getAccountName();
						credential= GoogleAccountCredential.usingOAuth2(MainActivity.this, DriveScopes.DRIVE);
						credential.setSelectedAccountName(accountName);
						Drive service=new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(),credential).setApplicationName("DriveExample").build();
						File newFile=new File();
						newFile.setTitle("Titolo");
						newFile.setMimeType("text/json");
						
						Files files = service.files();
						Insert insert;
						
						insert = files.insert(newFile, ByteArrayContent.fromString("text/plain", "{\"version\":1}"));
						File insertedFile=insert.execute();
					}
					catch (Exception e) {
						if(e instanceof UserRecoverableAuthIOException)
						{
							UserRecoverableAuthIOException recEx=(UserRecoverableAuthIOException) e;
							MainActivity.this.startActivityForResult(recEx.getIntent(), REQUEST_AUTHORIZATION);
						}
						else e.printStackTrace();
					}
					
				}}).start();
			
		}
	}
	
	private String getAccountName() {
		SharedPreferences prefMan = PreferenceManager.getDefaultSharedPreferences(this);
		return prefMan.getString(ACCOUNT_NAME, null);
	}

	public void logmeout(View view)
	{
		SharedPreferences prefMan = PreferenceManager.getDefaultSharedPreferences(this);
		GoogleAuthUtil.invalidateToken(this, prefMan.getString(ACCOUNT_TOKEN, ""));
		
		
        prefMan.edit().remove(ACCOUNT_TOKEN).commit();
        
        
        refreshButtonState();
	}

	 protected void onActivityResult(final int requestCode, final int resultCode,
	         final Intent data) {
	     if (requestCode == RESULT_PICK_ACCOUNT && resultCode == RESULT_OK) {
	         final String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
	         SharedPreferences prefMan = PreferenceManager.getDefaultSharedPreferences(this);
		     prefMan.edit().putString(ACCOUNT_NAME, accountName).commit();
		     
	         new Thread(new Runnable(){

				@Override
				public void run() {
					storeToken(MainActivity.this,accountName);
				}
	        	 
	         }).start();
	   }
	     else if(requestCode==REQUEST_AUTHORIZATION && resultCode==RESULT_OK)
	     {
	    	 Toast.makeText(this, "AUTHORIZATION", Toast.LENGTH_SHORT).show();
	    	 String token=data.getStringExtra(AccountManager.KEY_AUTHTOKEN);
	    	 SharedPreferences prefMan = PreferenceManager.getDefaultSharedPreferences(this);
		     prefMan.edit().putString(ACCOUNT_TOKEN, token).commit();
	     }
	 }
	 
	 protected void storeToken(Context context,String accountName)
	 {
		 try {
				String token=GoogleAuthUtil.getToken(this, accountName, "oauth2:"+DriveScopes.DRIVE_FILE);
				SharedPreferences prefMan = PreferenceManager.getDefaultSharedPreferences(this);
				prefMan.edit().putString(ACCOUNT_TOKEN,token).commit();
				
				Activity act=(Activity) context;
				act.runOnUiThread(new Runnable(){

					@Override
					public void run() {
						refreshButtonState();
					}});
				
			} catch (UserRecoverableAuthException e) {
				((Activity) context).startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (GoogleAuthException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	         
	        
	 }
}
