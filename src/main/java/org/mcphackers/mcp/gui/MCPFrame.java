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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
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
	public boolean loadingVersions = true;
	private JPanel middlePanel;
	
	public static BufferedImage ICON;
	
	static {
		try {
			URL resource = MCPFrame.class.getResource("/icon/rmcp.png");
			ICON = ImageIO.read(resource);
		} catch (Exception e) {
			System.err.println("Can't load icon");
		}
	}
	
	public MCPFrame(MainGUI mcp) {
		super("RetroMCP " + MCP.VERSION);
		this.mcp = mcp;
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setIconImage(ICON);
		initFrameContents();
		pack();
		setMinimumSize(getMinimumSize());
		setSize(new Dimension(940, 500));
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	private void initFrameContents() {

		Container contentPane = getContentPane();
		menuBar = new MenuBar(this);
		setJMenuBar(menuBar);
		contentPane.setLayout(new BorderLayout());
		FlowLayout layout = new WrapLayout(FlowLayout.LEFT);
		JPanel topLeftContainer = new JPanel();
		topLeftContainer.setLayout(layout);
		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent componentEvent) {
				SwingUtilities.invokeLater(() -> topLeftContainer.revalidate());
			}
		});

		for(TaskMode task : MainGUI.TASKS) {
			TaskButton button = mcp.getButton(task);
			buttons.add(button);
			topLeftContainer.add(button);
		}
		
		topRightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		reloadVersionList();
		updateButtonState();
		
		JPanel topContainer = new JPanel(new BorderLayout());
		
		topContainer.add(topLeftContainer, BorderLayout.CENTER);
		topContainer.add(topRightContainer, BorderLayout.EAST);
		topContainer.setMinimumSize(new Dimension(340, 96));
		topRightContainer.setMinimumSize(topRightContainer.getMinimumSize());
		contentPane.add(topContainer, BorderLayout.NORTH);

		JTextArea textArea = new JTextArea();
		middlePanel = new JPanel();
		middlePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), MCP.TRANSLATOR.translateKey("mcp.console")));
		middlePanel.setLayout(new BorderLayout());
		textArea.setEditable(false);
		PrintStream origOut = System.out;
		PrintStream interceptor = new TextAreaOutputStream(textArea, origOut);
		System.setOut(interceptor);
		origOut = System.err;
		interceptor = new TextAreaOutputStream(textArea, origOut);
		System.setErr(interceptor);
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		textArea.setFont(font);
		textArea.setForeground(Color.BLACK);
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(scroll);
		bottom = new JPanel(new GridBagLayout());
		bottom.setVisible(false);
		contentPane.add(middlePanel, BorderLayout.CENTER);
		contentPane.add(bottom, BorderLayout.SOUTH);
		reloadText();
	}
	
	public void reloadVersionList() {

		verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		verList = new JComboBox<>(new String[] {MCP.TRANSLATOR.translateKey("mcp.versionList.loading")});
		verLabel.setEnabled(false);
		verList.setEnabled(false);
		topRightContainer.removeAll();
		topRightContainer.add(this.verLabel);
		topRightContainer.add(this.verList);
		operateOnThread(() ->  {
		try {
			loadingVersions = true;
			verList = new JComboBox<>(VersionsParser.getVersionList().toArray(new String[0]));
			verList.addPopupMenuListener(new PopupMenuListener() {
	
				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				}
	
				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					operateOnThread(() ->  {
					if (verList.getSelectedItem() != null && !verList.getSelectedItem().equals(mcp.getCurrentVersion())) {
						int response = JOptionPane.showConfirmDialog(MCPFrame.this, MCP.TRANSLATOR.translateKey("mcp.confirmSetup"), MCP.TRANSLATOR.translateKey("mcp.confirmAction"), JOptionPane.YES_NO_OPTION);
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
				setCurrentVersion(VersionsParser.setCurrentVersion(mcp, new String(Files.readAllBytes(MCPPaths.get(mcp, MCPPaths.VERSION)))));
			}
			else {
				setCurrentVersion(null);
			}
			verList.setMaximumRowCount(20);
			verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		} catch (Exception e) {
			verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.failure"));
			verLabel.setBorder(new EmptyBorder(4, 0, 0, 2));
			verLabel.setForeground(Color.RED);
			verList = null;
		}
		SwingUtilities.invokeLater(() -> {
			topRightContainer.removeAll();
			topRightContainer.add(this.verLabel);
			if(verList != null) {
				topRightContainer.add(this.verList);
			}
			loadingVersions = false;
			synchronized (mcp) {
				if(mcp.isActive) {
					if(verList != null) verList.setEnabled(true);
					verLabel.setEnabled(true);
				}
			}
			topRightContainer.updateUI();
		});
		});
	}

	public void updateButtonState() {
		buttons.forEach(button -> button.setEnabled(button.getEnabled()));
		menuBar.start.entrySet().forEach(entry -> entry.getValue().setEnabled(TaskMode.START.isAvailable(mcp, entry.getKey())));
		if(verList != null && !loadingVersions) verList.setEnabled(true);
		if(!loadingVersions) verLabel.setEnabled(true);
		menuBar.menuOptions.setEnabled(true);
		menuBar.setComponentsEnabled(true);
	}
	
	public void setAllButtonsInactive() {
		buttons.forEach(button -> button.setEnabled(false));
		if(verList != null) verList.setEnabled(false);
		verLabel.setEnabled(false);
		menuBar.menuOptions.setEnabled(false);
		menuBar.setComponentsEnabled(false);
	}

	public void setCurrentVersion(String version) {
		mcp.currentVersion = version;
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
				name = tasks.get(i).side.getName();
			}
			progressBars[i] = new SideProgressBar();
			progressLabels[i] = new JLabel(name + ":", JLabel.TRAILING);
			progressLabels[i].setVisible(true);
			progressBars[i].setVisible(true);
			GridBagConstraintsBuilder cb = new GridBagConstraintsBuilder(new GridBagConstraints()).insetsUnscaled(4, 4);
			bottom.add(progressLabels[i], cb.pos(0, i).weightX(0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
			bottom.add(progressBars[i], cb.pos(1, i).weightX(1).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());
			setProgress(i, MCP.TRANSLATOR.translateKey("task.stage.idle"));
		}
		bottom.setVisible(true);
	}
	
	public void reloadText() {
		middlePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), MCP.TRANSLATOR.translateKey("mcp.console")));
		if(verList == null && !loadingVersions) {
			verLabel.setText(MCP.TRANSLATOR.translateKey("mcp.versionList.failure"));
		}
		else {
			verLabel.setText(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		}
		buttons.forEach(button -> button.updateName());
		menuBar.reloadText();
	}

}
