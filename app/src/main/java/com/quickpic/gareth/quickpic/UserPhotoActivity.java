package com.quickpic.gareth.quickpic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;

public class UserPhotoActivity extends AppCompatActivity
{
    public ListView listViewToView;
    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";
    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();
    public MobileServiceClient mClient;
    public ProgressBar mProgressBar;
    public MobileServiceTable<ToDoItem> mToDoTable;
    public MobileServiceTable<User> uToDoTable;
    public UserPhotoAdapter upAdapter;
    public TextView user;
    public TextView date;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_photo);

        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);
        upAdapter = new UserPhotoAdapter(this, R.layout.row_list_user_photo);
        user = (TextView) findViewById(R.id.nameTextView);
        date = (TextView) findViewById(R.id.dateTextView);
        listViewToView = (ListView) findViewById(R.id.listView);
        listViewToView.setAdapter(upAdapter);

        try
        {
            // Mobile Service URL and key
            mClient = new MobileServiceClient("https://todosamplereal.azure-mobile.net/", "XWUqHkNkBoZErttfAkxVeApajelIEB73", this).withFilter(new ProgressFilter());
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

        createTable();
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

        }
    }

    private void cacheUserToken(MobileServiceUser user)
    {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
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

    private void createTable()
    {
        mToDoTable = mClient.getTable(ToDoItem.class);

        refreshItemsFromTable();
    }


    public void refreshItemsFromTable()
    {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                Looper.prepare();
                try
                {
                    final List<ToDoItem> results = refreshItemsFromMobileServiceTable();

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            upAdapter.clear();

                            for (ToDoItem item : results)
                            {
                                upAdapter.add(item);
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

    private List<ToDoItem> refreshItemsFromMobileServiceTable() throws ExecutionException, InterruptedException
    {
        Bundle bundle = getIntent().getExtras();
        String connectionId = bundle.getString("connectionId");

        List<ToDoItem> userPhotos = mToDoTable.where().field("userId").eq(val(connectionId)).execute().get();

        uToDoTable = mClient.getTable(User.class);
        List<User> users = uToDoTable.where().field("id").eq(val(connectionId)).execute().get();

        if(users.get(0).getUsername() == null)
        {
            user.setText("" + connectionId);
            String shortDate = users.get(0).getDate();
            shortDate = shortDate.substring(0, 10);
            date.setText(shortDate);
        }
        else
        {
            user.setText("" + users.get(0).getUsername());
            String shortDate = users.get(0).getDate();
            shortDate = shortDate.substring(0, 10);
            date.setText(shortDate);
        }

        Collections.sort(userPhotos, new Comparator<ToDoItem>()
        {
            @Override
            public int compare(ToDoItem lhs, ToDoItem rhs)
            {
                return String.valueOf(rhs.getDate()).compareTo(lhs.getDate());
            }
        });

        return userPhotos;
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
                            if (mProgressBar != null)
                                mProgressBar.setVisibility(ProgressBar.GONE);
                        }
                    });

                    resultFuture.set(response);
                }
            });

            return resultFuture;
        }
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
}
