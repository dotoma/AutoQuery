import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class VQueryBrowser{
    public static void main (String args[]){
	Connection con = null;
	try {
	    Class.forName("com.mysql.jdbc.Driver");
	    con = DriverManager.getConnection("jdbc:mysql://localhost:3312", "MAD_exp", "altUnsyint");
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
}