package ma.ensa.dentaireprojet;

public class User {
    private static int lastId = 0;
    private int id;
    private String userName;
    private String firstName;
    private String lastName;
    private String password;
    private String image;

    public User(String username, String firstname, String lastname, String password, String image) {
        this.id = ++lastId;
        this.userName = username;
        this.firstName = firstname;
        this.lastName = lastname;
        this.password = password;
        this.image = image;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return userName;
    }

    public void setUsername(String username) {
        this.userName = username;
    }

    public String getFirstname() {
        return firstName;
    }

    public void setFirstname(String firstname) {
        this.firstName = firstname;
    }

    public String getLastname() {
        return lastName;
    }

    public void setLastname(String lastname) {
        this.lastName = lastname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}
