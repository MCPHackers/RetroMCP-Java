package org.mcphackers.mcp.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Options;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.Update;
import org.mcphackers.mcp.gui.MenuBar;
import org.mcphackers.mcp.gui.TextAreaOutputStream;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.VersionsParser;

import layout.SpringUtilities;

public class MainGUI extends JFrame implements MCP {
	
	private JButton decompileButton;
	private JButton recompileButton;
	private JButton reobfButton;
	private JButton buildButton;
	private JButton patchButton;
	private JButton md5Button;
	private JComboBox<String> verList;
	private JLabel verLabel;
	private JPanel topRightContainer;
	private JPanel bottom;
	private final List<JProgressBar> progressBars = new ArrayList<>();
	private final List<JLabel> progressLabels = new ArrayList<>();
	private MenuBar menuBar;
	public String currentVersion;
	public Path workingDir;
	
	public static void main(String[] args) throws Exception {
		new MainGUI();
	}
	
	public MainGUI() {
		super("RetroMCP " + MCP.VERSION);
		workingDir = Paths.get("");
		Update.attemptToDeleteUpdateJar();
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            URL resource = this.getClass().getResource("/rmcp.png");
            BufferedImage image = ImageIO.read(resource);
            setIconImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
		JavaCompiler c = ToolProvider.getSystemJavaCompiler();
		if (c == null) {
			JOptionPane.showMessageDialog(this, "Java Development Kit not found!", "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
        initFrameContents();
        
		setSize(840, 512);
		setMinimumSize(new Dimension(840,200));
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void initFrameContents() {

		Container contentPane = getContentPane();
		menuBar = new MenuBar(this);
		setJMenuBar(menuBar);
		contentPane.setLayout(new BorderLayout());
		JPanel topLeftContainer = new JPanel();

		for(int i = 0; i < 6; i++) {
			String[] buttonName = {"Decompile", "Recompile", "Reobfuscate", "Build", "Update MD5", "Create Patch"};
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
				this.md5Button = button;
				break;
			case 5:
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
	    middlePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), "Console output"));
        middlePanel.setLayout(new BoxLayout(middlePanel,
                BoxLayout.Y_AXIS));
		textArea.setEditable(false);
		PrintStream printStream = new PrintStream(new TextAreaOutputStream(textArea));
		System.setOut(printStream);
		System.setErr(printStream);
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		textArea.setFont(font);
		textArea.setForeground(Color.BLACK);
		JScrollPane scroll = new JScrollPane(textArea);
	    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(scroll);
		bottom = new JPanel(new SpringLayout());
        bottom.setVisible(false);
		contentPane.add(bottom, BorderLayout.SOUTH);
		contentPane.add(middlePanel, BorderLayout.CENTER);
	}
	
	@Override
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
        for (int i = 0; i < tasks.size(); i++) {
			String name = mode.getFullName();
			if(tasks.get(i).side == Side.CLIENT || tasks.get(i).side == Side.SERVER) {
				name = tasks.get(i).side.name;
			}
			progressBars.add(i, new JProgressBar());
			progressLabels.add(i, new JLabel(name + ":", JLabel.TRAILING));
			progressBars.get(i).setStringPainted(true);
			progressLabels.get(i).setVisible(true);
			progressBars.get(i).setVisible(true);
        	bottom.add(progressLabels.get(i));
        	progressLabels.get(i).setLabelFor(progressBars.get(i));
        	bottom.add(progressBars.get(i));
        	setProgress(i, "Idle");
        }
        SpringUtilities.makeCompactGrid(bottom,
        						tasks.size(), 2, //rows, cols
                                6, 6,        	 //initX, initY
                                6, 6);       	 //xPad, yPad
        bottom.setVisible(true);
	}

	@Override
	public void clearProgressBars() {
        bottom.setLayout(new SpringLayout());
        bottom.removeAll();
        bottom.setVisible(false);
        progressBars.clear();
        progressLabels.clear();
	}

