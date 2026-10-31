package com.astraforge.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Functional, dependency-free launcher reconstruction for the unavailable original resources. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(10, 15, 30);
    private static final int PANEL = Color.rgb(25, 33, 55);
    private static final int TEXT = Color.rgb(245, 247, 255);
    private static final int MUTED = Color.rgb(172, 184, 215);
    private final Handler handler = new Handler();
    private final List<ApplicationInfo> allApps = new ArrayList<>();
    private SharedPreferences prefs;
    private GridLayout grid;
    private EditText search;
    private TextView clock;
    private Runnable clockTick;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("astra_launcher", MODE_PRIVATE);
        loadApps();
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (prefs != null) { loadApps(); refreshGrid(); }
    }

    // Un lanceur ne se ferme pas avec « retour » : il ramène à l'écran d'accueil.
    @Override @Deprecated public void onBackPressed() { moveTaskToBack(true); }

    @Override protected void onDestroy() {
        if (clockTick != null) handler.removeCallbacks(clockTick);
        super.onDestroy();
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        allApps.clear();
        Set<String> seen = new HashSet<>();
        for (android.content.pm.ResolveInfo info : pm.queryIntentActivities(query, 0)) {
            ApplicationInfo app = info.activityInfo.applicationInfo;
            if (seen.add(app.packageName)) allApps.add(app);
        }
        Collections.sort(allApps, (a, b) -> label(a).compareToIgnoreCase(label(b)));
    }

    private String label(ApplicationInfo app) {
        return getPackageManager().getApplicationLabel(app).toString();
    }

    private void render() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 18, 20, 12);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        clock = text("", 14, MUTED);
        header.addView(clock, new LinearLayout.LayoutParams(0, 48, 1));
        Button settings = button(getString(R.string.settings_button), 48, 48);
        settings.setOnClickListener(v -> showSettings());
        header.addView(settings);
        root.addView(header, new LinearLayout.LayoutParams(-1, 52));

        TextView title = text(getString(R.string.launcher_title), 30, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, 48));
        TextView subtitle = text(getString(R.string.launcher_subtitle), 14, MUTED);
        root.addView(subtitle, new LinearLayout.LayoutParams(-1, 34));

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint(getString(R.string.search_hint));
        search.setHintTextColor(MUTED);
        search.setTextColor(TEXT);
        search.setTextSize(16);
        search.setPadding(18, 0, 18, 0);
        search.setBackgroundColor(PANEL);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int before, int count) { refreshGrid(); }
            public void afterTextChanged(Editable e) { }
        });
        root.addView(search, new LinearLayout.LayoutParams(-1, 54));

        ScrollView scroll = new ScrollView(this);
        grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setUseDefaultMargins(false);
        scroll.addView(grid, new ViewGroup.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        updateClock();
        clockTick = new Runnable() { public void run() { updateClock(); handler.postDelayed(this, 30000); } };
        handler.postDelayed(clockTick, 30000);
        refreshGrid();
    }

    private void updateClock() {
        if (clock != null) clock.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()).format(new Date()));
    }

    private void refreshGrid() {
        if (grid == null) return;
        grid.removeAllViews();
        String filter = search == null ? "" : search.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<ApplicationInfo> filtered = new ArrayList<>();
        for (ApplicationInfo app : allApps) if (filter.isEmpty() || label(app).toLowerCase(Locale.getDefault()).contains(filter)) filtered.add(app);
        Set<String> favorites = prefs.getStringSet("favorites", new HashSet<>());
        Collections.sort(filtered, (a, b) -> {
            boolean af = favorites.contains(a.packageName), bf = favorites.contains(b.packageName);
            if (af != bf) return af ? -1 : 1;
            return label(a).compareToIgnoreCase(label(b));
        });
        for (ApplicationInfo app : filtered) addAppCard(app, favorites.contains(app.packageName));
        if (filtered.isEmpty()) {
            TextView empty = text(getString(R.string.empty_state), 16, MUTED);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.columnSpec = GridLayout.spec(0, 4); p.width = -1; p.height = 100;
            grid.addView(empty, p);
        }
    }

    private void addAppCard(ApplicationInfo app, boolean favorite) {
        PackageManager pm = getPackageManager();
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL); card.setGravity(Gravity.CENTER); card.setPadding(4, 8, 4, 8);
        ImageView icon = new ImageView(this); icon.setImageDrawable(pm.getApplicationIcon(app));
        card.addView(icon, new LinearLayout.LayoutParams(58, 58));
        TextView name = text(favorite ? getString(R.string.favorite_marker, label(app)) : label(app), 12, TEXT); name.setGravity(Gravity.CENTER); name.setMaxLines(2);
        card.addView(name, new LinearLayout.LayoutParams(-1, 42));
        card.setOnClickListener(v -> { Intent launch = pm.getLaunchIntentForPackage(app.packageName); if (launch != null) startActivity(launch); });
        card.setOnLongClickListener(v -> { toggleFavorite(app.packageName); return true; });
        GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = 0; p.height = 118; p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); p.setMargins(2, 2, 2, 2);
        grid.addView(card, p);
    }

    private void toggleFavorite(String packageName) {
        Set<String> next = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        if (!next.add(packageName)) next.remove(packageName);
        prefs.edit().putStringSet("favorites", next).apply(); refreshGrid();
    }

    private void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(40, 24, 40, 24);
        TextView help = text(getString(R.string.settings_help), 16, TEXT);
        help.setPadding(0, 10, 0, 20);
        box.addView(help, new LinearLayout.LayoutParams(-1, 100));
        Button clear = button(getString(R.string.clear_favorites), -1, 54);
        clear.setOnClickListener(v -> { prefs.edit().remove("favorites").apply(); refreshGrid(); });
        box.addView(clear);
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.settings_title)
                .setView(box)
                .setPositiveButton(R.string.close, null)
                .show();
    }

    private TextView text(String value, int size, int color) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private Button button(String value, int width, int height) { Button b = new Button(this); b.setText(value); b.setTextColor(TEXT); b.setAllCaps(false); b.setMinWidth(0); b.setMinHeight(0); b.setPadding(8, 0, 8, 0); b.setLayoutParams(new LinearLayout.LayoutParams(width, height)); return b; }
}
