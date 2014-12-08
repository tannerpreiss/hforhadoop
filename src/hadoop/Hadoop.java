package hadoop;

import gossip.Gossip;
import gossip.Member;
import gossip.MemberManager;
import logger.Logger;
import vm_control.Shell;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by elmer on 12/6/14.
 */
public class Hadoop implements Runnable {

  private Logger        log;
  private MemberManager memberManager;
  private AtomicBoolean run_as_master = new AtomicBoolean(false);
  private HadoopMonitor monitor;
  private AtomicBoolean is_hadoop_running = new AtomicBoolean(false);
  private String master_addr;

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

  synchronized public void setMasterAddr(String addr) {
    master_addr = addr;
  }

  public void run() {
    try {
      log.addInfo("HADOOP: Waiting for signal to run Hadoop...");
      synchronized (monitor) {
        monitor.wait();
      }
      log.addInfo("HADOOP: Signal received! Executing Hadoop.");
      log.markHadoop();

      if (run_as_master.get()) {
        log.addInfo("HADOOP: Run hadoop as master!");
        updateXMLWithNewMaster(master_addr);
        startMasterHadoop();

      } else {
        log.addInfo("HADOOP: Run hadoop as slave!");
        updateXMLWithNewMaster(master_addr);
        startSlaveHadoop();

      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void updateXMLWithNewMaster(String new_master) {
    Shell temp_shell = new Shell();

    String findreplace_cmd = "python3 findreplace.py -m " + new_master;

    temp_shell.executeCommand(findreplace_cmd);
  }

  public void startMasterHadoop() {
    Shell temp_shell = new Shell();

    //TODO: replace with master command
    String start_master_cmd = "python3 findreplace.py -m ";

    temp_shell.executeCommand(start_master_cmd);
  }

  public void startSlaveHadoop() {
    Shell temp_shell = new Shell();

    //TODO: replace with slave command
    String start_slave_cmd = "python3 findreplace.py -m ";

    temp_shell.executeCommand(start_slave_cmd);

  }

  synchronized public void addMember(Member m) {
    log.addInfo("HADOOP: Add new member to file - " + m);
  }

  synchronized public void removeMember(Member m) {
    log.addInfo("HADOOP: Remove member from file - " + m);
  }

}