	private boolean isButtonActive(JButton button) {
		Side side = getSide();
		try {
			List<Path> clientPaths = new ArrayList<>();
			List<Path> serverPaths = new ArrayList<>();
			if(side == Side.SERVER && !VersionsParser.hasServer(currentVersion)) {
				return false;
			}
			if(button == decompileButton) {
				if(side == Side.CLIENT || side == Side.ANY) {
					clientPaths.add(MCPPaths.get(this, MCPPaths.CLIENT));
				}
				if(side == Side.SERVER || side == Side.ANY) {
					serverPaths.add(MCPPaths.get(this, MCPPaths.SERVER));
				}
			}
			if(button == recompileButton || button == reobfButton || button == md5Button || button == buildButton) {
				if(side == Side.CLIENT || side == Side.ANY) {
					clientPaths.add(MCPPaths.get(this, MCPPaths.CLIENT_SOURCES));
				}
				if(side == Side.SERVER || side == Side.ANY) {
					serverPaths.add(MCPPaths.get(this, MCPPaths.SERVER_SOURCES));
				}
			}
			if(button == patchButton) {
				if(side == Side.CLIENT || side == Side.ANY) {
					clientPaths.add(MCPPaths.get(this, MCPPaths.CLIENT_TEMP_SOURCES));
				}
				if(side == Side.SERVER || side == Side.ANY) {
					serverPaths.add(MCPPaths.get(this, MCPPaths.SERVER_TEMP_SOURCES));
				}
			}
			boolean allClientPathsReadable = true;
			for(Path path : clientPaths) {
				if(!Files.isReadable(path)) {
					allClientPathsReadable = false;
				}
			}
			boolean allServerPathsReadable = VersionsParser.hasServer(currentVersion);
			for(Path path : serverPaths) {
				if(!Files.isReadable(path)) {
					allServerPathsReadable = false;
				}
			}
			if(side == Side.ANY) {
				return allClientPathsReadable || allServerPathsReadable;
			}
			if(side == Side.CLIENT) {
				return allClientPathsReadable;
			}
			if(side == Side.SERVER) {
				return allServerPathsReadable;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private void addListeners() {
		decompileButton.addActionListener(event -> { operateOnThread(() -> {
			int response = -1;
			if(Files.exists(MCPPaths.get(this, MCPPaths.SRC))) {
				response = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete sources and decompile again?", "Confirm Action", JOptionPane.YES_NO_OPTION);
			}
			if(response <= 0) {
				if(response == 0) {
					setParameter(TaskParameter.SRC_CLEANUP, true);
					performTask(TaskMode.CLEANUP, Side.ANY, false, false);
				}
				performTask(TaskMode.DECOMPILE, getSide());
			}
		});
		});
		recompileButton.addActionListener(event -> operateOnThread(() -> performTask(TaskMode.RECOMPILE, getSide())));
		reobfButton.addActionListener(event -> operateOnThread(() -> performTask(TaskMode.REOBFUSCATE, getSide())));
		buildButton.addActionListener(event -> operateOnThread(() -> performTask(TaskMode.BUILD, getSide())));
		md5Button.addActionListener(event -> operateOnThread(() -> performTask(TaskMode.UPDATE_MD5, getSide())));
		patchButton.addActionListener(event -> operateOnThread(() -> performTask(TaskMode.CREATE_PATCH, getSide())));

		reloadVersionList();
	}
	
	public void reloadVersionList() {
		try {
			topRightContainer.removeAll();
			if(Files.exists(MCPPaths.get(this, MCPPaths.VERSION))) {
				currentVersion = VersionsParser.setCurrentVersion(this, new String(Files.readAllBytes(MCPPaths.get(this, MCPPaths.VERSION))));
			}
			else {
				currentVersion = null;
			}
			MainGUI mcp = this;
			this.verList = new JComboBox<String>(VersionsParser.getVersionList().toArray(new String[0]));
			this.verList.addPopupMenuListener(new PopupMenuListener() {
	
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				}
	
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					operateOnThread(() ->  {
			        if (verList.getSelectedItem() != null && !verList.getSelectedItem().equals(currentVersion)) {
			        	int response = JOptionPane.showConfirmDialog(mcp, "Are you sure you want to run setup for selected version?", "Confirm Action", JOptionPane.YES_NO_OPTION);
			        	switch (response) {
			        		case 0:
		    					setParameter(TaskParameter.SETUP_VERSION, verList.getSelectedItem());
		    					performTask(TaskMode.SETUP, Side.ANY, false, true);
			        			break;
			        		default:
			        			verList.setSelectedItem(getCurrentVersion());
			        			verList.repaint(); //No idea why but that fixes a random version when deselecting
			        			break;
			        	}
			        }
					});
				}
	
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
				}
				
			});
			this.verList.setSelectedItem(currentVersion);
			this.verList.setMaximumRowCount(20);
			this.verLabel = new JLabel("Current version:");
			topRightContainer.add(this.verLabel);
			topRightContainer.add(this.verList);
		} catch (Exception e) {
			verLabel = new JLabel("Unable to get current version!");
			verLabel.setForeground(Color.RED);
			topRightContainer.add(verLabel);
		}
		topRightContainer.updateUI();
	}

