package rmiserver;
import java.io.Serializable;

public class MessageObject implements Serializable
{
	public int request;
  	public int auctionid;
  	public int userid;
    public String text;
    private String username;

    public MessageObject(int id, String text, int userid) 
    {
    	this.userid = userid;
        this.auctionid = id;
        this.text = text;
    }
    
    public void set_request(int request)
    {
    	this.request = request;
    }
    public int get_request( )
    {
    	return this.request;
    }
    
    public void setUsername(String user)
    {
    	this.username = user;
    }
    
    public String getUsername()
    {
    	return this.username;
    }
    
    public int getUserID()
    {
    	return this.userid;
    }

    public int getAuctionId() {
        return auctionid;
    }

    public void setauctionId(int id) {
        this.auctionid = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
