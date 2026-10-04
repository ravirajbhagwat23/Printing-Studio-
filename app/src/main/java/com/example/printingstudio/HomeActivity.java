package com.example.printingstudio;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.graphics.pdf.PdfRenderer;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class HomeActivity extends AppCompatActivity {

    TextView tvFileName, tvCopies, tvPrice;
    Button btnBw, btnColor, btnNormal, btnUrgent, btnConfirm;

    private final ActivityResultLauncher<String[]> picker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) handleFile(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        OrderStore store = OrderStore.get();

        tvFileName = findViewById(R.id.tvFileName);
        tvCopies = findViewById(R.id.tvCopies);
        tvPrice = findViewById(R.id.tvPrice);
        btnBw = findViewById(R.id.btnBw);
        btnColor = findViewById(R.id.btnColor);
        btnNormal = findViewById(R.id.btnNormal);
        btnUrgent = findViewById(R.id.btnUrgent);
        btnConfirm = findViewById(R.id.btnConfirm);

        findViewById(R.id.btnHistory).setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));
        findViewById(R.id.btnProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        findViewById(R.id.uploadBox).setOnClickListener(v -> chooseFile());
        findViewById(R.id.btnChooseFile).setOnClickListener(v -> chooseFile());

        btnBw.setOnClickListener(v -> setPrintType(Order.BW));
        btnColor.setOnClickListener(v -> setPrintType(Order.COLOR));
        btnNormal.setOnClickListener(v -> setSpeed(Order.NORMAL));
        btnUrgent.setOnClickListener(v -> setSpeed(Order.URGENT));

        findViewById(R.id.btnMinus).setOnClickListener(v -> {
            if (store.copies > 1) store.copies--;
            refresh();
        });
        findViewById(R.id.btnPlus).setOnClickListener(v -> {
            store.copies++;
            refresh();
        });

        btnConfirm.setOnClickListener(v -> {
            if (store.fileName == null) return;
            store.submitOrder();
            startActivity(new Intent(this, OrderConfirmedActivity.class));
        });

        refresh();
    }

    private void chooseFile() {
        picker.launch(new String[]{"application/pdf"});
    }

    private void handleFile(Uri uri) {
        String name = "document.pdf";
        Cursor c = null;
        try {
            c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0) name = c.getString(i);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }

        ParcelFileDescriptor pfd = null;
        PdfRenderer renderer = null;
        try {
            pfd = getContentResolver().openFileDescriptor(uri, "r");
            renderer = new PdfRenderer(pfd);
            OrderStore.get().fileName = name;
            OrderStore.get().pageCount = renderer.getPageCount();
            refresh();
        } catch (Exception e) {
            Toast.makeText(this, "Could not read this PDF", Toast.LENGTH_LONG).show();
        } finally {
            if (renderer != null) renderer.close();
            if (pfd != null) {
                try { pfd.close(); } catch (IOException ignored) {}
            }
        }
    }

    private void setPrintType(String type) {
        OrderStore.get().printType = type;
        refresh();
    }

    private void setSpeed(String speed) {
        OrderStore.get().speed = speed;
        refresh();
    }

    private void refresh() {
        OrderStore store = OrderStore.get();
        tvFileName.setText(store.fileName == null ? "Tap to upload file" : store.fileName + " (" + store.pageCount + " pages)");
        tvCopies.setText(String.valueOf(store.copies));
        tvPrice.setText("Estimated price: ₹" + (int) store.estimatedPrice());

        btnBw.setBackgroundResource(store.printType.equals(Order.BW) ? R.drawable.chip_selected : R.drawable.chip_unselected);
        btnColor.setBackgroundResource(store.printType.equals(Order.COLOR) ? R.drawable.chip_selected : R.drawable.chip_unselected);
        btnNormal.setBackgroundResource(store.speed.equals(Order.NORMAL) ? R.drawable.chip_selected : R.drawable.chip_unselected);
        btnUrgent.setBackgroundResource(store.speed.equals(Order.URGENT) ? R.drawable.chip_selected : R.drawable.chip_unselected);

        btnConfirm.setEnabled(store.fileName != null);
    }
}