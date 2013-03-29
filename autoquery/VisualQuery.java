package autoquery;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.awt.event.*;
import javax.swing.event.*;



/**
 * Insipiré de GraphPanel de John B. Matthews; distribution per GPL.
 */


public class VisualQuery extends JComponent {
    private static final int WIDE = 640;
    private static final int HIGH = 480;


    JFrame parentFrame;
    AutoQuery autoQuery;

    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;
    private boolean joining = false;
    private boolean addingRelation = false;

    private Point mousePt = new Point(WIDE / 2, HIGH / 2);
    private Point pendingConnectionStartPoint;
    private Point pendingConnectionEndPoint;

    
    private List<NodeSet> nodeSets = new ArrayList<NodeSet>();
    private List<Edge> edges = new ArrayList<Edge>();
    

    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                JFrame f = new JFrame("VisualQuery");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                VisualQuery vq = new VisualQuery(f);
                f.add(new JScrollPane(vq), BorderLayout.CENTER);
                f.pack();
                f.setLocationByPlatform(true);
                f.setVisible(true);
            }
        });
    }
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(WIDE, HIGH);
    }


    private Action toQueryAction = new Queryfy("Générer la requête");
    private class Queryfy extends AbstractAction{
	Queryfy(String s){
	    super(s);
	}
	
	public void actionPerformed(ActionEvent e){
	    String requete = toQuery();
	    System.out.println(requete);
	    if (autoQuery != null){
		autoQuery.makeTabFromQuery(requete);
	    }
	}
    }
    
    // Permet un démarrage sans lancer l'application AutoQuery
    public VisualQuery(JFrame f) {
	JMenuBar menu_bar = new JMenuBar();
	JMenu menu_outils = new JMenu("Outils");
	menu_bar.add(menu_outils);
	menu_outils.add(new JMenuItem(toQueryAction));
	f.setJMenuBar(menu_bar);

        this.setOpaque(true);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
	this.parentFrame = f;
	this.autoQuery = null;
    }


    // Quand exécuté depuis AutoQuery
    public VisualQuery(JFrame f, AutoQuery autoQuery) {
	JMenuBar menu_bar = new JMenuBar();
	JMenu menu_outils = new JMenu("Outils");
	menu_bar.add(menu_outils);
	menu_outils.add(new JMenuItem(toQueryAction));
	f.setJMenuBar(menu_bar);

        this.setOpaque(true);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
	this.parentFrame = f;
	this.autoQuery = autoQuery;
    }


    public String toQuery(){
	StringBuffer retour = new StringBuffer("SELECT\n\t*\nFROM ");
	retour.append(toQuery(NodeSet.getFirst(), new ArrayList<Edge>()));
	return retour.toString();		      
    }


    /* Transcrire le schéma en requête MySQL */
    public String toQuery(NodeSet first, List<Edge> visitedEdges){
	StringBuffer retour = new StringBuffer();
	if (visitedEdges.size() == 0){ // Si c'est le premier appel de la fonction
	    retour.append(first.getName() + " AS " + first.getAlias());
	}
	for (Edge e : edges){
	    if (visitedEdges.contains(e)) continue;
	    Node n_s = null;
	    Node n_d = null; //source, destination 

	    if (e.isJoinType() && e.n1.getNodeSet() == first){// Si e part de ce NodeSet
		n_s = e.n1;
		n_d = e.n2;
	    } 
	    
	    if (n_s != null){ // Si on a trouvé une arête
		retour.append("\n" + e.getRepresentation() + " ");
		
		List<Edge> list = Edge.edgesBetween(first, n_d.getNodeSet(), edges);
		int size = list.size();
		if (size > 0){
		    retour.append(n_d.getNodeSet().getName() + " AS " + n_d.getNodeSet().getAlias());
		    retour.append(" ON (");
		    
		    retour.append(list.get(0).n1.getNodeSet().getAlias() + "." + list.get(0).n1.getName());
		    retour.append(" " + e.getConditionRepresentation() + " ");
		    retour.append(list.get(0).n2.getNodeSet().getAlias() + "." + list.get(0).n2.getName());
		    visitedEdges.add(list.get(0));
		    for (int i = 1; i < size ; i++){		    
			Edge cond = list.get(i);    
			retour.append("\n\tAND ");
			retour.append(cond.n1.getNodeSet().getAlias() + "." + cond.n1.getName());
			retour.append(" " + cond.getConditionRepresentation() + " ");
			retour.append(cond.n2.getNodeSet().getAlias() + "." + cond.n2.getName());
			visitedEdges.add(cond);
		    }
       		    retour.append(")\n");
		}
		retour.append(toQuery(n_d.getNodeSet(),
				      visitedEdges));
	    }
	}
	return retour.toString();
    }

    

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(0x00f0f0f0));
        g.fillRect(0, 0, getWidth(), getHeight());
	if (joining){
	    g.setColor(Color.RED);
	    g.drawLine(pendingConnectionStartPoint.x,
		       pendingConnectionStartPoint.y,
		       pendingConnectionEndPoint.x,
		       pendingConnectionEndPoint.y);
	} else if (addingRelation){
	    g.setColor(Color.BLUE);
	    g.drawLine(pendingConnectionStartPoint.x,
		       pendingConnectionStartPoint.y,
		       pendingConnectionEndPoint.x,
		       pendingConnectionEndPoint.y);
	    
	}
        for (NodeSet ns : nodeSets) {
            ns.draw(g);
        }

	// Les relations après les tables pour qu'elles soient dessinées par dessus
    	for (Edge e : edges) {
            e.draw(g);
    	}

        if (selecting) {
            g.setColor(Color.DARK_GRAY);
            g.drawRect(mouseRect.x, mouseRect.y,
                mouseRect.width, mouseRect.height);
        }
    }



    public void changeTypeOfSelectedEdges(int type){
	for (Edge e : edges){
	    if (e.isSelected()){
		System.out.println("Le type de cette arête est " + e.relationType);
		if (type == Edge.RIGHT && e.relationType == Edge.LEFT){ // ne fait rien dans le cas d'un IJ
		    Node tmp = e.n1;
		    e.n1 = e.n2;
		    e.n2 = tmp;
		} else {
		    
		    e.relationType = type;
		}
		System.out.println("Le type de cette arête est désormais " + e.relationType);
	    }
	}
    }


    public NodeSet addNodeSet(String table_nom, String[] champs){
	return addNodeSet(table_nom, champs, true);
    }

    public NodeSet addNodeSet(String table_nom, String [] champs, boolean repaint){
	NodeSet ns = new NodeSet(new Point(10, 10), table_nom, champs);
	ns.setSelected(false);
	nodeSets.add(ns);
	if (repaint) {
	    repaint();
	}
	return ns;
    }

    private class MouseMotionHandler extends MouseMotionAdapter {

        Point delta = new Point();

        @Override
        public void mouseDragged(MouseEvent e) {
            if (selecting) {
                mouseRect.setBounds(
                    Math.min(mousePt.x, e.getX()),
                    Math.min(mousePt.y, e.getY()),
                    Math.abs(mousePt.x - e.getX()),
                    Math.abs(mousePt.y - e.getY()));
                NodeSet.selectRect(nodeSets, mouseRect);
            } else if (joining){
		pendingConnectionEndPoint = e.getPoint();
            } else if (addingRelation){
		pendingConnectionEndPoint = e.getPoint();
	    } else {
                delta.setLocation(
                    e.getX() - mousePt.x,
                    e.getY() - mousePt.y);
                NodeSet.updatePosition(nodeSets, delta);
                mousePt = e.getPoint();
            }
            e.getComponent().repaint();
        }
    }



    private class MouseHandler extends MouseAdapter{
	private final static int TABLE = 0;
	private final static int EDGE = 1;
	Point origineMenu;

	// Popup des tables
	private JPopupMenu popupNodeSet;
	private Action deleteNodeSet = new DeleteAction("Retirer la table", MouseHandler.TABLE);
	private Action setFirstNodeSetAction = new FirstNodeSetAction("Table initiale");

	// Popup des Edge
	// Join Edge
	private JPopupMenu popupJoinEdge;
	private Action deleteEdge = new DeleteAction("Retirer la relation", MouseHandler.EDGE);
	private JMenu menu_change_join;
	private Action changeRelationTypeToLeftJoin = new ChangeRelationType("LEFT JOIN", Edge.LEFT);
	private Action changeRelationTypeToInnerJoin = new ChangeRelationType("INNER JOIN", Edge.INNER);
	private Action changeRelationTypeToRightJoin = new ChangeRelationType("RIGHT JOIN", Edge.RIGHT);
	// Relation Edge
	private JPopupMenu popupRelationEdge;
	private JMenu menu_change_relation;
	private Action changeRelationTypeToEqual = new ChangeRelationType("=", Edge.EQUAL);
	private Action changeRelationTypeToLower = new ChangeRelationType("<", Edge.LOWER);
	private Action changeRelationTypeToGreater = new ChangeRelationType(">", Edge.GREATER);


	public MouseHandler(){
	    popupNodeSet = new JPopupMenu();
	    popupNodeSet.add(new JMenuItem(deleteNodeSet));
	    popupNodeSet.add(new JMenuItem(setFirstNodeSetAction));

	    popupJoinEdge = new JPopupMenu();
	    popupJoinEdge.add(new JMenuItem(deleteEdge));
	    menu_change_join = new JMenu("Changer de jointure");
	    menu_change_join.add(new JMenuItem(changeRelationTypeToLeftJoin));
	    menu_change_join.add(new JMenuItem(changeRelationTypeToRightJoin));
	    menu_change_join.add(new JMenuItem(changeRelationTypeToInnerJoin));
	    popupJoinEdge.add(menu_change_join);

	    popupRelationEdge = new JPopupMenu();
	    popupRelationEdge.add(new JMenuItem(deleteEdge));
	    menu_change_relation = new JMenu("Changer de condition");
	    menu_change_relation.add(new JMenuItem(changeRelationTypeToEqual));
	    menu_change_relation.add(new JMenuItem(changeRelationTypeToLower));
	    menu_change_relation.add(new JMenuItem(changeRelationTypeToGreater));
	    popupRelationEdge.add(menu_change_relation);
	}

	private class FirstNodeSetAction extends AbstractAction{
	    FirstNodeSetAction(String s){
		super(s);
	    }

	    public void actionPerformed(ActionEvent ae){
		System.out.println("Origine du menu : " + origineMenu.toString());
		
		NodeSet ns = NodeSet.getNodeSetFromPoint(origineMenu, nodeSets);
		if (ns != null){
		    NodeSet.setFirst(ns);
		} else {
		    System.out.println("Aucun NodeSet trouvé");
		}
		repaint();
	    }

	}

	private class ChangeRelationType extends AbstractAction{
	    int type;

	    ChangeRelationType(String s, int type){
		super(s);
		this.type = type;
	    }
	    
	    public void actionPerformed(ActionEvent e){
		changeTypeOfSelectedEdges(type);
		repaint();
	    }
	}


	private class DeleteAction extends AbstractAction{
	    int kind;

	    DeleteAction(String s, int kind){
		super(s);
		this.kind = kind;
	    }
	    
	    public void actionPerformed(ActionEvent e){
		if (kind == MouseHandler.TABLE){
		    NodeSet.removeSelected(nodeSets, edges);
		} else if (kind == MouseHandler.EDGE){
		    Edge.removeSelected(edges);
		}
		repaint();
	    }
	}

        @Override
        public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();
	    // En cas de double clic sur un champ du type ID_TABLE,
	    // On ajoute la table TABLE sur le plan de travail
            if (e.getClickCount() == 2) {
		Node n = NodeSet.getNodeFromPoint(nodeSets, mousePt);
		if (n != null) { // Si on a trouvé un Node
		    if (n.getName().startsWith("ID_")) {
			String nom_table = n.getName().substring(3);
			String [] champs = autoQuery.getDBTreeMap().get("CONF_V3").get(nom_table).toArray(new String[0]);
			NodeSet ns = addNodeSet(nom_table, champs, false); // On ne repaint pas car on le fait juste après
			if (ns != null) {
			    Node n2 = ns.getNodeFromName("ID");
			    edges.add(new Edge(n, n2));
			    repaint(); // On ne repaint que si nécessaire
			}
		    }
		}
	    } else if (e.isControlDown() && e.isShiftDown()){ // Faire des relations supplémentaires pour rajouter des conditions à la jointure
		System.out.println("Condition supplémentaire");
		addingRelation = true;
		pendingConnectionStartPoint = pendingConnectionEndPoint =  e.getPoint();		
	    } else if (e.isShiftDown()) {
		/* Ajoute ou supprime un élément de la sélection */
		//		NodeSet.selectToggle(nodeSets, mousePt);
				String[] champs = {"Salut", "Comment", "Ça", "Va ?"};
				addNodeSet("Coucou", champs);
				// Me sert lorsque j'exécute VisualQuery en standalone, pour avoir des tables de jeu.

		
            } else if (e.isControlDown()) {
		joining = true;
		pendingConnectionStartPoint = pendingConnectionEndPoint =  e.getPoint();
	    } else if (e.isPopupTrigger()) {
		/* Si menu contextuel */
		if (NodeSet.selectOne(nodeSets, mousePt)){
		    showPopupNodeSet(e); 
		} else if (Edge.selectOne(edges, mousePt)){
		    showPopupEdge(e); 
		}
            } else if (NodeSet.selectOne(nodeSets, mousePt)) {
		/* Sélectionne l'élément contenant le point du clic 
		 si le clic est sur un élément */
                selecting = false;
	    } else if (Edge.selectOne(edges, mousePt)){
		/* Sélectionne l'élément contenant le point du clic 
		 si le clic est sur une arête */
                selecting = false;
            } else {
		/* Si le clic est hors de tout élément,
		 on déselectionne tous les éléments et on passe
		en mode sélection */
                NodeSet.selectNone(nodeSets);
		Edge.selectNone(edges);
                selecting = true;
            }
            e.getComponent().repaint();
        }


	/* Lorsqu'on lâche le clic */
        @Override
        public void mouseReleased(MouseEvent e) {
	    
	    /* On sort du mode sélection */
	    selecting = false;
            mouseRect.setBounds(0, 0, 0, 0); // On change la taille du rectangle de sélection

	    if (e.isPopupTrigger()) {
		/* Si menu contextuel */
		if (NodeSet.selectOne(nodeSets, mousePt)){
		    showPopupNodeSet(e); 
		} else if (Edge.selectOne(edges, mousePt)){
		    showPopupEdge(e); 
		}

	    }
	    if (joining){
		// Tester si on a connecté deux Nodes pour jointure
		Node n2 = NodeSet.getNodeFromPoint(nodeSets, e.getPoint());
		if (n2 != null){
		    Node n1 = NodeSet.getNodeFromPoint(nodeSets, pendingConnectionStartPoint);
		    if(NodeSet.existsJoinPath(n1.getNodeSet(), 
					      n2.getNodeSet(), 
					      nodeSets, 
					      new ArrayList<Edge>(),
					      edges)){
			System.out.println("Cycle !");
			JOptionPane.showMessageDialog(VisualQuery.this.parentFrame,
						      "Joindre ces deux tables créerait un cycle.",
						      "Jointure interdite",
						      JOptionPane.ERROR_MESSAGE);			
		    } else { 
			edges.add(new Edge(n1, n2));
		    }
			
		} else {
		    
		}
		pendingConnectionStartPoint = pendingConnectionEndPoint = null;
	    } else if (addingRelation){
		Node n2 = NodeSet.getNodeFromPoint(nodeSets, e.getPoint());
		if (n2 != null){
		    Node n1 = NodeSet.getNodeFromPoint(nodeSets, pendingConnectionStartPoint);
		    if (Edge.edgeBetween(n1, n2, edges)){
			JOptionPane.showMessageDialog(VisualQuery.this.parentFrame,
						      "Il existe déjà une relation entre ces deux champs.",
						      "Relation interdite",
						      JOptionPane.ERROR_MESSAGE);			

		    } else {
			edges.add(new Edge(n1, n2, Edge.EQUAL));
		    }
		}
	    }
	    addingRelation = false;
	    joining = false;
            e.getComponent().repaint();
        }

	private void showPopupNodeSet(MouseEvent e){
	    origineMenu = new Point(e.getX(), e.getY());
	    popupNodeSet.show(e.getComponent(), e.getX(), e.getY());
	}

	private void showPopupEdge(MouseEvent e){
	    origineMenu = new Point(e.getX(), e.getY());
	    Edge edge = Edge.getEdgeFromPoint(edges, e.getPoint());
	    if (edge.isJoinType()){
		popupJoinEdge.show(e.getComponent(), e.getX(), e.getY());
	    } else if (edge.isRelationType()){
		popupRelationEdge.show(e.getComponent(), e.getX(), e.getY());
	    }
	}

    }


    private static class NodeSet {
	private List<Node> nodes = new ArrayList<Node>();
	private Rectangle b = null;
	private boolean selected = false;
	private Point coin;
	private String name;
	private String alias;
	public static int compteur = 0;
	private static NodeSet first = null;
	public static final int HEADER_HEIGHT = 25;
	public static final int HORIZONTAL_PADDING = 5;

	public Node getNodeFromName(String name){
	    for (Node n : nodes) {
		if (n.getName().equals(name)) {
		    return n;
		}
	    }
	    return null;
	}

	public String getName(){
	    return name;
	}

	public String getAlias(){
	    return alias;
	}

	NodeSet(Point p, String name, String[] champs){
	    for (int i = 0; i < champs.length ; i++){
		nodes.add(new Node(new Point(p.x + NodeSet.HORIZONTAL_PADDING, 
					     p.y + NodeSet.HEADER_HEIGHT + i * Node.LINE_HEIGHT), 
				   Node.RADIUS,
				   champs[i],
				   this));
	    }
	    b = new Rectangle(p, new Dimension(0, 0));
	    this.name = name;
	    this.alias = name + Integer.toString(++compteur);
	    if (NodeSet.first == null){
		NodeSet.first = this;
	    }
	}


	public static NodeSet getFirst(){
	    return first;
	}

	public static void setFirst(NodeSet ns){
	    first = ns;
	}

	public boolean isFirst(){
	    return (this == first);
	}


        /**
         * Update each NodeSet's position by d (delta).
         */
        public static void updatePosition(List<NodeSet> nodeSets, Point d) {
            for (NodeSet ns : nodeSets) {
                if (ns.isSelected()) {
		    // Déplace le NodeSet
		    ns.b.setLocation(ns.b.x + d.x, ns.b.y + d.y);
		    

		    // Déplace tous les Node du NodeSet
		    Node.updatePosition(ns.nodes, d);
                }
            }
        }
	

	/* Détection de chemin de NS2 à NS1 */
	public static boolean existsJoinPath(NodeSet ns1, NodeSet ns2, List<NodeSet> nodeSets, List<Edge> visitedEdges, List<Edge> edges){
	    System.out.println("Début fonction --- NodeSet : " + ns2.hashCode());
	    for (Edge e : edges){
		if (e.isJoinType() && !visitedEdges.contains(e)){
		    System.out.println("Visite d'arête : " +  
				       e.n1.getNodeSet().hashCode() + " --> " +
				       e.n2.getNodeSet().hashCode());
		    
		    Node n_s = null;
		    Node n_d = null;; //source, destination 
		    if (e.n1.getNodeSet() == ns2){// Si e a une extremité dans ns2
			n_s = e.n1;
			n_d = e.n2;
			
	      	    } else if (e.n2.getNodeSet() == ns2){ 
			n_s = e.n2;
			n_d = e.n1;		
		    }
		    
		    if (n_s != null){ // Si on a trouvé une arête
			// trouver le NodeSet destination
			NodeSet ns_d = n_d.getNodeSet();

			if (ns_d == ns1){
			    return true;
			} else {
			    visitedEdges.add(e);
			    if (existsJoinPath(ns1, ns_d, nodeSets, visitedEdges, edges)) return true;
			}
			
		    } else {
			System.out.println("Ne part pas du bon NodeSet : " + ns2.hashCode());		
		    }
		} else {
		    System.out.println("Arête déjà visitée : " +
				       e.n1.getNodeSet().hashCode() + " --> " +
				       e.n2.getNodeSet().hashCode());		    
		}
	    }
	    return false;
	}




	public static void removeSelected(List<NodeSet> list, List<Edge> edges){
            ListIterator<NodeSet> iter = list.listIterator();
            while (iter.hasNext()) {
                NodeSet ns = iter.next();
                if (ns.isSelected()) {
                    removeEdges(ns, edges);
                    iter.remove();
                }
            }
	}

        public static void removeEdges(NodeSet ns, List<Edge> edges) {
            ListIterator<Edge> iter = edges.listIterator();
            while (iter.hasNext()) {
                Edge e = iter.next();
                if (e.n1.getNodeSet() == ns || e.n2.getNodeSet() == ns) {
                    iter.remove();
                }
            }
        }



        /**
         * Draw this NodeSet.
         */
        public void draw(Graphics g) {
	    if (b.width == 0){ // On a encore jamais tracé le rectangle
		FontMetrics fm = g.getFontMetrics();
		int max_width = fm.stringWidth(name.toUpperCase());
		for (Node n : nodes){
		    max_width = Math.max(max_width, fm.stringWidth(n.getName()));
		}
		int width = max_width + 2 * NodeSet.HORIZONTAL_PADDING + 2 * (2 * Node.RADIUS + Node.BETWEEN_SPACE);
		b.setSize(new Dimension(width, 
					nodes.size() * (Node.LINE_HEIGHT) + NodeSet.HEADER_HEIGHT));
	    }

            g.setColor(Color.BLACK);
            g.drawRect(b.x, b.y, b.width, b.height);

	    /* Header */
	    if (isFirst()){
		g.setColor(Color.BLACK);
	    } else {
		g.setColor(Color.GRAY);
	    }
	    g.fillRect(b.x, b.y+1, b.width, NodeSet.HEADER_HEIGHT - 1);
	    g.setColor(Color.WHITE);
	    g.drawString(name.toUpperCase(), 
			 b.x + NodeSet.HORIZONTAL_PADDING, 
			 b.y + NodeSet.HEADER_HEIGHT - (NodeSet.HEADER_HEIGHT - Node.LINE_HEIGHT)/2);


            if (selected) {
                g.setColor(Color.RED);
		// les -2 et +3 servent à avoir un rectangle plus
		// large autour d'un NodeSet sélectionné
                g.drawRect(b.x -2, b.y -2, b.width +4, b.height + 4);
            }

	    g.setColor(Color.GRAY);
	    for (Node n : nodes){
		n.draw(g, b.width);
	    }
        }



        /**
         * Return true if this node is selected.
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Mark this node as selected.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }


        /**
         * Select each node in r.
         */
        public static void selectRect(List<NodeSet> list, Rectangle r) {
            for (NodeSet ns : list) {
                ns.setSelected(r.contains(ns.b));
            }
        }

        /**
         * Select no nodeSet.
         */
        public static void selectNone(List<NodeSet> list) {
            for (NodeSet ns : list) {
                ns.setSelected(false);
            }
        }

        /**
         * Select a single NodeSet; return true if not already selected.
         */
        public static boolean selectOne(List<NodeSet> list, Point p) {
            for (NodeSet ns : list) {
                if (ns.contains(p)) {
                    if (!ns.isSelected()) {
                        NodeSet.selectNone(list);
                        ns.setSelected(true);
                    }
                    return true;
                }
            }
            return false;
        }


        /**
         * Toggle selected state of each node containing p.
         */
        public static void selectToggle(List<NodeSet> list, Point p) {
            for (NodeSet ns : list) {
                if (ns.contains(p)) {
                    ns.setSelected(!ns.isSelected());
                }
            }
        }

	public static NodeSet getNodeSetFromPoint(Point p, List<NodeSet> list){
	    for (NodeSet ns : list){
		if (ns.contains(p)){
		    return ns;
		}
	    }
	    return null;
	}


        /**
         * Return true if this nodeSet contains p.
         */
        public boolean contains(Point p) {
            return b.contains(p);
        }


        /**
         * Return the first found Node containing Point p
         */
        public static Node getNodeFromPoint(List<NodeSet> list, Point p) {
            for (NodeSet ns : list) {
                if (ns.contains(p)) {
		    for (Node n : ns.nodes){
			if (n.contains(p)){
			    return n;
			}
		    }
                }
            }
            return null;
        }



    }


    /**
     * A Node represents a node in a graph.
     */
    private static class Node {
        private Point p; // g pour gauche
        private int r;
	private String s;
        private Rectangle b = new Rectangle();
	private NodeSet nodeSet;
	public static final int BETWEEN_SPACE = 5;
	public static final int LINE_HEIGHT = 12;
	public static final int RADIUS = 5;

	public String getName(){
	    return s;
	}
	
        /**
         * Construct a new node.
         */
        public Node(Point p, int r, String s, NodeSet ns) {
            this.p = p;
            this.r = r;
	    this.s = s;
	    this.nodeSet = ns;
            setBoundary(b);
        }

	public NodeSet getNodeSet(){
	    return nodeSet;
	}

        /**
         * Calculate this node's rectangular boundary.
         */
        private void setBoundary(Rectangle b) {
            b.setBounds(p.x, p.y, 2 * r, 2 * r);
        }


	/* Renvoie les points haut gauche des Nodes 
	 à partir desquels calculer l'Edge */
	public static Point[] getPointsFromNodes(Node n1, Node n2){
	    Point p1 = n1.p;
	    Point p2 = n2.p;
	    double min = Point.distanceSq(n1.p.x, n1.p.y, n2.p.x, n2.p.y);
	    double tmp = Point.distanceSq(n1.p.x, n1.p.y, n2.p.x + n2.getInterspace(), n2.p.y);
	    if (tmp < min){
		min = tmp;
		p2 = new Point(n2.p.x + n2.getInterspace(), n2.p.y);
	    }
	    tmp = Point.distanceSq(n1.p.x + n1.getInterspace(), n1.p.y, n2.p.x, n2.p.y);
	    if (tmp < min){
		min = tmp;
		p1 = new Point(n1.p.x + n1.getInterspace(), n1.p.y);
		p2 = n2.p;
	    }
	    tmp = Point.distanceSq(n1.p.x + n1.getInterspace(), n1.p.y, n2.p.x + n2.getInterspace(), n2.p.y);
	    if (tmp < min){
		min = tmp;
		p1 = new Point(n1.p.x + n1.getInterspace(), n1.p.y);
		p2 = new Point(n2.p.x + n2.getInterspace(), n2.p.y);
	    }
	    
	    return new Point[] {p1, p2};
	}



        /**
         * Draw this node.
         */
        public void draw(Graphics g, int width) {
            g.setColor(Color.BLACK);
	    g.drawString(s, p.x + 2*r + Node.BETWEEN_SPACE, p.y +2*r);
	    // Rond de gauche
            g.drawOval(p.x, p.y, 2*r, 2*r);
	    // Rond de droite
            g.drawOval(p.x + getInterspace(),// - NodeSet.HORIZONTAL_PADDING + width - NodeSet.HORIZONTAL_PADDING - 2*r, 
		       p.y, 2*r, 2*r);
        }

	public void fill(Graphics g){
	    g.fillOval(p.x, p.y, 2 * Node.RADIUS, 2 * Node.RADIUS);
	    g.fillOval(p.x + getInterspace(), p.y, 2 * Node.RADIUS, 2 * Node.RADIUS);
	}

        /**
         * Return this node's location.
         */
        public Point getLocation() {
            return p;
        }

	

        /**
         * Return true if this node contains p.
         */
        public boolean contains(Point p) {
            return (b.contains(p) || b.contains(new Point(p.x - getInterspace(), p.y)));
        }


	public int getInterspace(){
	    return (nodeSet.b.width - (2 * NodeSet.HORIZONTAL_PADDING + 2 * r));
	}

        /**
         * Update each node's position by d (delta).
         */
        public static void updatePosition(List<Node> list, Point d) {
            for (Node n : list) {
		n.p.x += d.x;
		n.p.y += d.y;
                n.setBoundary(n.b);
	    }
	}
    }
    


    /**
     * An Edge is a pair of Nodes.
     */
    private static class Edge {

        private Node n1;
        private Node n2;
	private int relationType;
	private boolean selected = false;
	private final static int TOLERANCE = 10;
	private final static int ARROW_HALF_WIDTH = 5;
	private final static int ARROW_DEPTH = 15;
	private final static int JOIN_SIZE = 10;
	public final static int LIM_INF_JOIN = -1;
	public final static int LEFT = -1;
	public final static int INNER = 0; 
	public final static int RIGHT = 1; // Jamais un Edge ne doit être RIGHT
	public final static int LIM_SUP_JOIN = 1;
	public final static int LIM_INF_RELATION = 2;
	public final static int EQUAL = 2;
	public final static int LOWER = 3;
	public final static int GREATER = 4;
	public final static int LIM_SUP_RELATION = 4;

        public Edge(Node n1, Node n2) {
	    this(n1, n2, Edge.LEFT);
        }

	public Edge(Node n1, Node n2, int type){
            this.n1 = n1;
            this.n2 = n2;
	    this.relationType = type;
	}

	public String getRepresentation(){
	    switch(relationType){
	    case Edge.INNER: return("INNER JOIN");
	    case Edge.LEFT: return("LEFT JOIN"); 
	    }
	    return null;
	}

	public String getConditionRepresentation(){
	    switch(relationType){
	    case Edge.INNER: return("=");
	    case Edge.LEFT: return("="); 
	    case Edge.EQUAL: return("="); 
	    case Edge.LOWER: return("<");
	    case Edge.GREATER: return(">");
	    }
	    return null;
	}


	/* Type de relation */
	public boolean isJoinType(){
	    return (relationType >= Edge.LIM_INF_JOIN && relationType <= Edge.LIM_SUP_JOIN);
	}

	public boolean isRelationType(){
	    return (relationType >= Edge.LIM_INF_RELATION && relationType <= Edge.LIM_SUP_RELATION);
	}



        /**
         * Return the first found Node containing Point p
         */
        public static Edge getEdgeFromPoint(List<Edge> edges, Point p) {
            for (Edge e : edges) {
                if (e.contains(p)) {
		    return e;
		}
	    }
            return null;
        }


	
	// Edges entre deux NodeSets
	public static List<Edge> edgesBetween(NodeSet ns1, NodeSet ns2, List<Edge> list){
	    List<Edge> retour = new ArrayList<Edge>();
	    for (Edge e : list){
		if ((e.n1.getNodeSet() == ns1 && e.n2.getNodeSet() == ns2) ||
		    (e.n2.getNodeSet() == ns1 && e.n1.getNodeSet() == ns2)){
		    retour.add(e);
		}
	    }
	    return retour;
	}


	// Edges entre deux Nodes
	public static boolean edgeBetween(Node n1, Node n2, List<Edge> list){
	    for (Edge e : list){
		if ((e.n1 == n1 && e.n2 == n2) ||
		    (e.n2 == n1 && e.n1 == n2)){
		    return true;
		}
	    }
	    return false;

	}




	public static void removeSelected(List<Edge> list){
            ListIterator<Edge> iter = list.listIterator();
            while (iter.hasNext()) {
                Edge e = iter.next();
                if (e.isSelected()) {
                    iter.remove();
                }
            }
	}


        /**
         * Select a single Edge; return true if not already selected.
         */
        public static boolean selectOne(List<Edge> list, Point p) {
            for (Edge e : list) {
                if (e.contains(p)) {
                    if (!e.isSelected()) {
                        Edge.selectNone(list); // déselectionne les autres
                        e.setSelected(true);
                    }
		    return true;
                }
            }
            return false;
        }

        /**
         * Select no Edge.
         */
        public static void selectNone(List<Edge> list) {
            for (Edge e : list) {
                e.setSelected(false);
            }
        }


	public boolean contains(Point p){
	    Point[] points = Node.getPointsFromNodes(n1, n2);
	    Point p1 = points[0];
	    Point p2 = points[1];
	    if (p.x > Math.min(p1.x + Node.RADIUS, p2.x + Node.RADIUS) && 
		p.x < Math.max(p1.x + Node.RADIUS, p2.x + Node.RADIUS) &&
		p.y > Math.min(p1.y + Node.RADIUS, p2.y + Node.RADIUS) &&
		p.y < Math.max(p1.y + Node.RADIUS, p2.y + Node.RADIUS)){
		if (p2.x == p1.x){ // Si arête verticale
		    return (Math.abs(p.x - (p1.x + Node.RADIUS)) < Edge.TOLERANCE);
		} else {
		    double m = (double) (p2.y - p1.y) / (double) (p2.x - p1.x);		
		    if (Math.abs( (p.y - (p1.y + Node.RADIUS)) - m * (p.x - (p1.x + Node.RADIUS))) < Edge.TOLERANCE){
			return true;
		    }
		}
	    }
	    return false;
	}

 

        /**
         * Return true if this node is selected.
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Mark this node as selected.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
        }


      
       /* Dessin de l'Edge */
       public void draw(Graphics g) {
	   Point[] points = Node.getPointsFromNodes(n1, n2);
	   Point p1 = points[0];
	   Point p2 = points[1];
	   if (isJoinType()){
		if (selected) {
		    g.setColor(Color.RED);
		} else {
		    g.setColor(Color.GRAY);
		}

		// Remplit les nodes liés à l'arête
		g.drawLine(p1.x + Node.RADIUS, p1.y + Node.RADIUS, p2.x + Node.RADIUS, p2.y + Node.RADIUS);
		n1.fill(g);
		n2.fill(g);


		// Dessin du chapeau de la flèche
		double m = (double) (p2.y - p1.y) / (double) (p2.x - p1.x);
		double sx = Math.signum(p2.x - p1.x);
		double x3 = (p2.x + Node.RADIUS) - sx * (Edge.ARROW_DEPTH - m * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		double y3 = (p2.y + Node.RADIUS) - sx * (Edge.ARROW_DEPTH * m + Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		double x4 = x3 - sx * (2 * m * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		double y4 = y3 + sx * (2 * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		int[] x = {(int) x3, (int) x4, (int) p2.x + Node.RADIUS};
		int[] y = {(int) y3, (int) y4, (int) p2.y + Node.RADIUS};
		g.fillPolygon(x, y, 3);
		if (relationType == Edge.INNER){ // Traite le deuxième chapeau pour les IJ
		    x3 = (p1.x + Node.RADIUS) + sx * (Edge.ARROW_DEPTH - m * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		    y3 = (p1.y + Node.RADIUS) + sx * (Edge.ARROW_DEPTH * m + Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		    x4 = x3 + sx * (2 * m * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		    y4 = y3 - sx * (2 * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
		    int[] x_inner = {(int) x3, (int) x4, (int) p1.x + Node.RADIUS};
		    int[] y_inner = {(int) y3, (int) y4, (int) p1.y + Node.RADIUS};
		    g.fillPolygon(x_inner, y_inner, 3);
		}
	    


		// Dessin du type de relation
		// Le carré de jointure
		int milieux = Node.RADIUS + (p1.x + p2.x) / 2;
		int milieuy = Node.RADIUS + (p1.y + p2.y) / 2;
		g.setColor(Color.GRAY);
		g.fillRoundRect(milieux - Edge.JOIN_SIZE,
				milieuy - Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				Node.RADIUS,
				Node.RADIUS);
		if (selected) {
		    g.setColor(Color.RED);
		} else {
		    g.setColor(Color.BLACK);
		}
		g.drawRoundRect(milieux - Edge.JOIN_SIZE,
				milieuy - Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				Node.RADIUS,
				Node.RADIUS);
		// L'intérieur du carré
		g.setColor(Color.WHITE);
		String s = null;
		switch(relationType){
		case Edge.LEFT : 
		    s = "LJ";
		    break;
		case Edge.INNER :
		    s = "IJ";
		    break;
		}
		FontMetrics fm = g.getFontMetrics();
		int width = fm.stringWidth(s);
		g.drawString(s, 
			     milieux - Edge.JOIN_SIZE + (2 * Edge.JOIN_SIZE - width) / 2,
			     milieuy + Edge.JOIN_SIZE - (int) ((2 * Edge.JOIN_SIZE - 0.8 * fm.getHeight()) / 2));
	    } else if (isRelationType()){
		if (selected) {
		    g.setColor(Color.CYAN);
		} else {
		    g.setColor(Color.BLUE);
		}
		// Remplit les nodes liés à l'arête
		g.drawLine(p1.x + Node.RADIUS, p1.y + Node.RADIUS, p2.x + Node.RADIUS, p2.y + Node.RADIUS);
		g.fillOval(p1.x, p1.y, 2 * Node.RADIUS, 2 * Node.RADIUS);
		g.fillOval(p2.x, p2.y, 2 * Node.RADIUS, 2 * Node.RADIUS);


		// Dessin du type de relation
		// Le carré de la relation
		int tiersx = p1.x + Node.RADIUS + (int) ((p2.x - p1.x) * .4);
		int tiersy = p1.y + Node.RADIUS + (int) ((p2.y - p1.y) * .4);
		g.setColor(Color.BLUE);
		g.fillRoundRect(tiersx - Edge.JOIN_SIZE,
				tiersy - Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				Node.RADIUS,
				Node.RADIUS);
		if (selected) {
		    g.setColor(Color.CYAN);
		} else {
		    g.setColor(Color.BLACK);
		}
		g.drawRoundRect(tiersx - Edge.JOIN_SIZE,
				tiersy - Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				2 * Edge.JOIN_SIZE,
				Node.RADIUS,
				Node.RADIUS);
		// L'intérieur du carré
		g.setColor(Color.WHITE);
		String s = null;
		switch(relationType){
		case Edge.EQUAL : 
		    s = "=";
		    break;
		case Edge.LOWER :
		    s = "<";
		    break;
		case Edge.GREATER :
		    s = ">";
		    break;
		}
		FontMetrics fm = g.getFontMetrics();
		int width = fm.stringWidth(s);
		g.drawString(s, 
			     tiersx - Edge.JOIN_SIZE + (2 * Edge.JOIN_SIZE - width) / 2,
			     tiersy + Edge.JOIN_SIZE - (int) ((2 * Edge.JOIN_SIZE - 0.8 * fm.getHeight()) / 2));
	    }
       }
    }
}