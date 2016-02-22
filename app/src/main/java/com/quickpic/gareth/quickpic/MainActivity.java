package com.quickpic.gareth.quickpic;

import java.util.concurrent.ExecutionException;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsSharedAccessSignature;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.*;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends Activity
{
    ImageView imageView;
    ImageView myPhotosBtn;
    ImageView chooseBtn;
    ImageView takeBtn;
    ImageView refreshBtn;
    ImageView saveBtn;
    Intent chooseIntent;
    static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;
    private MobileServiceClient mClient;
    private ProgressBar mProgressBar;
    private MobileServiceTable<ToDoItem> mToDoTable;
    private ToDoItemAdapter mAdapter;
    private EditText mTextNewToDo;
    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";
    String[] imageUris;

    public Uri mPhotoFileUri = null;
    public File mPhotoFile = null;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

        chooseBtn = (ImageView) findViewById(R.id.chooseBtn); //uploads a photo from storage
        takeBtn = (ImageView) findViewById(R.id.takeBtn); // opens the camera to take a picture
        refreshBtn = (ImageView) findViewById(R.id.refreshBtn); // refreshes the list of images
        myPhotosBtn = (ImageView) findViewById(R.id.myPhotosBtn); // retrieve image from internet using volley
        saveBtn = (ImageView) findViewById(R.id.saveBtn); // retrieve image from internet using volley
        imageView = (ImageView) findViewById(R.id.imageView);



        try
        {
            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://todosamplereal.azure-mobile.net/",
                    "XWUqHkNkBoZErttfAkxVeApajelIEB73", this)
                    .withFilter(new ProgressFilter());


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

                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);

                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null)
                {
                    // Create the File where the photo should go
                    try
                    {
                        mPhotoFile = createImageFile();
                    }
                    catch (IOException ex)
                    {
                    }
                    // Continue only if the File was successfully created
                    if (mPhotoFile != null)
                    {
                        mPhotoFileUri = Uri.fromFile(mPhotoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoFileUri);
                    }
                }
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                uploadPhoto(v);
            }
        });

        refreshBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                refreshItemsFromTable();
            }
        });

        myPhotosBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                myPhotosBtn = (ImageView) v;

                Intent intent = new Intent(getApplicationContext(), MyPhotos.class);
                //String[] images = getImageUris();
                //intent.putExtra("imagesUris", images);

                startActivity(intent);
            }
        });

        chooseBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chooseIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(chooseIntent, 2);
            }
        });
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
            // Other threads may be blocked waiting to be notified when
            // authentication is complete.
            synchronized (mAuthenticationLock)
            {
                bAuthenticating = false;
                mAuthenticationLock.notifyAll();
            }
            createTable();
        }
    }

    private void cacheUserToken(MobileServiceUser user)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.apply();
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);

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






    // Options for either taking a photo or uploading a photo
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            // If the user takes a photo this sets the background for it
            if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            {
                Bitmap bp = (Bitmap) data.getExtras().get("data");
                Bitmap preview = convertToMutable(bp);
                Drawable preview2 = new BitmapDrawable(getResources(), preview);
                imageView.setBackground(preview2);
            }

            // If the user chooses to upload a photo from their own storage
            else if (requestCode == 2)
            {
                Uri selectedImage = data.getData();
                String[] filePath = {MediaStore.Images.Media.DATA};
                Cursor c = getContentResolver().query(selectedImage, filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));

                imageView.setImageResource(0);
                Bitmap preview = convertToMutable(thumbnail);
                Drawable preview2 = new BitmapDrawable(getResources(), preview);
                imageView.setBackground(preview2);
            }
        }
    }

    private File createImageFile() throws IOException
    {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }

    // method for setting the background for the choose photo button
    public static Bitmap convertToMutable(Bitmap imgIn)
    {
        try
        {
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
            imgIn.copyPixelsToBuffer(map);
            imgIn.recycle();
            System.gc();
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            imgIn.copyPixelsFromBuffer(map);
            channel.close();
            randomAccessFile.close();

            file.delete();

        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return imgIn;
    }


    public void setImageUris(String[] imageUris)
    {
        this.imageUris = imageUris;
    }

    public String[] getImageUris()
    {
        return imageUris;
    }






    private void createTable()
    {
        // Get the table instance to use.
        mToDoTable = mClient.getTable(ToDoItem.class);

        mTextNewToDo = (EditText) findViewById(R.id.textNewToDo);

        // Create an adapter to bind the items with the view.
        mAdapter = new ToDoItemAdapter(this, R.layout.row_list_to_do);
        ListView listViewToDo = (ListView) findViewById(R.id.listViewToDo);
        listViewToDo.setAdapter(mAdapter);

        // Load the items from Azure.
        refreshItemsFromTable();
    }



    /*
    public void checkItemInTable(ToDoItem item) throws ExecutionException, InterruptedException
    {
        mToDoTable.update(item).get();
    }*/














    public void uploadPhoto(View view)
    {
        if (mClient == null)
        {
            return;
        }

        // Create a new item
        final ToDoItem item = new ToDoItem();

        item.setText(mTextNewToDo.getText().toString());
        item.setComplete(true);
        item.setContainerName("todoitemimages");

        // Use a unigue GUID to avoid collisions.
        UUID uuid = UUID.randomUUID();
        String uuidInString = uuid.toString();
        item.setResourceName(uuidInString);

        // Send the item to be inserted. When blob properties are set this
        // generates an SAS in the response.
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try {

                    final ToDoItem entity = addItemInTable(item);

                    // If we have a returned SAS, then upload the blob.
                    if (entity.getSasQueryString() != null)
                    {

                        // Get the URI generated that contains the SAS
                        // and extract the storage credentials.
                        StorageCredentials cred = new StorageCredentialsSharedAccessSignature(entity.getSasQueryString());
                        URI imageUri = new URI(entity.getImageUri());

                        // Upload the new image as a BLOB from a stream.
                        CloudBlockBlob blobFromSASCredential = new CloudBlockBlob(imageUri, cred);

                        blobFromSASCredential.uploadFromFile(mPhotoFileUri.getPath());
                    }

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if(!entity.isComplete())
                            {
                                mAdapter.add(entity);
                            }
                        }
                    });
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

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            mAdapter.clear();

                            for (ToDoItem item : results)
                            {
                                mAdapter.add(item);
                            }
                        }
                    });

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

    /*
    public void makeImageArray() throws ExecutionException, InterruptedException
    {
        final Object[] imageObject = refreshItemsFromMobileServiceTable().toArray();
        String[] tempImageUris = new String[imageObject.length];

        for (int i = 0; i < imageObject.length; i++)
        {
            ToDoItem item = (ToDoItem) imageObject[i];
            tempImageUris[i] = "" + item.getImageUri();
        }

        setImageUris(tempImageUris);
    }*/

    private List<ToDoItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");

        List<ToDoItem> userPhotos = mToDoTable.where().field("userId").eq(val(userId)).execute().get();
        return userPhotos;
    }



    //Error Messaging Methods
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
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


    private class ProgressFilter implements ServiceFilter
    {
        @Override
        public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

            final SettableFuture<ServiceFilterResponse> resultFuture = SettableFuture.create();

            runOnUiThread(new Runnable()
            {

                @Override
                public void run()
                {
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
                    runOnUiThread(new Runnable()
                    {

                        @Override
                        public void run()
                        {
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
