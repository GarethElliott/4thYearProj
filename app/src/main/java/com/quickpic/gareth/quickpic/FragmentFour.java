package com.quickpic.gareth.quickpic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.val;

public class FragmentFour extends Fragment
{
    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();
    public MobileServiceClient mClient;
    public ProgressBar mProgressBar;
    public MobileServiceTable<UserConnection> ucToDoTable;
    public UserConAdapter ucAdapter;
    public MainActivity parent;
    View myView;
    ListView listViewUserCon;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        myView = inflater.inflate(R.layout.fragment_four, container, false);
        mProgressBar = (ProgressBar) myView.findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);

        ucAdapter = new UserConAdapter(getContext(), R.layout.row_list_userconnections);
        listViewUserCon = (ListView) myView.findViewById(R.id.listView);
        listViewUserCon.setAdapter(ucAdapter);
        parent = (MainActivity) getActivity();

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

        createTable();

        return myView;
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
        SharedPreferences prefs = getActivity().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.apply();
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = getActivity().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);

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
        ucToDoTable = mClient.getTable(UserConnection.class);

        refreshUserConFromTable();
    }


    public void refreshUserConFromTable()
    {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try
                {
                    final List<UserConnection> results = refreshUserConFromMobileServiceTable();

                    parent.runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ucAdapter.clear();

                            for (UserConnection userCon : results)
                            {
                                ucAdapter.add(userCon);
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

    private List<UserConnection> refreshUserConFromMobileServiceTable() throws ExecutionException, InterruptedException
    {
        String userId = mClient.getCurrentUser().getUserId();

        ucToDoTable = mClient.getTable(UserConnection.class);
        List<UserConnection> userCons = ucToDoTable.where().field("userid").eq(val(userId)).execute().get();

        return userCons;
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

            parent.runOnUiThread(new Runnable()
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
                    parent.runOnUiThread(new Runnable()
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }
}