package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.TaskMode;
import org.mcphackers.mcp.TaskParameter;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tools.MCPPaths;
import org.mcphackers.mcp.tools.VersionsParser;

public class MCPFrame extends JFrame {
	
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
	private JProgressBar[] progressBars = new JProgressBar[0];
	private JLabel[] progressLabels = new JLabel[0];
	private MenuBar menuBar;
	public MainGUI mcp;
	
	public MCPFrame(MainGUI mcp) {
		super("RetroMCP " + MCP.VERSION);
		this.mcp = mcp;
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
        //TODO display buttons in two rows so you could shrink it more horizontally
		setMinimumSize(new Dimension(842, 100));
        pack();
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
		scroll.setPreferredSize(new Dimension(600, 380));
	    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(scroll);
		bottom = new JPanel(new GridBagLayout());
        bottom.setVisible(false);
		contentPane.add(bottom, BorderLayout.SOUTH);
		contentPane.add(middlePanel, BorderLayout.CENTER);
	}
	
	private void addListeners() {
		decompileButton.addActionListener(event -> operateOnThread(() -> {
			int response = -1;
			if(Files.exists(MCPPaths.get(mcp, MCPPaths.SRC))) {
				response = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete sources and decompile again?", "Confirm Action", JOptionPane.YES_NO_OPTION);
			}
			if(response <= 0) {
				if(response == 0) {
					mcp.setParameter(TaskParameter.SRC_CLEANUP, true);
					mcp.performTask(TaskMode.CLEANUP, Side.ANY, false, false);
				}
				mcp.performTask(TaskMode.DECOMPILE, mcp.side);
			}
		}));
		recompileButton.addActionListener(performTask(TaskMode.RECOMPILE));
		reobfButton.addActionListener(performTask(TaskMode.REOBFUSCATE));
		buildButton.addActionListener(performTask(TaskMode.BUILD));
		md5Button.addActionListener(performTask(TaskMode.UPDATE_MD5));
		patchButton.addActionListener(performTask(TaskMode.CREATE_PATCH));

		reloadVersionList();
	}
	
	public ActionListener performTask(TaskMode mode) {
		return event -> operateOnThread(() -> mcp.performTask(mode, mcp.side));
	}
	
	public void reloadVersionList() {
		try {
			topRightContainer.removeAll();
			JFrame frame = this;
			
			this.verList = new JComboBox<>(VersionsParser.getVersionList().toArray(new String[0]));
			this.verList.addPopupMenuListener(new PopupMenuListener() {
	
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				}
	
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					operateOnThread(() ->  {
			        if (verList.getSelectedItem() != null && !verList.getSelectedItem().equals(mcp.getCurrentVersion())) {
			        	int response = JOptionPane.showConfirmDialog(frame, "Are you sure you want to run setup for selected version?", "Confirm Action", JOptionPane.YES_NO_OPTION);
			        	switch (response) {
			        		case 0:
		    					mcp.setParameter(TaskParameter.SETUP_VERSION, verList.getSelectedItem());
		    					mcp.performTask(TaskMode.SETUP, Side.ANY, false, true);
			        			break;
			        		default:
			        			verList.setSelectedItem(mcp.getCurrentVersion());
			        			verList.repaint();
			        			break;
			        	}
			        }
					});
				}
	
				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
				}
				
			});
			if(Files.exists(MCPPaths.get(mcp, MCPPaths.VERSION))) {
				setCurrentVersion(mcp.currentVersion = VersionsParser.setCurrentVersion(mcp, new String(Files.readAllBytes(MCPPaths.get(mcp, MCPPaths.VERSION)))));
			}
			else {
				setCurrentVersion(mcp.currentVersion = null);
			}
			this.verList.setMaximumRowCount(20);
			this.verLabel = new JLabel("Current version:");
			topRightContainer.add(this.verLabel);
			topRightContainer.add(this.verList);
		} catch (Exception e) {
			verLabel = new JLabel("Unable to get current version!");
			verLabel.setForeground(Color.RED);
			topRightContainer.add(verLabel);
			e.printStackTrace();
		}
		topRightContainer.updateUI();
	}

	private boolean isButtonActive(JButton button) {
		Side side = mcp.side;
		try {
			List<Path> clientPaths = new ArrayList<>();
			List<Path> serverPaths = new ArrayList<>();
			if(side == Side.SERVER && !VersionsParser.hasServer(mcp.getCurrentVersion())) {
				return false;
			}
			if(button == decompileButton) {
				if(side == Side.CLIENT || side == Side.ANY) {
					clientPaths.add(MCPPaths.get(mcp, MCPPaths.CLIENT));
				}
				if(side == Side.SERVER || side == Side.ANY) {
					serverPaths.add(MCPPaths.get(mcp, MCPPaths.SERVER));
				}
			}
			if(button == recompileButton || button == reobfButton || button == md5Button || button == buildButton) {
				if(side == Side.CLIENT || side == Side.ANY) {
					clientPaths.add(MCPPaths.get(mcp, MCPPaths.CLIENT_SOURCES));
				}
				if(side == Side.SERVER || side == Side.ANY) {
					serverPaths.add(MCPPaths.get(mcp, MCPPaths.SERVER_SOURCES));
				}
			}
			if(button == patchButton) {
				if(side == Side.CLIENT || side == Side.ANY) {
					clientPaths.add(MCPPaths.get(mcp, MCPPaths.CLIENT_TEMP_SOURCES));
				}
				if(side == Side.SERVER || side == Side.ANY) {
					serverPaths.add(MCPPaths.get(mcp, MCPPaths.SERVER_TEMP_SOURCES));
				}
			}
			boolean allClientPathsReadable = true;
			for(Path path : clientPaths) {
				if(!Files.isReadable(path)) {
					allClientPathsReadable = false;
				}
			}
			boolean allServerPathsReadable = VersionsParser.hasServer(mcp.getCurrentVersion());
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
	
	public void setAllButtonsInactive() {
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

	public void setCurrentVersion(String version) {
		verList.setSelectedItem(version);
		verList.repaint();
	}

	public void resetProgressBars() {
        bottom.removeAll();
        bottom.setVisible(false);
        progressBars = new JProgressBar[0];
        progressLabels = new JLabel[0];
	}

	public void setProgress(int side, int progress) {
		progressBars[side].setValue(progress);
	}

	public void setProgress(int side, String progressMessage) {
		progressBars[side].setString(progressBars[side].getValue() + "% " + progressMessage);
	}

	public void setProgressBars(List<Task> tasks, TaskMode mode) {
        int size = tasks.size();
		progressBars = new JProgressBar[size];
        progressLabels = new JLabel[size];
        for (int i = 0; i < size; i++) {
			String name = mode.getFullName();
			if(tasks.get(i).side == Side.CLIENT || tasks.get(i).side == Side.SERVER) {
				name = tasks.get(i).side.name;
			}
			progressBars[i] = new JProgressBar();
			progressLabels[i] = new JLabel(name + ":", JLabel.TRAILING);
			progressBars[i].setStringPainted(true);
			progressLabels[i].setVisible(true);
			progressBars[i].setVisible(true);
			GridBagConstraintsBuilder cb = new GridBagConstraintsBuilder(new GridBagConstraints()).insetsUnscaled(4, 4);
			bottom.add(progressLabels[i], cb.pos(0, i).weightX(0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
			bottom.add(progressBars[i], cb.pos(1, i).weightX(1).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());
        	setProgress(i, "Idle");
        }
        bottom.setVisible(true);
	}

}
