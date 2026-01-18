package org.mcphackers.mcp.gui;

import static org.mcphackers.mcp.tools.Util.enqueueRunnable;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.MCPPaths;
import org.mcphackers.mcp.Theme;
import org.mcphackers.mcp.main.MainGUI;
import org.mcphackers.mcp.tasks.Task;
import org.mcphackers.mcp.tasks.Task.Side;
import org.mcphackers.mcp.tasks.mode.TaskMode;
import org.mcphackers.mcp.tools.versions.VersionParser;
import org.mcphackers.mcp.tools.versions.VersionParser.VersionData;

public class MCPFrame extends JFrame implements WindowListener {

	public static final BufferedImage ICON;
	private static final long serialVersionUID = -3455157541499586338L;

	static {
		BufferedImage image = null;
		try {
			//TODO read all images from .ico
			URL resource = MCPFrame.class.getResource("/icon/rmcp.png");
			if (resource != null) {
				image = ImageIO.read(resource);
			}
		} catch (Exception ignored) {
		}
		ICON = image;
	}

	public final MainGUI mcp;
	private final List<TaskButton> buttons = new ArrayList<>();
	public MenuBar menuBar;
	public boolean loadingVersions = true;
	private JComboBox<?> verList;
	private JLabel verLabel;
	//private JButton verCleanup;
	private JPanel topRightContainer;
	private JPanel topLeftContainer;
	private JPanel bottom;
	private SideProgressBar[] progressBars = new SideProgressBar[0];
	private JLabel[] progressLabels = new JLabel[0];
	private JPanel middlePanel;

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

		for (TaskMode task : MainGUI.TASKS) {
			TaskButton button = mcp.getButton(task);
			button.updateName();
			buttons.add(button);
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
		verList = new JComboBox<Object>(new String[]{MCP.TRANSLATOR.translateKey("mcp.versionList.loading")});
		verLabel.setEnabled(false);
		verList.setEnabled(false);
		topRightContainer.removeAll();
		topRightContainer.add(this.verLabel);
		topRightContainer.add(this.verList);
		AtomicReference<JButton> reloadVersionListButton = new AtomicReference<>();
		enqueueRunnable(() -> {
			loadingVersions = true;
			VersionParser versionParser = this.mcp.getVersionParser();
			if (versionParser.failureCause != null) {
				versionParser.failureCause.printStackTrace();
				verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.failure"));
				verLabel.setBorder(new EmptyBorder(4, 0, 0, 2));
				verLabel.setForeground(Color.RED);
				verList = null;
				JButton reloadButton = new JButton(MCP.TRANSLATOR.translateKey("mcp.versionList.reload"));
				reloadButton.addActionListener(e -> this.reloadVersionList());
				reloadVersionListButton.set(reloadButton);
			} else {
				verList = new JComboBox<>(versionParser.getSortedVersions().toArray());
				verList.addPopupMenuListener(new PopupMenuListener() {

					@Override
					public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
					}

					@Override
					public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
						enqueueRunnable(() -> mcp.setupVersion((VersionData) verList.getSelectedItem()));
					}

					@Override
					public void popupMenuCanceled(PopupMenuEvent e) {
					}
				});

				setCurrentVersion(mcp.currentVersion == null ? null : versionParser.getVersion(mcp.currentVersion.id));
				verList.setMaximumRowCount(20);
				verLabel = new JLabel(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
			}
			topRightContainer.removeAll();
			topRightContainer.add(this.verLabel);
			if (verList != null) {
				topRightContainer.add(this.verList);
			} else if (reloadVersionListButton.get() != null) {
				JButton reloadButton = reloadVersionListButton.get();
				topRightContainer.add(reloadButton);
			}
			loadingVersions = false;
			synchronized (mcp) {
				if (mcp.isActive) {
					if (verList != null) verList.setEnabled(true);
					verLabel.setEnabled(true);
				}
			}

			topRightContainer.updateUI();
			revalidate();
			topLeftContainer.revalidate();
		});
		SwingUtilities.invokeLater(() -> {
			if (mcp.options.theme != null) {
				mcp.changeTheme(Theme.THEMES_MAP.get(mcp.options.theme));

				topRightContainer.updateUI();
				revalidate();
				topLeftContainer.revalidate();
			}
			setVisible(true);
		});
	}

	/**
	 * Checks availability of all buttons and enables them
	 */
	public void updateButtonState() {
		buttons.forEach(button -> button.setEnabled(button.getEnabled()));
		if (verList != null && !loadingVersions) verList.setEnabled(true);
		if (!loadingVersions) verLabel.setEnabled(true);
		menuBar.menuOptions.setEnabled(true);
		menuBar.setComponentsEnabled(true);
	}

	/**
	 * Disables all buttons
	 */
	public void setAllButtonsInactive() {
		buttons.forEach(button -> button.setEnabled(false));
		if (verList != null) verList.setEnabled(false);
		verLabel.setEnabled(false);
		menuBar.menuOptions.setEnabled(false);
		menuBar.setComponentsEnabled(false);
	}

	/**
	 * Refreshes version list with specified versionData
	 *
	 * @param versionData
	 */
	public void setCurrentVersion(VersionData versionData) {
		if (verList == null) {
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
	 * @param side
	 * @param progress
	 * @see MCP#setProgress(int, int)
	 */
	public void setProgress(int side, int progress) {
		progressBars[side].progress = progress;
		progressBars[side].updateProgress();
	}

	/**
	 * @param side
	 * @param progressMessage
	 * @see MCP#setProgress(int, String)
	 */
	public void setProgress(int side, String progressMessage) {
		progressBars[side].progressMsg = progressMessage;
		progressBars[side].updateProgress();
	}

	/**
	 * @param tasks
	 * @param mode
	 * @see MCP#setProgressBars(List, TaskMode)
	 */
	public void setProgressBars(List<Task> tasks, TaskMode mode) {
		int size = tasks.size();
		progressBars = new SideProgressBar[size];
		progressLabels = new JLabel[size];
		for (int i = 0; i < size; i++) {
			String name = mode.getFullName();
			if (tasks.get(i).side != Side.ANY) {
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
		if (verList == null && !loadingVersions) {
			verLabel.setText(MCP.TRANSLATOR.translateKey("mcp.versionList.failure"));
		} else {
			verLabel.setText(MCP.TRANSLATOR.translateKey("mcp.versionList.currentVersion"));
		}
		buttons.forEach(TaskButton::updateName);
		Dimension preferredButtonSize = new Dimension(0, 26);
		for (TaskButton button : buttons) {
			button.setPreferredSize(null);
			Dimension preferredButtonSize2 = button.getPreferredSize();
			preferredButtonSize.width = Math.max(preferredButtonSize2.width, preferredButtonSize.width);
			preferredButtonSize.height = Math.max(preferredButtonSize2.height, preferredButtonSize.height);
		}
		for (TaskButton button : buttons) {
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
