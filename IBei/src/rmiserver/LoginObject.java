package rmiserver;
import java.io.Serializable;

public class LoginObject implements Serializable 
{
	String USERNAME;
	String PASSWORD;
	int USER_ID;
	
	public LoginObject(String user, String password)
	{
		this.USERNAME = user;
		this.PASSWORD = password;
	}
	
	public void setUsername(String username)
	{
		this.USERNAME = username;
	}
	
	public void setPassword(String pass)
	{
		this.PASSWORD = pass;
	}
	
	public String getUsername()
	{
		return this.USERNAME;
	}
	public String getPassword()
	{
		return this.PASSWORD;
	}
	public void setUserID(int id )
	{
		this.USER_ID = id;
	}
	
	public int getUserID()
	{
		return this.USER_ID;
	}

}
