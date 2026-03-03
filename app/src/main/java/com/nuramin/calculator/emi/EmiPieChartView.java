package com.nuramin.calculator.emi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.nuramin.sunsetcoralcalculator.R;

/**
 * Simple pie chart showing Principal vs Interest portions.
 * Call setAmounts(principal, interest) then invalidate to redraw.
 */
public class EmiPieChartView extends View {

    private double principalAmount;
    private double interestAmount;
    private int principalColor;
    private int interestColor;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public EmiPieChartView(Context context) {
        super(context);
        init(context);
    }

    public EmiPieChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmiPieChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        principalColor = ContextCompat.getColor(context, R.color.emi_pie_principal);
        interestColor = ContextCompat.getColor(context, R.color.emi_pie_interest);
    }

    public void setPrincipalAmount(double principalAmount) {
        this.principalAmount = principalAmount;
    }

    public void setInterestAmount(double interestAmount) {
        this.interestAmount = interestAmount;
    }

    /**
     * Update pie with principal and interest (total interest payable).
     * Call invalidate() after setting values.
     */
    public void setAmounts(double principal, double interest) {
        this.principalAmount = principal;
        this.interestAmount = interest;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float size = Math.min(w, h);
        float pad = size * 0.05f;
        rect.set(pad, pad, size - pad, size - pad);

        double total = principalAmount + interestAmount;
        if (total <= 0) {
            paint.setColor(principalColor);
            canvas.drawOval(rect, paint);
            return;
        }

        float principalSweep = (float) (360.0 * principalAmount / total);
        float interestSweep = (float) (360.0 * interestAmount / total);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(principalColor);
        canvas.drawArc(rect, -90f, principalSweep, true, paint); // start from top
        paint.setColor(interestColor);
        canvas.drawArc(rect, -90f + principalSweep, interestSweep, true, paint);
    }
}
