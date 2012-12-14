/* GUI */
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JSplitPane;
import java.awt.Dimension;

/* SQL */
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;

/* Arbre */
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;


public class VQueryBrowserSwing{
    public static void main(String args[]){
	VQueryBrowser app = new VQueryBrowser(args);
    }
}


class VQueryBrowser implements KeyListener{
    /* Variables */
    private int keyTABCount = 0;

    /* Arborescence BDD */
    private JTree jt_arborescence_BDD;
    private TreeMap  tm_arborescence_BDD;
    private DefaultMutableTreeNode dmtn_root;


    /* Composants */
    JEditTextArea jeta_query;


    /* KeyListener pour traiter les frappes de touches dans le 
       JScrollPane jsp_query */
    public final void keyPressed(final KeyEvent evt) {
	if (evt.getKeyCode() == KeyEvent.VK_TAB){
	    System.out.println("TAB appuyé");
	    evt.consume();
	    actionOnKeyTAB();
	} else if (evt.getKeyCode() == KeyEvent.VK_SPACE){
	    System.out.println("SPACE appuyé");
	    evt.consume();
	    //actionOnKeySPACE();
	} else {
	    
	}
    }

    public final void keyReleased(final KeyEvent keyEvent) {

    }

    public final void keyTyped(final KeyEvent keyEvent) {

    }

    public void increaseKeyTABCount(){
	keyTABCount++;
    }

    public void resetKeyTABCount(){
	keyTABCount = 0;
    }

    public int getKeyTABCount(){
	return keyTABCount;
    }

    private void actionOnKeyTAB(){
	increaseKeyTABCount();
	if (getKeyTABCount() <= 1){ /* Une fois TAB appuyé */
	    String s_query = jeta_query.getText();
	    

	    /* Récupère le mot entre le dernier espace précédant le curseur et le curseur. S'il n'y a pas d'espace, ça prend toute la sous-chaîne gauche jusqu'au curseur. */
	    int i_caret = jeta_query.getCaretPosition(); /* Enregistre la position du curseur */
	    String s_pref = s_query.substring(0, i_caret); /* Chaîne a */
	    String s_suff = s_query.substring(i_caret); /* Chaîne b */
	    int i_debut = Math.max(s_pref.lastIndexOf(" ")+1, 0);
	    String s_extract = s_pref.substring(i_debut, i_caret);
	    System.out.println("Tentative de complétion à partir de '" + s_extract);	    
	    System.out.println("s_pref vaut '" + s_pref + "'");
	    System.out.println("s_suff vaut '" + s_suff + "'");
	} else { /* Plus d'une fois TAB appuyé */
	    
	}
    }

    public VQueryBrowser(String args[]){
	JFrame frame = new JFrame("VQueryBrowser");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
	/* Crée le TreeMap à partir de information_schema */
	Connection con = null;
	try {
	    loadDriver();
	    if (args.length == 2){
		System.out.println("Connexion lancée avec les paramètres suivants :");
		System.out.println("Serveur : " + args[0]);
		System.out.println("Port : " + args[1]);
		con = newConnection(args[0], args[1]);
	    } else {
		con = newConnection("prod-bdd-mono-master-read", "3306");
	    }
	    Statement st = con.createStatement();
	    String query = "SELECT TABLE_SCHEMA, TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION";
	    ResultSet rs = st.executeQuery(query);
	    tm_arborescence_BDD = makeTree(rs);
	} catch (Exception e){
	    System.err.println("Exception : " + e.getMessage());
	} finally {
	    try {
		if (con != null){
		    con.close();
		}
	    } catch (SQLException e) {}
	}
	

	/* Crée la réprésentation graphique de la BDD */
	dmtn_root = new DefaultMutableTreeNode("MOMACQ_V3");
	createTree(dmtn_root, tm_arborescence_BDD);
	jt_arborescence_BDD = new JTree(dmtn_root);




	/* Crée la fenêtre avec ses composants */
	JScrollPane jsp_treeView = new JScrollPane(jt_arborescence_BDD); /* Crée la hiérarchie de la BDD */
	jeta_query = new JEditTextArea(); /* Crée le composant pour écrire les requêtes */
	jeta_query.addKeyListener(this);
	jeta_query.setFocusTraversalKeysEnabled(false);
	jeta_query.setEditable(true);
	jeta_query.setTokenMarker(new SQLTokenMarker());

	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	splitPane.setLeftComponent(jeta_query);
	splitPane.setBottomComponent(jsp_treeView);
	

	jeta_query.setMinimumSize(new Dimension(600, 400));
	jsp_treeView.setMinimumSize(new Dimension(300, 400));
	splitPane.setDividerLocation(600); 
	splitPane.setPreferredSize(new Dimension(900, 400));

	frame.add(splitPane);
	frame.pack();
	frame.setVisible(true);
    }

    private void createTree(DefaultMutableTreeNode top, TreeMap <String, TreeMap<String, TreeSet<String>> > tm_arbre){
	DefaultMutableTreeNode dmtn_schema;
	DefaultMutableTreeNode dmtn_table;
	DefaultMutableTreeNode dmtn_column;
	
	/* Parcours sur les schémas */
	for (Map.Entry <String, TreeMap<String, TreeSet<String>> > schema : tm_arbre.entrySet()){
	    dmtn_schema = new DefaultMutableTreeNode(schema.getKey());
	    
	    /* Parcours sur les tables de ce schéma */
	    for (Map.Entry <String, TreeSet<String> > table : schema.getValue().entrySet()){
		dmtn_table = new DefaultMutableTreeNode(table.getKey());

		/* Parcours sur les colonnes de cette table */
		for (String column : table.getValue()){
		    dmtn_column = new DefaultMutableTreeNode(new String(column));
		    dmtn_table.add(dmtn_column);
		}		
		dmtn_schema.add(dmtn_table);
	    }	    
	    top.add(dmtn_schema);
	}
	
    }



    /* Charge le driver pour communiquer avec la base de données */
    private static void loadDriver() throws ClassNotFoundException {
	Class.forName("com.mysql.jdbc.Driver");
    }
    
    /* Obtient une connexion avec le moteur de gestion de BDD */
    private static Connection newConnection(String host, String port) throws SQLException {
	final String url = "jdbc:mysql://" + host + ":" + port;
	Connection con = DriverManager.getConnection(url, "MAD_exp", "altUnsyint");
	return con;
    }

    /* Crée l'arbre pour la complétion */
    private static TreeMap makeTree(ResultSet rs) throws SQLException{
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



}