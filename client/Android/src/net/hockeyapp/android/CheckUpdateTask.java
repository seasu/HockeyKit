package net.hockeyapp.android;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;

public class CheckUpdateTask extends AsyncTask<String, String, JSONArray>{
  private Context context = null;
  private String urlString = null;
  private String appIdentifier = null;
  
  public CheckUpdateTask(Context context, String urlString) {
    this.appIdentifier = null;
    this.context = context;
    this.urlString = urlString;
  }
  
  public CheckUpdateTask(Context context, String urlString, String appIdentifier) {
    this.appIdentifier = appIdentifier;
    this.context = context;
    this.urlString = urlString;
  }
  
  public void attach(Context context) {
    this.context = context;
  }
  
  public void detach() {
    context = null;
  }
  
  @Override
  protected JSONArray doInBackground(String... args) {
    try {
      int versionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA).versionCode;
      
      URL url = new URL(getURLString("json"));
      URLConnection connection = url.openConnection();
      connection.addRequestProperty("User-Agent", "Hockey/Android");
      connection.connect();

      InputStream inputStream = new BufferedInputStream(connection.getInputStream());
      String jsonString = convertStreamToString(inputStream);
      inputStream.close();
      
      JSONArray json = new JSONArray(jsonString);
      for (int index = 0; index < json.length(); index++) {
        JSONObject entry = json.getJSONObject(index);
        if (entry.getInt("version") > versionCode) {
          return json;
        }
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    return null;
  }

  @Override
  protected void onPostExecute(JSONArray updateInfo) {
    if (updateInfo != null) {
      showDialog(updateInfo);
    }
  }
  
  private String getURLString(String format) {
    return urlString + "api/2/apps/" + (this.appIdentifier != null ? this.appIdentifier : context.getPackageName()) + "?format=" + format;      
  }
  
  private void showDialog(final JSONArray updateInfo) {
    if (context == null) {
      return;
    }
    
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(R.string.update_dialog_title);
    builder.setMessage(R.string.update_dialog_message);

    builder.setNegativeButton(R.string.update_dialog_negative_button, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
      } 
    });
    
    builder.setPositiveButton(R.string.update_dialog_positive_button, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int which) {
        Intent intent = new Intent(context, UpdateActivity.class);
        intent.putExtra("json", updateInfo.toString());
        intent.putExtra("url", getURLString("apk"));
        context.startActivity(intent);
      } 
    });
    
    builder.create().show();
  }

  private static String convertStreamToString(InputStream inputStream) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream), 1024);
    StringBuilder stringBuilder = new StringBuilder();

    String line = null;
    try {
      while ((line = reader.readLine()) != null) {
        stringBuilder.append(line + "\n");
      }
    } 
    catch (IOException e) {
      e.printStackTrace();
    } 
    finally {
      try {
        inputStream.close();
      } 
      catch (IOException e) {
        e.printStackTrace();
      }
    }
    return stringBuilder.toString();
  }
}