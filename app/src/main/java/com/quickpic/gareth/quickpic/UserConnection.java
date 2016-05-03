package com.quickpic.gareth.quickpic;

public class UserConnection
{
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    @com.google.gson.annotations.SerializedName("userid")
    private String mUserId;

    @com.google.gson.annotations.SerializedName("conid")
    private String mConnectionId; // username

    @com.google.gson.annotations.SerializedName("conname")
    private String mConnectionName; // username

    @com.google.gson.annotations.SerializedName("__createdAt")
    private String mDate; // username

    public UserConnection() //ToDoItem constructor
    {
        mUserId = "";
        mConnectionId = "";
    }

    public UserConnection(String id, String userid, String username, String conid, String conname)
    {
        this.setId(id);
        this.setUserId(userid);
        this.setUserId(username);
        this.setConnectionId(conid);
        this.setConnectionName(conname);
    }

    public String getId()
    {
        return mId;
    }

    public final void setId(String id)
    {
        mId = id;
    }

    public String getUserId()
    {
        return mUserId;
    }

    public final void setUserId(String userid)
    {
        mUserId = userid;
    }

    public String getConnectionId()
    {
        return mConnectionId;
    }

    public final void setConnectionId(String connectionId)
    {
        mConnectionId = connectionId;
    }

    public String getConnectionName()
    {
        return mConnectionName;
    }

    public final void setConnectionName(String connectionName)
    {
        mConnectionName = connectionName;
    }

    public String getDate()
    {
        return mDate;
    }

    public final void setDate(String date)
    {
        mDate = date;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof User && ((UserConnection) o).mId == mId;
    }
}
