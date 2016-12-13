import java.io.Serializable;
public class AuctionObject implements Serializable{

	    public int id;
	    private String username;
	    private int code;
	    public AuctionObject(int id,String username) 
	    {
	        this.id = id;
	        this.username = username;
	       
	    }

	    
	    public void setUsername(String username)
	    {
	    	this.username = username;
	    }
	    public String getUsername(){
	    	return this.username;
	    }
	    public int getId() {
	        return id;
	    }

	    public void setId(int id) {
	        this.id = id;
	    }
	}


