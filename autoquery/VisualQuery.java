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

    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;
    private boolean connecting = false;

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

    public VisualQuery(JFrame f) {
        this.setOpaque(true);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
	this.parentFrame = f;
    }

    

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(0x00f0f0f0));
        g.fillRect(0, 0, getWidth(), getHeight());
	if (connecting){
	    g.setColor(Color.red);
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
            g.setColor(Color.darkGray);
            g.drawRect(mouseRect.x, mouseRect.y,
                mouseRect.width, mouseRect.height);
        }
    }

    public void addNodeSet(String table_nom, String[] champs){
	NodeSet ns = new NodeSet(new Point(10, 10), table_nom, champs);
	ns.setSelected(false);
	nodeSets.add(ns);
	repaint();
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
            } else if (connecting){
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
	private final static int RELATION = 1;

	// Popup des tables
	private JPopupMenu popupNodeSet;
	private Action deleteNodeSet = new DeleteAction("Retirer la table", TABLE);

	// Popup des relations
	private JPopupMenu popupEdge;
	private Action deleteEdge = new DeleteAction("Retirer la relation", RELATION);
	private Action changeRelationTypeToLeftJoin = new ChangeRelationType("LEFT JOIN", Edge.LEFT);
	private Action changeRelationTypeToRightJoin = new ChangeRelationType("RIGHT JOIN", Edge.RIGHT);
	private Action changeRelationTypeToInnerJoin = new ChangeRelationType("INNER JOIN", Edge.INNER);

	public MouseHandler(){
	    popupNodeSet = new JPopupMenu();
	    popupNodeSet.add(new JMenuItem(deleteNodeSet));
	    popupEdge = new JPopupMenu();
	    popupEdge.add(new JMenuItem(deleteEdge));
	    popupEdge.add(new JMenuItem(changeRelationTypeToLeftJoin));
	    popupEdge.add(new JMenuItem(changeRelationTypeToRightJoin));
	    popupEdge.add(new JMenuItem(changeRelationTypeToInnerJoin));
	    
	}

	private class ChangeRelationType extends AbstractAction{
	    int type;

	    ChangeRelationType(String s, int type){
		super(s);
		this.type = type;
	    }
	    
	    public void actionPerformed(ActionEvent e){
		Edge.changeTypeOfSelected(edges, type);
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
		} else if (kind == MouseHandler.RELATION){
		    Edge.removeSelected(edges);
		}
		repaint();
	    }
	}

        @Override
        public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();
            if (e.isShiftDown()) {
		/* Ajoute ou supprime un élément de la sélection */
				NodeSet.selectToggle(nodeSets, mousePt);
		//		String[] champs = {"Salut", "Comment", "Ça", "Va ?"};
		//		addNodeSet("Coucou", champs);
				// Me sert lorsque j'exécute VisualQuery en standalone, pour avoir des tables de jeu.

		
            } else if (e.isControlDown()) {
		connecting = true;
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
	    if (connecting){
		// Tester si on a connecté deux Nodes
		Node n2 = NodeSet.getNodeFromPoint(nodeSets, e.getPoint());
		if (n2 != null){
		    Node n1 = NodeSet.getNodeFromPoint(nodeSets, pendingConnectionStartPoint);
		    if(NodeSet.existsPath(n1.getNodeSet(), 
					  n2.getNodeSet(), 
					  nodeSets, 
					  Edge.edgesBetween(n1.getNodeSet(), n2.getNodeSet(), edges), 
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
	    }

	    connecting = false;
            e.getComponent().repaint();
        }

	private void showPopupNodeSet(MouseEvent e){
	    popupNodeSet.show(e.getComponent(), e.getX(), e.getY());
	}

	private void showPopupEdge(MouseEvent e){
	    popupEdge.show(e.getComponent(), e.getX(), e.getY());
	}

    }

   

    /**
     * The kinds of node in a graph.
     */
    private enum Kind {
        Table;
    }



    private static class NodeSet {
	private List<Node> nodes = new ArrayList<Node>();
	private Rectangle b = null;
	private boolean selected = false;
	private Point coin;
	private String name;
	public static final int HEADER_HEIGHT = 25;
	public static final int HORIZONTAL_PADDING = 5;

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
	

	/* Détection de chemin entre NS1 et NS2 */
	public static boolean existsPath(NodeSet ns1, NodeSet ns2, List<NodeSet> nodeSets, List<Edge> visitedEdges, List<Edge> edges){
	    System.out.println("Début fonction --- NodeSet : " + ns2.hashCode());
	    for (Edge e : edges){
		if (!visitedEdges.contains(e)){
		    System.out.println("Visite d'arête : " +  
				       e.n1.getNodeSet().hashCode() + " --> " +
				       e.n2.getNodeSet().hashCode());
		    
		    Node n_s = null;
		    Node n_d = null;; //source, destination 
		    if (ns2.nodes.contains(e.n1)){// Si e a une extremité dans ns2
			n_s = e.n1;
			n_d = e.n2;
			
		    } else if (ns2.nodes.contains(e.n2)){ 
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
			    if (existsPath(ns1, ns_d, nodeSets, visitedEdges, edges)) return true;
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
                if (ns.nodes.contains((Node) e.n1) || ns.nodes.contains((Node) e.n2)) {
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
		    max_width = Math.max(max_width, fm.stringWidth(n.getString()));
		}
		b.setSize(new Dimension(max_width + 2 * NodeSet.HORIZONTAL_PADDING + 2 * Node.RADIUS + Node.BETWEEN_SPACE, 
					nodes.size() * (Node.LINE_HEIGHT) + NodeSet.HEADER_HEIGHT));
	    }

            g.setColor(Color.black);
            g.drawRect(b.x, b.y, b.width, b.height);

	    /* Header */
	    g.setColor(Color.gray);
	    g.fillRect(b.x, b.y+1, b.width, NodeSet.HEADER_HEIGHT - 1);
	    g.setColor(Color.white);
	    g.drawString(name.toUpperCase(), 
			 b.x + NodeSet.HORIZONTAL_PADDING, 
			 b.y + NodeSet.HEADER_HEIGHT - (NodeSet.HEADER_HEIGHT - Node.LINE_HEIGHT)/2);



            if (selected) {
                g.setColor(Color.red);
		// les -2 et +3 servent à avoir un rectangle plus
		// large autour d'un objet sélectionné
                g.drawRect(b.x -2, b.y -2, b.width +4, b.height + 4);
            }
	    g.setColor(Color.gray);
	    for (Node n : nodes){
		n.draw(g);
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
        private Point p;
        private int r;
	private String s;
        private Rectangle b = new Rectangle();
	private NodeSet nodeSet;
	public static final int BETWEEN_SPACE = 5;
	public static final int LINE_HEIGHT = 12;
	public static final int RADIUS = 5;

	public String getString(){
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

        /**
         * Draw this node.
         */
        public void draw(Graphics g) {
            g.setColor(Color.black);
	    g.drawString(s, p.x + 2*r + Node.BETWEEN_SPACE, p.y +2*r);
            g.drawOval(p.x, p.y, 2*r, 2*r);
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
            return b.contains(p);
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
	private int relationType = Edge.LEFT;
	private boolean selected = false;
	private final static int TOLERANCE = 10;
	private final static int ARROW_HALF_WIDTH = 5;
	private final static int ARROW_DEPTH = 15;
	private final static int JOIN_SIZE = 10;
	public final static int LEFT = 0;
	public final static int RIGHT = 1;
	public final static int INNER = 2;

        public Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
        }


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


	public static void changeTypeOfSelected(List<Edge> list, int type){
	    for (Edge e : list){
		if (e.isSelected()){
		    e.relationType = type;
		}
	    }
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
	    if (p.x > Math.min(n1.p.x + Node.RADIUS, n2.p.x + Node.RADIUS) && 
		p.x < Math.max(n1.p.x + Node.RADIUS, n2.p.x + Node.RADIUS) &&
		p.y > Math.min(n1.p.y + Node.RADIUS, n2.p.y + Node.RADIUS) &&
		p.y < Math.max(n1.p.y + Node.RADIUS, n2.p.y + Node.RADIUS)){
		if (n2.p.x == n1.p.x){ // Si arête verticale
		    return (Math.abs(p.x - (n1.p.x + Node.RADIUS)) < Edge.TOLERANCE);
		} else {
		    double m = (double) (n2.p.y - n1.p.y) / (double) (n2.p.x - n1.p.x);		
		    if (Math.abs( (p.y - (n1.p.y + Node.RADIUS)) - m * (p.x - (n1.p.x + Node.RADIUS))) < Edge.TOLERANCE){
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

       public void draw(Graphics g) {
            Point p1 = n1.getLocation();
            Point p2 = n2.getLocation();
            if (selected) {
		g.setColor(Color.red);
	    } else {
		g.setColor(Color.gray);
	    }
            g.drawLine(p1.x + Node.RADIUS, p1.y + Node.RADIUS, p2.x + Node.RADIUS, p2.y + Node.RADIUS);
	    g.fillOval(p1.x, p1.y, 2 * Node.RADIUS, 2 * Node.RADIUS);
	    g.fillOval(p2.x, p2.y, 2 * Node.RADIUS, 2 * Node.RADIUS);


	    // Dessin du chapeau de la flèche
	    double m = (double) (n2.p.y - n1.p.y) / (double) (n2.p.x - n1.p.x);
	    double sx = Math.signum(n2.p.x - n1.p.x);
	    double x3 = (n2.p.x + Node.RADIUS) - sx * (Edge.ARROW_DEPTH - m * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
	    double y3 = (n2.p.y + Node.RADIUS) - sx * (Edge.ARROW_DEPTH * m + Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
	    double x4 = x3 - sx * (2 * m * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
	    double y4 = y3 + sx * (2 * Edge.ARROW_HALF_WIDTH) / Math.sqrt(1 + m*m);
	    int[] x = {(int) x3, (int) x4, (int) n2.p.x + Node.RADIUS};
	    int[] y = {(int) y3, (int) y4, (int) n2.p.y + Node.RADIUS};
	    g.fillPolygon(x, y, 3);
	    


	    // Dessin du type de relation
	    // Le carré
	    int milieux = Node.RADIUS + (n1.p.x + n2.p.x) / 2;
	    int milieuy = Node.RADIUS + (n1.p.y + n2.p.y) / 2;
	    g.setColor(Color.gray);
	    g.fillRoundRect(milieux - Edge.JOIN_SIZE,
			    milieuy - Edge.JOIN_SIZE,
			    2 * Edge.JOIN_SIZE,
			    2 * Edge.JOIN_SIZE,
			    Node.RADIUS,
			    Node.RADIUS);
	    if (selected) {
		g.setColor(Color.red);
	    } else {
		g.setColor(Color.black);
	    }
	    g.drawRoundRect(milieux - Edge.JOIN_SIZE,
			    milieuy - Edge.JOIN_SIZE,
			    2 * Edge.JOIN_SIZE,
			    2 * Edge.JOIN_SIZE,
			    Node.RADIUS,
			    Node.RADIUS);
	    // L'intérieur du carré
	    g.setColor(Color.white);
	    String s = null;
	    switch(relationType){
	    case Edge.LEFT : 
		s = "LJ";
		break;
	    case Edge.RIGHT :
		s = "RJ";
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
        }
    }
}