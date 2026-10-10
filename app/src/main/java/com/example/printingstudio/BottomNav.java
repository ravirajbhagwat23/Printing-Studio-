package com.example.printingstudio;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.ImageView;
import android.widget.TextView;

public class BottomNav {

    /** Pads a view so it is not hidden behind the phone's system navigation bar. */
    public static void padForNavBar(View v) {
        final int base = v.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(v, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(), view.getPaddingRight(), base + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(v);
    }

    public static void setup(Activity a, int selectedId) {
        View nav = a.findViewById(R.id.bottomNav);
        if (nav != null) padForNavBar(nav);
        bind(a, R.id.nav_order, R.id.nav_order_icon, R.id.nav_order_label, selectedId, HomeActivity.class);
        bind(a, R.id.nav_history, R.id.nav_history_icon, R.id.nav_history_label, selectedId, OrderHistoryActivity.class);
        bind(a, R.id.nav_profile, R.id.nav_profile_icon, R.id.nav_profile_label, selectedId, ProfileActivity.class);
    }

    private static void bind(Activity a, int itemId, int iconId, int labelId, int selectedId, Class<?> target) {
        View item = a.findViewById(itemId);
        ImageView icon = a.findViewById(iconId);
        TextView label = a.findViewById(labelId);
        if (item == null) return;
        boolean active = itemId == selectedId;
        int color = Color.parseColor(active ? "#2F6FED" : "#7A8499");
        icon.setColorFilter(color);
        label.setTextColor(color);
        item.setBackgroundResource(active ? R.drawable.ps_nav_active : 0);
        item.setOnClickListener(v -> {
            if (active) return;
            Intent i = new Intent(a, target);
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            a.startActivity(i);
            a.overridePendingTransition(0, 0);
        });
    }
}
