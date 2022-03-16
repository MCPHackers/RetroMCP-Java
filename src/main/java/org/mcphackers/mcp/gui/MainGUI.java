package org.mcphackers.mcp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.TaskDecompile;
import org.mcphackers.mcp.tasks.TaskSetup;
import org.mcphackers.mcp.tools.VersionsParser;

public class MainGUI implements MCP {
	
	private JButton decompileButton;
	private JButton recompileButton;
	private JButton reobfButton;
	private JButton buildButton;
	private JButton patchButton;
	private JComboBox verList;
	private JLabel verLabel;
	private JMenu menumain;
	private JPanel topRightContainer;
	private JProgressBar[] progressBars = new JProgressBar[2];
	private JLabel[] progressLabels = new JLabel[2];
	private final Map<String, JMenuItem> menuItems = new HashMap<>();
	private JFrame frame;
	
	public static void main(String[] args) throws Exception {
		new MainGUI();
	}
	
	public MainGUI() {
		frame = new JFrame("RetroMCP " + MCP.VERSION);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            URL resource = frame.getClass().getResource("/rmcp.png");
            BufferedImage image = ImageIO.read(resource);
            frame.setIconImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initFrameContents(frame);
        
		frame.setSize(800, 512);
		frame.setMinimumSize(new Dimension(740,200));
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
	
	private void initFrameContents(JFrame frame) {

		Container contentPane = frame.getContentPane();
		JMenuBar ui = new JMenuBar();
		this.menumain = new JMenu("Options");
		this.menumain.setMnemonic(KeyEvent.VK_O);
		String[] menugroupNames = new String[] {
				"Decompiling",
				"Recompiling",
				"Reobfuscation",
		};
		String[][] menuItemNames = new String[][] {
			new String[] {
					"Apply patches",
					"Replace constants",
			},
			new String[] {
					"Copy assets",
			},
			new String[] {
					"Fix classes without mappings",
			}
		};
		for(int i = 0; i < menugroupNames.length; i++) {
			JMenu menu = new JMenu(menugroupNames[i]);
			
			for(String itemName : menuItemNames[i]) {
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(itemName);
				menu.add(item);
			}
			menumain.add(menu);
		}
		ui.add(menumain);
		frame.setJMenuBar(ui);
		contentPane.setLayout(new BorderLayout());
		JPanel topLeftContainer = new JPanel();

		for(int i = 0; i < 5; i++) {
			String[] buttonName = new String[] {"Decompile", "Recompile", "Reobfuscate", "Build", "Create Patch"};
			JButton button = new JButton(buttonName[i]);
			button.setEnabled(false);
			topLeftContainer.add(button);
			switch (i) {
			case 0:
				this.decompileButton = button;
				break;
			case 1:
				this.recompileButton = button;
				break;
			case 2:
				this.reobfButton = button;
				break;
			case 3:
				this.buildButton = button;
				break;
			case 4:
				this.patchButton = button;
				break;
			}
		}
		
		topRightContainer = new JPanel();
		addListeners();
		updateButtonState();
		
		JPanel topContainer = new JPanel(new BorderLayout(4,4));
		topContainer.add(topLeftContainer, BorderLayout.WEST);
		topContainer.add(topRightContainer, BorderLayout.EAST);
		
		contentPane.add(topContainer, BorderLayout.NORTH);
		JTextArea textArea = new JTextArea();
		JPanel middlePanel = new JPanel();
	    middlePanel.setBorder (BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Console output"));
        middlePanel.setLayout(new BoxLayout(middlePanel,
                BoxLayout.Y_AXIS));
		textArea.setEditable(false);
		PrintStream printStream = new PrintStream(new TextAreaOutputStream(textArea));
		System.setOut(printStream);
		System.setErr(printStream);
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		textArea.setFont(font);
		textArea.setForeground(Color.BLACK);
		JScrollPane scroll = new JScrollPane (textArea);
	    scroll.setVerticalScrollBarPolicy (ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(scroll);
		for(int i = 0; i < 2; i++) {
			progressBars[i] = new JProgressBar();
			progressBars[i].setStringPainted(true);
			progressLabels[i] = new JLabel(i == 0 ? "Client:" : "Server:");
			setProgressBarActive(i, false);
		}
		JPanel bottom = new JPanel();
		GroupLayout layout = new GroupLayout(bottom);
		bottom.setLayout(layout);
		//TODO this but procedural and fix gaps being dependent on which one is shown
		layout.setHorizontalGroup(
				   layout.createSequentialGroup()
                   	  .addGap(8)
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
				    		  .addComponent(progressLabels[0])
				    		  .addComponent(progressLabels[1]))
                   	  .addGap(8)
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
				    		  .addComponent(progressBars[0])
				    		  .addComponent(progressBars[1]))
                   	  .addGap(8)
				);
				layout.setVerticalGroup(
				   layout.createSequentialGroup()
                	  .addGap(8)
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		  .addComponent(progressLabels[0])
				    		  .addComponent(progressBars[0]))
                   	  .addGap(8)
				      .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
				    		  .addComponent(progressLabels[1])
				    		  .addComponent(progressBars[1]))
                   	  .addGap(8)
				);
		contentPane.add(bottom, BorderLayout.SOUTH);
		contentPane.add(middlePanel, BorderLayout.CENTER);
	}
	
	private void addListeners() {
		decompileButton.addActionListener(event -> performTasks(new Task[] { new TaskDecompile(0, this), new TaskDecompile(1, this) }));

		try {
			MCP mcp = this;
			this.verList = new JComboBox(VersionsParser.getVersionList().toArray()) {
				public void firePopupMenuWillBecomeInvisible() {
					super.firePopupMenuWillBecomeInvisible();
			        if (!VersionsParser.getCurrentVersion().equals((String)getSelectedItem())) {
			        	int response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to re-run setup for selected version?", "Confirm Action", JOptionPane.YES_NO_OPTION);
			        	switch (response) {
			        		case 0:
			        			operateOnThread(() -> {
			        				setAllButtonsActive(false);
					    			try {
										new TaskSetup(mcp).doTask();
									} catch (Exception e1) {
						    			setSelectedItem(VersionsParser.getCurrentVersion());
										e1.printStackTrace();
									}
			        				setAllButtonsActive(true);
			        			});
			        			break;
			        		default:
			        			setSelectedItem(VersionsParser.getCurrentVersion());
			        			break;
			        	}
			        }
				}
			};
			if(Files.exists(Paths.get(MCPPaths.VERSION))) {
				VersionsParser.setCurrentVersion(new String(Files.readAllBytes(Paths.get(MCPPaths.VERSION))));
				this.verList.setSelectedItem(VersionsParser.getCurrentVersion());
			}
			else {
				this.verList.setSelectedItem(null);
			}
			this.verList.setMaximumRowCount(20);
			this.verLabel = new JLabel("Current version:");
			topRightContainer.add(this.verLabel);
			topRightContainer.add(this.verList);
		} catch (Exception e) {
			JLabel label = new JLabel("Unable to get current version!");
			label.setForeground(Color.RED);
			topRightContainer.add(label);
		}
	}
	
	private void performTasks(Task[] tasks) {
		ExecutorService pool = Executors.newFixedThreadPool(2);
		operateOnThread(() -> {
		setAllButtonsActive(false);
		for(int i = 0; i < tasks.length; i++) {
			final int side = i;
			pool.execute(() -> {
				setProgressBarActive(side, true);
				try {
					tasks[side].doTask();
				} catch (Exception e) {
					e.printStackTrace();
				}
				setProgress(side, "Finished!", 100);
			});
		}
		pool.shutdown();
		while (!pool.isTerminated()) {}
		for(int i = 0; i < tasks.length; i++) {
			setProgressBarActive(i, false);
			setProgress(i, "Idle", 0);
		}
		setAllButtonsActive(true);
		});
	}

	public Thread operateOnThread(Runnable function) {
		Thread thread = new Thread(function);
		thread.start();
		return thread;
	}

	private void updateButtonState() {
		decompileButton.setEnabled(!Files.exists(Paths.get(MCPPaths.SRC)));
		recompileButton.setEnabled(Files.exists(Paths.get(MCPPaths.SRC)));
		reobfButton.setEnabled(Files.exists(Paths.get(MCPPaths.SRC)));
		buildButton.setEnabled(Files.exists(Paths.get(MCPPaths.SRC)));
		patchButton.setEnabled(false);
		verList.setEnabled(true);
		verLabel.setEnabled(true);
		menumain.setEnabled(true);
	}
	
	private void setAllButtonsActive(boolean active) {
		if(active) {
			updateButtonState();
		}
		else {
			setAllButtonsInactive();
		}
	}
	
	private void setAllButtonsInactive() {
		decompileButton.setEnabled(false);
		recompileButton.setEnabled(false);
		reobfButton.setEnabled(false);
		buildButton.setEnabled(false);
		patchButton.setEnabled(false);
		verList.setEnabled(false);
		verLabel.setEnabled(false);
		menumain.setEnabled(false);
	}

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public boolean getBooleanParam(TaskParameter param) {
		switch (param) {
		case PATCHES:
			return true;
		default:
			return false;
		}
	}

	@Override
	public String[] getStringArrayParam(TaskParameter param) {
		switch (param) {
		case IGNORED_PACKAGES:
			return new String[]{"paulscode", "com/jcraft", "isom"};
		default:
			return null;
		}
	}

	@Override
	public String getStringParam(TaskParameter param) {
		switch (param) {
		case SETUP_VERSION:
			return (String)verList.getSelectedItem();
		case INDENTION_STRING:
			return "\t";
		default:
			return null;
		}
	}
	
	@Override
	public void setProgressBarActive(int side, boolean active) {
		progressLabels[side].setVisible(active);
		progressBars[side].setVisible(active);
	}

	@Override
	public void setProgress(int side, String progressMessage, int progress) {
		setProgress(side, progress);
		progressBars[side].setString(progress + "% " + progressMessage);
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		progressBars[side].setString(progressBars[side].getValue() + "% " + progressMessage);
	}

	@Override
	public void setProgress(int side, int progress) {
		progressBars[side].setValue(progress);
	}
}
