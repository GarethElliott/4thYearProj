package com.quickpic.gareth.quickpic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.UserAuthenticationCallback;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import java.net.MalformedURLException;
import android.widget.ArrayAdapter;

public class UserAdapter extends ArrayAdapter<User>
{
    Context mContext;
    int mLayoutResourceId;
    private MobileServiceClient mClient;
    public boolean bAuthenticating = false;
    public final Object mAuthenticationLock = new Object();

    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";

    public UserAdapter(Context context, int layoutResourceId)
    {
        super(context, layoutResourceId);

        mContext = context;
        mLayoutResourceId = layoutResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;

        try
        {
            mClient = new MobileServiceClient("https://todosamplereal.azure-mobile.net/", "XWUqHkNkBoZErttfAkxVeApajelIEB73", getContext());
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

        final User currentUser = getItem(position);

        if (row == null)
        {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
        }

        row.setTag(currentUser);

        final TextView userName = (TextView) row.findViewById(R.id.UserName);
        userName.setText(currentUser.getUsername());

        final TextView userDate = (TextView) row.findViewById(R.id.UserDate);

        String shortDate = currentUser.getDate();
        shortDate = shortDate.substring(0, 10);
        userDate.setText(shortDate);

        final ImageView conBtn = (ImageView) row.findViewById(R.id.connectBtn);

        conBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SharedPreferences prefs = getContext().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
                String userId = prefs.getString(USERIDPREF, "undefined");

                UserConnection uc = new UserConnection();
                uc.setUserId(userId);
                uc.setConnectionId(currentUser.getUserId());
                uc.setConnectionName(currentUser.getUsername());
                System.out.println("" + mClient.getCurrentUser().getUserId());
                mClient.getTable(UserConnection.class).insert(uc, new TableOperationCallback<UserConnection>()
                {
                    public void onCompleted(UserConnection entity, Exception exception, ServiceFilterResponse response)
                    {
                        if (exception == null)
                        {
                            System.out.println("Success");
                        }
                        else
                        {
                            System.out.println("The exception:  " + exception);

                        }
                    }
                });
            }
        });

        return row;
    }

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
        SharedPreferences prefs = getContext().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, user.getAuthenticationToken());
        editor.apply();
    }

    private boolean loadUserTokenCache(MobileServiceClient client)
    {
        SharedPreferences prefs = getContext().getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);

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