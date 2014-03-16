package es.getbox.android.getboxapp.box;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang.ObjectUtils.Null;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.box.boxandroidlibv2.BoxAndroidClient;
import com.box.boxandroidlibv2.activities.OAuthActivity;
import com.box.boxandroidlibv2.dao.BoxAndroidOAuthData;
import com.box.boxjavalibv2.authorization.OAuthRefreshListener;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.interfaces.IAuthData;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFileUploadRequestObject;
import com.box.boxjavalibv2.requests.requestobjects.BoxFolderRequestObject;

import es.getbox.android.getboxapp.interfaces.AsyncTaskCompleteListener;
import es.getbox.android.getboxapp.utils.Item;
import es.getbox.android.getboxapp.utils.SQL;

public class BoxStorageProvider { 

	final static private String TAG = "BoxSP";
	private Context context;
	private int boxAccount;
	private BoxAndroidClient mClient;
	public static final String CLIENT_ID = "nq0so01we5nic9rqysh6zmn5fd1g15ma";
    public static final String CLIENT_SECRET = "g5O8FfBPaeTYJR35aVhVfZ9VURBn2rZ6";
    public static final String REDIRECT_URL = "http://localhost/";
    private SQL sql;
    private ArrayList<Item> currentDirectory;
    private ArrayList<String> directories;
    private long space;
	
    public BoxStorageProvider(Context context, int newBoxAccount){
    	this.context=context;
    	this.boxAccount=newBoxAccount;
    	this.sql=new SQL(context);
    	this.currentDirectory=new ArrayList<Item>();
    	this.directories=new ArrayList<String>();
    	this.directories.add("0");
    }
        
    public BoxAndroidClient getClient(){
    	return this.mClient;
    }
    
    public void onAuthenticated(int resultCode, Intent data) {
		
    	if (Activity.RESULT_OK != resultCode) {
   		Toast.makeText(context, "fail", Toast.LENGTH_LONG).show();
       }
       else {
    	   BoxAndroidOAuthData oauth = data.getParcelableExtra(OAuthActivity.BOX_CLIENT_OAUTH);
           BoxAndroidClient client = new BoxAndroidClient(this.CLIENT_ID, this.CLIENT_SECRET, null, null);
           client.authenticate(oauth);
           if (client == null) {
               Toast.makeText(context, "fail", Toast.LENGTH_LONG).show();
           }
           else {
           	this.mClient=client;
           	String accesstoken=oauth.getAccessToken();
           	Log.i(TAG,accesstoken);
           	
           	Log.i(TAG,"done");
           	mClient.addOAuthRefreshListener(new OAuthRefreshListener() {

                @Override
                public void onRefresh(IAuthData newAuthData) {
                	try {
						BoxOAuthToken oauthObject=mClient.getAuthData();
						String accesstoken=newAuthData.getAccessToken();
						sql.openDatabase();
			           	sql.updateBoxToken(boxAccount, accesstoken);
			            sql.closeDatabase();
			            oauthObject.setAccessToken(accesstoken);
			            mClient.authenticate(oauthObject);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}                	
                }

            });
           	this.sql.openDatabase();
           	sql.insertBox(boxAccount, accesstoken,getUser(),getSpace());
            Toast.makeText(context, "authenticated", Toast.LENGTH_LONG).show();
       		this.sql.closeDatabase();
           }
       }

   }
    
    public void autenticate(){ 
		this.sql.openDatabase();
    	BoxOAuthToken oauthObject=new BoxOAuthToken();
    	oauthObject.setAccessToken(sql.getBoxTokens(boxAccount));
    	BoxAndroidClient client = new BoxAndroidClient(this.CLIENT_ID, this.CLIENT_SECRET, null, null);
		client.authenticate(oauthObject);
		if (client == null) {
			Toast.makeText(context, "fail", Toast.LENGTH_LONG).show();
		}
		else {
        	this.mClient=client;
        	mClient.addOAuthRefreshListener(new OAuthRefreshListener() {

             @Override
             public void onRefresh(IAuthData newAuthData) {
            	 try{
            		 BoxOAuthToken oauthObject=mClient.getAuthData();
            		 String accesstoken=newAuthData.getAccessToken();
            		 sql.openDatabase();
			         sql.updateBoxToken(boxAccount, accesstoken);
			         sql.closeDatabase();
			         oauthObject.setAccessToken(accesstoken);
			         mClient.authenticate(oauthObject);
            	 }catch(Exception e){}
             }
 
         });
        }
		this.sql.closeDatabase();
    }
    
    public String getUserName(){
		this.sql.openDatabase();
    	String account_aux= sql.getBoxUserName(boxAccount);
		this.sql.closeDatabase();
		return account_aux;
	}
    
    public String getUser(){
    	BoxGetUser dgu=new BoxGetUser(mClient);
		try {
			dgu.execute();
			String a=dgu.get();
			return a;
		} catch (InterruptedException e) {
			return "";
		} catch (ExecutionException e) {
			return "";
		}
    }
    
    public long getSpaceUsed(){
		this.sql.openDatabase();
    	long space= sql.getBoxSpace(boxAccount);
		this.sql.closeDatabase();
		return space;
	}
    
    public void setSpaceUsed(long space){
		this.sql.openDatabase();
    	sql.updateBoxSpace(boxAccount,space);
		this.sql.closeDatabase();
	}
    
    public long getSpace(){
    	BoxGetSpace dgs=new BoxGetSpace(mClient);
		try {
			dgs.execute();
			long a=dgs.get();
			return a;
		} catch (InterruptedException e) {
			return 0;
		} catch (ExecutionException e) {
			return 0;
		}
    }
    
