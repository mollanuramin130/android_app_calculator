package com.nuramin.calculator.interest;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Prints an existing PDF file via the system print UI.
 */
public final class InterestPdfPrintDocumentAdapter extends PrintDocumentAdapter {

    private final File pdfFile;
    /** {@link PrintDocumentInfo#PAGE_COUNT_UNKNOWN} (-1) for variable-length PDFs (e.g. EMI schedule). */
    private final int pageCount;

    public InterestPdfPrintDocumentAdapter(@NonNull File pdfFile) {
        this(pdfFile, 1);
    }

    public InterestPdfPrintDocumentAdapter(@NonNull File pdfFile, int pageCount) {
        this.pdfFile = pdfFile;
        this.pageCount = pageCount;
    }

    @Override
    public void onLayout(
            PrintAttributes oldAttributes,
            PrintAttributes newAttributes,
            CancellationSignal cancellationSignal,
            LayoutResultCallback callback,
            Bundle extras) {
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }
        int pc = pageCount < 0 ? PrintDocumentInfo.PAGE_COUNT_UNKNOWN : pageCount;
        PrintDocumentInfo info = new PrintDocumentInfo.Builder(pdfFile.getName())
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pc)
                .build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(
            PageRange[] pages,
            ParcelFileDescriptor destination,
            CancellationSignal cancellationSignal,
            WriteResultCallback callback) {
        try (InputStream in = new FileInputStream(pdfFile);
             OutputStream out = new java.io.FileOutputStream(destination.getFileDescriptor())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    return;
                }
                out.write(buf, 0, n);
            }
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (IOException e) {
            callback.onWriteFailed(null);
        }
    }
}
