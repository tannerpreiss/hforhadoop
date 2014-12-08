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

      updateXMLWithNewMaster(master_addr);

      if (run_as_master.get()) {
        log.addInfo("HADOOP: Run hadoop as master!");
        startMasterHadoop();
      } else {
        log.addInfo("HADOOP: Run hadoop as slave!");
        startSlaveHadoop();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void updateXMLWithNewMaster(String new_master) {
    String[] words = new_master.split("\\.");
    int index = words[words.length - 1].indexOf(":");
    String word = words[words.length - 1].substring(0, index);
    int num = Integer.parseInt(word);
    String name = String.format("%03d", num);
    name= "slave" + name;
    log.addInfo("HADOOP: Updating XML to master - " + name);

    String findreplace_cmd = "python3 fix_xml_files.py -m " + new_master;

    Shell.executeCommand(findreplace_cmd);
  }

  public void startMasterHadoop() {
//    Shell temp_shell = new Shell();
//
//    //TODO: replace with master command
//    String start_master_cmd = "start-master.sh";

    Shell.executeCommand("start-master.sh");
  }

  public void startSlaveHadoop() {
//    Shell temp_shell = new Shell();
//
//    //TODO: replace with slave command
//    String start_slave_cmd = "start-slave.sh";

    Shell.executeCommand("start-slave.sh");

  }

  synchronized public void addMember(Member m) {
    log.addInfo("HADOOP: Add new member to file - " + m);
  }

  synchronized public void removeMember(Member m) {
    log.addInfo("HADOOP: Remove member from file - " + m);
  }

}
