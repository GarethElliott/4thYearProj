package com.quickpic.gareth.quickpic;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

public class UserPhotoAdapter extends ArrayAdapter<ToDoItem>
{
    Context mContext;
    int mLayoutResourceId;

    public UserPhotoAdapter(Context context, int layoutResourceId)
    {
        super(context, layoutResourceId);

        mContext = context;
        mLayoutResourceId = layoutResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;
        final ToDoItem currentItem = getItem(position);

        if (row == null)
        {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(mLayoutResourceId, parent, false);
        }

        row.setTag(currentItem);

        final TextView textT = (TextView) row.findViewById(R.id.TextToView);
        textT.setText(currentItem.getText());

        final TextView textD = (TextView) row.findViewById(R.id.DateToView);
        String date = currentItem.getDate();
        String shortDate = date.substring(0, 10);
        String time = date.substring(11, 16);
        textD.setText(time + " - " + shortDate);

        ImageLoader mImageLoader;
        mImageLoader = Single.getInstance(getContext()).getImageLoader();
        final NetworkImageView imageV = (NetworkImageView) row.findViewById(R.id.NetworkImageView);
        imageV.setImageUrl(currentItem.getImageUri(), mImageLoader);

        return row;
    }
}

class Single
{
    private static Single mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    private Single(Context context)
    {
        mCtx = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache()
        {
            private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

            @Override
            public Bitmap getBitmap(String url)
            {
                return cache.get(url);
            }

            @Override
            public void putBitmap(String url, Bitmap bitmap)
            {
                cache.put(url, bitmap);
            }
        });
    }

    public static synchronized Single getInstance(Context context)
    {
        if (mInstance == null)
        {
            mInstance = new Single(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue()
    {
        if (mRequestQueue == null)
        {
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public ImageLoader getImageLoader()
    {
        return mImageLoader;
    }
}