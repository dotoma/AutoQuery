import java.sql.*;
import java.sql.ResultSet;

public class VQueryBrowser{
    public static void main (String args[]){
	Connection con = null;
	try {
	    loadDriver();
	    con = newConnection();
	    Statement st = con.createStatement();
	    String query = "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS ORDER BY ORDINAL_POSITION";
	    ResultSet rs = st.executeQuery(query);
	    while (rs.next()) {
		System.out.println(rs.getString("TABLE_SCHEMA") + " " + rs.getString("TABLE_NAME") + " " + rs.getString("COLUMN_NAME"));
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