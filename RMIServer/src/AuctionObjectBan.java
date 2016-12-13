import java.io.Serializable;

public class AuctionObjectBan implements Serializable{

	    private int auctionid;
	    private int amount;
	    
	    public AuctionObjectBan(int id,int amount) 
	    {
	        this.auctionid = id;
	        this.amount = amount;      
	    }
	    
	    public int getAuctionID(){
	    	return this.auctionid;
	    }
	    public int getAmount() {
	        return amount;
	    }
	}
