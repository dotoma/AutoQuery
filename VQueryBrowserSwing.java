/* GUI */
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import javax.swing.JSplitPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.BorderLayout;
import java.util.EventObject;
import java.util.Vector;


/* SQL */
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;

/* Arbre */
import java.util.TreeMap;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


public class VQueryBrowserSwing{
    public static void main(String args[]){
	VQueryBrowser app = new VQueryBrowser(args);
    }
}


class VQueryBrowser extends JFrame implements ActionListener{

    /* Variables */
    private int keyTABCount = 0;
    private Vector<JEditTextArea> vector_jeta = new Vector(); /* Liste tous les JEditTextArea */ 

    /* Arborescence BDD */
    private JTree jt_arborescence_BDD;
    private TreeMap <String, TreeMap<String, TreeSet<String>> >  tm_arborescence_BDD;
    private DefaultMutableTreeNode dmtn_root;
    private TreeMap<String, TreeSet> tm_alias;


    /* Composants */
    StatusBar status_bar;
    JPopupMenu popup;
    JTabbedPane jtp_onglets;
    //JEditTextArea jeta_query; /* Le JEditTextArea de l'onglet affiché */

    public void increaseKeyTABCount(){
	keyTABCount++;
    }

    public void resetKeyTABCount(){
	keyTABCount = 0;
    }

    public int getKeyTABCount(){
	return keyTABCount;
    }

    public void actionOnKey(KeyEvent evt){
	if (evt.getKeyCode() == KeyEvent.VK_TAB){
	    this.actionOnKeyTAB();
	} else if (evt.getKeyCode() == KeyEvent.VK_R && evt.getKeyModifiersText(evt.getModifiers()).equals("Ctrl") ) {	
	    resetKeyTABCount();
	    this.looksForAnAliasAndCreate();
	} else {
	    resetKeyTABCount();
	    status_bar.showStatus("");
	}
    }

    private void looksForAnAliasAndCreate(){
	JEditTextArea jeta_query = getActiveJEditTextArea();
	    int i_caret = jeta_query.getCaretPosition();
	    String s_ta = jeta_query.getText().replace("\n", " ").replace("\r", " ");
	    String[] sa_words = s_ta.substring(0, i_caret).split(" ");
	    System.out.println("Il y a " + sa_words.length + " mots.");
	    /* Trouve les deux (trois s'il y a un "AS") derniers mots */ 
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

	    if (b_alias_trouve && b_chemin_trouve){
	       	status_bar.showStatus("Ajout alias : " + sa_words[i_alias] + " pour " + sa_words[i_chemin]);
		System.out.println("Ajout alias : " + sa_words[i_alias] + " pour " + sa_words[i_chemin]);
		addAlias(sa_words[i_alias], sa_words[i_chemin]);
	    }

	    
	    System.out.println("Alias : " + sa_words[i_alias] + ", chemin : " + sa_words[i_chemin]);
    }
	    
    public void addAlias(String alias, String path){
	System.out.println("Ajout de l'alias " + alias + " pour " + path + ".");
	
	String[] sa_path = path.split("\\.");
	if (sa_path.length == 2){
	    if (tm_arborescence_BDD.containsKey(sa_path[0]) && tm_arborescence_BDD.get(sa_path[0]).containsKey(sa_path[1])){
		tm_alias.put(alias, (TreeSet) tm_arborescence_BDD.get(sa_path[0]).get(sa_path[1])); // Ajoute un lien vers les champs
		System.out.println("Alias ajouté");
	    }
	}
    }

