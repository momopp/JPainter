package com.jp.jpainter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jp.jcanvas.JCanvas;
import com.jp.jcanvas.colorpicker.ColorPicker;
import com.jp.jpainter.utils.SDUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PainterActivity extends AppCompatActivity {

    private int mPaintWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);

        TextView tvUndo = findViewById(R.id.tv_undo);
        TextView tvRedo = findViewById(R.id.tv_redo);
        TextView tvScale = findViewById(R.id.tv_scale);
        TextView tvColor = findViewById(R.id.tv_color);
        TextView tvWidth = findViewById(R.id.tv_width);
        TextView tvPaint = findViewById(R.id.tv_paint);
        TextView tvEraser = findViewById(R.id.tv_eraser);
        TextView tvSave = findViewById(R.id.tv_save);
        TextView tvClear = findViewById(R.id.tv_clear);

        CardView cPicker = findViewById(R.id.cv_color_picker);
        ColorPicker cp = findViewById(R.id.cp_picker);
        JCanvas painter = findViewById(R.id.sp_painter);

        tvScale.setText("1.0x");
        cp.setColor(Color.RED);
        painter.setPaintColor(cp.getColor());

        tvPaint.setBackgroundColor(Color.CYAN);

        tvUndo.setOnClickListener(v -> painter.undo());
        tvRedo.setOnClickListener(v -> painter.redo());

        tvColor.setOnClickListener(v -> {
            if (View.VISIBLE == cPicker.getVisibility()) {
                painter.setPaintColor(cp.getColor());
                cPicker.setVisibility(View.GONE);
            } else {
                cPicker.setVisibility(View.VISIBLE);
            }
        });

        tvWidth.setOnClickListener(v -> {
            SeekBar seekBar = new SeekBar(getApplicationContext());
            seekBar.setMax(99);
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mPaintWidth = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            seekBar.setProgress((int) painter.getPaintWidth());

            new AlertDialog.Builder(this)
                    .setTitle("设置笔尖大小")
                    .setView(seekBar)
                    .setPositiveButton("ok", (dialog, which) -> {
                        painter.setPaintWidth(mPaintWidth);
                    })
                    .setNegativeButton("no", null)
                    .create()
                    .show();
        });

        tvPaint.setOnClickListener(v -> {
            painter.usePaint();
            tvPaint.setBackgroundColor(Color.CYAN);
            tvEraser.setBackgroundColor(Color.parseColor("#FFAAAAAA"));
        });

        tvEraser.setOnClickListener(v -> {
            painter.useEraser();
            tvEraser.setBackgroundColor(Color.CYAN);
            tvPaint.setBackgroundColor(Color.parseColor("#FFAAAAAA"));
        });

        tvSave.setOnClickListener(v -> {
            if (!SDUtil.initBitmapDir()) {
                new AlertDialog.Builder(this)
                        .setTitle("保存失败0")
                        .setPositiveButton("ok", null)
                        .show();
                return;
            }

            DateFormat formatter
                    = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
            String fileName = formatter.format(new Date()) + ".png";

            if (SDUtil.saveBitmap(fileName, painter.getBitmap())) {
                new AlertDialog.Builder(this)
                        .setTitle("保存成功")
                        .setPositiveButton("new", (dialog, which) -> {
                            painter.resetCanvas();
                        })
                        .setNegativeButton("ok", null)
                        .show();

            } else {
                new AlertDialog.Builder(this)
                        .setTitle("保存失败1")
                        .setPositiveButton("ok", null)
                        .show();
            }
        });

        tvClear.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("清空？")
                    .setPositiveButton("ok", (dialog, which) -> {
                        painter.resetCanvas();
                    })
                    .setNegativeButton("no", null)
                    .show();
        });

        painter.setOnScaleChangeListener(new JCanvas.OnScaleChangeListener() {
            @Override
            public void onScaleChangeStart(float startScale) {
                tvScale.setText(
                        String.valueOf((float) (Math.round(startScale * 10)) / 10).concat("x"));
            }

            @Override
            public void onScaleChange(float currentScale) {
                tvScale.setText(
                        String.valueOf((float) (Math.round(currentScale * 10)) / 10).concat("x"));
            }

            @Override
            public void onScaleChangeEnd(float endScale) {
                tvScale.setText(
                        String.valueOf((float) (Math.round(endScale * 10)) / 10).concat("x"));
            }
        });

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (PackageManager.PERMISSION_DENIED == permission) {
            String[] req = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, req, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
