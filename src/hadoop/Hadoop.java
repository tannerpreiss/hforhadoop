package hadoop;

import gossip.Gossip;
import gossip.Member;
import gossip.MemberManager;
import logger.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by elmer on 12/6/14.
 */
public class Hadoop implements Runnable {

  private Logger        log;
  private MemberManager memberManager;
  private AtomicBoolean run_as_master = new AtomicBoolean(false);
  private HadoopMonitor monitor;
  public AtomicBoolean is_hadoop_running = new AtomicBoolean(false);

  private class HadoopMonitor {}

  public Hadoop(MemberManager m_manager, Logger l) {
    memberManager = m_manager;
    log = l;
    monitor = new HadoopMonitor();
  }

  synchronized public boolean isRunning() {
    return is_hadoop_running.get();
  }

  synchronized public void startHadoop() {
    is_hadoop_running.set(true);
    synchronized (monitor) {
      monitor.notify();
    }
  }

  synchronized public void setAsMaster() {
    run_as_master.set(true);
  }

  public void run() {
    try {
      log.addInfo("HADOOP: Waiting for signal to run Hadoop...");
      synchronized (monitor) {
        monitor.wait();
      }
      log.addInfo("HADOOP: Signal received! Executing Hadoop.");

      if (run_as_master.get()) {
        log.addInfo("HADOOP: Run hadoop as master!");
      } else {
        log.addInfo("HADOOP: Run hadoop as slave!");
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  synchronized public void addMember(Member m) {
    log.addInfo("HADOOP: Add new member to file - " + m);
  }

  synchronized public void removeMember(Member m) {
    log.addInfo("HADOOP: Remove member from file - " + m);
  }

}
