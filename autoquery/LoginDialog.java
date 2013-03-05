package autoquery;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
 
public class LoginDialog extends JDialog {
	private JTextField tfHost;
	private JLabel lbHost;
	private JTextField tfPort;
	private JLabel lbPort;
	private JTextField tfUsername;
	private JLabel lbUsername;
	private JPasswordField pfPassword;
	private JLabel lbPassword;	
	private JButton btnLogin;
	private JButton btnCancel;
	private boolean succeeded = false;
 
    public LoginDialog(Frame parent) {
        super(parent, "Gestionnaire de connexion", true);
        //
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
 
        cs.fill = GridBagConstraints.HORIZONTAL;
 
        lbHost = new JLabel("Hôte");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbHost, cs);
 
        tfHost = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfHost, cs);
        
        lbPort = new JLabel("Port ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPort, cs);
 
        tfPort = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(tfPort, cs);
       
        lbUsername = new JLabel("Utilisateur");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);
 
        tfUsername = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);
 
        lbPassword = new JLabel("Mot de passe ");
        cs.gridx = 0;
        cs.gridy = 3;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);
 
        pfPassword = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 3;
        cs.gridwidth = 2;
        panel.add(pfPassword, cs);
        panel.setBorder(new LineBorder(Color.GRAY));
 
        btnLogin = new JButton("Login");
 
        btnLogin.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent ae) {
            	    /* Test de connexion avec les paramètres fournis */ 
            	    Connection con = null;
            	    try {
            	    	    System.out.println("Chargement du pilote...");
            	    	    Class.forName("com.mysql.jdbc.Driver");
            	    	    System.out.println("Connexion...");
            	    	    final String url = "jdbc:mysql://" + getHost() + ":" + getPort();
            	    	    con = DriverManager.getConnection(url, getUsername(), getPassword());
            	    	    succeeded = true;
             	    } catch (Exception e){
            	    	    System.err.println("Exception : " + e.getMessage());
            	    } finally {
            	    	    if (con != null) try { con.close(); } catch (SQLException ignore) {}
            	    }            	    
            	  
            	    if (succeeded == false) {
            	    	    JOptionPane.showMessageDialog(LoginDialog.this,
            	    	    	    "Connexion impossible.",
            	    	    	    "Login",
            	    	    	    JOptionPane.ERROR_MESSAGE);
                    	pfPassword.setText("");
                    } else {
                    	dispose();	    
                    }
                }
            });
        btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        JPanel bp = new JPanel();
        bp.add(btnLogin);
        bp.add(btnCancel);
 
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);
 
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }
 
 
    public boolean loginSucceeded() {
        return succeeded;
    }
    
    public String getHost(){
    	return tfHost.getText();	    
    }

    public String getPort(){
    	return tfPort.getText();	    
    }
    
    public String getUsername(){
    	return tfUsername.getText();	    
    }
    
    public String getPassword(){
    	return pfPassword.getText();	    
    }    

}