	private Side getSide() {
		return menuBar.side;
	}

	public Thread operateOnThread(Runnable function) {
		Thread thread = new Thread(function);
		thread.start();
		return thread;
	}

	public void updateButtonState() {
		decompileButton.setEnabled(isButtonActive(decompileButton));
		recompileButton.setEnabled(isButtonActive(recompileButton));
		reobfButton.setEnabled(isButtonActive(reobfButton));
		buildButton.setEnabled(isButtonActive(buildButton));
		md5Button.setEnabled(isButtonActive(md5Button));
		patchButton.setEnabled(isButtonActive(patchButton));
		if(verList != null) verList.setEnabled(true);
		verLabel.setEnabled(true);
		menuBar.menuOptions.setEnabled(true);
		menuBar.mcpMenu.setEnabled(true);
	}
	
	@Override
	public void setActive(boolean active) {
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
		md5Button.setEnabled(false);
		patchButton.setEnabled(false);
		if(verList != null) verList.setEnabled(false);
		verLabel.setEnabled(false);
		menuBar.menuOptions.setEnabled(false);
		menuBar.mcpMenu.setEnabled(false);
	}
	
	public void setParameter(TaskParameter param, Object value) {
		getOptions().setParameter(param, value);
	}

	@Override
	public String getCurrentVersion() {
		return currentVersion;
	}

	@Override
	public void log(String msg) {
		System.out.println(msg);
	}

	@Override
	public Options getOptions() {
		return menuBar.options;
	}

	@Override
	public void setProgress(int side, String progressMessage) {
		progressBars.get(side).setString(progressBars.get(side).getValue() + "% " + progressMessage);
	}

	@Override
	public void setProgress(int side, int progress) {
		progressBars.get(side).setValue(progress);
	}

	@Override
	public boolean yesNoInput(String title, String msg) {
		return JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION) == 0;
	}

	@Override
	public String inputString(String title, String msg) {
		return JOptionPane.showInputDialog(this, msg, title, JOptionPane.PLAIN_MESSAGE);
	}

	public void showMessage(String title, String msg, int type) {
		switch (type) {
		case Task.INFO:
			type = JOptionPane.INFORMATION_MESSAGE;
			break;
		case Task.WARNING:
			type = JOptionPane.WARNING_MESSAGE;
			break;
		case Task.ERROR:
			type = JOptionPane.ERROR_MESSAGE;
			break;
		}
		JOptionPane.showMessageDialog(this, msg, title, type);
	}

	@Override
	public void setCurrentVersion(String version) {
		currentVersion = version;
		verList.setSelectedItem(version);
		verList.repaint();
	}

	@Override
	public Path getWorkingDir() {
		return workingDir;
	}
}
