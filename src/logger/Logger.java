package logger;

import gossip.Config;
import gossip.Gossip;
import gossip.Node;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Logger {

  private Node             myNode;
  private ArrayList<Event> events;
  private boolean printToConsole = true;
  private HashMap<String, Boolean> state;
  private Config      config   = Gossip.config;
  private InetAddress hostAddr = null;
  private int         hostPort = 0;
  private LogServer   server   = null;

  public Logger(boolean printToConsole, boolean enableServer, int serverPort) {
    this.printToConsole = printToConsole;
    events = new ArrayList<Event>();
    state = new HashMap<String, Boolean>();
    state.put("gossip", true);
    state.put("in_group", false);
    state.put("master_elected", false);
    state.put("hadoop_started", false);

    if (enableServer) {
      this.addInfo("LOGGER: Start log server");
      try {
        server = new LogServer(this, serverPort);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  synchronized public void addEvent(LogType type, String str) {
    str = str.replaceAll("(\r\n|\n)", "<br />");
    Event e = new Event(type, str);
    events.add(e);
    if (printToConsole) {
      if (!config.PRINT_DEBUG && type == LogType.DEBUG) { return; }
      System.out.println(e);
    }
  }

  synchronized public void markInGroup() {
    state.put("in_group", true);
    sendMessage("in_group");
  }

  synchronized public void markElected() {
    state.put("master_elected", true);
    sendMessage("master_elected");
  }

  synchronized public void markHadoop() {
    state.put("hadoop_started", true);
    sendMessage("hadoop_started");
  }

  synchronized public void sendMessage(String msg) {
    this.addInfo("LOGGER: Sending event message - " + msg);
    server.msg = msg;
    synchronized (this) {
      this.notify();
    }
  }

  synchronized public void addDebug(String str) { addEvent(LogType.DEBUG, str); }

  synchronized public void addInfo(String str) { addEvent(LogType.INFO, str); }

  synchronized public void addWarning(String str) { addEvent(LogType.WARNING, str); }

  synchronized public void addError(String str) { addEvent(LogType.ERROR, str); }

  synchronized public void setHostAddr(InetAddress addr) { hostAddr = addr; }

  synchronized public void setHostPort(int port) { hostPort = port; }

  synchronized public InetAddress getHostAddr() { return hostAddr; }

  synchronized public int getHostPort() { return hostPort; }

  public void setNodeObj(Node n) { myNode = n; }

  public JSONArray getMembersJSON() {
    return myNode.getMemberManager().getMembersJSON();
  }

  @SuppressWarnings("unchecked")
  synchronized public JSONArray getEventsJSON() {
    JSONArray json = new JSONArray();
    for (Event e : events) {
      JSONObject obj = new JSONObject();
      obj.put("log_type", e.type.name());
      obj.put("message", e.msg);
      json.add(obj);
    }
    events.clear();
    return json;
  }

  public enum LogType {
    DEBUG,
    INFO,
    WARNING,
    ERROR;
  }

  class Event {
    private LogType type;
    private String  msg;

    public Event(LogType logType, String message) {
      type = logType;
      msg = message;
    }

    @Override
    public String toString() {
      return type + "\t" + msg;
    }
  }
}
