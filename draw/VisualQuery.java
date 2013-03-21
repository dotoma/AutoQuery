package draw;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.*;
import javax.swing.event.*;

public class VisualQuery extends JComponent {
    private static final int WIDE = 640;
    private static final int HIGH = 480;
    private Rectangle mouseRect = new Rectangle();
    private boolean selecting = false;
    private boolean connecting = false;

    private Point mousePt = new Point(WIDE / 2, HIGH / 2);
    private Point pendingConnectionStartPoint;
    private Point pendingConnectionEndPoint;

    
    private List<Node> nodes = new ArrayList<Node>();
    private List<Node> selected = new ArrayList<Node>();
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
        for (Node n : nodes) {
            n.draw(g);
        }
        if (selecting) {
            g.setColor(Color.darkGray);
            g.drawRect(mouseRect.x, mouseRect.y,
                mouseRect.width, mouseRect.height);
        }
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
                Node.selectRect(nodes, mouseRect);
            } else if (connecting){
		pendingConnectionEndPoint = e.getPoint();
	    } else {
                delta.setLocation(
                    e.getX() - mousePt.x,
                    e.getY() - mousePt.y);
                Node.updatePosition(nodes, delta);
                mousePt = e.getPoint();
            }
            e.getComponent().repaint();
        }
    }



    private class MouseHandler extends MouseAdapter{

        @Override
        public void mousePressed(MouseEvent e) {
            mousePt = e.getPoint();
            if (e.isShiftDown()) {
		/* Ajoute ou supprime un élément de la sélection
		  Node.selectToggle(nodes, mousePt); */
            Node.selectNone(nodes);
            Point p = mousePt.getLocation();
            Node n = new Node(p, 30, Color.black, Kind.Table);
            n.setSelected(false);
            nodes.add(n);

		
            } else if (e.isControlDown()) {
		connecting = true;
		pendingConnectionStartPoint = pendingConnectionEndPoint =  e.getPoint();
	    } else if (e.isPopupTrigger()) {
		/* Si menu contextuel
                Node.selectOne(nodes, mousePt);
                showPopup(e); */
            } else if (Node.selectOne(nodes, mousePt)) {
		/* Sélectionne l'élément contenant le point du clic 
		 si le clic est sur un élément */
                selecting = false;
            } else {
		/* Si le clic est hors de tout élément,
		 on déselectionne tous les éléments et on passe
		en mode sélection */
                Node.selectNone(nodes);
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

	    if (connecting){
		connecting = false;
		// Tester si on a connecté deux Nodes
		Node n2 = Node.getNodeFromPoint(nodes, e.getPoint());
		if (n2 != null){
		    Node n1 = Node.getNodeFromPoint(nodes, pendingConnectionStartPoint);
		    edges.add(new Edge(n1, n2));

		    System.out.println("Connexion !");
		} else {
		    System.out.println("Pas de connexion :-(");
		    
		}
		pendingConnectionStartPoint = pendingConnectionEndPoint = null;
	    }

	    // Si c'est un clic pour menu
            if (e.isPopupTrigger()) {
                //showPopup(e);
            }
            e.getComponent().repaint();
        }

	

    }

   

    /**
     * The kinds of node in a graph.
     */
    private enum Kind {
        Table;
    }




    /**
     * A Node represents a node in a graph.
     */
    private static class Node {

        private Point p;
        private int r;
        private Color color;
        private Kind kind; 
        private boolean selected = false;
        private Rectangle b = new Rectangle();

        /**
         * Construct a new node.
         */
        public Node(Point p, int r, Color color, Kind kind) {
            this.p = p;
            this.r = r;
            this.color = color;
            this.kind = kind;
            setBoundary(b);
        }

        /**
         * Calculate this node's rectangular boundary.
         */
        private void setBoundary(Rectangle b) {
            b.setBounds(p.x - r, p.y - r, 2 * r, 2 * r);
        }

        /**
         * Draw this node.
         */
        public void draw(Graphics g) {
            g.setColor(this.color);
            if (this.kind == Kind.Table) {
                g.fillRect(b.x, b.y, b.width, b.height);
            }
            if (selected) {
                g.setColor(Color.red);
		// les -2 et +4 servent à avoir un rectangle plus
		// large autour d'un objet sélectionné
                g.drawRect(b.x-2, b.y-2, b.width+3, b.height+3);
            }
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
         * Collected all the selected nodes in list.
         */
        public static void getSelected(List<Node> list, List<Node> selected) {
            selected.clear();
            for (Node n : list) {
                if (n.isSelected()) {
                    selected.add(n);
                }
            }
        }

        /**
         * Select no nodes.
         */
        public static void selectNone(List<Node> list) {
            for (Node n : list) {
                n.setSelected(false);
            }
        }

        /**
         * Select a single node; return true if not already selected.
         */
        public static boolean selectOne(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    if (!n.isSelected()) {
                        Node.selectNone(list);
                        n.setSelected(true);
                    }
                    return true;
                }
            }
            return false;
        }

        /**
         * Return the first found Node containing Point p
         */
        public static Node getNodeFromPoint(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    return n;
                }
            }
            return null;
        }




        /**
         * Select each node in r.
         */
        public static void selectRect(List<Node> list, Rectangle r) {
            for (Node n : list) {
                n.setSelected(r.contains(n.p));
            }
        }

        /**
         * Toggle selected state of each node containing p.
         */
        public static void selectToggle(List<Node> list, Point p) {
            for (Node n : list) {
                if (n.contains(p)) {
                    n.setSelected(!n.isSelected());
                }
            }
        }

        /**
         * Update each node's position by d (delta).
         */
        public static void updatePosition(List<Node> list, Point d) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.p.x += d.x;
                    n.p.y += d.y;
                    n.setBoundary(n.b);
                }
            }
        }

        /**
         * Update each node's radius r.
         */
        public static void updateRadius(List<Node> list, int r) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.r = r;
                    n.setBoundary(n.b);
                }
            }
        }

        /**
         * Update each node's color.
         */
        public static void updateColor(List<Node> list, Color color) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.color = color;
                }
            }
        }

        /**
         * Update each node's kind.
         */
        public static void updateKind(List<Node> list, Kind kind) {
            for (Node n : list) {
                if (n.isSelected()) {
                    n.kind = kind;
                }
            }
        }
    }


    /**
     * An Edge is a pair of Nodes.
     */
    private static class Edge {

        private Node n1;
        private Node n2;

        public Edge(Node n1, Node n2) {
            this.n1 = n1;
            this.n2 = n2;
        }

        public void draw(Graphics g) {
            Point p1 = n1.getLocation();
            Point p2 = n2.getLocation();
            g.setColor(Color.gray);
            g.drawLine(p1.x, p1.y, p2.x, p2.y);
        }
    }


}