package com.nuramin.calculator.emi;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.FileProvider;

import com.nuramin.calculator.interest.InterestPdfPrintDocumentAdapter;
import com.nuramin.sunsetcoralcalculator.R;

import java.io.File;
import java.util.List;

/**
 * Share or print EMI schedule PDF (same flow as Interest breakdown export).
 */
public final class EmiScheduleExporter {

    private EmiScheduleExporter() {}

    public static void showMenu(
            Context context,
            View anchor,
            EmiLoanSnapshot snapshot,
            List<EmiScheduleRow> rows) {
        if (snapshot == null || rows == null || rows.isEmpty()) {
            Toast.makeText(context, R.string.emi_pdf_error, Toast.LENGTH_SHORT).show();
            return;
        }
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_emi_schedule_export, popup.getMenu());
        popup.setForceShowIcon(true);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_emi_schedule_share) {
                sharePdf(context, snapshot, rows);
            } else if (id == R.id.action_emi_schedule_print) {
                printPdf(context, snapshot, rows);
            }
            return true;
        });
        popup.show();
    }

    private static String fileProviderAuthority(Context c) {
        return c.getPackageName() + ".fileprovider";
    }

    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return context instanceof Activity ? (Activity) context : null;
    }

    private static void sharePdf(Context context, EmiLoanSnapshot snapshot, List<EmiScheduleRow> rows) {
        Context app = context.getApplicationContext();
        try {
            File pdf = EmiSchedulePdfWriter.write(app, snapshot, rows);
            android.net.Uri uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), pdf);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.emi_pdf_subject));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(intent, context.getString(R.string.int_share_pdf));
            if (findActivity(context) == null) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(chooser);
        } catch (Throwable t) {
            Toast.makeText(context, R.string.emi_pdf_error, Toast.LENGTH_SHORT).show();
        }
    }

    private static void printPdf(Context context, EmiLoanSnapshot snapshot, List<EmiScheduleRow> rows) {
        Activity activity = findActivity(context);
        if (activity == null) {
            Toast.makeText(context, R.string.emi_pdf_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Context app = activity.getApplicationContext();
        try {
            File pdf = EmiSchedulePdfWriter.write(app, snapshot, rows);
            PrintManager pm = (PrintManager) activity.getSystemService(Context.PRINT_SERVICE);
            if (pm == null) {
                Toast.makeText(activity, R.string.emi_pdf_error, Toast.LENGTH_SHORT).show();
                return;
            }
            String job = activity.getString(R.string.emi_pdf_print_job_name);
            pm.print(job, new InterestPdfPrintDocumentAdapter(pdf, PrintDocumentInfo.PAGE_COUNT_UNKNOWN), null);
        } catch (Throwable t) {
            Toast.makeText(activity, R.string.emi_pdf_error, Toast.LENGTH_SHORT).show();
        }
    }
}
