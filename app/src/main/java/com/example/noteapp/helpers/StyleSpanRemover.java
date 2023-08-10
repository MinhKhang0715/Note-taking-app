package com.example.noteapp.helpers;

import android.text.Spannable;
import android.text.style.CharacterStyle;
import android.text.style.MetricAffectingSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;

/**
 * I can't remove the style of the selected text as the way i'm intend to<br>
 * This class help me with that<br>
 * All credit goes to <a href="https://stackoverflow.com/a/17922181">ramaral on Stackoverflow</a>
 */
public class StyleSpanRemover {

    public void removeOne(Spannable spannable, int startSelection, int endSelection, Class<? extends CharacterStyle> style) {
        ArrayList<SpanParts> spansParts = getSpanParts(spannable, startSelection, endSelection);
        removeOneSpan(spannable, startSelection, endSelection, style);
        restoreSpans(spannable, spansParts);
    }

    public void removeStyle(Spannable spannable, int startSelection, int endSelection, int styleToRemove) {
        ArrayList<SpanParts> spansParts = getSpanParts(spannable, startSelection, endSelection);
        removeStyleSpan(spannable, startSelection, endSelection, styleToRemove);
        restoreSpans(spannable, spansParts);
    }

    private void restoreSpans(Spannable spannable, ArrayList<SpanParts> spansParts) {
        for (SpanParts spanParts : spansParts) {
            if (spanParts.part1.canApply())
                spannable.setSpan(spanParts.part1.span, spanParts.part1.start,
                        spanParts.part1.end, spanParts.spanFlag);
            if (spanParts.part2.canApply())
                spannable.setSpan(spanParts.part2.span, spanParts.part2.start,
                        spanParts.part2.end, spanParts.spanFlag);
        }
    }

    private void removeOneSpan(Spannable spannable, int startSelection, int endSelection, Class<? extends CharacterStyle> style) {
        CharacterStyle[] spansToRemove = spannable.getSpans(startSelection, endSelection, CharacterStyle.class);
        for (CharacterStyle span : spansToRemove) {
            if (span.getUnderlying().getClass() == style)
                spannable.removeSpan(span);
        }
    }

    private void removeStyleSpan(Spannable spannable, int startSelection, int endSelection, int styleToRemove) {
        MetricAffectingSpan[] spans = spannable.getSpans(startSelection, endSelection, MetricAffectingSpan.class);
        for (MetricAffectingSpan span : spans) {
            int stylesApplied;
            int stylesToApply;
            int spanStart;
            int spanEnd;
            int spanFlag;
            Object spanUnd = span.getUnderlying();
            if (spanUnd instanceof StyleSpan) {
                spanFlag = spannable.getSpanFlags(spanUnd);
                stylesApplied = ((StyleSpan) spanUnd).getStyle();
                stylesToApply = stylesApplied & ~styleToRemove;

                spanStart = spannable.getSpanStart(span);
                spanEnd = spannable.getSpanEnd(span);
                if (spanEnd >= 0 && spanStart >= 0) {
                    spannable.removeSpan(span);
                    spannable.setSpan(new StyleSpan(stylesToApply), spanStart, spanEnd, spanFlag);
                }
            }
        }
    }

    private ArrayList<SpanParts> getSpanParts(Spannable spannable, int startSelection, int endSelection) {
        ArrayList<SpanParts> spansParts = new ArrayList<>();
        Object[] spans = spannable.getSpans(startSelection, endSelection, Object.class);
        SpanParts spanParts = new SpanParts();
        for (Object span : spans) {
            if (span instanceof CharacterStyle) {
                int spanStart = spannable.getSpanStart(span);
                int spanEnd = spannable.getSpanEnd(span);
                if (spanStart == startSelection && spanEnd == endSelection) continue;
                spanParts.spanFlag = spannable.getSpanFlags(span);
                spanParts.part1.span = CharacterStyle.wrap((CharacterStyle) span);
                spanParts.part1.start = spanStart;
                spanParts.part1.end = startSelection;
                spanParts.part2.span = CharacterStyle.wrap((CharacterStyle) span);
                spanParts.part2.start = endSelection;
                spanParts.part2.end = spanEnd;
                spansParts.add(spanParts);
            }
        }
        return spansParts;
    }

    private static class SpanParts {
        int spanFlag;
        Part part1, part2;

        SpanParts() {
            part1 = new Part();
            part2 = new Part();
        }
    }

    private static class Part {
        CharacterStyle span;
        int start, end;

        boolean canApply() {
            return start < end;
        }
    }
}
