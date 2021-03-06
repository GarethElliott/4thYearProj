package com.quickpic.gareth.quickpic;

public class ToDoItem
{
    // these columns correspond to the columns in the Mobile Service
    @com.google.gson.annotations.SerializedName("text")
    private String mText; //Item text

    @com.google.gson.annotations.SerializedName("__deleted")
    private Boolean mDeleted; //Item deleted

    @com.google.gson.annotations.SerializedName("id")
    private String mId; //Item Id

    @com.google.gson.annotations.SerializedName("userId")
    private String mUserId; //User Id

    @com.google.gson.annotations.SerializedName("username")
    private String mUserName; //UserName

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

    @com.google.gson.annotations.SerializedName("__createdAt")
    private String mDate;

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

    public ToDoItem(String text, String id, String containerName, String resourceName, String imageUri, String sasQueryString, String date, String userId, Boolean deleted)
    {
        this.setText(text);
        this.setId(id);
        this.setContainerName(containerName);
        this.setResourceName(resourceName);
        this.setImageUri(imageUri);
        this.setSasQueryString(sasQueryString);
        this.setDate(date);
        this.setUserId(userId);
        this.setDeleted(deleted);
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
        mId = id;
    }

    public boolean isComplete()
    {
        return mComplete;//Indicates if the item is marked as completed
    }

    public void setComplete(boolean complete)
    {
        mComplete = complete;//Marks the item as completed or incompleted
    }

    public String getDate()
    {
        return mDate;
    }

    public final void setDate(String date)
    {
        mDate = date;
    }

    public Boolean getDeleted()
    {
        return mDeleted;
    }

    public final void setDeleted(Boolean deleted)
    {
        mDeleted = deleted;
    }

    public String getUserId()
    {
        return mUserId;
    }

    public final void setUserId(String userId)
    {
        mUserId = userId;
    }

    public String getUserName()
    {
        return mUserName;
    }

    public final void setUserName(String userName)
    {
        mUserName = userName;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof ToDoItem && ((ToDoItem) o).mId == mId;
    }
}
