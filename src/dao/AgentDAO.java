package dao;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;

import util.ZKUtil;

public class AgentDAO {
	ZooKeeper zk = null;

	public AgentDAO() {
		zk = ZKUtil.createConnection(ZKUtil.CONNECTION_STRING, ZKUtil.SESSION_TIMEOUT);
	}

	public Stat isExist(String path) {
		try {
			return zk.exists(path, true);
		} catch (Exception e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		}
		return null;
	}

	public boolean createPath(String path, String data) {
		try {
			zk.create(path, data.getBytes(), Ids.OPEN_ACL_UNSAFE,
					CreateMode.EPHEMERAL);
		} catch (KeeperException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		} catch (InterruptedException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		}
		return true;

	}

	public boolean writeData(String path, String data) {
		try {
			zk.setData(path, data.getBytes(), -1);
		} catch (KeeperException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		} catch (InterruptedException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		}
		return false;
	}

	public String readData(String path) {
		try {
			return new String(zk.getData(path, false, null)); // 注意这个null，这里可以设置watcher
		} catch (KeeperException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		} catch (InterruptedException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		}
		return null;
	}

	public void deleteNode(String path) {
		try {
			zk.delete(path, -1);
		} catch (KeeperException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		} catch (InterruptedException e) {
			ZKUtil.writeIntoRedis(path, e.getClass());
		}
	}

}
