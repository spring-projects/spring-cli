package org.springframework.cli.runtime.engine.actions.handlers.json;

import org.openrewrite.shaded.jgit.diff.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConversionUtils {

	private static final Logger log = LoggerFactory.getLogger(ConversionUtils.class);

	private static EditList getDiff(String txt1, String txt2) {
		RawText rt1 = new RawText(txt1.getBytes(StandardCharsets.UTF_8));
		RawText rt2 = new RawText(txt2.getBytes(StandardCharsets.UTF_8));
		EditList diffList = new EditList();
		diffList.addAll(new HistogramDiff().diff(RawTextComparator.DEFAULT, rt1, rt2));
		return diffList;
	}

	public static Optional<Lsp.TextDocumentEdit> computeTextDocEdit(String uri, String before, String after, String changeAnnotationId) {
		ListLineTracker lines = new ListLineTracker();
		lines.set(before);

		ListLineTracker newLines = new ListLineTracker();
		newLines.set(after);

		EditList diff = getDiff(before, after);
		if (!diff.isEmpty()) {
			Lsp.TextDocumentEdit edit = new Lsp.TextDocumentEdit(new Lsp.TextDocumentIdentifier(uri), new ArrayList<>());
			List<Lsp.TextEdit> textEdits = edit.edits();
			int start, end;
			Lsp.Range range;
			String newText;
			for (Edit e : diff) {
				try {
					switch(e.getType()) {
						case DELETE:
							start = lines.getLineOffset(e.getBeginA());
							end = getStartOfLine(lines, e.getEndA());
							range = new Lsp.Range(toPosition(lines, start), toPosition(lines, end));
							textEdits.add(new Lsp.TextEdit(range, "", changeAnnotationId));
							break;
						case INSERT:
							Lsp.Position position = toPosition(lines, lines.getLineOffset(e.getBeginA()));
							range = new Lsp.Range(position, position);
							newText = after.substring(newLines.getLineOffset(e.getBeginB()), getStartOfLine(newLines, e.getEndB()));
							textEdits.add(new Lsp.TextEdit(range, newText, changeAnnotationId));
							break;
						case REPLACE:
							start = lines.getLineOffset(e.getBeginA());
							end = getStartOfLine(lines, e.getEndA());
							range = new Lsp.Range(toPosition(lines, start), toPosition(lines, end));
							newText = after.substring(newLines.getLineOffset(e.getBeginB()), getStartOfLine(newLines, e.getEndB()));
							textEdits.add(new Lsp.TextEdit(range, newText, changeAnnotationId));
							break;
						case EMPTY:
							break;
					}
				} catch (BadLocationException ex) {
					log.error("Diff conversion failed", ex);
				}
			}
			return Optional.of(edit);
		}
		return Optional.empty();
	}

	private static int getStartOfLine(ListLineTracker lines, int lineNumber) throws BadLocationException {
		IRegion lineInformation = lines.getLineInformation(lineNumber);
		if (lineInformation != null) {
			return lineInformation.getOffset();
		}
		if (lineNumber > 0) {
			IRegion currentLine = lines.getLineInformation(lineNumber - 1);
			return currentLine.getOffset() + currentLine.getLength();
		}
		return 0;
	}

	public static Lsp.Position toPosition(ListLineTracker lines, int offset) throws BadLocationException {
		int line = lines.getLineNumberOfOffset(offset);
		int startOfLine = lines.getLineInformation(line).getOffset();
		int column = offset - startOfLine;
		return new Lsp.Position(line, column);
	}


}
