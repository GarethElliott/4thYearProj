package com.quickpic.gareth.quickpic;

public class UserConnection
{
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    @com.google.gson.annotations.SerializedName("user")
    private String mUser;

    @com.google.gson.annotations.SerializedName("connection")
    private String mConnection; // username

    public UserConnection() //ToDoItem constructor
    {
        mUser = "";
        mConnection = "";
    }

    public UserConnection(String id, String user, String connection)
    {
        this.setId(id);
        this.setUser(user);
        this.setConnection(connection);
    }

    public String getId()
    {
        return mId;
    }

    public final void setId(String id)
    {
        mId = id;
    }

    public String getUser()
    {
        return mUser;
    }

    public final void setUser(String user)
    {
        mUser = user;
    }

    public String getConnection()
    {
        return mConnection;
    }

    public final void setConnection(String connection)
    {
        mConnection = connection;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof User && ((UserConnection) o).mId == mId;
    }
}
