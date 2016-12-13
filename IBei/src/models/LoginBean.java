package models;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import rmiserver.RMI;


public class LoginBean {

	private String username;
	private String password;
	private RMI server;

	public LoginBean()
	{
		try {
			server = (RMI) Naming.lookup("rmi://169.254.138.228:6001/rmiserver");
			
			
		}
		catch(NotBoundException|MalformedURLException|RemoteException e) {
			e.printStackTrace(); 
		}
	}

	public int getLoginValidation() throws RemoteException {
		
		System.out.println("Username :" + this.username +" pass : "+this.password);
		return server.validate_authentication(this.getUsername(), this.getPassword());
	}
	
	public String getUsername() 
	{
		return username;
	}

	public void setUsername(String username) 
	{
		this.username = username;
	}

	public String getPassword() 
	{
		return password;
	}

	public void setPassword(String password) 
	{
		this.password = password;
	}
}
