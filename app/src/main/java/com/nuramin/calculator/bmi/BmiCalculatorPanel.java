package com.nuramin.calculator.bmi;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.nuramin.sunsetcoralcalculator.R;
import com.nuramin.calculator.util.ConverterUiHelper;

/**
 * BMI Calculator panel. Metric formula: BMI = weightKg / (heightM * heightM).
 * Reuses common TopBar; content only below it.
 */
public final class BmiCalculatorPanel {

    private static final int HEIGHT_MIN_CM = 100;
    private static final int HEIGHT_MAX_CM = 250;
    private static final int WEIGHT_MIN_KG = 20;
    private static final int WEIGHT_MAX_KG = 250;
    private static final int AGE_MIN = 5;
    private static final int AGE_MAX = 120;

    private static final double BMI_UNDER = 18.5;
    private static final double BMI_NORMAL_MAX = 24.9;
    private static final double BMI_OVER_MAX = 29.9;

    public static void setup(View panel, @Nullable DrawerLayout drawerLayout, @Nullable View.OnClickListener onOverflowClick) {
        if (panel == null) return;

        TextView heightValue = panel.findViewById(R.id.bmi_height_value);
        TextView weightValue = panel.findViewById(R.id.bmi_weight_value);
        TextView ageValue = panel.findViewById(R.id.bmi_age_value);
        ImageButton heightPlus = panel.findViewById(R.id.bmi_height_plus);
        ImageButton heightMinus = panel.findViewById(R.id.bmi_height_minus);
        ImageButton weightPlus = panel.findViewById(R.id.bmi_weight_plus);
        ImageButton weightMinus = panel.findViewById(R.id.bmi_weight_minus);
        ImageButton agePlus = panel.findViewById(R.id.bmi_age_plus);
        ImageButton ageMinus = panel.findViewById(R.id.bmi_age_minus);
        Button calculateBtn = panel.findViewById(R.id.bmi_calculate_btn);
        MaterialCardView resultCard = panel.findViewById(R.id.bmi_result_card);
        TextView bmiValueTv = panel.findViewById(R.id.bmi_value);
        TextView categoryLabel = panel.findViewById(R.id.bmi_category_label);
        View segUnder = panel.findViewById(R.id.bmi_seg_under);
        View segNormal = panel.findViewById(R.id.bmi_seg_normal);
        View segOver = panel.findViewById(R.id.bmi_seg_over);
        View segObese = panel.findViewById(R.id.bmi_seg_obese);

        LinearLayout genderMale = panel.findViewById(R.id.bmi_gender_male);
        LinearLayout genderFemale = panel.findViewById(R.id.bmi_gender_female);

        int[] height = { 170 };
        int[] weight = { 65 };
        int[] age = { 25 };
        boolean[] isMale = { true };

        Runnable updateHeight = () -> {
            if (heightValue != null) heightValue.setText(String.valueOf(height[0]));
        };
        Runnable updateWeight = () -> {
            if (weightValue != null) weightValue.setText(String.valueOf(weight[0]));
        };
        Runnable updateAge = () -> {
            if (ageValue != null) ageValue.setText(String.valueOf(age[0]));
        };

        setClickListener(heightPlus, () -> {
            if (height[0] < HEIGHT_MAX_CM) {
                height[0]++;
                updateHeight.run();
            }
        });
        setClickListener(heightMinus, () -> {
            if (height[0] > HEIGHT_MIN_CM) {
                height[0]--;
                updateHeight.run();
            }
        });
        setClickListener(weightPlus, () -> {
            if (weight[0] < WEIGHT_MAX_KG) {
                weight[0]++;
                updateWeight.run();
            }
        });
        setClickListener(weightMinus, () -> {
            if (weight[0] > WEIGHT_MIN_KG) {
                weight[0]--;
                updateWeight.run();
            }
        });
        setClickListener(agePlus, () -> {
            if (age[0] < AGE_MAX) {
                age[0]++;
                updateAge.run();
            }
        });
        setClickListener(ageMinus, () -> {
            if (age[0] > AGE_MIN) {
                age[0]--;
                updateAge.run();
            }
        });

        if (genderMale != null) {
            genderMale.setOnClickListener(v -> {
                isMale[0] = true;
                genderMale.setBackgroundResource(R.drawable.bmi_toggle_selected);
                if (genderFemale != null) genderFemale.setBackgroundResource(R.drawable.bmi_toggle_unselected);
                if (genderFemale != null) {
                    TextView ft = (TextView) ((LinearLayout) genderFemale).getChildAt(0);
                    if (ft != null) ft.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.bmi_text_secondary));
                }
                TextView mt = (TextView) ((LinearLayout) genderMale).getChildAt(0);
                if (mt != null) mt.setTextColor(ContextCompat.getColor(panel.getContext(), android.R.color.white));
            });
        }
        if (genderFemale != null) {
            genderFemale.setOnClickListener(v -> {
                isMale[0] = false;
                genderFemale.setBackgroundResource(R.drawable.bmi_toggle_selected);
                if (genderMale != null) genderMale.setBackgroundResource(R.drawable.bmi_toggle_unselected);
                if (genderMale != null) {
                    TextView mt = (TextView) ((LinearLayout) genderMale).getChildAt(0);
                    if (mt != null) mt.setTextColor(ContextCompat.getColor(panel.getContext(), R.color.bmi_text_secondary));
                }
                TextView ft = (TextView) ((LinearLayout) genderFemale).getChildAt(0);
                if (ft != null) ft.setTextColor(ContextCompat.getColor(panel.getContext(), android.R.color.white));
            });
        }

        if (calculateBtn != null && resultCard != null && bmiValueTv != null && categoryLabel != null) {
            calculateBtn.setOnClickListener(v -> {
                int h = height[0];
                int w = weight[0];
                int a = age[0];
                if (h < HEIGHT_MIN_CM || h > HEIGHT_MAX_CM || w < WEIGHT_MIN_KG || w > WEIGHT_MAX_KG || a < AGE_MIN || a > AGE_MAX) {
                    Snackbar.make(panel, R.string.bmi_error_invalid, Snackbar.LENGTH_SHORT).show();
                    return;
                }
                double heightM = h / 100.0;
                double bmi = w / (heightM * heightM);
                bmi = Math.round(bmi * 10) / 10.0;

                int categoryRes;
                int categoryColorRes;
                if (bmi < BMI_UNDER) {
                    categoryRes = R.string.bmi_underweight_label;
                    categoryColorRes = R.color.bmi_underweight;
                } else if (bmi <= BMI_NORMAL_MAX) {
                    categoryRes = R.string.bmi_normal_label;
                    categoryColorRes = R.color.bmi_normal;
                } else if (bmi <= BMI_OVER_MAX) {
                    categoryRes = R.string.bmi_overweight_label;
                    categoryColorRes = R.color.bmi_overweight;
                } else {
                    categoryRes = R.string.bmi_obese_label;
                    categoryColorRes = R.color.bmi_obese;
                }

                int color = ContextCompat.getColor(panel.getContext(), categoryColorRes);
                bmiValueTv.setText(String.valueOf(bmi));
                bmiValueTv.setTextColor(color);
                categoryLabel.setText(categoryRes);
                categoryLabel.setTextColor(color);

                highlightSegment(panel, segUnder, segNormal, segOver, segObese, categoryColorRes);

                resultCard.setVisibility(View.VISIBLE);

                ObjectAnimator scaleX = ObjectAnimator.ofFloat(bmiValueTv, View.SCALE_X, 0.9f, 1f);
                ObjectAnimator scaleY = ObjectAnimator.ofFloat(bmiValueTv, View.SCALE_Y, 0.9f, 1f);
                scaleX.setDuration(200);
                scaleY.setDuration(200);
                AnimatorSet set = new AnimatorSet();
                set.playTogether(scaleX, scaleY);
                set.start();

                ConverterUiHelper.hideSoftKeyboard(panel);
                ConverterUiHelper.scrollToShowResult(panel, resultCard);
            });
        }

        updateHeight.run();
        updateWeight.run();
        updateAge.run();

        if (segUnder != null && segNormal != null && segOver != null && segObese != null) {
            highlightSegment(panel, segUnder, segNormal, segOver, segObese, R.color.bmi_normal);
        }
    }

    private static void setClickListener(View v, Runnable action) {
        if (v != null) v.setOnClickListener(x -> action.run());
    }

    private static void highlightSegment(View panel, View segUnder, View segNormal, View segOver, View segObese, int categoryColorRes) {
        float dim = 0.4f;
        float full = 1f;
        segUnder.setAlpha(categoryColorRes == R.color.bmi_underweight ? full : dim);
        segNormal.setAlpha(categoryColorRes == R.color.bmi_normal ? full : dim);
        segOver.setAlpha(categoryColorRes == R.color.bmi_overweight ? full : dim);
        segObese.setAlpha(categoryColorRes == R.color.bmi_obese ? full : dim);
    }
}
