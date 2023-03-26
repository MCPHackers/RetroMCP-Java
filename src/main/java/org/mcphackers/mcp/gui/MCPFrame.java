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
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;

public class MCPFrame extends JFrame implements WindowListener {

	private static final long serialVersionUID = -3455157541499586338L;

	private JComboBox<?> verList;
	private final List<TaskButton> buttons = new ArrayList<>();
	private JLabel verLabel;
	//private JButton verCleanup;
	private JPanel topRightContainer;
	private JPanel topLeftContainer;
	private JPanel bottom;
	private SideProgressBar[] progressBars = new SideProgressBar[0];
	private JLabel[] progressLabels = new JLabel[0];
	public MenuBar menuBar;
	public MainGUI mcp;
	public boolean loadingVersions = true;
	private JPanel middlePanel;

	public static final BufferedImage ICON;

	static {
		BufferedImage image = null;
		try {
			//TODO read all images from .ico
			URL resource = MCPFrame.class.getResource("/icon/rmcp.png");
			image = ImageIO.read(resource);
		} catch (Exception ignored) {}
		ICON = image;
	}

	public MCPFrame(MainGUI mcp) {
		super("RetroMCP " + MCP.VERSION);
		this.mcp = mcp;
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(this);
		setIconImage(ICON);
		initFrameContents();
		pack();
		setMinimumSize(getMinimumSize());
		setSize(new Dimension(900, 520));
		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initFrameContents() {

		Container contentPane = getContentPane();
		menuBar = new MenuBar(this);
		setJMenuBar(menuBar);
		contentPane.setLayout(new BorderLayout());
		FlowLayout layout = new WrapLayout(FlowLayout.LEFT);
		topLeftContainer = new JPanel();
		topLeftContainer.setLayout(layout);
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent componentEvent) {
				SwingUtilities.invokeLater(() -> topLeftContainer.revalidate());
			}
		});

		for(TaskMode task : MainGUI.TASKS) {
			TaskButton button = mcp.getButton(task);
			buttons.add(button);
			button.setToolTipText(task.getDesc());
			topLeftContainer.add(button);
		}

		topRightContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		reloadVersionList();
		updateButtonState();

		JPanel topContainer = new JPanel(new BorderLayout());

		topContainer.add(topLeftContainer, BorderLayout.CENTER);
		topContainer.add(topRightContainer, BorderLayout.EAST);
		contentPane.add(topContainer, BorderLayout.NORTH);

		JTextPane textArea = mcp.textPane;
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 12);
		textArea.setFont(font);
		textArea.setEditable(false);
		middlePanel = new JPanel();
		middlePanel.setPreferredSize(new Dimension(0, 380));
		middlePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), MCP.TRANSLATOR.translateKey("mcp.console")));
		middlePanel.setLayout(new BorderLayout());
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		middlePanel.add(scroll);
		bottom = new JPanel(new GridBagLayout());
		bottom.setVisible(false);
		contentPane.add(middlePanel, BorderLayout.CENTER);
		contentPane.add(bottom, BorderLayout.SOUTH);
		reloadText();
	}

	/**
	 * Reloads version list and reads current version from {@link MCPPaths#VERSION}
	 */
	public void reloadVersionList() {

		verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		verList = new JComboBox<Object>(new String[] {MCP.TRANSLATOR.translateKey("mcp.versionList.loading")});
		//verCleanup = mcp.getButton(TaskMode.CLEANUP);
		//verCleanup.setEnabled(false);
		verLabel.setEnabled(false);
		verList.setEnabled(false);
		topRightContainer.removeAll();
		topRightContainer.add(this.verLabel);
		topRightContainer.add(this.verList);
		//topRightContainer.add(this.verCleanup);
		operateOnThread(() ->  {
		loadingVersions = true;
		if(VersionParser.INSTANCE.failureCause != null) {
			VersionParser.INSTANCE.failureCause.printStackTrace();
			verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.failure"));
			verLabel.setBorder(new EmptyBorder(4, 0, 0, 2));
			verLabel.setForeground(Color.RED);
			verList = null;
		} else {
			verList = new JComboBox<>(VersionParser.INSTANCE.getVersions().toArray());
			verList.addPopupMenuListener(new PopupMenuListener() {

				@Override
				public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				}

				@Override
				public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
					operateOnThread(() -> mcp.setupVersion((VersionData)verList.getSelectedItem()));
				}

				@Override
				public void popupMenuCanceled(PopupMenuEvent e) {
				}
			});

			setCurrentVersion(mcp.currentVersion == null ? null : VersionParser.INSTANCE.getVersion(mcp.currentVersion.id));
			verList.setMaximumRowCount(20);
			verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		}
		SwingUtilities.invokeLater(() -> {
			topRightContainer.removeAll();
			topRightContainer.add(this.verLabel);
			if(verList != null) {
				topRightContainer.add(this.verList);
			}
			//topRightContainer.add(this.verCleanup);
			loadingVersions = false;
			synchronized (mcp) {
				if(mcp.isActive) {
					if(verList != null) verList.setEnabled(true);
					verLabel.setEnabled(true);
					//verCleanup.setEnabled(true);
				}
			}
			topRightContainer.updateUI();
			revalidate();
			topLeftContainer.revalidate();
		});
		});
	}

	/**
	 * Checks availability of all buttons and enables them
	 */
	public void updateButtonState() {
		buttons.forEach(button -> button.setEnabled(button.getEnabled()));
		if(verList != null && !loadingVersions) verList.setEnabled(true);
		if(!loadingVersions) verLabel.setEnabled(true);
		//if(!loadingVersions) verCleanup.setEnabled(true);
		menuBar.menuOptions.setEnabled(true);
		menuBar.setComponentsEnabled(true);
	}

	/**
	 * Disables all buttons
	 */
	public void setAllButtonsInactive() {
		buttons.forEach(button -> button.setEnabled(false));
		if(verList != null) verList.setEnabled(false);
		verLabel.setEnabled(false);
		//verCleanup.setEnabled(false);
		menuBar.menuOptions.setEnabled(false);
		menuBar.setComponentsEnabled(false);
	}

	/**
	 * Refreshes version list with specified versionData
	 * @param versionData
	 */
	public void setCurrentVersion(VersionData versionData) {
		if(verList == null) {
			return;
		}
		verList.setSelectedItem(versionData);
		verList.repaint();
	}

	/**
	 * @see MCP#clearProgressBars()
	 */
	public void resetProgressBars() {
		bottom.removeAll();
		bottom.setVisible(false);
		progressBars = new SideProgressBar[0];
		progressLabels = new JLabel[0];
	}

	/**
	 * @see MCP#setProgress(int, int)
	 * @param side
	 * @param progress
	 */
	public void setProgress(int side, int progress) {
		progressBars[side].progress = progress;
		progressBars[side].updateProgress();
	}

	/**
	 * @see MCP#setProgress(int, String)
	 * @param side
	 * @param progressMessage
	 */
	public void setProgress(int side, String progressMessage) {
		progressBars[side].progressMsg = progressMessage;
		progressBars[side].updateProgress();
	}

	/**
	 * @see MCP#setProgressBars(List, TaskMode)
	 * @param tasks
	 * @param mode
	 */
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
			progressLabels[i] = new JLabel(name + ":", SwingConstants.TRAILING);
			progressLabels[i].setVisible(true);
			progressBars[i].setVisible(true);
			GridBagConstraintsBuilder cb = new GridBagConstraintsBuilder(new GridBagConstraints()).insetsUnscaled(4, 4);
			bottom.add(progressLabels[i], cb.pos(0, i).weightX(0).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.NONE).build());
			bottom.add(progressBars[i], cb.pos(1, i).weightX(1).anchor(GridBagConstraints.LINE_END).fill(GridBagConstraints.HORIZONTAL).build());
			setProgress(i, MCP.TRANSLATOR.translateKey("task.stage.idle"));
		}
		bottom.setVisible(true);
	}

	/**
	 * Called upon {@link MCP#changeLanguage(org.mcphackers.mcp.Language)}
	 * Reloads text on all translatable components
	 */
	public void reloadText() {
		middlePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), MCP.TRANSLATOR.translateKey("mcp.console")));
		if(verList == null && !loadingVersions) {
			verLabel.setText(MCP.TRANSLATOR.translateKey("mcp.versionList.failure"));
		}
		else {
			verLabel.setText(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		}
		buttons.forEach(TaskButton::updateName);
		Dimension preferredButtonSize = new Dimension(0, 26);
		for(TaskButton button : buttons) {
			button.setPreferredSize(null);
			Dimension preferredButtonSize2 = button.getPreferredSize();
			preferredButtonSize.width = Math.max(preferredButtonSize2.width, preferredButtonSize.width);
			preferredButtonSize.height = Math.max(preferredButtonSize2.height, preferredButtonSize.height);
		}
		for(TaskButton button : buttons) {
			button.setPreferredSize(preferredButtonSize);
		}
		menuBar.reloadText();
		revalidate();
		topLeftContainer.revalidate();
	}

	@Override
	public void windowOpened(WindowEvent e) {
	}

	@Override
	public void windowClosing(WindowEvent e) {
		mcp.exit();
	}

	@Override
	public void windowClosed(WindowEvent e) {
	}

	@Override
	public void windowIconified(WindowEvent e) {
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
	}

	@Override
	public void windowActivated(WindowEvent e) {
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
	}

}
