package com.quickpic.gareth.quickpic;

public class ToDoItem
{
    @com.google.gson.annotations.SerializedName("text")
    private String mText; //Item text

    @com.google.gson.annotations.SerializedName("id")
    private String mId; //Item Id

    @com.google.gson.annotations.SerializedName("complete")
    private boolean mComplete; //Indicates if the item is completed

    public ToDoItem() //ToDoItem constructor
    {
        mContainerName = "";
        mResourceName = "";
        mImageUri = "";
        mSasQueryString = "";
    }

    @com.google.gson.annotations.SerializedName("imageUri")
    private String mImageUri; // imageUri - points to location in storage where photo will go

    public String getImageUri()
    {
        return mImageUri;
    }

    public final void setImageUri(String ImageUri)
    {
        mImageUri = ImageUri; //Sets the item ImageUri
    }

    @com.google.gson.annotations.SerializedName("containerName")
    private String mContainerName; //ContainerName - like a directory, holds blobs


    public String getContainerName()
    {
        return mContainerName; //Returns the item ContainerName
    }

    public final void setContainerName(String ContainerName)
    {
        mContainerName = ContainerName;// Sets the item ContainerName
    }

    @com.google.gson.annotations.SerializedName("resourceName")
    private String mResourceName;

    public String getResourceName()
    {
        return mResourceName;// Returns the item ResourceName
    }


    public final void setResourceName(String ResourceName)
    {
        mResourceName = ResourceName;// Sets the item ResourceName
    }

    @com.google.gson.annotations.SerializedName("sasQueryString")
    private String mSasQueryString; //SasQueryString - permission to write to storage

    public String getSasQueryString()
    {
        return mSasQueryString; //Returns the item SasQueryString
    }

    public final void setSasQueryString(String SasQueryString)
    {
        mSasQueryString = SasQueryString; //Sets the item SasQueryString
    }

    @Override
    public String toString()
    {
        return getText();
    }

    public ToDoItem(String text, String id, String containerName, String resourceName, String imageUri, String sasQueryString)
    {
        this.setText(text);
        this.setId(id);
        this.setContainerName(containerName);
        this.setResourceName(resourceName);
        this.setImageUri(imageUri);
        this.setSasQueryString(sasQueryString);
        // Initializes a new ToDoItem
    }

    public String getText()
    {
        return mText; //        returns the item text;
    }

    public final void setText(String text)
    {
        mText = text;//Sets the item text
    }

    public String getId()
    {
        return mId;// Returns the item id
    }

    public final void setId(String id)
    {
        mId = id; // Indicates if the item is marked as completed
    }

    public boolean isComplete()
    {
        return mComplete;//Indicates if the item is marked as completed
    }


    public void setComplete(boolean complete)
    {
        mComplete = complete;//Marks the item as completed or incompleted
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ToDoItem && ((ToDoItem) o).mId == mId;
    }


}