    private void actionOnKeyTAB(){
	JEditTextArea jeta_query = getActiveJEditTextArea();
	increaseKeyTABCount();
	if (getKeyTABCount() <= 1){ /* Une fois TAB appuyé */
	    String s_query = jeta_query.getText().replace("\n", " ").replace("\r", " ");
	    

	    /* Récupère le mot entre le dernier espace précédant le curseur et le curseur. S'il n'y a pas d'espace, ça prend toute la sous-chaîne gauche jusqu'au curseur. */
	    int i_caret = jeta_query.getCaretPosition(); /* Enregistre la position du curseur */
	    String s_pref = s_query.substring(0, i_caret); /* Chaîne a */
	    String s_suff = s_query.substring(i_caret); /* Chaîne b */
	    int i_debut = Math.max(s_pref.lastIndexOf(" ")+1, 0);
	    String s_extract = s_pref.substring(i_debut, i_caret);
	    System.out.println("Tentative de complétion à partir de '" + s_extract + "'");	    
	    System.out.println("s_pref vaut '" + s_pref + "'");
	    System.out.println("s_suff vaut '" + s_suff + "'");


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
		    s_insert = complete(tm_arborescence_BDD, s_to_complete);
		    if (s_insert == null){ // Si ce n'est pas un schéma qu'il faut chercher mais un alias
			System.out.println("Pas de schéma trouvé commençant par " + s_to_complete);
			System.out.println("Recherche d'un alias correspondant");
			s_insert = complete( (SortedMap) tm_alias, s_to_complete);
			if (s_insert == null){ // Si ce n'est pas non plus un début d'alias, on empeche la complétion
			    this.resetKeyTABCount();
			}
			    
		    }

		    System.out.println("Cherche à insérer : " + s_insert);
		    break;
		case 1:
		    System.out.println("Complétion de table");
		    s_schema = extract(s_extract, 1);
		    s_to_complete = extract(s_extract, 2);
		    if (s_to_complete != null){
			s_insert = (tm_arborescence_BDD.containsKey(s_schema)) ? complete(tm_arborescence_BDD.get(s_schema), s_to_complete) : null;
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
		    s_insert = complete((SortedSet) tm_arborescence_BDD.get(s_schema).get(s_table), s_to_complete);
		    System.out.println("Cherche à insérer : " + s_insert + " à partir de " + s_to_complete);
		    break;		
		}

		if (s_insert != null){ /* Si s_insert est bien le préfixe d'un schéma, d'un alias, d'une table ou d'un champ*/
		    jeta_query.setSelectedText(s_insert.substring(s_to_complete.length()));
		    jeta_query.setCaretPosition( jeta_query.getText().length() - (s_query.length() - i_caret)); // Remet le curseur où il était avant la complétion
		    if (s_insert.length() == s_to_complete.length()) { // Si on n'insère rien parce qu'il y a plusieurs possibilités
			resetKeyTABCount(); // Remet le compteur de TAB à zéro puisqu'il y a eu une complétion
		    }
		}	    
		System.out.println("TAB appuyé");


	} else { /* Plus d'une fois TAB appuyé */
	    System.out.println("TAB appuyé deux fois");
	    if (popup != null){
		add(popup);
		System.out.println("Affichage d'un popup des choix possibles");
		popup.show(jeta_query, 12, 20); // Position pifométrique
	    }
	    this.resetKeyTABCount();
	    
	}
    }


    /* Renvoie le i-ème élément dans une série d'éléments séparés par '.' */
    private String extract(String s, int i){
	String[] composants = s.split("\\.");
	if (i > composants.length){
	    return("");
	} else {
	    return(composants[i-1]);
	}
    }

    /* Si plus d'une solution ou pas de solution, renvoie NULL */
    private String complete(SortedMap tm, String s_pref){
	Object[] sa_suff = filterPrefix(tm, s_pref).keySet().toArray();	
	int nb_comp = sa_suff.length; // nombre de complétion(s) possible(s)
	status_bar.showStatus(nb_comp + " complétion(s) possible(s)");
	if (nb_comp == 1){
	    return ((String) sa_suff[0]);
	} else if (nb_comp > 1){
	    String s_premier = (String)sa_suff[0];
	    int i = s_pref.length();
	    StringBuffer sb_plus_long_prefixe = new StringBuffer(s_pref);
	    System.out.println("Préfixe le plus long à partir de " + sb_plus_long_prefixe.toString());
	    
	    while (i < s_premier.length()){
		System.out.println(sb_plus_long_prefixe.toString() + s_premier.charAt(i) + " est un préfixe commun ?");
		
		Object[] sa_comp = filterPrefix(tm, sb_plus_long_prefixe.toString() + s_premier.charAt(i)).keySet().toArray();
		if (sa_comp.length == sa_suff.length){
		    sb_plus_long_prefixe.append(s_premier.charAt(i));
		    i++;
		} else {
		    break;
		}
	    }
	    /* Remplissage du menu contextuel */
	    popup = new JPopupMenu("Complétions possibles");
	    JMenuItem mi_completion;
	    for (int j=0 ; j < sa_suff.length ; j++){
		mi_completion = new JMenuItem((String)sa_suff[j]);
		popup.add(mi_completion);
		mi_completion.addActionListener(this);
		}
	    return sb_plus_long_prefixe.toString();	    
	    
	}

	/* Assert: le tableau est vide */
	return null;
    }

