package org.mcphackers.mcp.gui;

import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextPane;

public class TextAreaOutputStream extends PrintStream {
	private JTextPane textPane;

	public TextAreaOutputStream(JTextPane textArea, OutputStream out) {
		super(out, true);
		this.textPane = textArea;
	}

	@Override
	public void print(Object o) {
		printString(String.valueOf(o));
		super.print(o);
	}

	@Override
	public void println(Object o) {
		super.println(o);
		printString("\n");
	}

	@Override
	public void print(String s) {
		printString(s);
		super.print(s);
	}

	@Override
	public void println(String s) {
		super.println(s);
		printString("\n");
	}

    private void printString(String msg) {
        int len = textPane.getDocument().getLength();
        textPane.setCaretPosition(len);
        textPane.replaceSelection(msg);
    }
}