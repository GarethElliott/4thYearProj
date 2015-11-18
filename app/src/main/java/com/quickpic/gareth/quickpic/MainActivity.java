package com.quickpic.gareth.quickpic;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.android.volley.Network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends ActionBarActivity
{
    ImageView viewImage;
    Button cameraBtn;
    Button netBtn;
    Button netImageBtn;
    Button chooseBtn;
    Button saveBtn;
    Intent imageIntent;
    Intent chooseIntent;
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraBtn = (Button)findViewById(R.id.cameraBtn);
        chooseBtn = (Button)findViewById(R.id.chooseBtn);
        netBtn = (Button)findViewById(R.id.netBtn);
        netImageBtn = (Button)findViewById(R.id.netImageBtn);
        saveBtn = (Button)findViewById(R.id.saveBtn);
        viewImage = (ImageView)findViewById(R.id.viewImage);

        cameraBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                imageIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(imageIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });

        netBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                netBtn=(Button) v;
                Intent intent = new Intent(MainActivity.this, ActivityNetwork.class);
                startActivity(intent);
            }
        });

        netImageBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                netImageBtn=(Button) v;
                Intent intent = new Intent(MainActivity.this, NetImage.class);
                startActivity(intent);
            }
        });


        saveBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                File f = new File(Environment.getExternalStorageDirectory(), "temp.jpg");
                imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                startActivityForResult(imageIntent, 1);
            }
        });

        chooseBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                chooseIntent = new   Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(chooseIntent, 2);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == RESULT_OK)
        {
            if (requestCode ==  CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
            {
                Bitmap bp = (Bitmap) data.getExtras().get("data");
                viewImage.setImageBitmap(bp);
            }

            if (requestCode == 1)
            {
                File f = new File(Environment.getExternalStorageDirectory().toString());

                for (File temp : f.listFiles())
                {
                    if (temp.getName().equals("temp.jpg"))
                    {
                        f = temp;
                        break;
                    }
                }

                try
                {
                    Bitmap bitmap;

                    bitmap = (Bitmap) data.getExtras().get("data");

                    String path = android.os.Environment.getExternalStorageDirectory() + File.separator + "Phoenix" + File.separator + "default";
                    f.delete();
                    OutputStream outFile = null;
                    File file = new File(path, String.valueOf(System.currentTimeMillis()) + ".jpg");

                    try
                    {
                        outFile = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outFile);
                        outFile.flush();
                        outFile.close();
                    }

                    catch (FileNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            else if (requestCode == 2)
            {
                Uri selectedImage = data.getData();
                String[] filePath = { MediaStore.Images.Media.DATA };
                Cursor c = getContentResolver().query(selectedImage,filePath, null, null, null);
                c.moveToFirst();
                int columnIndex = c.getColumnIndex(filePath[0]);
                String picturePath = c.getString(columnIndex);
                c.close();
                Bitmap thumbnail = (BitmapFactory.decodeFile(picturePath));

                Bitmap preview = convertToMutable(thumbnail);
                preview.setWidth(3072);
                preview.setHeight(4096);
                Drawable preview2 = new BitmapDrawable(getResources(), preview);
                //viewImage.setImageBitmap(preview);
                viewImage.setBackground(preview2);
            }
        }
    }

    public static Bitmap convertToMutable(Bitmap imgIn)
    {
        try
        {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return imgIn;
    }
}