    /* Si plus d'une solution ou pas de solution, renvoie NULL */
    /* Remarque : cette méthode est en tout point identique à celle concernant les SortedMaps. 
       Comment n'en faire qu'une pour les deux ? */
    private String complete(SortedSet ss, String s_pref){
	Object[] sa_suff = filterPrefix(ss, s_pref).toArray();	
	int nb_comp = sa_suff.length; // nombre de complétion(s) possible(s)
	status_bar.showStatus(nb_comp + " complétion(s) possible(s)");
	if (nb_comp == 1){
	    return ((String) sa_suff[0]);
	} else if (nb_comp > 1){
	    String s_premier = (String)sa_suff[0];
	    int i = s_pref.length();
	    StringBuffer sb_plus_long_prefixe = new StringBuffer(s_pref);
	    System.out.println("Préfixe le plus long à partir de " + sb_plus_long_prefixe.toString());
	    while (i < s_premier.length()){
		System.out.println(s_pref + s_premier.charAt(i) + " est un préfixe commun ?");
		
		Object[] sa_comp = filterPrefix(ss, sb_plus_long_prefixe.toString() + s_premier.charAt(i)).toArray();
		if (sa_comp.length == sa_suff.length){
		    sb_plus_long_prefixe.append(s_premier.charAt(i));
		    i++;
		} else {
		    break;
		}
	    }
	    /* Remplissage du menu contextuel */
	    popup = new JPopupMenu("Complétions possibles");
	    JMenuItem mi_completion;
	    for (int j=0 ; j < sa_suff.length ; j++){
		mi_completion = new JMenuItem((String)sa_suff[j]);
		popup.add(mi_completion);
		mi_completion.addActionListener(this);
		}

	    return sb_plus_long_prefixe.toString();	    
	}

	/* Assert: le tableau est vide */
	return null;

    }

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




