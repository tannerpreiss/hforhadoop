package gossip;

import logger.Logger;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by elmer on 12/5/14.
 */
public class ClusterInfo implements Serializable {

  private ArrayList<Member> memberList;
  private boolean           master_elected;
  private transient Config config = Gossip.config;
  private transient Logger log;

  public ClusterInfo(Logger l) {
    memberList = new ArrayList<Member>();
    master_elected = false;
    log = l;
  }

  synchronized public boolean hasElectedMaster() {
    return master_elected;
  }

  synchronized public boolean hasMember(Member remoteMember) {
    return memberList.contains(remoteMember);
  }

  synchronized public void addMember(Member newLocalMember) {
    memberList.add(newLocalMember);
  }

  synchronized public boolean removeMember(Member member) {
    return memberList.remove(member);
  }

  synchronized public Member getMember(int index) {
    return memberList.get(index);
  }

  synchronized public Member getMatchingMember(Member member) {
    return memberList.get(memberList.indexOf(member));
  }

  synchronized public ArrayList<Member> getMemberList() {
    return memberList;
  }

  synchronized public int getMemberCount() {
    return memberList.size();
  }

  synchronized public void electMaster(ClusterInfo remoteInfo) {
    if (master_elected) { return; }
    // Check if remote node has already elected a master
    if (remoteInfo.hasElectedMaster()) {
      log.addInfo("ELECT: Remote has already elected master. Update local.");
      master_elected = true;
    }
    // Check if this cluster CAN elect a master
    if (!master_elected && getMemberCount() >= config.NODE_THRESHOLD) {
      log.addInfo("ELECT: Cluster is ready to elect. Electing...");
      log.markElected();
      // Get the member with lowest time stamp.
      Member master = null;
      long low_time = Long.MAX_VALUE;
      for (Member m : memberList) {
        if (m.getTimestamp() < low_time) {
          low_time = m.getTimestamp();
          master = m;
        }
      }
      // Set master to member to lowest time
      if (master != null) {
        master.setAsMaster();
        master_elected = true;
        log.addInfo("ELECT: Master node elected - " + master.toString());
        log.markElected();
      }
    }
  }

  synchronized public String toString() {
    StringBuilder str = new StringBuilder();
    for (Member m : memberList) {
      str.append(m).append("\n");
    }
    return str.toString();
  }

  public static byte[] serializeClusterInfo(ClusterInfo info) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(info);
    return baos.toByteArray();
  }

  public static ClusterInfo deserializeClusterInfo(byte[] info) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bais = new ByteArrayInputStream(info);
    ObjectInputStream ois = new ObjectInputStream(bais);
    Object obj = ois.readObject();
    if (obj instanceof ClusterInfo) {
      return (ClusterInfo)obj;
    } else {
      throw new ClassNotFoundException("Class is not of type ClusterInfo");
    }
  }

}
