package SerializationClasses;

public class Message {
    private String username;
    private String date;
    private String msg;

    public Message(String username, String msg, String date) {
        this.username = username;
        this.date = date;
        this.msg = msg;
    }

    public String getName() {
        return this.username;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
