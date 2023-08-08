package com.example.noteapp.helpers;

import androidx.annotation.NonNull;

/**
 * This class is used to store the information of the styled text inside the EditText<br>
 * An object of this class will represent the styled segment of the EditText and will have the following attributes<br>
 * <pre>
 * <b>spanStart</b> - the start indices of the styled segment
 * <b>spanEnd</b> - the end indices of the styled segment
 * <b>isBold</b> - indicate if the segment with {@code spanStart} and {@code spanEnd} is formatted with Bold
 * <b>isItalic</b> - indicate if the segment with {@code spanStart} and {@code spanEnd} is formatted with Italic
 * <b>isUnderlined</b> - indicate if the segment with {@code spanStart} and {@code spanEnd} is formatted with Underline
 * <b>isStrikethrough</b> - indicate if the segment with {@code spanStart} and {@code spanEnd} is formatted with Strikethrough
 * </pre>
 */
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
