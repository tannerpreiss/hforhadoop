package logger;

import gossip.Config;
import gossip.Node;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;

public class Logger {

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

  private Node             myNode;
  private ArrayList<Event> events;
  private boolean printToConsole = true;

  public Logger(boolean printToConsole, boolean enableServer, int serverPort) {
    this.printToConsole = printToConsole;

    if (enableServer) {
      System.out.println("Start log server");
      try {
        LogServer server = new LogServer(this, serverPort);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    events = new ArrayList<Event>();
  }

  synchronized public void addEvent(LogType type, String str) {
    str = str.replaceAll("(\r\n|\n)", "<br />");
    Event e = new Event(type, str);
    if (printToConsole) {
      System.out.println(e);
    }
    events.add(e);
  }

  synchronized public void addInfo(String str) {
    addEvent(LogType.INFO, str);
  }

  synchronized public void addWarning(String str) { addEvent(LogType.WARNING, str); }

  synchronized public void addError(String str) { addEvent(LogType.ERROR, str); }

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
}
