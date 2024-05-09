package server;

public class User {
    String name;
    String pass;
    private String room;

    public User(String name, String pass) {
        this.name = name;
        this.pass = pass;
    }

    public User(String name) {
        this.name = name;
    }

    public String getRoom() {
        return room;
    }

    
    public void setRoom(String room) {
        this.room = room;
    }
    
    
}
