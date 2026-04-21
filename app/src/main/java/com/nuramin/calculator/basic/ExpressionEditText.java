package com.nuramin.calculator.basic;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;

/**
 * Expression line for the basic/scientific calculator. Used with {@code match_parent} width and
 * {@code scrollHorizontally} so long formulas scroll inside the field (Android’s normal pattern).
 */
public class ExpressionEditText extends AppCompatEditText {

    public ExpressionEditText(Context context) {
        super(context);
    }

    public ExpressionEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpressionEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
