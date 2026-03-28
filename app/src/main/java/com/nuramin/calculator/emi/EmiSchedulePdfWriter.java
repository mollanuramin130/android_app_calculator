package com.nuramin.calculator.emi;

import android.content.Context;

import com.nuramin.calculator.util.CalculatorUtils;
import com.nuramin.calculator.util.LocaleFormatManager;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Multi-page PDF for loan amortization (styled header/footer, Play Store link on last page).
 */
public final class EmiSchedulePdfWriter {

    private static volatile boolean pdfBoxInitialized;

    private static final float MARGIN = 40f;
    private static final float HEADER_H = 88f;
    private static final float FOOTER_H = 78f;
    private static final float ROW_H = 11f;
    private static final float TABLE_FONT = 8f;
    private static final float COL_HEADER_FONT = 8.5f;
    private static final float MIN_Y_BEFORE_NEW_PAGE = 115f;

    private static final float HR = 0.847f;
    private static final float HG = 0.494f;
    private static final float HB = 0.392f;

    private EmiSchedulePdfWriter() {}

    private static void ensurePdfBox(Context appContext) {
        if (!pdfBoxInitialized) {
            synchronized (EmiSchedulePdfWriter.class) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(appContext);
                    pdfBoxInitialized = true;
                }
            }
        }
    }

    public static File write(Context appContext, EmiLoanSnapshot snapshot, List<EmiScheduleRow> rows)
            throws IOException {
        Context ctx = appContext.getApplicationContext();
        ensurePdfBox(ctx);
        File out = new File(ctx.getCacheDir(), "emi_schedule_" + System.currentTimeMillis() + ".pdf");

        PDFont bold = PDType1Font.HELVETICA_BOLD;
        PDFont regular = PDType1Font.HELVETICA;

        double emi = EmiCalculatorMath.monthlyEmi(snapshot.principal, snapshot.annualRatePct, snapshot.months);
        String emiFormatted = LocaleFormatManager.formatCurrency(ctx, emi);
        String loan = LocaleFormatManager.formatCurrency(ctx, snapshot.principal);
        String rateStr = CalculatorUtils.formatNumber(snapshot.annualRatePct) + "%";
        String tenureStr = ctx.getString(R.string.emi_schedule_tenure_months, snapshot.months);

        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        float pageW = page.getMediaBox().getWidth();
        float pageH = page.getMediaBox().getHeight();

        String storeUrl = ShareController.getPlayStoreListingUrl(ctx);
        float urlBaseline = 24f;
        float urlWidth = pageW - 2f * MARGIN;

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        float y = drawFirstPageTop(cs, bold, regular, ctx, pageW, pageH, emiFormatted, loan, rateStr, tenureStr);

        y = drawColumnHeaders(cs, bold, regular, ctx, y);
        y -= ROW_H;

        for (EmiScheduleRow row : rows) {
            if (y < MIN_Y_BEFORE_NEW_PAGE) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                pageW = page.getMediaBox().getWidth();
                pageH = page.getMediaBox().getHeight();
                cs = new PDPageContentStream(doc, page);
                y = pageH - 52f;
                drawText(cs, bold, 11f, MARGIN, y, ascii(ctx.getString(R.string.emi_pdf_continued)));
                y -= 18f;
                y = drawColumnHeaders(cs, bold, regular, ctx, y);
                y -= ROW_H;
            }
            drawDataRow(cs, regular, ctx, row, y);
            y -= ROW_H;
        }

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

        cs.close();

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

    private static float drawFirstPageTop(
            PDPageContentStream cs,
            PDFont bold,
            PDFont regular,
            Context ctx,
            float pageW,
            float pageH,
            String emiFormatted,
            String loan,
            String rateStr,
            String tenureStr) throws IOException {
        cs.setNonStrokingColor(HR, HG, HB);
        cs.addRect(0, pageH - HEADER_H, pageW, HEADER_H);
        cs.fill();

        cs.setNonStrokingColor(1f, 1f, 1f);
        drawText(cs, bold, 20f, MARGIN, pageH - 36f, ascii(ctx.getString(R.string.emi_pdf_document_title)));
        drawText(cs, regular, 10.5f, MARGIN, pageH - 56f, ascii(ctx.getString(R.string.emi_pdf_subtitle)));
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        drawText(cs, regular, 9f, pageW - MARGIN - 130f, pageH - 56f, ascii(dateStr));

        cs.setNonStrokingColor(0.12f, 0.16f, 0.22f);
        float y = pageH - HEADER_H - 24f;
        drawText(cs, bold, 11f, MARGIN, y, ascii(ctx.getString(R.string.emi_pdf_summary_title)));
        y -= 16f;
        String sum = ctx.getString(R.string.emi_schedule_summary_line, loan, rateStr, tenureStr);
        drawText(cs, regular, 10f, MARGIN, y, ascii(sum));
        y -= 14f;
        drawText(cs, regular, 10f, MARGIN, y, ascii(ctx.getString(R.string.emi_schedule_emi_line, emiFormatted)));
        y -= 18f;
        return y;
    }

    private static float drawColumnHeaders(
            PDPageContentStream cs, PDFont bold, PDFont regular, Context ctx, float y) throws IOException {
        drawText(cs, bold, COL_HEADER_FONT, MARGIN, y, ascii(ctx.getString(R.string.emi_schedule_col_mo)));
        drawText(cs, bold, COL_HEADER_FONT, MARGIN + 38f, y, ascii(ctx.getString(R.string.emi_schedule_col_principal)));
        drawText(cs, bold, COL_HEADER_FONT, MARGIN + 198f, y, ascii(ctx.getString(R.string.emi_schedule_col_interest)));
        drawText(cs, bold, COL_HEADER_FONT, MARGIN + 358f, y, ascii(ctx.getString(R.string.emi_schedule_col_balance)));
        return y;
    }

    private static void drawDataRow(
            PDPageContentStream cs, PDFont regular, Context ctx, EmiScheduleRow row, float y) throws IOException {
        cs.setNonStrokingColor(0.12f, 0.16f, 0.22f);
        String m = String.valueOf(row.month);
        String p = ascii(LocaleFormatManager.formatCurrency(ctx, row.principalPart));
        String i = ascii(LocaleFormatManager.formatCurrency(ctx, row.interestPart));
        String b = ascii(LocaleFormatManager.formatCurrency(ctx, row.closingBalance));
        drawText(cs, regular, TABLE_FONT, MARGIN, y, m);
        drawText(cs, regular, TABLE_FONT, MARGIN + 38f, y, p);
        drawText(cs, regular, TABLE_FONT, MARGIN + 198f, y, i);
        drawText(cs, regular, TABLE_FONT, MARGIN + 358f, y, b);
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
            if (c >= 32 && c <= 126) {
                out.append(c);
            } else {
                out.append('?');
            }
        }
        return out.toString();
    }
}
