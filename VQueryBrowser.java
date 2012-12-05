import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class VQueryBrowser{
    public static void main (String args[]){
	Connection con = null;
	try {
	    loadDriver();
	    con = newConnection();
	    if ( ! con.isClosed()){
		System.out.println("Connexion au serveur MySQL par TCP/IP...");
	    }
	} catch (Exception e){
	    System.err.println("Exception : " + e.getMessage());
	} finally {
	    try {
		if (con != null){
		    con.close();
		}
	    } catch (SQLException e) {}
	}
    }

    static void loadDriver() throws ClassNotFoundException {
	Class.forName("com.mysql.jdbc.Driver");
    }

    static Connection newConnection() throws SQLException {
	final String url = "jdbc:mysql://localhost:3312";
	Connection con = DriverManager.getConnection(url, "MAD_exp", "altUnsyint");
	return con;
    }
}