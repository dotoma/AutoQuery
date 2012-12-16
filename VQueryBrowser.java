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


class MaFenetre extends Frame implements KeyListener, ActionListener {
    public static final int HEIGHT = 400;
    public static final int WIDTH = 600;
    public static final int HEIGHT_STATUS_BAR = 40;
    private TextArea ta;
    private StatusBar status_bar;
    private ToolBar tool_bar;
    private PopupMenu  popup;
    short i_TAB_press;
    TreeMap<String, TreeMap> completor;
    TreeMap<String, TreeSet> tm_alias;
    MaFenetre(TreeMap comp){
	super("VQueryBrowser");
	setLayout(new BorderLayout());
	setSize(WIDTH, HEIGHT);
	completor = comp;    

	/* Sauvegarde des alias */
	tm_alias = new TreeMap();

	/* TextArea dans laquelle on tape les requêtes */
	ta = new TextArea();
	ta.addKeyListener(this);
	ta.setFocusTraversalKeysEnabled(false);
	add("Center", ta);

	/* Status bar */
	status_bar = new StatusBar();
	add("South", status_bar);

	i_TAB_press = 0;
	
	/* ToolBar */
	//tool_bar = new ToolBar();
	//add("North", tool_bar);

	setVisible(true);
	addWindowListener(
			  new WindowAdapter() {
			      public void windowClosing(WindowEvent e) {
				  System.exit(0);
			      }
			  });
    }
			  
    public final void actionPerformed(final ActionEvent e) {
	// Dans le cas d'un clic ou ENTER dans le menu contextuel :
	// Insérer la complétion choisie
	System.out.println("String lié au clic : " + e.getActionCommand());
	int i_LastDotPosition = ta.getText().substring(0, ta.getCaretPosition()).lastIndexOf(".");
	String s_pref = ta.getText().substring(0, i_LastDotPosition+1);
	String s_suff = ta.getText().substring(ta.getCaretPosition());
	ta.setText(s_pref + e.getActionCommand() + s_suff);
	ta.setCaretPosition( s_pref.length() + e.getActionCommand().length());
	// Supprimer ce menu
	remove(popup);
	popup = null;
    }
       
    
    
    
    public void keyTyped(KeyEvent evt){}
    public void keyPressed(KeyEvent evt){
	/* Essaie de lire l'alias si combinaison CTRL + R */
	if (evt.getKeyCode() == KeyEvent.VK_R && evt.getKeyModifiersText(evt.getModifiers()).equals("Ctrl") ) {
	    /* Remet à zéro le compteur de TAB */
	    i_TAB_press = 0;


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
		status_bar.showStatus("Ajout alias : " + sa_words[i_alias] + " pour " + sa_words[i_chemin]);
		addAlias(sa_words[i_alias], sa_words[i_chemin]);
	    }

	    
	    System.out.println("Alias : " + sa_words[i_alias] + ", chemin : " + sa_words[i_chemin]);
	    

	}

	/* Lance la complétion */
	else if (evt.getKeyCode() == KeyEvent.VK_TAB){
	    /* Incrémente le compteur de TAB */
	    i_TAB_press++;
	    evt.consume();

	    if (i_TAB_press <= 1){
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
			System.out.println("Pas de schéma trouvé commençant par " + s_to_complete);
			System.out.println("Recherche d'un alias correspondant");
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
		    ta.insert(s_insert.substring(s_to_complete.length()), ta.getCaretPosition());
		    ta.setCaretPosition( ta.getText().length() - (s_ta.length() - i_caret)); // Remet le curseur où il était avant la complétion
		    if (s_insert.length() == s_to_complete.length()) { // Si on n'insère rien
			i_TAB_press = 0; // Remet le compteur de TAB à zéro puisqu'il y a eu une complétion
		    }
		}	    
		System.out.println("TAB appuyé");
	    } else { // Plus d'une fois TAB appuyé
		System.out.println("TAB appuyé deux fois");
		if (popup != null){
		    add(popup);
		    popup.show(ta, 12, 20); // Position pifométrique
		}
		i_TAB_press = 0;
	    }
	} else {
	    i_TAB_press = 0;
	}
    }

    public void keyReleased(KeyEvent e) {
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

    
    /* Renvoie le i-ème élément dans une série d'éléments séparés par '.' */
    private String extract(String s, int i){
	return(s.split("\\.")[i-1]);
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
	    popup = new PopupMenu("Complétions possibles");
	    MenuItem mi_completion;
	    for (int j=0 ; j < sa_suff.length ; j++){
		mi_completion = new MenuItem((String)sa_suff[j]);
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
	    return sb_plus_long_prefixe.toString();	    
	}

	/* Assert: le tableau est vide */
	return null;

    }


    public void addAlias(String alias, String path){
	System.out.println("Ajout de l'alias " + alias + " pour " + path + ".");
	
	String[] sa_path = path.split("\\.");
	if (sa_path.length == 2){
	    if (completor.containsKey(sa_path[0]) && completor.get(sa_path[0]).containsKey(sa_path[1])){
		tm_alias.put(alias, (TreeSet) completor.get(sa_path[0]).get(sa_path[1])); // Ajoute un lien vers les champs
		System.out.println("Alias ajouté");
	    }
	}
    }
}


