import java.io.Serializable;

public class DetailAuctionObject implements Serializable {
    public int id;
    private String title;
    private String code;
    public DetailAuctionObject(int id,String title, String code)
    {
        this.id = id;
        this.title = title;
        this.code = code;
    }

    public void setCode(String code)
    {
    	this.code = code;
    }
    
    public String getCode()
    {
    	return this.code;
    }
    
    public void setTitle(String title)
    {
    	this.title = title;
    }
    public String getTitle(){
    	return this.title;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
