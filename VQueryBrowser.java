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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class VQueryBrowser {
    public static void main (String args[]){
	Connection con = null;
	try {
	    loadDriver();
	    if (args.length == 3){
		con = newConnection(args[1], args[2]);
	    } else {
		con = newConnection("prod-bdd-mono-master-read", "3306");
	    }
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
    
    /* Crée l'arbre pour la complétion */
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
		/* table pas encore répertoriée */
		tm_schemas.get(s_schema).put(s_table, new TreeSet<String>());
		System.out.println("\t Ajout de la table : " + s_table);
	    }
	    tm_schemas.get(s_schema).get(s_table).add(s_champ);
	    System.out.println("\t\t Ajout du champ : " + s_champ);
      	}
	return tm_schemas;
    }


    /* Charge le driver pour communiquer avec la base de données */
    private static void loadDriver() throws ClassNotFoundException {
	Class.forName("com.mysql.jdbc.Driver");
    }

    private static Connection newConnection(String host, String port) throws SQLException {
	final String url = "jdbc:mysql://" + host + ":" + port;
	Connection con = DriverManager.getConnection(url, "MAD_exp", "altUnsyint");
	return con;
    }
}


class MaFenetre extends Frame implements KeyListener {
    TextArea ta;
    TreeMap<String, TreeMap> completor;
    TreeMap<String, TreeSet> tm_alias;
    MaFenetre(TreeMap comp){
	super("VQueryBrowser");
	completor = comp;    
	tm_alias = new TreeMap();
	ta = new TextArea();
	ta.addKeyListener(this);
	ta.setFocusTraversalKeysEnabled(false);
	add(ta);
	setSize(400, 300);
	setVisible(true);
	addWindowListener(
			  new WindowAdapter() {
			      public void windowClosing(WindowEvent e) {
				  System.exit(0);
			      }
			  });
    }
			  

    
    public void keyTyped(KeyEvent evt){}
    public void keyPressed(KeyEvent evt){
	/* Essaie de lire l'alias si combinaison CTRL + R */
	if (evt.getKeyCode() == KeyEvent.VK_R && evt.getKeyModifiersText(evt.getModifiers()).equals("Ctrl") ) {
	    int i_caret = ta.getCaretPosition();
	    String s_ta = ta.getText();
	    String[] sa_words = s_ta.substring(0, i_caret).split(" ");
	    System.out.println("Il y a " + sa_words.length + " mots.");
	    /* Trouve les deux (trois s'il y a un alias) derniers mots */ 
	    boolean b_alias_trouve = false;
	    boolean b_chemin_trouve = false;
	    int i_alias = -1;
	    int i_chemin = -1;
	    int i = sa_words.length - 1;
	    while (i >= 0 && (!b_alias_trouve || !b_chemin_trouve)){
		if (sa_words[i].equals("") || sa_words[i].equalsIgnoreCase("as")){
		    i--;
		    continue;
		}

		if (!b_alias_trouve){
		    b_alias_trouve = true;
		    i_alias = i;
		    i--;
		} else {
		    b_chemin_trouve = true;
		    i_chemin = i;
		    i--;
		}
	    }

	    /* Plutôt qu'une fenêtre de confirmation, ce serait mieux que juste la création de l'alias soit notée dans une sorte de status bar en bas de l'application */
	    if (b_alias_trouve && b_chemin_trouve){
		new ConfirmAlias(this, sa_words[i_alias], sa_words[i_chemin]);
	    }

	    
	    System.out.println("Alias : " + sa_words[i_alias] + ", chemin : " + sa_words[i_chemin]);
	    

	}

	/* Lance la complétion */
	if (evt.getKeyCode() == KeyEvent.VK_TAB){
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
		if (s_insert == null){ // Si ce n'est pas un schéma qu'il faut chercher mais un alias
		    s_insert = complete( (SortedMap) tm_alias, s_to_complete);
		}

		System.out.println("Cherche à insérer : " + s_insert);
		break;
	    case 1:
		System.out.println("Complétion de table");
		s_schema = extract(s_extract, 1);
		s_to_complete = extract(s_extract, 2);
		if (s_to_complete != null){
		    s_insert = (completor.containsKey(s_schema)) ? complete(completor.get(s_schema), s_to_complete) : null;
		    if (s_insert == null && tm_alias.containsKey(s_schema)){ // Si ce n'est pas une table qu'il faut chercher mais un champ
			s_insert = complete( (SortedSet) tm_alias.get(s_schema), s_to_complete);
		    }
		    
		}
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
    

    /* Renvoie le sous-arbre dont les clefs dont le préfixe est prefix */
    private <V> SortedMap<String, V> filterPrefix(SortedMap<String, V> baseMap, String prefix){
	if(prefix.length() > 0){
	    char nextLetter = (char) (prefix.charAt(prefix.length()-1)+1);
	    String end = prefix.substring(0, prefix.length()-1) + nextLetter;
	    return baseMap.subMap(prefix, end);
	}
	return baseMap;
    }


    /* Renvoie le sous-ensemble des éléments dont le préfixe est prefix */
    private SortedSet<String> filterPrefix(SortedSet<String> baseSet, String prefix){
	if(prefix.length() > 0){
	    char nextLetter = (char) (prefix.charAt(prefix.length()-1)+1);
	    String end = prefix.substring(0, prefix.length()-1) + nextLetter;
	    System.out.println("Éléments entre " + prefix + " et " + end);
	    return baseSet.subSet(prefix, end);
	}
	return baseSet;
    }

    
    /* Renvoie le i-ème élément dans une série d'éléments séparés par '.' */
    String extract(String s, int i){
	return(s.split("\\.")[i-1]);
    }

    /* Si plus d'une solution ou pas de solution, renvoie NULL */
    String complete(SortedMap tm, String s_pref){
	Object[] sa_suff = filterPrefix(tm, s_pref).keySet().toArray();	
	
	if (sa_suff.length == 1){
	    return ((String) sa_suff[0]);
	}
	return null;
    }

    /* Si plus d'une solution ou pas de solution, renvoie NULL */
    String complete(SortedSet ss, String s_pref){
	Object[] sa_suff = filterPrefix(ss, s_pref).toArray();	
	if (sa_suff.length == 1){
	    return ((String) sa_suff[0]);
	} 
	return null;
    }


    public void addAlias(String alias, String path){
	System.out.println("Ajout de l'alias " + alias + " pour " + path + ".");
	
	String[] sa_path = path.split("\\.");
	if (sa_path.length == 2){
	    System.out.println("Entrés dans le test. sa_path[0] : " + sa_path[0] + ", sa_path[1] : " + sa_path[1]);
		

	    if (completor.containsKey(sa_path[0]) && completor.get(sa_path[0]).containsKey(sa_path[1])){
		tm_alias.put(alias, (TreeSet) completor.get(sa_path[0]).get(sa_path[1])); // Ajoute un lien vers les champs
	    }
	}
    }
}

class ConfirmAlias extends Dialog implements ActionListener {
    private Button yes, no;
    private String alias, chemin;
    
    public ConfirmAlias(Frame parent, String alias, String chemin) {
	super(parent, "Confirmation d'ajout d'alias", true);
	this.alias = alias;
	this.chemin = chemin;
	setLayout(new FlowLayout());
	add(new Label("Ajouter l'alias \n" + alias + " pour \n" + chemin + " ?"));
	yes = new Button("Oui");
	yes.addActionListener(this);
	no = new Button("Non");
	no.addActionListener(this);
	add(yes);
	add(no);
	pack();
	setVisible(true);
    }

    public void actionPerformed(ActionEvent event) {
	if (event.getSource() == yes) {
	    ((MaFenetre) this.getParent()).addAlias(alias, chemin);
	    dispose();
	} else {
	    dispose();
	}
    }
}
