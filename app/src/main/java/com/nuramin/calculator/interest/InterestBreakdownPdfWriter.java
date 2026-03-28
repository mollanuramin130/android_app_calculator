package com.nuramin.calculator.interest;

import android.content.Context;

import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.sunsetcoralcalculator.ai.system.ShareController;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.interactive.action.PDActionURI;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Styled PDF with header/footer bands and a clickable Play Store URI (PDF link annotation).
 */
public final class InterestBreakdownPdfWriter {

    private static volatile boolean pdfBoxInitialized;

    private static final float MARGIN = 40f;
    private static final float HEADER_H = 92f;
    private static final float FOOTER_H = 78f;
    private static final float LINE = 15f;
    private static final int MAX_CHARS = 88;

    private static final float HR = 0.847f;
    private static final float HG = 0.494f;
    private static final float HB = 0.392f;

    private InterestBreakdownPdfWriter() {}

    /** PdfBox-Android requires this before any PDF API use (fonts/resources). */
    private static void ensurePdfBox(Context appContext) {
        if (!pdfBoxInitialized) {
            synchronized (InterestBreakdownPdfWriter.class) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(appContext);
                    pdfBoxInitialized = true;
                }
            }
        }
    }

    public static File write(Context appContext, InterestBreakdownExportData d) throws IOException {
        Context ctx = appContext.getApplicationContext();
        ensurePdfBox(ctx);
        File out = new File(ctx.getCacheDir(), "interest_breakdown_" + System.currentTimeMillis() + ".pdf");

        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float pageW = page.getMediaBox().getWidth();
        float pageH = page.getMediaBox().getHeight();

        PDFont bold = PDType1Font.HELVETICA_BOLD;
        PDFont regular = PDType1Font.HELVETICA;

        float urlBaseline = 24f;
        float urlWidth = pageW - 2f * MARGIN;
        String storeUrl = ShareController.getPlayStoreListingUrl(ctx);

        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.setNonStrokingColor(HR, HG, HB);
            cs.addRect(0, pageH - HEADER_H, pageW, HEADER_H);
            cs.fill();

            cs.setNonStrokingColor(1f, 1f, 1f);
            drawText(cs, bold, 20f, MARGIN, pageH - 38f, ascii(ctx.getString(R.string.int_pdf_document_title)));
            drawText(cs, regular, 10.5f, MARGIN, pageH - 58f, ascii(ctx.getString(R.string.int_pdf_header_tagline)));
            String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
            drawText(cs, regular, 9f, pageW - MARGIN - 130f, pageH - 58f, ascii(dateStr));

            cs.setNonStrokingColor(0.12f, 0.16f, 0.22f);
            float y = pageH - HEADER_H - 28f;

            y = drawSection(cs, bold, regular, ctx, y,
                    R.string.int_pdf_section_inputs,
                    new String[]{
                            ctx.getString(R.string.int_pdf_line_mode, d.interestModeLabel),
                            ctx.getString(R.string.int_pdf_line_principal, d.principalLine),
                            ctx.getString(R.string.int_pdf_line_rate, d.rateLine),
                            ctx.getString(R.string.int_pdf_line_time, d.timeLine),
                    });
            y -= 8f;

            y = drawSection(cs, bold, regular, ctx, y,
                    R.string.int_pdf_section_result,
                    new String[]{
                            ctx.getString(R.string.int_pdf_line_total_interest, d.totalInterest),
                            d.totalAmountLine,
                    });
            y -= 8f;

            y = drawSection(cs, bold, regular, ctx, y,
                    R.string.int_pdf_section_breakdown,
                    new String[]{
                            ctx.getString(R.string.int_pdf_line_principal_amount, d.principalBreakdown),
                            ascii(d.interestLabel + ": " + d.interestBreakdown),
                    });

            cs.setNonStrokingColor(0.94f, 0.95f, 0.97f);
            cs.addRect(0, 0, pageW, FOOTER_H);
            cs.fill();

            cs.setStrokingColor(0.85f, 0.87f, 0.90f);
            cs.setLineWidth(0.8f);
            cs.moveTo(MARGIN, FOOTER_H);
            cs.lineTo(pageW - MARGIN, FOOTER_H);
            cs.stroke();

            cs.setNonStrokingColor(0.35f, 0.39f, 0.45f);
            float fy = 56f;
            drawText(cs, bold, 11f, MARGIN, fy, ascii(ctx.getString(R.string.app_name)));
            fy -= 16f;
            drawText(cs, regular, 9.5f, MARGIN, fy, ascii(ctx.getString(R.string.int_pdf_footer_get_app)));
            fy -= 14f;
            drawText(cs, bold, 10f, MARGIN, fy, ascii(ctx.getString(R.string.int_pdf_footer_link_label)));
            fy -= 13f;
            urlBaseline = fy;
            cs.setNonStrokingColor(0.12f, 0.35f, 0.85f);
            drawText(cs, regular, 9f, MARGIN, urlBaseline, ascii(storeUrl));
        }

        PDAnnotationLink link = new PDAnnotationLink();
        PDRectangle linkRect = new PDRectangle(MARGIN, urlBaseline - 4f, urlWidth, 13f);
        link.setRectangle(linkRect);
        PDActionURI action = new PDActionURI();
        action.setURI(storeUrl);
        link.setAction(action);
        page.getAnnotations().add(link);

        doc.save(out);
        doc.close();
        return out;
    }

    private static float drawSection(
            PDPageContentStream cs,
            PDFont bold,
            PDFont regular,
            Context ctx,
            float y,
            int titleRes,
            String[] lines) throws IOException {
        drawText(cs, bold, 12.5f, MARGIN, y, ascii(ctx.getString(titleRes)));
        y -= 18f;
        for (String line : lines) {
            for (String part : wrap(ascii(line))) {
                drawText(cs, regular, 11f, MARGIN, y, part);
                y -= LINE;
            }
            y -= 3f;
        }
        return y;
    }

    private static void drawText(PDPageContentStream cs, PDFont font, float size, float x, float y, String text)
            throws IOException {
        if (text == null || text.isEmpty()) {
            return;
        }
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static String ascii(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\u20B9", "")
                .replace("\u00a0", " ")
                .replace("\u2013", "-")
                .replace("\u2014", "-");
        StringBuilder out = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            // Helvetica WinAnsi: keep printable ASCII only to avoid showText() IllegalArgumentException.
            if (c >= 32 && c <= 126) {
                out.append(c);
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }

    private static List<String> wrap(String s) {
        List<String> parts = new ArrayList<>();
        if (s.isEmpty()) {
            parts.add("");
            return parts;
        }
        if (s.length() <= MAX_CHARS) {
            parts.add(s);
            return parts;
        }
        int start = 0;
        while (start < s.length()) {
            int end = Math.min(start + MAX_CHARS, s.length());
            if (end < s.length()) {
                int sp = s.lastIndexOf(' ', end);
                if (sp > start + 20) {
                    end = sp;
                }
            }
            parts.add(s.substring(start, end).trim());
            start = end;
            while (start < s.length() && s.charAt(start) == ' ') {
                start++;
            }
        }
        return parts;
    }
}
