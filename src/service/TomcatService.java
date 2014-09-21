package service;

import idao.AgentInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import dao.AgentDAO;

public class TomcatService extends AgentDAO implements AgentInterface, Runnable {
	
	public static int TOMCATNUMBERBER = 6;
	public Process process = null;
	String hostname = null;
	Boolean deployResult = false;
	String[] array = readData("/header").split(";");
	String[] webs = array[3].split("->");
	@Override
	public void processData() {
		Thread t = new Thread(new TomcatService());
		t.start();
	}

	@Override
	public void run() {
		while (true) {
			try{
				if (isExist("/header") != null) {
					
					process = Runtime.getRuntime().exec("hostname");
					hostname = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
					if(array[0].equals(hostname)){
						if(array[1].equals("update")){
							Runtime.getRuntime().exec("mv -r /usr/local/tomcat/webapp/" + webs[0] + " ~/backup/");
							Runtime.getRuntime().exec("rm /usr/local/tomcat/webapp/" + webs[1]);
							Runtime.getRuntime().exec("cp ~/tmp/" + webs[1] + " /usr/local/tomcat/webapp/");
							if(isDeployMentSuccess() == true){
								Runtime.getRuntime().exec("scp /tmp/" + webs[1] + " soft01@***" + " ~/tmp/");
							}else {//rollback
								Runtime.getRuntime().exec("rm /usr/local/tomcat/webapp/" + webs[1]);
								Runtime.getRuntime().exec("mv -r ~/backup/" + webs[0] + " /usr/local/tomcat/webapp/");
							}
						}else if(array[1].equals("config")){
							
						}
					}else {
					}
				}else {
				}
			} catch(Exception e){
			}
		}
	}
	public boolean isDeployMentSuccess(){
		try{
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					Thread.sleep(3000);
					deployResult = new File("/usr/local/tomcat/webapp/"+webs[1]).isDirectory();
					if(deployResult==true){
						break;
					}
				}
				if(deployResult==true){
					break;
				}
				Runtime.getRuntime().exec("rm /usr/local/tomcat/webapp/" + webs[1]);
				Runtime.getRuntime().exec("cp ~/tmp/" + webs[1] + " /usr/local/tomcat/webapp/");
			}
		}catch(Exception e){
			
		}
		return deployResult;
	}
}
