/*
 * Kotoba-chan
 *
 * Copyright (C) 2013 Siim Meerits <sh0@yutani.ee>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

// Package
package ee.yutani.kotoba;

// Imports
import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

// Text view with furigana display
public class FuriganaView extends View
{
    private class TextFurigana
    {
        // Info
        private String m_text;

        // Coordinates
        float m_offset = 0.0f;
        float m_width = 0.0f;

        // Constructor
        public TextFurigana(String text)
        {
            // Info
            m_text = text;

            // Coordinates
            m_width = m_paint_f.measureText(m_text);
        }

        // Info
        //private String text() { return m_text; }

        // Coordinates
        public float offset_get() { return m_offset; }
        public void offset_set(float value) { m_offset = value; }
        public float width() { return m_width; }

        // Draw
        public void draw(Canvas canvas, float x, float y)
        {
            x -= m_width / 2.0f;
            canvas.drawText(m_text, 0, m_text.length(), x, y, m_paint_f);
        }
    }

    private class TextNormal
    {
        // Info
        private String m_text;
        private boolean m_is_marked;

        // Widths
        private float m_width_total;
        private float[] m_width_chars;

        // Constructor
        public TextNormal(String text, boolean is_marked)
        {
            // Info
            m_text = text;
            m_is_marked = is_marked;

            // Character widths
            m_width_chars = new float[m_text.length()];
            if (m_is_marked) {
                m_paint_k_mark.getTextWidths(m_text, m_width_chars);
            } else {
                m_paint_k_norm.getTextWidths(m_text, m_width_chars);
            }

            // Total width
            m_width_total = 0.0f;
            for (float v : m_width_chars)
                m_width_total += v;
        }

        // Info
        public int length() { return m_text.length(); }

        // Widths
        public float[] width_chars() { return m_width_chars; }

        // Split
        public TextNormal[] split(int offset)
        {
            return new TextNormal[]{
                new TextNormal(m_text.substring(0, offset), m_is_marked),
                new TextNormal(m_text.substring(offset), m_is_marked)
            };
        }

        // Draw
        public float draw(Canvas canvas, float x, float y)
        {
            if (m_is_marked) {
                canvas.drawText(m_text, 0, m_text.length(), x, y, m_paint_k_mark);
            } else {
                canvas.drawText(m_text, 0, m_text.length(), x, y, m_paint_k_norm);
            }
            return m_width_total;
        }
    }

    private class LineFurigana
    {
        // Text
        private Vector<TextFurigana> m_text = new Vector<TextFurigana>();
        private Vector<Float> m_offset = new Vector<Float>();

        // Add
        public void add(TextFurigana text)
        {
            if (text != null)
                m_text.add(text);
        }

        // Calculate
        public void calculate()
        {
            // Check size
            if (m_text.size() == 0)
                return;

            /*
             * Constraint optimization
             *   minimize sum (r[i] - c[i])^2 over i
             *   c[i] + w[i]/2 <= c[i+1] - w[i+1]/2
             *   c[0] >= 0.0f
             *   c[n] <= m_linemax
             */

            /*
            // Debug
            String str = "";
            for (TextFurigana text : m_text)
                str += "'" + text.text() + "' ";
            */

            // r[] - ideal offsets
            float[] r = new float[m_text.size()];
            for (int i = 0; i < m_text.size(); i++)
                r[i] = m_text.get(i).offset_get();

            // a[] - constraint matrix
            float[][] a = new float[m_text.size() + 1][m_text.size()];
            for (int i = 0; i < a.length; i++)
                for (int j = 0; j < a[0].length; j++)
                    a[i][j] = 0.0f;
            a[0][0] = 1.0f;
            for (int i = 1; i < a.length - 2; i++) {
                a[i][i - 1] = -1.0f;
                a[i][i] = 1.0f;
            }
            a[a.length - 1][a[0].length - 1] = -1.0f;

            // b[] - constraint vector
            float[] b = new float[m_text.size() + 1];
            b[0] = -r[0] + (0.5f * m_text.get(0).width());
            for (int i = 1; i < b.length - 2; i++)
                b[i] = (0.5f * (m_text.get(i).width() + m_text.get(i - 1).width())) + (r[i - 1] - r[i]);
            b[b.length - 1] = -m_linemax + r[r.length -1] + (0.5f * m_text.get(m_text.size() - 1).width());

            // Calculate
            float[] x = new float[m_text.size()];
            for (int i = 0; i < x.length; i++)
                x[i] = 0.0f;
            QuadraticOptimizer co = new QuadraticOptimizer(a, b);
            co.calculate(x);
            for (int i = 0; i < x.length; i++)
                m_offset.add(x[i] + r[i]);
        }

        // Draw
        public void draw(Canvas canvas, float y)
        {
            y -= m_paint_f.descent();
            if (m_offset.size() == m_text.size()) {
                // Render with fixed offsets
                for (int i = 0; i < m_offset.size(); i++)
                    m_text.get(i).draw(canvas, m_offset.get(i), y);
            } else {
                // Render with original offsets
                for (TextFurigana text : m_text)
                    text.draw(canvas, text.offset_get(), y);
            }
        }
    }

    private class LineNormal
    {
        // Text
        private Vector<TextNormal> m_text = new Vector<TextNormal>();

        // Elements
        public int size() { return m_text.size(); }
        public void add(Vector<TextNormal> text) { m_text.addAll(text); }

        // Draw
        public void draw(Canvas canvas, float y)
        {
            y -= m_paint_k_norm.descent();
            float x = 0.0f;
            for (TextNormal text : m_text)
                x += text.draw(canvas, x, y);
        }
    }

    private class Span
    {
        // Text
        private TextFurigana m_furigana = null;
        private Vector<TextNormal> m_normal = new Vector<TextNormal>();

        // Widths
        private Vector<Float> m_width_chars = new Vector<Float>();
        private float m_width_total = 0.0f;

        // Constructors
        public Span(String text_f, String text_k, int mark_s, int mark_e)
        {
            // Furigana text
            if (text_f.length() > 0)
                m_furigana = new TextFurigana(text_f);

            // Normal text
            if (mark_s < text_k.length() && mark_e > 0 && mark_s < mark_e) {

                // Fix marked bounds
                mark_s = Math.max(0, mark_s);
                mark_e = Math.min(text_k.length(), mark_e);

                // Prefix
                if (mark_s > 0)
                    m_normal.add(new TextNormal(text_k.substring(0, mark_s), false));

                // Marked
                if (mark_e > mark_s)
                    m_normal.add(new TextNormal(text_k.substring(mark_s, mark_e), true));

                // Postfix
                if (mark_e < text_k.length())
                    m_normal.add(new TextNormal(text_k.substring(mark_e), false));

            } else {

                // Non marked
                m_normal.add(new TextNormal(text_k, false));

            }

            // Widths
            widths_calculate();
        }

        public Span(Vector<TextNormal> normal)
        {
            // Only normal text
            m_normal = normal;

            // Widths
            widths_calculate();
        }

        // Text
        public TextFurigana furigana(float x) {
            if (m_furigana == null)
                return null;
            m_furigana.offset_set(x + (m_width_total / 2.0f));
            return m_furigana;
        }
        public Vector<TextNormal> normal() { return m_normal; }

        // Widths
        public Vector<Float> widths() { return m_width_chars; }
        private void widths_calculate()
        {
            // Chars
            if (m_furigana == null) {
                for (TextNormal normal : m_normal)
                    for (float v : normal.width_chars())
                        m_width_chars.add(v);
            } else {
                float sum = 0.0f;
                for (TextNormal normal : m_normal)
                    for (float v : normal.width_chars())
                        sum += v;
                m_width_chars.add(sum);
            }

            // Total
            m_width_total = 0.0f;
            for (float v : m_width_chars)
                m_width_total += v;
        }

        // Split
        public void split(int offset, Vector<TextNormal> normal_a, Vector<TextNormal> normal_b)
        {
            // Check if no furigana
            assert(m_furigana == null);

            // Split normal list
            for (TextNormal cur : m_normal) {
                if (offset <= 0) {
                    normal_b.add(cur);
                } else if (offset >= cur.length()) {
                    normal_a.add(cur);
                } else {
                    TextNormal[] split = cur.split(offset);
                    normal_a.add(split[0]);
                    normal_b.add(split[1]);
                }
                offset -= cur.length();
            }
        }
    }

    // Paints
    private TextPaint m_paint_f = new TextPaint();
    private TextPaint m_paint_k_norm = new TextPaint();
    private TextPaint m_paint_k_mark = new TextPaint();

    // Sizes
    private float m_linesize = 0.0f;
    private float m_height_n = 0.0f;
    private float m_height_f = 0.0f;
    private float m_linemax = 0.0f;

    // Spans and lines
    private Vector<Span> m_span = new Vector<Span>();
    private Vector<LineNormal> m_line_n = new Vector<LineNormal>();
    private Vector<LineFurigana> m_line_f = new Vector<LineFurigana>();

    // Constructors
    public FuriganaView(Context context) { super(context); }
    public FuriganaView(Context context, AttributeSet attrs) { super(context, attrs); }
    public FuriganaView(Context context, AttributeSet attrs, int style) { super(context, attrs, style); }

    // Text functions
    public void text_set(TextPaint tp, String text, int mark_s, int mark_e)
    {
        // Text
        m_paint_k_norm = new TextPaint(tp);
        m_paint_k_mark = new TextPaint(tp);
        m_paint_k_mark.setFakeBoldText(true);
        m_paint_f = new TextPaint(tp);
        m_paint_f.setTextSize(m_paint_f.getTextSize() / 2.0f);

        // Linesize
        m_height_n = m_paint_k_norm.descent() - m_paint_k_norm.ascent();
        m_height_f = m_paint_f.descent() - m_paint_f.ascent();
        m_linesize = m_height_n + m_height_f;

        // Clear spans
        m_span.clear();

        // Sizes
        m_linesize = m_paint_f.getFontSpacing() + Math.max(m_paint_k_norm.getFontSpacing(), m_paint_k_mark.getFontSpacing());

        // Spannify text
        while (text.length() > 0) {
            int idx = text.indexOf('{');
            if (idx >= 0) {
                // Prefix string
                if (idx > 0) {
                    // Spans
                    m_span.add(new Span("", text.substring(0, idx), mark_s, mark_e));

                    // Remove text
                    text = text.substring(idx);
                    mark_s -= idx;
                    mark_e -= idx;
                }

                // End bracket
                idx = text.indexOf('}');
                if (idx < 1) {
                    // Error
                    text = "";
                    break;
                } else if (idx == 1) {
                    // Empty bracket
                    text = text.substring(2);
                    continue;
                }

                // Spans
                String[] split = text.substring(1, idx).split(";");
                m_span.add(new Span(((split.length > 1) ? split[1] : ""), split[0], mark_s, mark_e));

                // Remove text
                text = text.substring(idx + 1);
                mark_s -= split[0].length();
                mark_e -= split[0].length();

            } else {
                // Single span
                m_span.add(new Span("", text, mark_s, mark_e));
                text = "";
            }
        }

        // Invalidate view
        this.invalidate();
        this.requestLayout();
    }

    // Size calculation
    @Override protected void onMeasure(int width_ms, int height_ms)
    {
        // Modes
        int wmode = View.MeasureSpec.getMode(width_ms);
        int hmode = View.MeasureSpec.getMode(height_ms);

        // Dimensions
        int wold = View.MeasureSpec.getSize(width_ms);
        int hold = View.MeasureSpec.getSize(height_ms);

        // Draw mode
        if (wmode == View.MeasureSpec.EXACTLY || wmode == View.MeasureSpec.AT_MOST && wold > 0) {
            // Width limited
            text_calculate(wold);
        } else {
            // Width unlimited
            text_calculate(-1.0f);
        }

        // New height
        int hnew = (int)Math.round(Math.ceil(m_linesize * (float)m_line_n.size()));
        int wnew = wold;
        if (wmode != View.MeasureSpec.EXACTLY && m_line_n.size() <= 1)
            wnew = (int)Math.round(Math.ceil(m_linemax));
        if (hmode != View.MeasureSpec.UNSPECIFIED && hnew > hold)
            hnew |= MEASURED_STATE_TOO_SMALL;

        // Set result
        setMeasuredDimension(wnew, hnew);
    }

    private void text_calculate(float line_max)
    {
        // Clear lines
        m_line_n.clear();
        m_line_f.clear();

        // Sizes
        m_linemax = 0.0f;

        // Check if no limits on width
        if (line_max < 0.0) {

            // Create single normal and furigana line
            LineNormal line_n = new LineNormal();
            LineFurigana line_f = new LineFurigana();

            // Loop spans
            for (Span span : m_span) {
                // Text
                line_n.add(span.normal());
                line_f.add(span.furigana(m_linemax));

                // Widths update
                for (float width : span.widths())
                    m_linemax += width;
            }

            // Commit both lines
            m_line_n.add(line_n);
            m_line_f.add(line_f);

        } else {

            // Lines
            float line_x = 0.0f;
            LineNormal line_n = new LineNormal();
            LineFurigana line_f = new LineFurigana();

            // Initial span
            int span_i = 0;
            Span span = m_span.get(span_i);

            // Iterate
            while (span != null) {
                // Start offset
                float line_s = line_x;

                // Calculate possible line size
                Vector<Float> widths = span.widths();
                int i = 0;
                for (i = 0; i < widths.size(); i++) {
                    if (line_x + widths.get(i) <= line_max)
                        line_x += widths.get(i);
                    else
                        break;
                }

                // Add span to line
                if (i >= 0 && i < widths.size()) {

                    // Span does not fit entirely
                    if (i > 0) {
                        // Split half that fits
                        Vector<TextNormal> normal_a = new Vector<TextNormal>();
                        Vector<TextNormal> normal_b = new Vector<TextNormal>();
                        span.split(i, normal_a, normal_b);
                        line_n.add(normal_a);
                        span = new Span(normal_b);
                    }

                    // Add new line with current spans
                    if (line_n.size() != 0) {
                        // Add
                        m_linemax = (m_linemax > line_x ? m_linemax : line_x);
                        m_line_n.add(line_n);
                        m_line_f.add(line_f);

                        // Reset
                        line_n = new LineNormal();
                        line_f = new LineFurigana();
                        line_x = 0.0f;

                        // Next span
                        continue;
                    }

                } else {

                    // Span fits entirely
                    line_n.add(span.normal());
                    line_f.add(span.furigana(line_s));

                }

                // Next span
                span = null;
                span_i++;
                if (span_i < m_span.size())
                    span = m_span.get(span_i);
            }

            // Last span
            if (line_n.size() != 0) {
                // Add
                m_linemax = (m_linemax > line_x ? m_linemax : line_x);
                m_line_n.add(line_n);
                m_line_f.add(line_f);
            }
        }

        // Calculate furigana
        for (LineFurigana line : m_line_f)
            line.calculate();
    }

    // Drawing
    @Override public void onDraw(Canvas canvas)
    {
        /*
        // Debug background
        Paint paint = new Paint();
        paint.setARGB(0x30, 0, 0, 0xff);
        Rect rect = new Rect();
        canvas.getClipBounds(rect);
        canvas.drawRect(rect, paint);
        */

        // Check
        assert(m_line_n.size() == m_line_f.size());

        // Coordinates
        float y = m_linesize;

        // Loop lines
        for (int i = 0; i < m_line_n.size(); i++) {
            m_line_n.get(i).draw(canvas, y);
            m_line_f.get(i).draw(canvas, y - m_height_n);
            y += m_linesize;
        }
    }
}
