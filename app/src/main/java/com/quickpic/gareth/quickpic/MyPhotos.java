package com.quickpic.gareth.quickpic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.LruCache;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;


public class MyPhotos extends Activity
{
    Button prevImageBtn;
    Button nextImageBtn;
    int imageNum = 0;
    String deleteImageUri;
    private ProgressBar mProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_net_image);

        //Intent intent = getIntent();


        //final String[] images = intent.getStringArrayExtra("imageUris");

        nextImageBtn = (Button) findViewById(R.id.nextImageBtn);
        prevImageBtn = (Button) findViewById(R.id.prevImageBtn);
        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);
        mProgressBar.setVisibility(ProgressBar.GONE);
        final ImageLoader mImageLoader;
        NetworkImageView mNetworkImageView = (NetworkImageView) findViewById(R.id.networkImageView);
        mImageLoader = MySingleton.getInstance(this).getImageLoader();

        //mNetworkImageView.setImageUrl(images[getImageNum()], mImageLoader);

        nextImageBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                nextImageBtn = (Button) v;
                setImageNum(getImageNum() + 1);
                //mNetworkImageView.setImageUrl(images[getImageNum()], mImageLoader);
            }
        });

        prevImageBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                prevImageBtn = (Button) v;
                setImageNum(getImageNum() - 1);
                //mNetworkImageView.setImageUrl(images[getImageNum()], mImageLoader);
            }
        });
    }

    public void setImageNum(int imageNum)
    {
        this.imageNum = imageNum;
    }

    public int getImageNum()
    {
        return imageNum;
    }

    public void setDeleteImageUri(String deleteImageUri)
    {
        this.deleteImageUri = deleteImageUri;
    }

    public String getDeleteImageUri()
    {
        return deleteImageUri;
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
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public ImageLoader getImageLoader()
    {
        return mImageLoader;
    }
}