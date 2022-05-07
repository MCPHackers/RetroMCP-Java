package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.operateOnThread;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tasks.mode.TaskParameter;
import org.mcphackers.mcp.tools.VersionsParser;

public class MCPFrame extends JFrame {
	
	private JComboBox<String> verList;
	private List<TaskButton> buttons = new ArrayList<>();
	private JLabel verLabel;
	private JPanel topRightContainer;
	private JPanel bottom;
	private SideProgressBar[] progressBars = new SideProgressBar[0];
	private JLabel[] progressLabels = new JLabel[0];
	private MenuBar menuBar;
	public MainGUI mcp;
	
	public MCPFrame(MainGUI mcp) {
		super("RetroMCP " + MCP.VERSION);
		this.mcp = mcp;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        try {
            URL resource = getClass().getResource("/rmcp.png");
            BufferedImage image = ImageIO.read(resource);
            setIconImage(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initFrameContents();
		setMinimumSize(new Dimension(300, 100));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
	}
	
	private void initFrameContents() {

		Container contentPane = getContentPane();
		menuBar = new MenuBar(this);
		setJMenuBar(menuBar);
		contentPane.setLayout(new BorderLayout());
		JPanel topLeftContainer = new JPanel(new FlowLayout());

		for(TaskMode task : MainGUI.TASKS) {
			TaskButton button;
			if(task == TaskMode.DECOMPILE) {
				ActionListener defaultActionListener = event -> operateOnThread(() -> {
					int response = 0;
					if(TaskMode.RECOMPILE.isAvailable(mcp, mcp.getSide())) {
						response = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete sources and decompile again?", "Confirm Action", JOptionPane.YES_NO_OPTION);
					}
					if(response == 0) {
						mcp.performTask(TaskMode.DECOMPILE, mcp.getSide());
					}
				});
				button = new TaskButton(this, task, defaultActionListener);
			}
			else if(task == TaskMode.UPDATE_MD5) {
				ActionListener defaultActionListener = event -> operateOnThread(() -> {
					int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to regenerate original hashes?", "Confirm Action", JOptionPane.YES_NO_OPTION);
					if(response == 0) {
						mcp.performTask(task, mcp.getSide());
					}
				});
				button = new TaskButton(this, task, defaultActionListener);
			}
			else {
				button = new TaskButton(this, task);
			}
			buttons.add(button);
			button.setEnabled(false);
			topLeftContainer.add(button);
		}
		
		topRightContainer = new JPanel(new FlowLayout());
		reloadVersionList();
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
		    					mcp.performTask(TaskMode.SETUP, Side.ANY);
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
			verLabel = new JLabel("Unable to get version list!");
			verLabel.setForeground(Color.RED);
			topRightContainer.add(verLabel);
		}
		topRightContainer.updateUI();
	}

	public void updateButtonState() {
		buttons.forEach(button -> button.setEnabled(button.getEnabled()));
		menuBar.start.entrySet().forEach(entry -> entry.getValue().setEnabled(TaskMode.START.isAvailable(mcp, entry.getKey())));
		if(verList != null) verList.setEnabled(true);
		verLabel.setEnabled(true);
		menuBar.menuOptions.setEnabled(true);
		menuBar.mcpMenu.setEnabled(true);
	}
	
	public void setAllButtonsInactive() {
		buttons.forEach(button -> button.setEnabled(false));
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
        progressBars = new SideProgressBar[0];
        progressLabels = new JLabel[0];
	}

	public void setProgress(int side, int progress) {
		progressBars[side].progress = progress;
		progressBars[side].updateProgress();
	}

	public void setProgress(int side, String progressMessage) {
		progressBars[side].progressMsg = progressMessage;
		progressBars[side].updateProgress();
	}

	public void setProgressBars(List<Task> tasks, TaskMode mode) {
        int size = tasks.size();
		progressBars = new SideProgressBar[size];
        progressLabels = new JLabel[size];
        for (int i = 0; i < size; i++) {
			String name = mode.getFullName();
			if(tasks.get(i).side != Side.ANY) {
				name = tasks.get(i).side.name;
			}
			progressBars[i] = new SideProgressBar();
			progressLabels[i] = new JLabel(name + ":", JLabel.TRAILING);
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
