package actions;

import org.apache.struts2.interceptor.SessionAware;

import java.rmi.RemoteException;
import java.util.Map;
import com.opensymphony.xwork2.ActionSupport;


import models.LoginBean;

public class LoginAction extends ActionSupport implements SessionAware {

	private String username = null;
	private String password = null;
	private Map<String, Object> session;

	public String execute() throws RemoteException
	{
	
		if(this.username != null && !username.equals("") && this.password != null && !password.equals(""))
		{
			this.getLoginBean().setUsername(this.username);
			this.getLoginBean().setPassword(this.password);
			
			if(this.getLoginBean().getLoginValidation() == 1)
			{
				session.put("username", username);
				session.put("loggedin", true); // this marks the user as logged in
				return SUCCESS;	
			}
			
			return LOGIN;
			
		}
		return LOGIN;
		
		
	}

	
	public LoginBean getLoginBean() {
		if(!session.containsKey("loginBean"))
			this.setHeyBean(new LoginBean());
		return (LoginBean) session.get("loginBean");
	}
	

	public void setHeyBean(LoginBean heyBean) {
		this.session.put("loginBean", heyBean);
	}

	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}


	@Override
	public void setSession(Map<String, Object> session) {
		this.session = session;
	}
	
}
