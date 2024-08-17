package com.example.audioboog.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.audioboog.R;
import com.example.audioboog.source.Audiobook;

public class AudiobookOptions extends Dialog {
    Audiobook audiobook;
    private Button deleteButton, resetButton, cancelButton;
    private TextView audiobookNameText;

    public AudiobookOptions(@NonNull Context context, Audiobook audiobook) {
        super(context);
        this.audiobook = audiobook;
        initializeGui();
    }

    private void initializeGui() {
        this.setContentView(R.layout.audiobook_options);
        deleteButton = this.findViewById(R.id.deleteButton);
        resetButton = this.findViewById(R.id.resetButton);
        cancelButton = this.findViewById(R.id.cancelButton);
        audiobookNameText = this.findViewById(R.id.audiobookNameText);

        audiobookNameText.setText(audiobook.getName());
        audiobookNameText.setSelected(true);
        cancelButton.setOnClickListener(v -> dismiss());
    }

    public void setDeleteButtonListener(View.OnClickListener l) {
        deleteButton.setOnClickListener(l);
    }

    public void setResetButtonListener(View.OnClickListener l) {
        resetButton.setOnClickListener(l);
    }
}
