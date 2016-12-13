package rmiserver;
import java.io.Serializable;

public class BanUser implements Serializable{
	
	private int userID;

	
	public BanUser(int user)
	{
		this.userID = user;
		
	}

	
	public int getUserID() { return this.userID; }
}
