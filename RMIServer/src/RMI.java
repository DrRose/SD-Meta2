import java.rmi.*;
import java.util.ArrayList;
import java.util.HashMap;

public interface RMI extends Remote
{
	public boolean register_user(String username , String password) throws RemoteException;
	public int validate_authentication(String username , String password) throws RemoteException;
	public void start_connection() throws RemoteException;
	public void auction_up_todate() throws RemoteException;
	public boolean create_auction(CreateAuctionObject obj ) throws RemoteException;
	public void update_user_data(int userid) throws RemoteException;
	public ArrayList<DetailAuctionObject> search_auction_bycode(String arg, String type ) throws RemoteException;
	public ArrayList<DetailAuctionObject> search_auction_by_user(String clienteid, String onwner) throws  RemoteException;
	public CreateAuctionObject detail_auction(int auctionid) throws RemoteException;
	public int bid(BidObject bid) throws RemoteException;
	public boolean update_max_amount(BidObject bid) throws RemoteException;
	public boolean edit_auction(CreateAuctionObject auction, int id) throws RemoteException;
	public boolean message( MessageObject m) throws RemoteException;
	public ArrayList<Integer> send_message_notification( MessageObject m ) throws RemoteException;
	public void save_messages(MessageObject m) throws RemoteException;
	public ArrayList<MessageObject> get_user_message(int userid) throws RemoteException;
	public ArrayList<MessageObject> get_all_messages(int auctionid) throws RemoteException;
	public void delete_messages(int user_id) throws RemoteException;
	public String get_username_by_id(int userid) throws RemoteException;
	public int get_bidcount_by_auctionid( int auctionid ) throws RemoteException;
	public void set_status(int userid, String type) throws RemoteException;
	public int check_online_users() throws RemoteException;
	public boolean is_online(int userid) throws RemoteException;
	public boolean check_auction_status(int auction_id) throws RemoteException;
	public ArrayList<Integer> get_users_in_auction( int auction_id) throws RemoteException;
	public void cancel_auction_by_id ( int auction) throws RemoteException;

    
    //_____________________________ADMIN_____________________________________________//
	
    public boolean cancelAuction(HashMap<String, String> obj) throws RemoteException;
    public ArrayList<AuctionObject> getCreatedAuctions() throws RemoteException;
    public ArrayList<AuctionObject> getWonAuctions() throws RemoteException;
    public ArrayList<AuctionObject> getAuctions10() throws RemoteException;
    public boolean doALlStuffOfBanUser(HashMap<String, String> obj) throws RemoteException;
    public boolean cancelAllUserProjects(BanUser obj) throws RemoteException;
    public boolean deleteBids(int auctionid, int minbid) throws RemoteException;
    public  ArrayList<AuctionObjectBan> findMinBid(BanUser obj) throws RemoteException;
    public  Boolean deleteUser(BanUser obj) throws RemoteException;



}
