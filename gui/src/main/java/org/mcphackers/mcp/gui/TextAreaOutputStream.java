package org.mcphackers.mcp.gui;

import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.*;
import javax.swing.text.BadLocationException;

public class TextAreaOutputStream extends PrintStream {
	private final JTextPane textPane;

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
		if (s != null && !s.isEmpty()) {
			super.println(s);
			printString("\n");
		}
	}

	private void printString(String msg) {
		try {
			textPane.getStyledDocument().insertString(textPane.getStyledDocument().getLength(), msg, null);
		} catch (BadLocationException ex) {
			ex.printStackTrace();
		}
	}
}
