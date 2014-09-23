package util;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooKeeper;

import redis.clients.jedis.Jedis;

public class ZKUtil implements Watcher{
	
	public static final String host = "RedisIP";
	public static final int port = 6379;
	public static final int SESSION_TIMEOUT = 10000;
	public static final String CONNECTION_STRING = "slave1:2181,slave2:2181,slave3:2181";
	private static CountDownLatch connectedSemaphore = new CountDownLatch(1);
	
	public static ZooKeeper createConnection(String connectString, int sessionTimeout) {
		ZooKeeper zk = null;
		try {
			zk = new ZooKeeper(connectString, sessionTimeout, new ZKUtil());
			connectedSemaphore.await();
			return zk;
		} catch (InterruptedException e) {
			writeIntoRedis("zkConnection", e.getClass());
		} catch (IOException e) {
			writeIntoRedis("zkConnection", e.getCause());
		}
		return null;
	}

	@Override
	public void process(WatchedEvent event) {
		if (KeeperState.SyncConnected == event.getState()) {
			connectedSemaphore.countDown();
		}
	}
	
	public static void releaseConnection(ZooKeeper zk) {
		if (zk!=null) {
			try {
				zk.close();
			} catch (InterruptedException e) {
				writeIntoRedis("zkConnection", e.getClass());
			}
		}
	}
	
	public static void writeIntoRedis(String key, Object obj){
		Date date = new Date();
		Jedis jedis = new Jedis(host, port ); 
		String exceptionInfo = date.toString()+": "+obj.toString();
		jedis.set(key, exceptionInfo);
		jedis.close();
	}


}
