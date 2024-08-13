package com.example.audioboog.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.audioboog.R;

public class OptionsPicker extends Dialog {
    private Button setButton, cancelButton;
    private NumberPicker numberPicker;
    private TextView pickedVariableText;

    public OptionsPicker(@NonNull Context context, String[] displayedValues, String pickedVariable) {
        super(context);
        initializeGui(displayedValues, pickedVariable);
    }

    public OptionsPicker(@NonNull Context context, int themeResId, String[] displayedValues, String pickedVariable) {
        super(context, themeResId);
        initializeGui(displayedValues, pickedVariable);
    }

    protected OptionsPicker(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener, String[] displayedValues, String pickedVariable) {
        super(context, cancelable, cancelListener);
        initializeGui(displayedValues, pickedVariable);
    }

    private void initializeGui(String[] displayedValues, String pickedVariable) {
        this.setContentView(R.layout.number_picker);
        setButton = this.findViewById(R.id.setButton);
        cancelButton = this.findViewById(R.id.cancelButton);
        numberPicker = this.findViewById(R.id.numberPicker1);
        pickedVariableText = this.findViewById(R.id.pickedVariableText);

        numberPicker.setMaxValue(displayedValues.length - 1);
        numberPicker.setMinValue(0);
        numberPicker.setDisplayedValues(displayedValues);
        numberPicker.setWrapSelectorWheel(false);

        pickedVariableText.setText(pickedVariable);
        cancelButton.setOnClickListener(v -> dismiss());
    }

    public void setValueSetListener(View.OnClickListener l) {
        setButton.setOnClickListener(l);
    }

    public void setCancelListener(View.OnClickListener l) {
        cancelButton.setOnClickListener(l);
    }

    public int getPickedValue() {
        return numberPicker.getValue();
    }

    public void setDefaultValue(int value) {
        numberPicker.setValue(value);
    }
}