    public VQueryBrowser(String args[]){
	super("VQueryBrowser");
	
	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	

	/* Sauvegarde des alias */
	tm_alias = new TreeMap();


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
		    System.out.println("Fermeture de la connexion à la BDD");
		    con.close();
		}
	    } catch (SQLException e) {}
	}
	

	/* Crée la réprésentation graphique de la BDD */
	dmtn_root = new DefaultMutableTreeNode("MOMACQ_V3");
	if (tm_arborescence_BDD != null) {
		createTree(dmtn_root, tm_arborescence_BDD);
		jt_arborescence_BDD = new JTree(dmtn_root);
	} 





	/* Crée la fenêtre avec ses composants */
	JScrollPane jsp_treeView = new JScrollPane(jt_arborescence_BDD); /* Crée la hiérarchie de la BDD */

	/* Crée le composant qui accueille les onglets */
	jtp_onglets = new JTabbedPane(SwingConstants.TOP);
	jtp_onglets.setOpaque(true);

	/* Ajoute un onglet aux composants gérant les onglets*/
	makeTab();

	/* Crée la status bar */
	status_bar = new StatusBar();


	/* Crée le panel de gauche */
	JPanel panel_gauche = new JPanel();
	/** Accueil des composants **/
	panel_gauche.add("Center", jtp_onglets);
	panel_gauche.add("South", status_bar);


	/* Crée le JSplitPane qui accueille les onglets à gauche et l'arbre à droite */
	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	splitPane.setLeftComponent(panel_gauche);
	splitPane.setBottomComponent(jsp_treeView);
	

	jsp_treeView.setMinimumSize(new Dimension(300, 500));
	splitPane.setDividerLocation(670); 
	splitPane.setPreferredSize(new Dimension(1000, 500));

	add(splitPane);

	JMenuBar menu_bar = new JMenuBar();
	JMenu menu_onglets = new JMenu("Onglets");
	JMenuItem menu_onglets_ajout = new JMenuItem("Ajouter", KeyEvent.VK_A);
	menu_onglets_ajout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK));
	menu_onglets_ajout.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent e){
		    makeTab();
		}
	    });
	menu_onglets.add(menu_onglets_ajout);
	menu_bar.add(menu_onglets);
	add("North", menu_bar);

	pack();
	setVisible(true);
    }

    /** Renvoie le JEditTextArea sur l'onglet sélectionné **/
    private JEditTextArea getActiveJEditTextArea(){
	return vector_jeta.elementAt(jtp_onglets.getSelectedIndex());
    }

    private void makeTab(){
	jtp_onglets.addTab("Onglet " + (jtp_onglets.getTabCount()+1), makePanelForTab());
	jtp_onglets.setTabComponentAt(jtp_onglets.getTabCount()-1, new ButtonTabComponent(jtp_onglets));
    }

    private JPanel makePanelForTab(){
	int nbTabs = jtp_onglets.getTabCount();

	/** Crée le contenu d'un onglet
	    Crée le composant pour écrire les requêtes **/
	JEditTextArea jeta_query = new JEditTextArea(); 
	jeta_query.setFocusTraversalKeysEnabled(false);
	jeta_query.setEditable(true);
	jeta_query.setTokenMarker(new SQLTokenMarker());
	jeta_query.setMinimumSize(new Dimension(500, 300));

	/** Enregistre le JEditTextArea pour pouvoir faire des opérations plus tard dessus **/
	System.out.println("Ajout d'un JEditTextArea (dont le document est " + jeta_query.getDocument().hashCode() + ") en position " + nbTabs);
	vector_jeta.add(nbTabs, jeta_query); 
	
	/** Crée le JPanel dans lequel on met les composants de chaque onglet **/
	JPanel panel = new JPanel(new BorderLayout());
	panel.add("Center", jeta_query);
	return panel;
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

    /* Retrouve l'instance de VQueryBrowser qui contrôle l'objet lié à l'événement */
    public static VQueryBrowser getVQueryBrowserParent(EventObject evt){
	if(evt != null)
	    {
		Object o = evt.getSource();
		if(o instanceof Component)
		    {
			// find the parent VQueryBrowser
			Component c = (Component)o;
			for(;;)
			{
				if(c instanceof VQueryBrowser)
					return (VQueryBrowser)c;
				else if(c == null)
					break;
				if(c instanceof JPopupMenu)
					c = ((JPopupMenu)c)
						.getInvoker();
				else
					c = c.getParent();
			}
		}
	}
	
	return null; /* Cas pas possible */
	
    }

    public final void actionPerformed(final ActionEvent e) {
	JEditTextArea jeta_query = getActiveJEditTextArea();
	// Dans le cas d'un clic ou ENTER dans le menu contextuel :
	// Insérer la complétion choisie
	System.out.println("String lié au clic : " + e.getActionCommand());
	int i_LastDotPosition = jeta_query.getText().substring(0, jeta_query.getCaretPosition()).lastIndexOf(".");
	String s_pref = jeta_query.getText().substring(0, i_LastDotPosition+1);
	String s_suff = jeta_query.getText().substring(jeta_query.getCaretPosition());
	jeta_query.setText(s_pref + e.getActionCommand() + s_suff);
	jeta_query.setCaretPosition( s_pref.length() + e.getActionCommand().length());
	// Supprimer ce menu
	remove(popup);
	popup = null;
    }


}


class StatusBar extends JPanel
{
	private JLabel info;

	// The constructor 
	public StatusBar()
	{
		setLayout(new BorderLayout());

		// Je crée un Label bourré d'espaces parce que je ne sais pas
		// pas faire en sorte que le Label puisse accueillir un grand
		// texte sinon.
		add("West", info = new JLabel("                                                                                                                       ", JLabel.LEFT));
		info.setMinimumSize(this.getSize());
       	}

	public void showStatus(String status)
	{
		info.setText(status);
	}
}
