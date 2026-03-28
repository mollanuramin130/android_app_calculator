package com.nuramin.calculator.interest;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.print.PrintManager;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.nuramin.sunsetcoralcalculator.R;

import java.io.File;

/**
 * Share or print interest breakdown as PDF (includes Play Store link in the document).
 */
public final class InterestBreakdownExporter {

    private InterestBreakdownExporter() {}

    public static void showMenu(View panel, View anchor) {
        InterestBreakdownExportData data = InterestBreakdownExportData.from(panel);
        if (data == null) {
            Toast.makeText(panel.getContext(), R.string.int_calculate_first, Toast.LENGTH_SHORT).show();
            return;
        }
        Context c = panel.getContext();
        PopupMenu popup = new PopupMenu(c, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_interest_export, popup.getMenu());
        popup.setForceShowIcon(true);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_share_pdf) {
                sharePdf(c, data);
            } else if (id == R.id.action_print_pdf) {
                printPdf(c, data);
            }
            return true;
        });
        popup.show();
    }

    private static String fileProviderAuthority(Context c) {
        return c.getPackageName() + ".fileprovider";
    }

    /** Unwraps Activity from themed/wrapped contexts used by PopupMenu and views. */
    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context instanceof Activity ? (Activity) context : null;
    }

    private static void sharePdf(Context context, InterestBreakdownExportData data) {
        Context app = context.getApplicationContext();
        try {
            File pdf = InterestBreakdownPdfWriter.write(app, data);
            android.net.Uri uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), pdf);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.int_pdf_subject));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.int_share_pdf));
            if (findActivity(context) == null) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(chooser);
        } catch (Throwable t) {
            Toast.makeText(context, R.string.int_pdf_error, Toast.LENGTH_SHORT).show();
        }
    }

    private static void printPdf(Context context, InterestBreakdownExportData data) {
        Activity activity = findActivity(context);
        if (activity == null) {
            Toast.makeText(context, R.string.int_pdf_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Context app = activity.getApplicationContext();
        try {
            File pdf = InterestBreakdownPdfWriter.write(app, data);
            PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
            if (pm == null) {
                Toast.makeText(activity, R.string.int_pdf_error, Toast.LENGTH_SHORT).show();
                return;
            }
            String job = activity.getString(R.string.int_pdf_print_job_name);
            pm.print(job, new InterestPdfPrintDocumentAdapter(pdf), null);
        } catch (Throwable t) {
            Toast.makeText(activity, R.string.int_pdf_error, Toast.LENGTH_SHORT).show();
        }
    }
}
