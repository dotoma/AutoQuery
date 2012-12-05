import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Vector;

public class VQueryBrowser{
    public static void main (String args[]){
	Connection con = null;
	try {
	    loadDriver();
	    con = newConnection();
	    Statement st = con.createStatement();
	    String query = "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION";
	    ResultSet rs = st.executeQuery(query);
	    makeTree(rs);

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

    static HashMap makeTree(ResultSet rs) throws SQLException{
	HashMap <String, HashMap<String, Vector<String>> > hm_schemas = new HashMap();
	while (rs.next()) {
	    String s_schema = rs.getString("TABLE_SCHEMA");
	    String s_table = rs.getString("TABLE_NAME");
	    String s_champ = rs.getString("COLUMN_NAME");
	    if ( ! hm_schemas.containsKey(s_schema)){
		/* schéma pas encore répertorié */
		hm_schemas.put(s_schema, new HashMap<String, Vector<String>>());
		System.out.println("Ajout du schéma : " + s_schema);
	    }
	    
    	    if ( ! hm_schemas.get(s_schema).containsKey(s_table)){
		/* table pas encore répertorié */
		hm_schemas.get(s_schema).put(s_table, new Vector<String>());
		System.out.println("\t Ajout de la table : " + s_table);
	    }

	    hm_schemas.get(s_schema).get(s_table).add(s_champ);
	    System.out.println("\t\t Ajout du champ : " + s_champ);
      	}

	return hm_schemas;
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