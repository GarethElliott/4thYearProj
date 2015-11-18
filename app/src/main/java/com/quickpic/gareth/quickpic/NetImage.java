package com.quickpic.gareth.quickpic;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.LruCache;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;


public class NetImage extends ActionBarActivity
{
    private static final String IMAGE_URL = "http://i.imgur.com/MhLtBBL.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_image);

        ImageLoader mImageLoader;
        NetworkImageView mNetworkImageView;


        // Get the NetworkImageView that will display the image.
        mNetworkImageView = (NetworkImageView) findViewById(R.id.networkImageView);

        // Get the ImageLoader through your singleton class.
        mImageLoader = MySingleton.getInstance(this).getImageLoader();

        // Set the URL of the image that should be loaded into this view, and
        // specify the ImageLoader that will be used to make the request.
        mNetworkImageView.setImageUrl(IMAGE_URL, mImageLoader);
    }
}

class MySingleton
{
    private static MySingleton mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    private MySingleton(Context context)
    {
        mCtx = context;
        mRequestQueue = getRequestQueue();

        mImageLoader = new ImageLoader(mRequestQueue,
                new ImageLoader.ImageCache()
                {
                    private final LruCache<String, Bitmap>
                            cache = new LruCache<String, Bitmap>(20);

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

    public static synchronized MySingleton getInstance(Context context)
    {
        if (mInstance == null)
        {
            mInstance = new MySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue()
    {
        if (mRequestQueue == null)
        {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req)
    {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader()
    {
        return mImageLoader;
    }
}
