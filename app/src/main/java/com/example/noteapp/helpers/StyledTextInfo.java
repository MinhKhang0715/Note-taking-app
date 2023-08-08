package com.example.noteapp.helpers;

import androidx.annotation.NonNull;

public class StyledTextInfo {
    private final int spanStart, spanEnd;
    private boolean isBold, isItalic, isUnderlined, isStrikethrough;

    public StyledTextInfo(int spanStart, int spanEnd, boolean isBole, boolean isItalic, boolean isUnderlined, boolean isStrikethrough) {
        this.spanStart = spanStart;
        this.spanEnd = spanEnd;
        this.isBold = isBole;
        this.isItalic = isItalic;
        this.isUnderlined = isUnderlined;
        this.isStrikethrough = isStrikethrough;
    }

    public int getSpanStart() {
        return spanStart;
    }

    public int getSpanEnd() {
        return spanEnd;
    }

    public boolean isBold() {
        return isBold;
    }

    public boolean isItalic() {
        return isItalic;
    }

    public boolean isUnderlined() {
        return isUnderlined;
    }

    public boolean isStrikethrough() {
        return isStrikethrough;
    }

    public void setBold(boolean bold) {
        isBold = bold;
    }

    public void setItalic(boolean italic) {
        isItalic = italic;
    }

    public void setUnderlined(boolean underlined) {
        isUnderlined = underlined;
    }

    public void setStrikethrough(boolean strikethrough) {
        isStrikethrough = strikethrough;
    }

    @NonNull
    @Override
    public String toString() {
        return "StyledTextInfo{" +
                "spanStart=" + spanStart +
                ", spanEnd=" + spanEnd +
                ", isBold=" + isBold +
                ", isItalic=" + isItalic +
                ", isUnderlined=" + isUnderlined +
                ", isStrikethrough=" + isStrikethrough +
                '}';
    }
}
