package autoquery;

import javax.swing.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;

/**
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and 
 * a JButton to close the tab it belongs to 
 */ 
public class ButtonTabComponent extends JPanel {
    private final JTabbedPane pane;
    private final AutoQuery app;

    private boolean cannot_be_closed = false;

    public ButtonTabComponent(final JTabbedPane pane, final AutoQuery app) {
        //unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        System.out.println("Réf : " + ButtonTabComponent.this.hashCode());
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        setOpaque(false);
	this.app = app;

        //make JLabel read titles from JTabbedPane
        final JLabel label = new JLabel() {
            public String getText() {
                int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) {
                    return pane.getTitleAt(i);
                }
                return null;
            }
        };
        
        label.addMouseListener(new MouseAdapter() { 
		public void mouseReleased(MouseEvent e) {
		    maybeShowPopup(e); // Sous Windows
		}


		public void mousePressed(MouseEvent e) {
		    maybeShowPopup(e); // Sous Linux
		}
		
		private void maybeShowPopup(MouseEvent e) {
		    if (e.isPopupTrigger()) {
			JPopupMenu popupMenu = new JPopupMenu();
			/* Cas où la colonne est liée à une table de référence */
			String s = (cannot_be_closed) ? "Déverrouiller" : "Verrouiller";
			JMenuItem menuVerrouillerOnglet = new JMenuItem(s);
			menuVerrouillerOnglet.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent ae){
				    cannot_be_closed = !cannot_be_closed;
				    
				}
			    });
			popupMenu.add(menuVerrouillerOnglet);
			popupMenu.show(e.getComponent(), e.getX(), e.getY());
		    }
		}


		public void mouseClicked(MouseEvent e) 
		{ 
		    if (e.getClickCount() == 2) 
			{ 
			    JTextField editor = getEditorComponent(label, label.getText()); 
 
			    pane.setTabComponentAt(pane.indexOfTabComponent(ButtonTabComponent.this), editor); 
			    editor.requestFocus(); 
			    editor.selectAll(); 
			    if (editor.getPreferredSize().width < 100) 
				editor.setPreferredSize(new Dimension(100, editor.getPreferredSize().height)); 
			} 
		    else 
			{ 
			    if (pane.getSelectedIndex() != pane.indexOfTabComponent(ButtonTabComponent.this)) 
				pane.setSelectedIndex(pane.indexOfTabComponent(ButtonTabComponent.this)); 
			    //pane.requestFocus(); Mis en commentaire par MAD sinon problèmes de focus
			} 
		}
		private JTextField getEditorComponent(final JLabel tabLabel, String text) 
		{ 
		    final JTextField editor = new JTextField(text); 
		    editor.addKeyListener(new KeyAdapter() 
			{ 
			    public void keyReleased(KeyEvent e) 
			    { 
				if (e.getKeyCode() == KeyEvent.VK_ENTER) 
				    { 
					pane.setTitleAt(pane.getSelectedIndex(), editor.getText()); 
					pane.setTabComponentAt(pane.getSelectedIndex(), ButtonTabComponent.this); 
				    } 
			    } 
			}); 
		    editor.addFocusListener(new FocusAdapter() 
			{ 
			    public void focusLost(FocusEvent e) 
			    { 
				tabLabel.setText(editor.getText()); 
				pane.setTabComponentAt(pane.getSelectedIndex(), ButtonTabComponent.this);
			    } 
			}); 
		    return editor; 
		} 
            
        }); 
       
        add(label);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        //tab button
        JButton button = new TabButton();
        add(button);
        //add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    
    private class TabButton extends JButton implements ActionListener {
	Color closeableBackground;
	
        public TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("close this tab");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            //Close the proper tab by clicking the button
            addActionListener(this);

	    closeableBackground = getBackground();
        }

        public void actionPerformed(ActionEvent e) {
	    int i = pane.indexOfTabComponent(ButtonTabComponent.this);
	    if (!cannot_be_closed){ // S'il n'a pas été protégé contre la fermeture
		if (i != -1) {
		    app.deleteTab(i);
		}
	    } else {
		app.showStatus(i, "L'onglet est verrouillé.");
	    }
	}

        //we don't want to update UI for this button
        public void updateUI() {
        }

        //paint the cross
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
	    
	    if (cannot_be_closed){
		setBackground(Color.RED);
	    } else {
		setBackground(closeableBackground);
	    }

            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover() && !cannot_be_closed) {
                g2.setColor(Color.RED);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}
