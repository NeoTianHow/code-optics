package com.psa.capstone.be.utilities;

import java.util.List;

public class DiffHunk {
    final int startLine;
    final int endLine;
    final String content;
    final List<String> existingComments;

    public DiffHunk(int startLine, int endLine, String content, List<String> existingComments) {
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.existingComments = existingComments;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getContent() {
        return content;
    }

    public List<String> getExistingComments() {
        return existingComments;
    }

}