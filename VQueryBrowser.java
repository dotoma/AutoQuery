import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.TreeSet;
import java.util.SortedSet;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class VQueryBrowser {
    public static void main (String args[]){
	Connection con = null;
	try {
	    loadDriver();
	    con = newConnection();
	    Statement st = con.createStatement();
	    String query = "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION";
	    ResultSet rs = st.executeQuery(query);
	    TreeMap tm = makeTree(rs);
	    makeWindow(tm);

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

    static void makeWindow(TreeMap comp){
	MaFenetre f = new MaFenetre(comp);
    }

    static TreeMap makeTree(ResultSet rs) throws SQLException{
	TreeMap <String, TreeMap<String, TreeSet<String>> > tm_schemas = new TreeMap();
	while (rs.next()) {
	    String s_schema = rs.getString("TABLE_SCHEMA");
	    String s_table = rs.getString("TABLE_NAME");
	    String s_champ = rs.getString("COLUMN_NAME");
	    if ( ! tm_schemas.containsKey(s_schema)){
		/* schéma pas encore répertorié */
		tm_schemas.put(s_schema, new TreeMap<String, TreeSet<String>>());
		System.out.println("Ajout du schéma : " + s_schema);
	    }
	    
    	    if ( ! tm_schemas.get(s_schema).containsKey(s_table)){
		/* table pas encore répertorié */
		tm_schemas.get(s_schema).put(s_table, new TreeSet<String>());
		System.out.println("\t Ajout de la table : " + s_table);
	    }

	    tm_schemas.get(s_schema).get(s_table).add(s_champ);
	    System.out.println("\t\t Ajout du champ : " + s_champ);
      	}

	return tm_schemas;
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


class MaFenetre extends Frame implements KeyListener {
    TextArea ta;
    TreeMap<String, TreeMap> completor;
    MaFenetre(TreeMap comp){
	super("Essai");
	ta = new TextArea();
	ta.addKeyListener(this);
	ta.setFocusTraversalKeysEnabled(false);
	add(ta);
	setSize(400, 300);
	setVisible(true);
	completor = comp;    
	addWindowListener(
			  new WindowAdapter() {
			      public void windowClosing(WindowEvent e) {
				  System.exit(0);
			      }
			  });
    }
			  

    public void keyTyped(KeyEvent evt){}
    public void keyPressed(KeyEvent evt){	if (evt.getKeyCode() == KeyEvent.VK_TAB){
	    evt.consume();
	    String s_ta = ta.getText();
	    

	    /* Récupère le mot entre l'espace précédant le curseur et le curseur. S'il n'y a pas d'espace, ça prend toute la sous-chaîne gauche jusqu'au curseur. */
	    /* <----a-------> <---b---->
	       _ _ _ _ _ PREF| _ _ _ _ _ 
	                  curseur
			  = i_caret 
	    */
	    int i_caret = ta.getCaretPosition(); // Enregistre la position du curseur
	    String s_pref = s_ta.substring(0, i_caret); // Chaîne a
	    String s_suff = s_ta.substring(i_caret, s_ta.length()); // Chaîne b 
	    int i_debut = Math.max(s_pref.lastIndexOf(" ")+1, 0);
	    String s_extract = s_pref.substring(i_debut, i_caret);
	    System.out.println("Tentative de complétion à partir de '" + s_extract + "'.");

	    /* Compte le nombre de points pour savoir s'il faut compléter un nom de schéma, de table ou de champ */
	    int dotCount = s_extract.replaceAll("[^.]", "").length();
	    System.out.println("Nombre de points : " + dotCount);
	    
	    String s_insert = null;
	    String s_schema = null;
	    String s_to_complete = null;
	    String s_table = null;

	    switch(dotCount){
	    case 0: 
		System.out.println("Complétion de schéma");
		s_to_complete = s_extract;
		s_insert = complete(completor, s_to_complete);
		System.out.println("Cherche à insérer : " + s_insert);
		break;
	    case 1:
		System.out.println("Complétion de table");
		s_schema = extract(s_extract, 1);
		s_to_complete = extract(s_extract, 2);
		s_insert = complete(completor.get(s_schema), s_to_complete);
		System.out.println("Cherche à insérer : " + s_insert);
		break;
	    case 2:
		System.out.println("Complétion du champ");
		s_schema = extract(s_extract, 1);
		s_table = extract(s_extract, 2);
		s_to_complete = extract(s_extract, 3);
		s_insert = complete((SortedSet) completor.get(s_schema).get(s_table), s_to_complete);
		System.out.println("Cherche à insérer : " + s_insert + " à partir de " + s_to_complete);
		break;		
	    }

	    if (s_insert != null){
		ta.setText(s_pref + s_insert.substring(s_to_complete.length()) + s_suff); // Insère la complétion
		ta.setCaretPosition( ta.getText().length() - (s_ta.length() - i_caret)); // Remet le curseur où il était avant la complétion
	    }	    
	    System.out.println("TAB appuyé");
	}
    }

    public void keyReleased(KeyEvent evt){}
    
    private <V> SortedMap<String, V> filterPrefix(SortedMap<String, V> baseMap, String prefix){
	if(prefix.length() > 0){
	    char nextLetter = (char) (prefix.charAt(prefix.length()-1)+1);
	    String end = prefix.substring(0, prefix.length()-1) + nextLetter;
	    return baseMap.subMap(prefix, end);
	}
	return baseMap;
    }

    private SortedSet<String> filterPrefix(SortedSet<String> baseSet, String prefix){
	if(prefix.length() > 0){
	    char nextLetter = (char) (prefix.charAt(prefix.length()-1)+1);
	    String end = prefix.substring(0, prefix.length()-1) + nextLetter;
	    System.out.println("Éléments entre " + prefix + " et " + end);
	    return baseSet.subSet(prefix, end);
	}
	return baseSet;
    }

    
    String extract(String s, int i){
	return(s.split("\\.")[i-1]);
    }

    String complete(SortedMap tm, String s_pref){
	Object[] sa_suff = filterPrefix(tm, s_pref).keySet().toArray();	
	if (sa_suff.length == 1){
	    return ((String) sa_suff[0]);
	}
	return null;
    }

    String complete(SortedSet ss, String s_pref){
	Object[] sa_suff = filterPrefix(ss, s_pref).toArray();	
	if (sa_suff.length == 1){
	    return ((String) sa_suff[0]);
	} 
	return null;
    }


    
}
