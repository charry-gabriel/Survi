package fr.miuby.survi18.database;

public class DbCredentials {
    private String host;
    private String user;
    private String pass;
    private String dbName;

    public DbCredentials(String host, String user, String pass, String dbName) {
        this.host = host;
        this.user = user;
        this.pass = pass;
        this.dbName = dbName;
    }

    public String toURI() {
        final StringBuilder sb = new StringBuilder();

        sb.append("jdbc:mysql://")
                .append(host)
                .append(":3306")
                .append("/")
                .append(dbName);

        return sb.toString();
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }
}
