package cloud.app.vvf.network.api.alldebrid.model;

public class AllDebridUserInfor {
    public String avatar;
    public String email;
    public String expiration;
    public Integer id;
    public String locale;
    public Integer points;
    public Integer premium;
    public String type;
    public String username;

    public AllDebridUserInfor(String avatar, String email, String expiration, Integer id, String locale, Integer points, Integer premium, String type, String username) {
        this.avatar = avatar;
        this.email = email;
        this.expiration = expiration;
        this.id = id;
        this.locale = locale;
        this.points = points;
        this.premium = premium;
        this.type = type;
        this.username = username;
    }

    @Override
    public String toString() {
        return "AllDebridUserInfor{" +
                "avatar='" + avatar + '\'' +
                ", email='" + email + '\'' +
                ", expiration='" + expiration + '\'' +
                ", id=" + id +
                ", locale='" + locale + '\'' +
                ", points=" + points +
                ", premium=" + premium +
                ", type='" + type + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
