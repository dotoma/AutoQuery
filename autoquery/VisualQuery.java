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
    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;
    private boolean connecting = false;

    private Point mousePt = new Point(WIDE / 2, HIGH / 2);
    private Point pendingConnectionStartPoint;
    private Point pendingConnectionEndPoint;

    
    private List<NodeSet> nodeSets = new ArrayList<NodeSet>();
    private List<NodeSet> selected = new ArrayList<NodeSet>();
    private List<Edge> edges = new ArrayList<Edge>();
    

    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                JFrame f = new JFrame("VisualQuery");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                VisualQuery vq = new VisualQuery();
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

    public VisualQuery() {
        this.setOpaque(true);
        this.addMouseListener(new MouseHandler());
        this.addMouseMotionListener(new MouseMotionHandler());
    }

    

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(0x00f0f0f0));
        g.fillRect(0, 0, getWidth(), getHeight());
    	for (Edge e : edges) {
            e.draw(g);
    	}
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
	private JPopupMenu popup;
	private Action delete = new DeleteAction("Retirer la table");

	public MouseHandler(){
	    popup = new JPopupMenu();
	    popup.add(new JMenuItem(delete));
	}

	private class DeleteAction extends AbstractAction{
	    DeleteAction(String s){
		super(s);
	    }
	    
	    public void actionPerformed(ActionEvent e){
		NodeSet.removeSelected(nodeSets, edges);
		repaint();
	    }
	}

        @Override
        public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();
            if (e.isShiftDown()) {
		/* Ajoute ou supprime un élément de la sélection */
				NodeSet.selectToggle(nodeSets, mousePt);
		//				String[] champs = {"Salut", "Comment", "Ça", "Va ?"};
		//		addNodeSet("Coucou", champs);
				// Me sert lorsque j'exécute VisualQuery en standalone, pour avoir des tables de jeu.

		
            } else if (e.isControlDown()) {
		connecting = true;
		pendingConnectionStartPoint = pendingConnectionEndPoint =  e.getPoint();
	    } else if (e.isPopupTrigger()) {
		/* Si menu contextuel */
		if (NodeSet.selectOne(nodeSets, mousePt)){
		    showPopup(e); 
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
		    showPopup(e); 
		}
	    }
	    if (connecting){
		// Tester si on a connecté deux Nodes
		Node n2 = NodeSet.getNodeFromPoint(nodeSets, e.getPoint());
		if (n2 != null){
		    Node n1 = NodeSet.getNodeFromPoint(nodeSets, pendingConnectionStartPoint);
		    edges.add(new Edge(n1, n2));

		} else {
		    
		}
		pendingConnectionStartPoint = pendingConnectionEndPoint = null;
	    }

	    connecting = false;
            e.getComponent().repaint();
        }

	private void showPopup(MouseEvent e){
	    popup.show(e.getComponent(), e.getX(), e.getY());
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
				   champs[i]));
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
	public static final int BETWEEN_SPACE = 5;
	public static final int LINE_HEIGHT = 12;
	public static final int RADIUS = 5;

	public String getString(){
	    return s;
	}
	
        /**
         * Construct a new node.
         */
        public Node(Point p, int r, String s) {
            this.p = p;
            this.r = r;
	    this.s = s;
            setBoundary(b);
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
	private boolean selected = false;
	private final static int TOLERANCE = 10;
	private final static int ARROW_HALF_WIDTH = 5;
	private final static int ARROW_DEPTH = 15;
        public Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
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
	    if (p.x > Math.min(n1.p.x, n2.p.x) && 
		p.x < Math.max(n1.p.x, n2.p.x) &&
		p.y > Math.min(n1.p.y, n2.p.y) &&
		p.y < Math.max(n1.p.y, n2.p.y)){
		if (n2.p.x == n1.p.x){ // Si arête verticale
		    return (Math.abs(p.x - n1.p.x) < Edge.TOLERANCE);
		} else {
		    double m = (double) (n2.p.y - n1.p.y) / (double) (n2.p.x - n1.p.x);		
		    if (Math.abs( (p.y - n1.p.y) - m * (p.x - n1.p.x)) < Edge.TOLERANCE){
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
	    

        }
    }
}