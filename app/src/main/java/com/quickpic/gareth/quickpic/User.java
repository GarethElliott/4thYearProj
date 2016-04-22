package com.quickpic.gareth.quickpic;

public class User
{
    @com.google.gson.annotations.SerializedName("id")
    private String mId;

    @com.google.gson.annotations.SerializedName("username")
    private String mUsername; // username

    @com.google.gson.annotations.SerializedName("__createdAt")
    private String mDate; // username

    public User() //ToDoItem constructor
    {
        mId = "";
        mUsername = "";
    }

    public User(String username, String userid)
    {
        this.setId(userid);
        this.setUsername(username);
    }

    public String getId()
    {
        return mId;
    }

    public final void setId(String id)
    {
        mId = id;
    }

    public String getUsername()
    {
        return mUsername;
    }

    public final void setUsername(String username)
    {
        mUsername = username;
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
        return o instanceof User && ((User) o).mId == mId;
    }
}
