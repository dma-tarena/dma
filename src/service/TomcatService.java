package service;

import idao.AgentInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import dao.AgentDAO;

public class TomcatService extends AgentDAO implements AgentInterface, Runnable {
	//
	public static int TOMCATNUMBERBER = 6;
	public Process process = null;
	String hostname = null;
	Boolean deployResult = false;
	String[] array = readData("/header").split(";");
	String[] webs = array[3].split("->");//   /usr/local/tomcat/webapps/;update;old.war=>new.war
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
							
							Runtime.getRuntime().exec("mv -r /usr/local/tomcat/webapps/" + webs[0] + " ~/backup/");
							Runtime.getRuntime().exec("rm /usr/local/tomcat/webapps/" + webs[1]);
							Runtime.getRuntime().exec("cp ~/tmp/" + webs[1] + " /usr/local/tomcat/webapps/");
							if(isDeployMentSuccess() == true){
								Runtime.getRuntime().exec("scp /tmp/" + webs[1] + " soft01@***" + " ~/tmp/");
							}else {//rollback
								Runtime.getRuntime().exec("rm /usr/local/tomcat/webapps/" + webs[1]);
								Runtime.getRuntime().exec("mv -r ~/backup/" + webs[0] + " /usr/local/tomcat/webapps/");
							}
						}else if(array[1].equals("config")){
							String[] nodes =array[3].split(",");// /usr/local/tomcat/conf/server.xml;config;server,port,8005,8006
							for(int i=0;i<3;i++){
								boolean status=modifyXml(array[2], webs[0], webs[1], webs[2], webs[3]);
								if(status){
									Runtime.getRuntime().exec("sh /usr/local/tomcat/bin/shutdown.sh");
									Runtime.getRuntime().exec("sh /usr/local/tomcat/bin/startup.sh");
									break;
								}
							}
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
					deployResult = new File("/usr/local/tomcat/webapps/"+webs[1]).isDirectory();
					if(deployResult==true){
						break;
					}
				}
				if(deployResult==true){
					break;
				}
				Runtime.getRuntime().exec("rm /usr/local/tomcat/webapps/" + webs[1]);
				Runtime.getRuntime().exec("cp ~/tmp/" + webs[1] + " /usr/local/tomcat/webapps/");
			}
		}catch(Exception e){
			
		}
		return deployResult;
	}
	public boolean modifyXml(String path, String node, String attr, String oldValue, String newValue ) {
		try {
			Document doc = new SAXReader().read(new File(path));
			System.out.println(doc.getName());
			List<Element> list = doc.selectNodes("//"+node+"[@"+attr+"='"+oldValue+"']");
			System.out.println(list.size());
			for (Element element : list) {
				element.attribute(attr).setValue(newValue);
			}
			XMLWriter writer = new XMLWriter();
			writer.setOutputStream(new FileOutputStream(path));
			writer.write(doc);
			writer.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
