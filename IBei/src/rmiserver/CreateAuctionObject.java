package rmiserver;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

public class CreateAuctionObject implements Serializable 
{
	private int userID;
	private int auctionID;
	private String code;
    private String title;
    private String description;
    private float amount;
    private Timestamp deadline;

    public CreateAuctionObject(String code, String title, String description, float amount, Timestamp deadline, int userid) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.deadline = deadline;
        this.userID = userid;
    }
    
    public void setUserID ( int id )
    {
    	this.userID = id;
    }
    public void setAuctionId(int id)
    {
    	this.auctionID = id;
    }

    public String getCode() {
        return code;
    }
    
    public int getOwnerId()
    {
    	return userID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public Timestamp getDeadline() {
        return deadline;
    }

    public void setDeadline(Timestamp deadline) {
        this.deadline = deadline;
    }
    
    public String toString()
    {
    	return "title: "+this.title+" , description: "+this.description+" , deadline: "+this.deadline;
    }
}

