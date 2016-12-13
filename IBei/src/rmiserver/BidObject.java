package rmiserver;
import java.io.Serializable;

public class BidObject implements Serializable 
{
	public int user_id;
	public int auctionid;
    public float amount;

    public BidObject(int id, float amount, int user_id)
    {
    	this.user_id = user_id;
        this.auctionid = id;
        this.amount = amount;
    }

    public int getUserId()
    {
    	return this.user_id;
    }
    
    public int getBidId() {
        return auctionid;
    }

    public void setBidId(int id) {
        this.auctionid = id;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

}