    public void getFiles(String directory_path,AsyncTaskCompleteListener<ArrayList<Item>> cb,boolean dialog){
    	BoxListDirectory task = new BoxListDirectory(directory_path,cb, this.getClient(),boxAccount);
        task.execute();
    }
    
    public void downloadFile(String file_name, String file_id) {
        final String fPath= file_id;
        final String fName=  file_name;
        AsyncTask<Null, Integer, Null> task = new AsyncTask<Null, Integer, Null>() {

            @Override
            protected void onPostExecute(Null result) {
                Toast.makeText(context, "done downloading", Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            }

            @Override
            protected void onPreExecute() {
                Toast.makeText(context, "start downloading", Toast.LENGTH_LONG).show();
                super.onPreExecute();
            }

            @Override
            protected Null doInBackground(Null... params) {
                BoxAndroidClient client = mClient;
                try {
                    File f = new File(Environment.getExternalStorageDirectory().getPath()+"/GetBox/", fName);
                    System.out.println(f.getAbsolutePath());
                    client.getFilesManager().downloadFile(fPath, f, null, null);
                }
                catch (Exception e) {
                }
                return null;
            }
        };
        task.execute();
    }

    public void uploadFile(String file_name, String file_id) {
        final String fPath=file_id;
        final String fName=file_name;
        AsyncTask<Null, Integer, Null> task = new AsyncTask<Null, Integer, Null>() {

            @Override
            protected void onPostExecute(Null result) {
                Toast.makeText(context, "done uploading", Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            }

            @Override
            protected void onPreExecute() {
                Toast.makeText(context, "start uploading", Toast.LENGTH_LONG).show();
                super.onPreExecute();
            }

            @Override
            protected Null doInBackground(Null... params) {
                BoxAndroidClient client = mClient;
                try {
                    File file = new File(fName);                    
                    client.getFilesManager().uploadFile(
                        BoxFileUploadRequestObject.uploadFileRequestObject(fPath, file.getName(), file, client.getJSONParser()));
                }
                catch (Exception e) {
                	Log.i("BoxSP",e.getMessage()+", "+fName);
                }
                return null;
            }
        };
        task.execute();
    }    
    
    
    public void deleteFile(String file_name, String file_id) {
        final String fPath=file_id;
        final String fName=file_name;
        AsyncTask<Null, Integer, Null> task = new AsyncTask<Null, Integer, Null>() {

            @Override
            protected void onPostExecute(Null result) {
                Toast.makeText(context, "done deleting", Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            }

            @Override
            protected void onPreExecute() {
                Toast.makeText(context, "start deleting", Toast.LENGTH_LONG).show();
                super.onPreExecute();
            }

            @Override
            protected Null doInBackground(Null... params) {
                BoxAndroidClient client = mClient;
                try {
                	BoxFileRequestObject requestObj =
                		    BoxFileRequestObject.deleteFileRequestObject();
                		client.getFilesManager().deleteFile(fPath, requestObj);
                }
                catch (Exception e) {
                }
                return null;
            }
        };
        task.execute();
    }    
    
    public void deleteFolder(String file_name, String file_id) {
        final String fPath=file_id;
        final String fName=file_name;
        AsyncTask<Null, Integer, Null> task = new AsyncTask<Null, Integer, Null>() {

            @Override
            protected void onPostExecute(Null result) {
                Toast.makeText(context, "done deleting", Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            }

            @Override
            protected void onPreExecute() {
                Toast.makeText(context, "start deleting", Toast.LENGTH_LONG).show();
                super.onPreExecute();
            }

            @Override
            protected Null doInBackground(Null... params) {
                BoxAndroidClient client = mClient;
                try {
                	BoxFolderRequestObject requestObj =
                		    BoxFolderRequestObject.deleteFolderRequestObject(true);
                		client.getFoldersManager().deleteFolder(fPath, requestObj);
                }
                catch (Exception e) {
                }
                return null;
            }
        };
        task.execute();
    }   
    
    public void uploadFolder(String file_name, String file_id) {
        final String fPath=file_id;
        final String fName=file_name;
        AsyncTask<Null, Integer, Null> task = new AsyncTask<Null, Integer, Null>() {

            @Override
            protected void onPostExecute(Null result) {
                Toast.makeText(context, "done uploading", Toast.LENGTH_LONG).show();
                super.onPostExecute(result);
            }

            @Override
            protected void onPreExecute() {
                Toast.makeText(context, "start uploading", Toast.LENGTH_LONG).show();
                super.onPreExecute();
            }

            @Override
            protected Null doInBackground(Null... params) {
                BoxAndroidClient client = mClient;
                try {
                    client.getFoldersManager().createFolder(
                        BoxFolderRequestObject.createFolderRequestObject(fName,fPath));
                }
                catch (Exception e) {
                }
                return null;
            }
        };
        task.execute();
    }   
    

    public void restartRoutes(){
    	directories.clear();
    	directories.add("0");
    	currentDirectory.clear();
    }
    
    public ArrayList<String> getDirectories() {
		return directories;
	}
    
    public String getDirectory(int position) {
		return directories.get(position);
	}

	public void addDirectory(String folder) {
		this.directories.add(folder);
	}
	
	public void removeDirectory(int position){
		this.directories.remove(position);
	}

	public void saveDirectory(ArrayList<Item> currentDirectory){
    	this.currentDirectory=currentDirectory;
    }
    
    public ArrayList<Item> getCurrentDirectory(){
    	return this.currentDirectory;
    }
}