package org.mcphackers.mcp.gui;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.mcphackers.mcp.MCP;
import org.mcphackers.mcp.main.MainGUI;

public class TextAreaContextMenu extends JPopupMenu {
	private final MCP mcp;

	public TextAreaContextMenu(MCP mcp) {
		this.mcp = mcp;
		this.addItems();
	}

	private void addItems() {
		JMenuItem clearText = new JMenuItem(MCP.TRANSLATOR.translateKey("mcp.clearConsole"));
		clearText.addActionListener(actionEvent -> {
			if (this.mcp instanceof MainGUI) {
				MainGUI mainGUI = (MainGUI) this.mcp;
				mainGUI.textPane.setText("");

				// Clear the styles
				StyledDocument doc = mainGUI.textPane.getStyledDocument();
				Style defaultStyle = mainGUI.textPane.getStyle(StyleContext.DEFAULT_STYLE);
				doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);
			}
		});
		this.add(clearText);
	}
}
