package com.quickpic.gareth.quickpic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;

import java.io.File;
import java.io.FileInputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;

public class FragmentOne extends Fragment
{
    MainActivity parent;
    View rootView;
    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();
    private ProgressBar mProgressBar;
    private MobileServiceTable<ToDoItem> mToDoTable;
    private EditText mTextNewToDo;
    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";
    public String path;

    private MobileServiceClient mClient;

    public String username;

    ImageView mImageView;
    ImageView takeBtn;
    ImageView saveBtn;
    public Uri picUri;
    public boolean photoTaken;

    public String storageConnectionString =
            "DefaultEndpointsProtocol=http;" +
                    "AccountName=todosample;" +
                    "AccountKey=zsfmWrTGkYPmnW+sBn6b/YXHwkJQxkpXwpthZ/iIJ/L/QL8UivKKWLStAZS8gB6PrjoSfYPJYHYfnRKEIKxHdA==";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        parent = (MainActivity) getActivity();
        rootView = inflater.inflate(R.layout.fragment_one, container, false);
        mImageView = ((ImageView) rootView.findViewById(R.id.imageView));
        mProgressBar = (ProgressBar) rootView.findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);
        takeBtn = (ImageView) rootView.findViewById(R.id.takeBtn); // opens the camera to take a picture
        saveBtn = (ImageView) rootView.findViewById(R.id.saveBtn);
        mTextNewToDo = (EditText) rootView.findViewById(R.id.textNewToDo);
        photoTaken = false;

        try
        {
            // Mobile Service URL and key
            mClient = new MobileServiceClient("https://todosamplereal.azure-mobile.net/", "XWUqHkNkBoZErttfAkxVeApajelIEB73", getContext()).withFilter(new ProgressFilter());
            authenticate(false);
        }

        catch (MalformedURLException e)
        {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        }
        catch (Exception e)
        {
            createAndShowDialog(e, "Error");
        }

        takeBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                takeBtn = (ImageView) v;

                Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                File file = getOutputMediaFile(1);
                picUri = Uri.fromFile(file); // create
                i.putExtra(MediaStore.EXTRA_OUTPUT, picUri); // set the image file
                path = file.getPath();
                startActivityForResult(i, 1);
                photoTaken = true;
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(photoTaken == true)
                {
                    uploadPhoto(v);
                }
                else
                {
                    Toast.makeText(getContext(), "Photo Not Taken", Toast.LENGTH_SHORT);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {

            Intent i;
            switch (requestCode)
            {
                case 1:
                    //THIS IS YOUR Uri
                    Uri uri = picUri;
                    break;
            }
        }
    }

    private  File getOutputMediaFile(int type)
    {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QuickPic");

        /**Create the storage directory if it does not exist*/
        if (! mediaStorageDir.exists())
        {
            if (! mediaStorageDir.mkdirs())
            {
                return null;
            }
        }

        /**Create a media file name*/
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == 1)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".png");
        }
        else
        {
            return null;
        }

        return mediaFile;
    }


    private void createTable()
    {
        mToDoTable = mClient.getTable(ToDoItem.class);

        refreshItemsFromTable();
    }

    public void uploadPhoto(View view)
    {
        if (mClient == null)
        {
            return;
        }

        final ToDoItem item = new ToDoItem();
        SharedPreferences prefs = parent.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");

        item.setText(mTextNewToDo.getText().toString());
        item.setComplete(false);
        item.setContainerName("todoitemimages");
        item.setUserId(userId);
        UUID uuid = UUID.randomUUID();
        final String uuidInString = uuid.toString();
        item.setResourceName(uuidInString);
        item.setImageUri("http://todosample.blob.core.windows.net:80/todoitemimages/" + uuidInString);

        mClient.getTable(ToDoItem.class).insert(item, new TableOperationCallback<ToDoItem>()
        {
            public void onCompleted(ToDoItem entity, Exception exception, ServiceFilterResponse response)
            {
                if (exception == null)
                {
                    System.out.println("Success");
                }
                else
                {
                    System.out.println(exception);
                }
            }
        });

        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                Looper.prepare();
                try
                {
                    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

                    StrictMode.setThreadPolicy(policy);

                    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);
                    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
                    CloudBlobContainer container = blobClient.getContainerReference("todoitemimages");
                    File source = new File(path);
                    CloudBlockBlob blob = container.getBlockBlobReference(uuidInString);
                    blob.upload(new FileInputStream(source), source.length());
                }
                catch (final Exception e)
                {
                    createAndShowDialogFromTask(e, "Error");
                }
                return null;
            }
        };

        runAsyncTask(task);

        mTextNewToDo.setText("");
    }









    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        else
        {
            return task.execute();
        }
    }


    public ToDoItem addItemInTable(ToDoItem item) throws ExecutionException, InterruptedException
    {
        ToDoItem entity = mToDoTable.insert(item).get();
        return entity;
    }

    private void refreshItemsFromTable()
    {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try
                {
                    final List<ToDoItem> results = refreshItemsFromMobileServiceTable();
                }
                catch (final Exception e)
                {
                    createAndShowDialogFromTask(e, "Error 7");
                }

                return null;
            }
        };

        runAsyncTask(task);
    }

    private List<ToDoItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException
    {
        SharedPreferences prefs = parent.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");

        List<ToDoItem> userPhotos = mToDoTable.where().field("userId").eq(val(userId)).execute().get();

        return userPhotos;
    }


    //User Login with Facebook
    private void authenticate(boolean bRefreshCache)
    {
        bAuthenticating = true;

        if (bRefreshCache || !loadUserTokenCache(mClient))
        {
            // New login using the provider and update the token cache.
            mClient.login(MobileServiceAuthenticationProvider.Facebook, new UserAuthenticationCallback()
            {
                @Override
                public void onCompleted(MobileServiceUser user, Exception exception, ServiceFilterResponse response)
                {
                    synchronized (mAuthenticationLock)
                    {
                        if (exception == null)
                        {
                            cacheUserToken(mClient.getCurrentUser());
                            createTable();
                        }
                        else
                        {
                            createAndShowDialog(exception.getMessage(), "Login Error 4");
                        }
                        bAuthenticating = false;
                        mAuthenticationLock.notifyAll();
                    }
                }
            });
        }
        else
        {
            synchronized (mAuthenticationLock)
            {
                bAuthenticating = false;
                mAuthenticationLock.notifyAll();
            }
            createTable();
        }
    }

    //Error Messaging Methods
    private void cacheUserToken(MobileServiceUser user)
    {
        SharedPreferences prefs = parent.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.apply();
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = parent.getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);

        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId.equals("undefined"))
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token.equals("undefined"))
            return false;

        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        client.setCurrentUser(user);

        return true;
    }

    private void createAndShowDialogFromTask(final Exception exception, String title)
    {
        createAndShowDialog(exception, "Error");
    }

    private void createAndShowDialog(Exception exception, String title)
    {
        Throwable ex = exception;
        if (exception.getCause() != null)
        {
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    private void createAndShowDialog(final String message, final String title)
    {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }


    private class ProgressFilter implements ServiceFilter
    {
        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();

            parent.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.VISIBLE);
                }
            });

            ListenableFuture<ServiceFilterResponse> future = nextServiceFilterCallback.onNext(request);

            Futures.addCallback(future, new FutureCallback<ServiceFilterResponse>()
            {
                @Override
                public void onFailure(Throwable e)
                {
                    resultFuture.setException(e);
                }

                @Override
                public void onSuccess(ServiceFilterResponse response)
                {
                    parent.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (mProgressBar != null) mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
    }
}
