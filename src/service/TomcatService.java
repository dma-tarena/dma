package service;
/*
 * 升级webApp项目: nginx1-tomcat1;update;/usr/local/tomcat/webapps/;old.war->new.war
 * 修改server配置: nginx1-tomcat1;config;/usr/local/tomcat/conf/server.xml;server,port,8005,8006
 */
import idao.AgentInterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import util.ZKUtil;
import dao.AgentDAO;

public class TomcatService extends AgentDAO implements AgentInterface {
	//
	public static final int NGINX_NMUBERS = 6;
	public static final int[] TOMCAT_NUMBERS = {4, 3, 5 , 5, 10, 2};
	public Process process;
	public String hostname;
	public Boolean deployResult = false;
	public String[] znodeContent;
	public String[] parameters;  
	
	@Override
	public void processData() {
		while (true) {
			try{
				if (isExist("/header") != null) {
					znodeContent = readData("/header").split(";");
					hostname = InetAddress.getLocalHost().getHostName();
					if(znodeContent[0].equals(hostname)){
						if(znodeContent[1].equals("update")){
							parameters = znodeContent[3].split("->");
							Runtime.getRuntime().exec("mv -r /usr/local/tomcat/webapps/" + parameters[0] + " ~/backup/");
							Runtime.getRuntime().exec("rm /usr/local/tomcat/webapps/" + parameters[1]);
							Runtime.getRuntime().exec("cp ~/tmp/" + parameters[1] + " /usr/local/tomcat/webapps/");
							if(isDeployMentSuccess() == true){
								
								sendToOthers();
							}else {//rollback
								Runtime.getRuntime().exec("rm /usr/local/tomcat/webapps/" + parameters[1]);
								Runtime.getRuntime().exec("mv -r ~/backup/" + parameters[0] + " /usr/local/tomcat/webapps/");
							}
						}else if(znodeContent[1].equals("config")){
							parameters = znodeContent[3].split(",");
							for (int i = 0; i < 3; i++) {
								boolean modifyResult = modifyXml(znodeContent[2], parameters[0], parameters[1], parameters[2], parameters[3]);
								if(modifyResult == true) {
									Runtime.getRuntime().exec("sh /usr/local/tomcat/bin/shutdown.sh");
									Runtime.getRuntime().exec("sh /usr/local/tomcat/bin/startup.sh");
									ZKUtil.writeIntoRedis(hostname, "1,,");//CPU,RAM
									break;
								}
							}
						}
					}
				}
				
				ZKUtil.writeIntoRedis(hostname, isTomcatAlive() + "," + sysInfo());//CPU,RAM
						
				Thread.sleep(3000);
			} catch(Exception e){
				
			}
		}
	}
	public int isTomcatAlive(){
		try {
			Process process = Runtime.getRuntime().exec("ps -ef | grep tomcat");
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			//判断过程
			return 1;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;
		
	}
	public String sysInfo(){
		Sigar sigar = new Sigar();
		try {
			CpuInfo infos[] = sigar.getCpuInfoList();
			CpuPerc[] cpuList = sigar.getCpuPercList();
			double sum = 0;
			for (int i = 0; i < infos.length; i++) {// 不管是单块CPU还是多CPU都适用
				sum += cpuList[i].getCombined();
			}
			int cpuPerc = (int)(sum/infos.length*100);
			Mem mem = sigar.getMem();
			int memPerc = 0;
			if (mem != null && mem.getTotal() != 0) {
				memPerc = (int)(mem.getUsed()/mem.getTotal()*100);	
			}
			InetAddress addr = InetAddress.getLocalHost();
			return cpuPerc + "%," + memPerc + "%," + addr.getHostAddress();
			
		} catch (SigarException e) {
			e.printStackTrace();
		}catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	public boolean isDeployMentSuccess(){
		try{
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					Thread.sleep(3000);
					File file =  new File("/usr/local/tomcat/webapps/"+parameters[1]);
					if(file.isDirectory()==true){
						deployResult = true;
						break;
					}
				}
				if(deployResult==true){
					break;
				}
				Runtime.getRuntime().exec("rm /usr/local/tomcat/webapps/" + parameters[1]);
				Runtime.getRuntime().exec("cp ~/tmp/" + parameters[1] + " /usr/local/tomcat/webapps/");
				
			}
		}catch(Exception e){
			ZKUtil.writeIntoRedis(hostname, e.getClass());
		}
		return deployResult;
	}
	public void sendToOthers() throws IOException{
		for (int i = 0; i < NGINX_NMUBERS; i++) {
			for (int j = 0; j < TOMCAT_NUMBERS[i]; j++) {
				String hostname = "nginx" + i + "-tomcat" + j;
				Runtime.getRuntime().exec("scp ~/tmp/" + parameters[1] + " soft01@" + hostname + " ~/tmp/");
			}
		}
	}
	public boolean modifyXml(String path, String node, String attr, String oldValue, String newValue ) {
		try {
			Document doc = new SAXReader().read(new File(path));
			List<Element> list = doc.selectNodes("//"+node+"[@"+attr+"='"+oldValue+"']");
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
	public static void main(String[] args) {
		new TomcatService().processData();
	}
}
