package gossip;

import logger.LogType;
import logger.Logger;
import org.json.simple.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

public class MemberManager {

  private ClusterInfo cluster_info;
//  private ArrayList<Member> memberList;
  private ArrayList<Member> deadList;
  private Member me = null;
  private Node   node;
  private Logger log;
  private Random random;
  private Config config = Gossip.config;

  public MemberManager(Node n, Logger l) {
    node = n;
    log = l;
    random = new Random();
    cluster_info = new ClusterInfo(l);
    deadList = new ArrayList<Member>();
  }

  synchronized public String getMasterAddr() {
    for (Member m : cluster_info.getMemberList()) {
      if (m.isMaster()) {
        return m.getAddress();
      }
    }
    return null;
  }

  /**
   * Merge remote list (received from peer), and our local member list.
   * Simply, we must update the heartbeats that the remote list has with
   * our list.  Also, some additional logic is needed to make sure we have
   * not timed out a member and then immediately received a list with that
   * member.
   *
   * @param info Cluster info from remote node
   */
  synchronized public void mergeLists(ClusterInfo info) {
    ArrayList<Member> remoteList = info.getMemberList();
    // Iterate through every member of the remote list
    for (Member remoteMember : remoteList) {
      if (cluster_info.hasMember(remoteMember)) {
        // The remote member matches a local member. Synchronize heartbeats.
        log.addInfo("MERGE: Synchronize remote member - " + remoteMember.getAddress());
        synchronizeHeartbeats(remoteMember);
        synchronizeMasterStatus(remoteMember);
      } else {
        // the local list does not contain the remote member

        // the remote member is either brand new, or a previously declared dead member
        // if its dead, check the heartbeat because it may have come back from the dead

        if (deadList.contains(remoteMember)) {
          // Remote member is in the deadlist. Determine if it has come back to life.
          Member localDeadMember = deadList.get(deadList.indexOf(remoteMember));
          if (remoteMember.getHeartbeat() > localDeadMember.getHeartbeat()) {
            // it's baa-aack
            log.addInfo("MERGE: Revive member");
            reviveMember(localDeadMember, remoteMember);
          } // else ignore
        } else {
          // brand spanking new member - welcome
          log.addInfo("MERGE: Add remote member - " + remoteMember);
          insertMember(remoteMember, false);
        }
      }
    }
    cluster_info.electMaster(info);
  }

  
  /**
   * Get member object for myself
   * @return Member object for myself
   */
  public Member getMe() { return me; }

  /**
   * Add new member to list.
   * @param newMember New member object.
   */
  synchronized public void addNewMember(Member newMember, boolean addAsMe) {
    if (cluster_info.hasMember(newMember)) {
      log.addWarning("ADD: Could not add duplicate member - " + newMember);
    } else {
      insertMember(newMember, addAsMe);
    }
  }

  /**
   * Clone member object and add to list. It does NOT check for duplicated
   * @param newMember New member
   */
  synchronized private void insertMember(Member newMember, boolean addAsMe) {
    Member newLocalMember = new Member(newMember.getAddress(), // IP Address
                                       newMember.getHeartbeat(), // Heartbeat
                                       node, // Node object
                                       config.GOSSIP_CLEAN, // Time to cleanup
                                       newMember.getTimestamp()); // Created at timestamp
    cluster_info.addMember(newLocalMember);
    if (!addAsMe) {
      newLocalMember.startTimeoutTimer();
    } else {
      me = newLocalMember;
    }
    log.addInfo("ADD: Added new member to list - " + newLocalMember);
  }

  /**
   * Synchronize heartbeats of remote member and matching local member.
   * @param remoteMember Remove member.
   */
  synchronized public void synchronizeHeartbeats(Member remoteMember) {
    Member localMember = cluster_info.getMatchingMember(remoteMember);
    if (remoteMember.getHeartbeat() > localMember.getHeartbeat()) {
      // update local list with latest heartbeat
      localMember.setHeartbeat(remoteMember.getHeartbeat());
      // and reset the timeout of that member
      localMember.resetTimeoutTimer();
    }
  }

  synchronized public void synchronizeMasterStatus(Member remote) {
    if (remote.isMaster()) {
      Member localMember = cluster_info.getMatchingMember(remote);
      localMember.setAsMaster();
    }
  }

  /**
   * Bring member back from the dead list. Update its heartbeats to match the remote member.
   * @param localDeadMember Local member in dead list.
   * @param remoteMember Remote member.
   */
  synchronized public void reviveMember(Member localDeadMember, Member remoteMember) {
    deadList.remove(localDeadMember);
    Member newLocalMember = new Member(remoteMember.getAddress(),
                                       remoteMember.getHeartbeat(),
                                       node,
                                       config.GOSSIP_CLEAN,
                                      localDeadMember.getTimestamp());
    cluster_info.addMember(newLocalMember);
    newLocalMember.startTimeoutTimer();
  }

  @SuppressWarnings("unchecked")
  synchronized public JSONArray getMembersJSON() {
    ArrayList<Member> memberList = cluster_info.getMemberList();
    JSONArray json = new JSONArray();
    for (Member m : memberList) {
      json.add(m.toJSON(m == me));
    }
    return json;
  }

  /**
   * Send member list to a random member
   */
  synchronized public void sendClusterInfo() {
    // Increase my own heartbeat
	if (me == null) {
		log.addError("SEND: I am null!");
	}
    me.setHeartbeat(me.getHeartbeat() + 1);

    // Send member list to random member
    try {
      Member member = getRandomMember();

      if (member != null) {
        // Convert member list into byte array
        byte[] buf = ClusterInfo.serializeClusterInfo(cluster_info);
        if (buf.length > config.PACKET_SIZE) {
          log.addError("Member list is larger than packet size");
        }

        // Get address info of target member
        String address = member.getAddress();
        String host = address.split(":")[0];
        int port = Integer.parseInt(address.split(":")[1]);

        InetAddress dest = InetAddress.getByName(host);
        log.addInfo("SEND: Sending member list to - " + dest);

        //simulate some packet loss ~25%
        int percentToSend = random.nextInt(100);
        if (percentToSend > 25) {
          DatagramSocket socket = new DatagramSocket();
          DatagramPacket datagramPacket = new DatagramPacket(buf, buf.length, dest, port);
          socket.send(datagramPacket);
          socket.close();
        }
      }

    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  /**
   * Find a random peer from the local membership list.
   * Ensure that we do not select ourselves, and keep
   * trying 10 times if we do.  Therefore, in the case
   * where this client is the only member in the list,
   * this method will return null
   *
   * @return Member random member if list is greater than 1, null otherwise
   */
  synchronized private Member getRandomMember() {
    Member member = null;

    if (cluster_info.getMemberCount() > 1) {
      int tries = 10;
      do {
        int randomNeighborIndex = random.nextInt(cluster_info.getMemberCount() - 1) + 1;
        member = cluster_info.getMember(randomNeighborIndex);
        if (--tries <= 0) {
          member = null;
          break;
        }
      } while (member.getAddress().equals(me.getAddress()));
    } else {
      log.addInfo("ALONE: I'm alone in this world.");
    }

    return member;
  }

  synchronized public void killMember(Member m) {
    cluster_info.removeMember(m);
    deadList.add(m);
    log.addInfo("KILL: killed member - " + m);
  }

  synchronized public boolean hasElected() {
    return cluster_info.hasElectedMaster();
  }
}