/*class ToolBar extends Panel
{
	// The constructor - called first when object is created
	public ToolBar()
	{
		setLayout(new FlowLayout());

		// Adds three buttons to the panel, which are
		// laid out according to FlowLayout
		add(new Button("Open"));
		add(new Button("Save"));
		add(new Button("Close"));

		// A Choice component which needs to be
		// added to the panel after the choices have
		// been included.
		Choice c = new Choice();
		c.addItem("Times Roman");
		c.addItem("Helvetica");
		c.addItem("System"); 
		add(c);

		// Add one last button
		add(new Button("Help"));
	}
}
*/


/*
public class Test {

    public class SuggestionPanel {
        private JList list;
        private JPopupMenu popupMenu;
        private String subWord;
        private final int insertionPosition;

        public SuggestionPanel(JTextArea textarea, int position, String subWord, Point location) {
            this.insertionPosition = position;
            this.subWord = subWord;
            popupMenu = new JPopupMenu();
            popupMenu.removeAll();
            popupMenu.setOpaque(false);
            popupMenu.setBorder(null);
            popupMenu.add(list = createSuggestionList(position, subWord), BorderLayout.CENTER);
            popupMenu.show(textarea, location.x, textarea.getBaseline(0, 0) + location.y);
        }

        public void hide() {
            popupMenu.setVisible(false);
            if (suggestion == this) {
                suggestion = null;
            }
        }

        private JList createSuggestionList(final int position, final String subWord) {
            Object[] data = new Object[10];
            for (int i = 0; i < data.length; i++) {
                data[i] = subWord + i;
            }
            JList list = new JList(data);
            list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setSelectedIndex(0);
            list.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        insertSelection();
                    }
                }
            });
            return list;
        }

        public boolean insertSelection() {
            if (list.getSelectedValue() != null) {
                try {
                    final String selectedSuggestion = ((String) list.getSelectedValue()).substring(subWord.length());
                    textarea.getDocument().insertString(insertionPosition, selectedSuggestion, null);
                    return true;
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }
                hideSuggestion();
            }
            return false;
        }

        public void moveUp() {
            int index = Math.min(list.getSelectedIndex() - 1, 0);
            selectIndex(index);
        }

        public void moveDown() {
            int index = Math.min(list.getSelectedIndex() + 1, list.getModel().getSize() - 1);
            selectIndex(index);
        }

        private void selectIndex(int index) {
            final int position = textarea.getCaretPosition();
            list.setSelectedIndex(index);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    textarea.setCaretPosition(position);
                };
            });
        }
    }

    private SuggestionPanel suggestion;
    private JTextArea textarea;

    protected void showSuggestionLater() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                showSuggestion();
            }

        });
    }

    protected void showSuggestion() {
        hideSuggestion();
        final int position = textarea.getCaretPosition();
        Point location;
        try {
            location = textarea.modelToView(position).getLocation();
        } catch (BadLocationException e2) {
            e2.printStackTrace();
            return;
        }
        String text = textarea.getText();
        int start = Math.max(0, position - 1);
        while (start > 0) {
            if (!Character.isWhitespace(text.charAt(start))) {
                start--;
            } else {
                start++;
                break;
            }
        }
        if (start > position) {
            return;
        }
        final String subWord = text.substring(start, position);
        if (subWord.length() < 2) {
            return;
        }
        suggestion = new SuggestionPanel(textarea, position, subWord, location);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                textarea.requestFocusInWindow();
            }
        });
    }

    private void hideSuggestion() {
        if (suggestion != null) {
            suggestion.hide();
        }
    }

    protected void initUI() {
        final JFrame frame = new JFrame();
        frame.setTitle("Test frame on two screens");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout());
        textarea = new JTextArea(24, 80);
        textarea.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        textarea.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    if (suggestion != null) {
                        if (suggestion.insertSelection()) {
                            e.consume();
                            final int position = textarea.getCaretPosition();
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        textarea.getDocument().remove(position - 1, 1);
                                    } catch (BadLocationException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestion != null) {
                    suggestion.moveDown();
                } else if (e.getKeyCode() == KeyEvent.VK_UP && suggestion != null) {
                    suggestion.moveUp();
                } else if (Character.isLetterOrDigit(e.getKeyChar())) {
                    showSuggestionLater();
                } else if (Character.isWhitespace(e.getKeyChar())) {
                    hideSuggestion();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }
        });
        panel.add(textarea, BorderLayout.CENTER);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                new Test().initUI();
            }
        });
    }

    }*/