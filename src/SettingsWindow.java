import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.Color;
import javax.swing.JLabel;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SettingsWindow {

	private JFrame frmSettings;
	
	private GUI gui;
	
	/**
	 * Launch the application.
	 */
	public static void showSettings(GUI gui) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SettingsWindow window = new SettingsWindow(gui);
					window.frmSettings.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public SettingsWindow(GUI gui) {
		this.gui = gui;
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmSettings = new JFrame();
		frmSettings.setTitle("Settings");
		frmSettings.getContentPane().setBackground(Color.BLACK);
		frmSettings.getContentPane().setLayout(null);
		
		JLabel lblSettings = new JLabel("Settings");
		lblSettings.setHorizontalAlignment(SwingConstants.CENTER);
		lblSettings.setFont(new Font("Trajan Pro 3", Font.PLAIN, 18));
		lblSettings.setForeground(Color.WHITE);
		lblSettings.setBounds(117, 11, 200, 29);
		frmSettings.getContentPane().add(lblSettings);
		
		JPanel panel = new JPanel();
		panel.setBackground(Color.BLACK);
		panel.setBounds(60, 74, 314, 29);
		frmSettings.getContentPane().add(panel);
		
		JLabel lblNationSelection = new JLabel("Nation:");
		panel.add(lblNationSelection);
		lblNationSelection.setForeground(Color.WHITE);
		lblNationSelection.setFont(new Font("Trajan Pro 3", Font.PLAIN, 12));
		
		JComboBox comboBox = new JComboBox();
		comboBox.setFont(new Font("Trajan Pro 3", Font.PLAIN, 11));
		comboBox.setModel(new DefaultComboBoxModel(new String[] {"Random", "Achaemenid Empire", "Assyrian Empire", "Athenian Empire", "Egyptian Empire", "Han Dynasty", "Macedonian Empire", "Phoenician Empire", "Roman Empire", "Aztec Empire", "Byzantine Empire", "Holy Roman Empire", "Inca Empire", "Islamic Caliphate", "Mali Empire", "Mongol Empire", "Vikings", "Duchy of Venice", "Mughal Empire", "Ottoman Empire", "Portuguese Empire", "Qing Dynasty", "Safavid Empire", "Spanish Empire", "British Empire", "Dutch Empire", "French Empire", "German Empire", "Japanese Empire", "Russian Empire", "People's Republic of China", "Soviet Union", "State of Israel", "United States of America"}));
		comboBox.setSelectedItem(gui.nation);
		comboBox.setMaximumRowCount(35);
		panel.add(comboBox);
		
		JPanel panel_1 = new JPanel();
		panel_1.setBackground(Color.BLACK);
		panel_1.setBounds(60, 114, 314, 29);
		frmSettings.getContentPane().add(panel_1);
		
		JLabel lblAiDifficulty = new JLabel("AI Difficulty:");
		lblAiDifficulty.setForeground(Color.WHITE);
		lblAiDifficulty.setFont(new Font("Trajan Pro 3", Font.PLAIN, 12));
		panel_1.add(lblAiDifficulty);
		
		JComboBox comboBox_1 = new JComboBox();
		comboBox_1.setModel(new DefaultComboBoxModel(new String[] {"Novice", "Experienced", "Grandmaster", "Hostile Grandmaster"}));
		comboBox_1.setSelectedIndex(gui.aiDifficulty);
		comboBox_1.setMaximumRowCount(35);
		comboBox_1.setFont(new Font("Trajan Pro 3", Font.PLAIN, 11));
		panel_1.add(comboBox_1);
		
		JButton saveButton = new JButton("Save and Close");
		saveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				gui.aiDifficulty = comboBox_1.getSelectedIndex();
				gui.nation = (String)comboBox.getSelectedItem();
				
				frmSettings.dispose();
				
			}
		});
		saveButton.setBackground(Color.BLACK);
		saveButton.setForeground(Color.WHITE);
		saveButton.setFont(new Font("Trajan Pro 3", Font.PLAIN, 12));
		saveButton.setIcon(null);
		saveButton.setBounds(140, 211, 153, 39);
		frmSettings.getContentPane().add(saveButton);
		frmSettings.setBounds(100, 100, 450, 300);
		frmSettings.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
}
