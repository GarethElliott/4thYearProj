package com.quickpic.gareth.quickpic;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Build;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

public class ViewItemAdapter extends ArrayAdapter<ToDoItem>
{
    Context mContext;
    int mLayoutResourceId;
    public MobileServiceTable<ToDoItem> mToDoTable;

    public String storageConnectionString =
            "DefaultEndpointsProtocol=http;" +
                    "AccountName=todosample;" +
                    "AccountKey=zsfmWrTGkYPmnW+sBn6b/YXHwkJQxkpXwpthZ/iIJ/L/QL8UivKKWLStAZS8gB6PrjoSfYPJYHYfnRKEIKxHdA==";

    public ViewItemAdapter(Context context, int layoutResourceId)
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
        mImageLoader = Singleton.getInstance(getContext()).getImageLoader();
        final NetworkImageView imageV = (NetworkImageView) row.findViewById(R.id.NetworkImageView);
        imageV.setImageUrl(currentItem.getImageUri(), mImageLoader);

        final Button delBtn = (Button) row.findViewById(R.id.deleteBtn);

        delBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                deleteBlob(currentItem.getId(), currentItem.getResourceName());
            }
        });

        return row;
    }

    public void deleteBlob(final String id, final String resourceName)
    {
        AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                try
                {
                    mToDoTable.delete(id);

                    CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

                    CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

                    CloudBlobContainer container = blobClient.getContainerReference("todoitemimages");

                    CloudBlockBlob blob = container.getBlockBlobReference("" + resourceName);

                    blob.deleteIfExists();
                }

                catch (final Exception e)
                {
                    System.out.println("Exception:  " + e);
                }
                return null;
            }
        };

        runAsyncTask(task);
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
}

class Singleton
{
    private static Singleton mInstance;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private static Context mCtx;

    private Singleton(Context context)
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

    public static synchronized Singleton getInstance(Context context)
    {
        if (mInstance == null)
        {
            mInstance = new Singleton(context);
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