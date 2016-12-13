import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.sql.*;
import java.rmi.server.*;
import java.net.*;
import java.net.UnknownHostException;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


public class RMIServer extends UnicastRemoteObject implements RMI
{
	
	/* TODO RMI: 
	 * Backup
	 * Config File with RMI and RMI Back server : Port and Ip Address
	 * Read all data from BD
	 */

	private String rmiAddress;
	private Properties prop = new Properties();
	private int RMIPORT;

	static final String JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
	static final String DATABASE_URL ="jdbc:oracle:thin:@localhost:1521:xe";
	static final String USERNAME = "bd";
	static final String PASSWORD = "bd";
	//static final int RMIPORT = 6001;
	Connection conn = null;
	Statement stmt = null;


	public RMIServer() throws RemoteException
	{
		read_config_file();
		
		start_connection();
	}

	public static void main (String args[])
	{
		System.getProperties().put("java.security.policy", "policy.all");
		//System.setSecurityManager(new RMISecurityManager());
		while(true){
			try {
				RMIServer rmiserver = new RMIServer();
				LocateRegistry.createRegistry(rmiserver.RMIPORT);
				Naming.rebind (rmiserver.rmiAddress,rmiserver );
				break;
			} catch (RemoteException re)
			{
				System.err.println("Remote Exception!" + re.getMessage());
				try
				{
					System.out.println("A chatear o RMI");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}catch ( MalformedURLException e)
			{
				System.out.println("Malformed url exception: " + e.getMessage());
			}
		}

	}

	private void read_config_file()
	{
		//String configFile = "config.properties";

		InputStream in;

		try {
			in = getClass().getResourceAsStream("/config.properties");
			prop.load(in);
			RMIPORT = Integer.parseInt(prop.getProperty("RMIPORT"));
			rmiAddress = prop.getProperty("RMIADDRESS");
		} catch (IOException e) {
			System.out.println("Could not find property file or the file is corrupted.");

		}
	}

	public void start_connection() throws RemoteException
	{
		System.out.println("Connecting to database...");
		try
		{
			Class.forName(JDBC_DRIVER);

			//System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(DATABASE_URL,USERNAME,PASSWORD);
			//System.out.println("Success!.");
			//System.out.println("Creating statement...");
			stmt = conn.createStatement();
			//System.out.println("Success!.");
			conn.setAutoCommit(false);
			System.out.println("Connected!");

		}catch(SQLException s)
		{
			System.out.println("SQL Exception: " + s.getMessage());
			System.out.println("Can't connect to database...\nTry again..");
			System.exit(0);
		}
		catch(Exception e)
		{
			System.out.println("Class.forName:" + e.getMessage());
		}
	}

	private void update_database()
	{
		try{
			conn.commit();
		}catch(SQLException se){
			System.err.println("Database update failed!");
		}
	}

	/**
	 * Regists a new user in the database 
	 * @return false if the user is already registed 
	 */
	@Override
	public synchronized boolean register_user(String username , String password) throws RemoteException {
		PreparedStatement prepStat;
		String sql_request = "INSERT INTO CLIENT(USERID,USERNAME,PASSWORD,NUM_AUCTIONS_WON, NUM_AUCTIONS_CREATED)VALUES (CLIENT_SEQ.NEXTVAL,?,?,0,0)";
		try
		{
			prepStat = conn.prepareStatement(sql_request);
			prepStat.setString(1,username);
			prepStat.setString(2,password);

			if ( validate_authentication(username , password) != -1 )
			{
				System.out.println("Login already in use!");
				return false;
			}
			else
			{
				prepStat.executeUpdate();
				update_database();
			}
			prepStat.close();
		}catch(SQLException se)
		{
			System.out.println("REGISTER SQL: " + se.getMessage());
			return false;
		}


		return true;
	}
	/**
	 * Validate a new user or a login
	 * @return user_id
	 */
	@Override
	public synchronized int validate_authentication(String username , String password) throws RemoteException {

		System.out.println("LOgin");
		String sql_request = "SELECT * FROM CLIENT WHERE USERNAME = ? AND PASSWORD = ?";
		ResultSet rs;
		try
		{
			PreparedStatement prepStat;
			prepStat = conn.prepareStatement(sql_request);
			prepStat.setString(1, username);
			prepStat.setString(2, password);
			rs = prepStat.executeQuery();
			if ( rs.next() )
			{
				return rs.getInt(1);
			}
			rs.close();
			prepStat.close();
		}catch(SQLException se)
		{
			System.out.println("SQL AUT: "+se.getMessage());
		}
		return -1;
	}


	@Override
	public synchronized void auction_up_todate()
	{
		String sql_request = "UPDATE AUCTION SET status = 0 WHERE DATA <= ?";
		PreparedStatement prepStat;

		try
		{
			Timestamp b= new Timestamp(System.currentTimeMillis());
			prepStat = conn.prepareStatement(sql_request);
			prepStat.setTimestamp(1, b);
			prepStat.executeUpdate();
			update_database();
			//System.out.println("AUCTION ACABOU");
			prepStat.close();

		} catch(SQLException se)
		{
			System.out.println(se.getMessage());

		}
	}

	public synchronized boolean create_auction(CreateAuctionObject obj) throws RemoteException
	{

		String sql_request = "INSERT INTO AUCTION(auctionid,onwner,code,title,description,max_amount,data) VALUES (auction_seq.nextval,?,?,?,?,?,?)";


		// Get user auctions
		PreparedStatement prepStat;
		try
		{
			prepStat = conn.prepareStatement(sql_request);
			//Create Auction
			prepStat.setInt(1, obj.getOwnerId());
			prepStat.setString(2, obj.getCode());
			prepStat.setString(3, obj.getTitle());
			prepStat.setString(4, obj.getDescription());
			prepStat.setFloat(5,obj.getAmount());
			prepStat.setTimestamp(6, obj.getDeadline());
			prepStat.executeUpdate();
			update_database();
			update_user_data(obj.getOwnerId());
			prepStat.close();
			System.out.println("Created Auction");
		}catch(SQLException se)
		{
			System.out.println("CREATING AUCTION SQL: " + se.getMessage());
			return false;
		}

		return true;

	}

	@Override
	public synchronized void update_user_data(int userid) throws RemoteException
	{
		int num_auctions = 0;;
		String sql_request1 = "SELECT NUM_AUCTIONS_CREATED FROM CLIENT WHERE USERID = " + userid;
		String sql_request2 = "UPDATE CLIENT SET NUM_AUCTIONS_CREATED = ? WHERE USERID = ?";

		PreparedStatement prepStat;
		PreparedStatement prepStat1;

		try {
			prepStat = conn.prepareStatement(sql_request1);
			ResultSet rs = prepStat.executeQuery();

			if ( rs.next() )
			{
				//System.out.println(rs.getInt("NUM_AUCTIONS_CREATED"));
				num_auctions = rs.getInt(1);
				//System.out.println(num_auctions);
			}
			prepStat.close();
		} catch (SQLException e)
		{
			System.out.println("GET OWNER ID SQL: " + e.getMessage());
		}

		try {
			prepStat1 = conn.prepareStatement(sql_request2);
			prepStat1.setInt(1, num_auctions + 1);
			prepStat1.setInt(2, userid);
			prepStat1.execute();

			update_database();
			prepStat1.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
	}

	/*
	 * search_auction && my_auctions
	 * 
	 * @param: arg: ISBN , ID: USER
	 * @return: Detalhes do Leilão
	 */
	@Override
	public synchronized ArrayList<DetailAuctionObject> search_auction_bycode(String arg, String type) throws RemoteException {
		ArrayList<DetailAuctionObject> leiloes = new ArrayList<>();

		String sql_request = "SELECT * FROM AUCTION WHERE "+type+" =  " + arg;
		String title;
		String code;
		int auction_id;

		PreparedStatement ps;
		ResultSet rs;
		try
		{
			//TODO: Handling auctions where user has activity
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			while ( rs.next() )
			{
				code = rs.getString("CODE");
				title = rs.getString("TITLE");
				auction_id = rs.getInt("AUCTIONID");

				DetailAuctionObject auction = new DetailAuctionObject(auction_id, title,code);
				leiloes.add(auction);


			}
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}

		return leiloes;
	}

	@Override
	public ArrayList<DetailAuctionObject> search_auction_by_user(String clienteid, String onwner) throws RemoteException {

		ArrayList<DetailAuctionObject> leiloes = new ArrayList<>();

		int arg = Integer.parseInt(clienteid);
		String sql_request_owner = "SELECT DISTINCT * FROM AUCTION WHERE ONWNER = "+ arg;
		String sql_request_bids = "SELECT * FROM (SELECT auctionid from bid where userid = "+arg+")a , AUCTION WHERE auction.auctionid = a.auctionid";
		String title;
		String code;
		int auction_id = 0;

		PreparedStatement ps;
		ResultSet rs;
		try
		{
			//TODO: Handling auctions where user has activity
			ps = conn.prepareStatement(sql_request_owner);
			rs = ps.executeQuery();

			while ( rs.next() )
			{
				code = rs.getString("CODE");
				title = rs.getString("TITLE");
				auction_id = rs.getInt("AUCTIONID");

				DetailAuctionObject auction = new DetailAuctionObject(auction_id, title,code);
				leiloes.add(auction);
			}
			ps.close();
			rs.close();

		} catch (SQLException e) {

			e.printStackTrace();
		}

		try
		{
			//TODO: Handling auctions where user has activity
			ps = conn.prepareStatement(sql_request_bids);
			rs = ps.executeQuery();

			while ( rs.next() )
			{
				code = rs.getString("CODE");
				title = rs.getString("TITLE");
				auction_id = rs.getInt("AUCTIONID");

				DetailAuctionObject auction = new DetailAuctionObject(auction_id, title,code);
				leiloes.add(auction);
			}
			ps.close();
			rs.close();

		} catch (SQLException e) {

			e.printStackTrace();
		}

		return leiloes;
	}


	/*
	 * Recebe o id do leilão que procura e retorna todos os detalhes do mesmo;
	 * @param: auctionid
	 * @return: Auction
	 */
	@Override
	public synchronized CreateAuctionObject detail_auction(int auctionid) throws RemoteException
	{
		String sql_request = "SELECT * FROM AUCTION WHERE AUCTIONID = "+ auctionid;
		PreparedStatement ps;
		ResultSet rs;

		CreateAuctionObject leilao = null;
		int amount,userid;
		String title, description, code;
		Timestamp deadline;
		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			if ( rs.next() )
			{
				code = rs.getString("CODE");
				amount = rs.getInt("MAX_AMOUNT");
				userid = rs.getInt("ONWNER");

				title = rs.getString("TITLE");
				description = rs.getString("DESCRIPTION");
				deadline = rs.getTimestamp("DATA");

				leilao = new CreateAuctionObject(code, title, description, amount, deadline, userid);
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
		return leilao;
	}

	@Override
	public synchronized int bid(BidObject bid) throws RemoteException
	{

		String sql_request = "SELECT MAX_AMOUNT FROM AUCTION WHERE AUCTIONID = "+ bid.getBidId();
		String sql_request1 = null;
		String sql_request2 = "INSERT INTO BID(BIDID,USERID,AUCTIONID,AMOUNT) VALUES (BID_SEQ.NEXTVAL,"+bid.getUserId()+","+ bid.getBidId()+","+bid.getAmount()+")";
		PreparedStatement ps;
		ResultSet rs;
		float current_bid = 0;

		if ( check_auction_status(bid.getBidId()))
		{
			try
			{
				ps = conn.prepareStatement(sql_request);
				rs = ps.executeQuery();
				if( rs.next() )
				{
					current_bid = rs.getFloat("MAX_AMOUNT");
				}
				ps.close();
				rs.close();

			} catch (SQLException e)
			{
				e.printStackTrace();
			}
			try
			{
				ps = conn.prepareStatement(sql_request2);
				ps.executeUpdate();
				ps.close();
				update_database();
				if ( current_bid > bid.getAmount() )
					return 1;
				else
					return 0;

			}catch(SQLException e)
			{
				e.printStackTrace();
			}
		}
		return -1;
	}
	@Override
	public boolean update_max_amount(BidObject bid) throws RemoteException
	{
		String sql_request1 = "UPDATE AUCTION SET MAX_AMOUNT = " + bid.getAmount()  + "WHERE AUCTIONID = " + bid.getBidId();
		PreparedStatement ps;
		try
		{
			ps = conn.prepareStatement(sql_request1);
			ps.executeUpdate();
			ps.close();
			update_database();

		} catch (SQLException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;

	}

	@Override
	public synchronized boolean edit_auction(CreateAuctionObject auction, int auctionid) throws RemoteException
	{
		String sql_request = "";
		String sql_request1 = "INSERT INTO AUCTION_PAST SELECT * FROM AUCTION WHERE AUCTIONID = " + auctionid;

		String title, description;
		float max_amount;
		Timestamp deadline;
		PreparedStatement ps;

		try {
			ps = conn.prepareStatement(sql_request1);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		if ( auction.getDeadline() != null )
		{
			deadline = auction.getDeadline();
			sql_request = "UPDATE AUCTION SET DEADLINE = ? WHERE AUCTIONID = "+ auctionid;
			try {
				ps = conn.prepareStatement(sql_request);
				ps.setTimestamp(1,deadline);
				ps.executeUpdate();
				update_database();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		if ( auction.getTitle() != null )
		{
			title = auction.getTitle();
			sql_request = "UPDATE AUCTION SET TITLE = '"+title+"' WHERE AUCTIONID = "+ auctionid;
			try {
				ps = conn.prepareStatement(sql_request);
				ps.executeUpdate();
				update_database();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		if ( auction.getDescription() != null )
		{
			description = auction.getDescription();
			sql_request = "UPDATE AUCTION SET DESCRIPTION = '"+description+"' WHERE AUCTIONID = "+ auctionid;
			try {
				ps = conn.prepareStatement(sql_request);
				ps.executeUpdate();
				update_database();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		if ( auction.getAmount() != 0 )
		{
			max_amount = auction.getAmount();
			title = auction.getTitle();
			sql_request = "UPDATE AUCTION SET MAX_AMOUNT = "+max_amount+" WHERE AUCTIONID = "+ auctionid;
			try {
				ps = conn.prepareStatement(sql_request);
				ps.executeUpdate();
				update_database();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	/*
	 * Escreve uma mensagem de um determinado User num leião pré-defino ( auction_id ) 
	 * @param: MessageObject
	 * @return boolean
	 */
	@Override
	public synchronized boolean message(MessageObject m) throws RemoteException
	{
		String sql_request = "INSERT INTO MESSAGE(AUCTIONID,USERID,MESSAGE) VALUES ("+m.getAuctionId()+", "+m.getUserID()+", '"+m.getText()+"')";
		PreparedStatement ps;
		try {
			ps = conn.prepareStatement(sql_request);
			ps.executeUpdate();
			update_database();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/*
	 * Obtem todas as mensagens de um determinado leilão;
	 * @param: auctionid
	 * @return: ArrayList
	 */
	@Override
	public synchronized ArrayList<MessageObject> get_all_messages(int auctionid) throws RemoteException {

		String sql_request = "SELECT * FROM MESSAGE WHERE AUCTIONID = " + auctionid;
		ArrayList<MessageObject> m = new ArrayList<>();
		String text;
		String username;
		int user_id = 0;
		//Irrelevant
		int auction_id;
		PreparedStatement ps;
		ResultSet rs;
		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			while(rs.next())
			{
				auction_id = rs.getInt("AUCTIONID");
				text = rs.getString("MESSAGE");
				user_id = rs.getInt("USERID");
				username = get_username_by_id(user_id);

				MessageObject message  = new MessageObject(auction_id,text,user_id);
				message.setUsername(username);
				m.add(message);
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return m;
	}

	@Override
	public synchronized String get_username_by_id(int userid) throws RemoteException
	{
		String sql_request = "SELECT * FROM CLIENT WHERE USERID = "+ userid;
		String username = "";

		PreparedStatement ps;
		ResultSet rs;

		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			if ( rs.next() )
			{
				username = rs.getString("USERNAME");
			}
			else
			{
				System.out.println("USERID NOT FOUND");
			}
			ps.close();
			rs.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}

		return username;
	}

	/*
	 * @param: auctionid
	 * @return: numero de licitações num leilão
	 */
	@Override
	public synchronized int get_bidcount_by_auctionid(int auctionid) throws RemoteException {
		int count = 0;
		String sql_request = "SELECT * FROM BID WHERE AUCTIONID = "+auctionid;

		PreparedStatement ps;
		ResultSet rs;
		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			while( rs.next())
			{
				count++;
			}
			ps.close();
			rs.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}
		return count;
	}

	@Override
	public synchronized void set_status(int userid, String type) throws RemoteException
	{
		int status = 0;
		if ( type.equals("ONLINE"))
			status = 1;
		else
			status = 0;
		String sql_request = "UPDATE CLIENT SET STATUS = "+status+" WHERE USERID = "+ userid;
		PreparedStatement ps;

		try
		{
			ps = conn.prepareStatement(sql_request);
			ps.executeUpdate();
			update_database();
			ps.close();
		} catch (SQLException e)
		{
			e.printStackTrace();
		}

	}

	@Override
	public synchronized int check_online_users() throws RemoteException
	{
		int online = 0;
		String sql_request = "SELECT * FROM CLIENT WHERE STATUS = 1";
		PreparedStatement ps;
		ResultSet rs;
		try {
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();

			while(rs.next())
				online++;

			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return online;
	}

	@Override
	public synchronized void cancel_auction_by_id(int auction) throws RemoteException
	{
		String sql_request = "UPDATE AUCTION SET STATUS = 0 WHERE AUCTIONID = " +auction;
		PreparedStatement ps;

		try
		{
			ps = conn.prepareStatement(sql_request);
			ps.executeUpdate();
			update_database();
			ps.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
	}
	@Override
	public synchronized boolean check_auction_status(int auction_id) throws RemoteException
	{
		String sql_request = "SELECT * FROM AUCTION WHERE AUCTIONID = "+ auction_id;
		PreparedStatement ps;
		ResultSet rs;

		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			if ( rs.next() )
			{
				if ( rs.getInt("STATUS") == 1 )
					return true;
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
		return false;
	}

	@Override
	public synchronized ArrayList<Integer> get_users_in_auction(int auction_id) throws RemoteException {

		String sql_request = "SELECT ONWNER FROM AUCTION WHERE AUCTIONID = " + auction_id;
		ArrayList<Integer> i = new ArrayList<>();
		PreparedStatement ps;
		ResultSet rs;

		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			while( rs.next() )
			{
				i.add(rs.getInt("ONWNER"));
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
		String sql_request1 = "SELECT DISTINCT * FROM BID WHERE AUCTIONID = " + auction_id;

		try {
			ps = conn.prepareStatement(sql_request1);
			rs = ps.executeQuery();
			while( rs.next() )
			{
				i.add(rs.getInt("USERID"));
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return i;
	}

	@Override
	public boolean is_online(int userid) throws RemoteException
	{
		String sql = "SELECT * FROM CLIENT WHERE USERID = "+userid+" AND STATUS = 1";
		PreparedStatement ps;
		ResultSet rs;

		try
		{
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			if ( rs.next() )
				return true;
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}

		return false;
	}

	/*
	 * Obter todos os users que mandaram mensagens para um determinado leilão
	 * Obter tambem o dono desse leilão
	 * 
	 */
	@Override
	public synchronized ArrayList<Integer> send_message_notification(MessageObject m) throws RemoteException
	{
		String sql_request = "SELECT ONWNER FROM AUCTION WHERE AUCTIONID =  " + m.getAuctionId();
		String sql_request1 = "SELECT DISTINCT * FROM MESSAGE WHERE AUCTIONID = " + m.getAuctionId();

		ArrayList<Integer> array = new ArrayList<>();

		PreparedStatement ps;
		ResultSet rs;

		try
		{
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			if ( rs.next() )
			{
				array.add(rs.getInt("ONWNER"));
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}

		try {
			ps = conn.prepareStatement(sql_request1);
			rs = ps.executeQuery();
			while(rs.next())
			{
				if (!array.contains(rs.getInt("USERID")))
					array.add(rs.getInt("USERID"));
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
		return array;
	}

	@Override
	public synchronized void save_messages(MessageObject m) throws RemoteException
	{
		String sql = "INSERT INTO MESSAGE_DB(MESSAGE,USERID,STATUS,AUCTIONID,REQUEST) VALUES ('"+m.getText()+"', "+m.getUserID()+",'FALSE',"+m.getAuctionId()+", "+m.get_request()+")";
		PreparedStatement ps;
		try
		{
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
			update_database();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}


	}

	@Override
	public synchronized ArrayList<MessageObject> get_user_message(int userid) throws RemoteException {
		ArrayList<MessageObject> m = new ArrayList<>();
		String sql = "SELECT * FROM MESSAGE_DB WHERE REQUEST = "+userid+" AND STATUS = 'FALSE'";
		MessageObject mo;
		PreparedStatement ps;
		ResultSet rs;
		int user;
		int auction_id;
		String text;

		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while(rs.next())
			{
				text = rs.getString("MESSAGE");
				user = rs.getInt("USERID");
				auction_id = rs.getInt("AUCTIONID");

				mo = new MessageObject(auction_id, text, user);
				m.add(mo);
			}
			ps.close();
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return m;
	}

	@Override
	public void delete_messages(int user_id) throws RemoteException
	{
		String sql = "UPDATE MESSAGE_DB SET STATUS = 'TRUE' WHERE REQUEST = "+ user_id;
		PreparedStatement ps ;

		try {
			ps = conn.prepareStatement(sql);
			ps.executeUpdate();
			ps.close();
			update_database();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	//-------------------------------------------------------------------------------------------//

	//_____________________________________ADMIN________________________________________________//

	public synchronized boolean cancelAuction(HashMap<String, String> obj) throws RemoteException
	{


		String sql_request = "UPDATE AUCTION SET status=0 WHERE auctionid = ?";
		PreparedStatement prepStat;

		try
		{

			prepStat = conn.prepareStatement(sql_request);
			prepStat.setString(1, obj.get("auction_id"));
			prepStat.executeUpdate();
			update_database();
			System.out.println("Auction Canceled");
			return true;

		} catch(SQLException se)
		{
			System.out.println("CANCEL AUCTION:"+se.getMessage());
			return false;
		}


	}


	public synchronized ArrayList<AuctionObject> getCreatedAuctions() throws RemoteException
	{
		String sql_request = "SELECT username , num_auctions_created FROM (SELECT username , num_auctions_created FROM client ORDER BY num_auctions_created DESC) WHERE rownum <= 5";
		ArrayList<AuctionObject> created = new ArrayList<>();
		PreparedStatement ps1;
		ResultSet rs;
		try{
			System.out.println("meto 1:");
			ps1 = conn.prepareStatement(sql_request);
			rs = ps1.executeQuery();
			System.out.println("entreiii");
			while(rs.next())
			{
				String username = rs.getString("USERNAME");
				int num_auctions_won = rs.getInt("NUM_AUCTIONS_CREATED");
				System.out.print("username:"+num_auctions_won);
				System.out.println("count:"+username);
				AuctionObject ob = new AuctionObject(num_auctions_won, username);
				created.add(ob);
			}


		}catch (Exception e) {

		}
		return created;


	}

	public synchronized ArrayList<AuctionObject> getWonAuctions() throws RemoteException
	{
		ArrayList<AuctionObject> created = new ArrayList<>();
		String sql_request = "SELECT  username , num_auctions_won FROM (SELECT username , num_auctions_won FROM client ORDER BY num_auctions_won DESC) WHERE rownum <= 5";
		PreparedStatement ps;
		ResultSet rs;

		try{
			System.out.println("meto 2:");
			ps = conn.prepareStatement(sql_request);
			rs = ps.executeQuery();
			System.out.println("entreiii");
			while(rs.next())
			{
				String username = rs.getString("USERNAME");
				int num_auctions_won = rs.getInt("NUM_AUCTIONS_won");
				System.out.print("username:"+num_auctions_won);
				System.out.println("count:"+username);
				AuctionObject ob = new AuctionObject(num_auctions_won, username);
				created.add(ob);
			}
		}catch (Exception e) {}
		return created;

	}

	public synchronized ArrayList<AuctionObject> getAuctions10() throws RemoteException
	{
		String sql_request3 = "SELECT auctionid , title FROM auction WHERE deadline > ? - 10 AND deadline < ?";
		ArrayList<AuctionObject> daysAuctions = new ArrayList<>();
		PreparedStatement ps;
		ResultSet rs;

		try{
			System.out.println("meto 3:");


			System.out.println("time:");

			Timestamp b= new Timestamp(System.currentTimeMillis());
			ps = conn.prepareStatement(sql_request3);
			ps.setTimestamp(1, b);
			ps.setTimestamp(2, b);
			rs = ps.executeQuery();

			while(rs.next())
			{
				//System.out.println("entrou:"+dateFormat.format(cal.getTime()));
				String tittle = rs.getString("title");
				int auctionID = rs.getInt("auctionid");
				AuctionObject ob = new AuctionObject(auctionID, tittle);
				daysAuctions.add(ob);
			}
			return daysAuctions;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public synchronized Boolean deleteUser(BanUser obj) throws RemoteException
	{
		String sql_request = "DELETE FROM client WHERE userid = ?";
		PreparedStatement prepStat;

		try
		{

			prepStat = conn.prepareStatement(sql_request);
			prepStat.setInt(1, obj.getUserID());
			prepStat.executeUpdate();
			update_database();
			System.out.println("User Deleted");
			return true;

		} catch(SQLException se)
		{
			System.out.println("DELETE USER:"+se.getMessage());
			return false;
		}


	}

	public synchronized ArrayList<AuctionObjectBan> findMinBid(BanUser obj) throws RemoteException
	{
		String sql_request3 = "SELECT auctionid , Min(amount) FROM bid WHERE userid = ? GROUP BY auctionid";
		ArrayList<AuctionObjectBan> daysAuctions = new ArrayList<>();
		PreparedStatement ps;
		ResultSet rs;

		try{

			ps = conn.prepareStatement(sql_request3);
			ps.setInt(1, obj.getUserID());
			rs = ps.executeQuery();

			while(rs.next())
			{
				int auctionid = rs.getInt("auctionid");
				int amount = rs.getInt("Min(amount)");
				System.out.print("cena:"+auctionid);
				System.out.println("cena:"+amount);
				AuctionObjectBan ob = new AuctionObjectBan(auctionid, amount);
				daysAuctions.add(ob);
			}
			return daysAuctions;

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;

	}

	public synchronized boolean deleteBids(int auctionid, int minbid)
	{

		String sql_request = "DELETE FROM bid WHERE auctionid = ? AND amount >= ?";
		PreparedStatement prepStat;

		try
		{

			prepStat = conn.prepareStatement(sql_request);
			prepStat.setLong(1, auctionid);
			prepStat.setLong(2, minbid);
			prepStat.executeUpdate();
			//update_database();
			System.out.println("Bids Deleted");


		} catch(SQLException se)
		{
			System.out.println("DELETE BIDS:"+se.getMessage());
			return false;
		}
		return true;
	}

	public synchronized boolean cancelAllUserProjects(BanUser obj)
	{
		String sql_request = "UPDATE AUCTION SET status=  0 WHERE onwner = ?";
		PreparedStatement prepStat;

		try
		{

			prepStat = conn.prepareStatement(sql_request);
			prepStat.setInt(1, obj.getUserID());
			prepStat.executeUpdate();
			//update_database();
			System.out.println("AUCTION CANCELED");


		} catch(SQLException se)
		{
			System.out.println("AUCTION CANCELED:"+se.getMessage());
			return false;
		}
		return true;

	}


	public synchronized boolean doALlStuffOfBanUser (HashMap<String, String> obj)
	{

		int i;
		ArrayList<AuctionObjectBan> res;
		BanUser user = new BanUser(Integer.parseInt(obj.get("userid")));
		try {
			res = findMinBid(user);
			System.out.println(res);
			cancelAllUserProjects(user);


			for(i = 0;i<res.size();i++)
			{
				deleteBids(res.get(i).getAuctionID(), res.get(i).getAmount());
			}

			deleteUser(user);
			return true;

		} catch (RemoteException e1) {
			e1.printStackTrace();
		}


		return false;

	}

	public synchronized boolean auction_up_to_date()
	{
		String sql_request = "UPDATE AUCTION SET status = 0 WHERE deadline <= ?";
		PreparedStatement prepStat;

		try
		{
			Timestamp b= new Timestamp(System.currentTimeMillis());
			prepStat = conn.prepareStatement(sql_request);
			prepStat.setTimestamp(1, b);
			prepStat.executeUpdate();
			//update_database();
			System.out.println("AUCTION ACABOU");


		} catch(SQLException se)
		{
			System.out.println("AUCTION NAO ACABOU:"+se.getMessage());
			return false;
		}
		return true;

	}


}



