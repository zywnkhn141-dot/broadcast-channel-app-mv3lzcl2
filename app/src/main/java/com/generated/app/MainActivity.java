package com.generated.app;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "channel_prefs";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_ADMIN = "is_admin";
    private static final String DEFAULT_ADMIN_PIN = "1234";

    private static final int REQUEST_CHOOSE_ACCOUNT = 1001;

    private SharedPreferences prefs;
    private DatabaseHelper dbHelper;

    private String currentUserEmail = null;
    private String currentUserName = null;
    private boolean isAdminMode = false;
    private String selectedCategory = "הכל";
    private String currentSearchQuery = "";

    // UI references
    private LinearLayout mainContentContainer;
    private LinearLayout postsFeedContainer;
    private TextView emptyFeedView;
    private TextView userStatusText;
    private Button btnGoogleAuth;
    private Button btnAdminAction;
    private Button btnNewBroadcast;
    private EditText searchEditText;

    // Filter Chips
    private TextView chipAll;
    private TextView chipUpdates;
    private TextView chipAnnouncements;
    private TextView chipImportant;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        dbHelper = new DatabaseHelper(this);

        // Load saved session
        currentUserEmail = prefs.getString(KEY_USER_EMAIL, null);
        currentUserName = prefs.getString(KEY_USER_NAME, null);
        isAdminMode = prefs.getBoolean(KEY_IS_ADMIN, false);

        // Build UI programmatically using standard Android Framework
        View rootView = buildUserInterface();
        setContentView(rootView);

        refreshAuthState();
        reloadPosts();
    }

    private View buildUserInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setBackgroundColor(Color.parseColor("#F1F5F9"));

        // Header View
        root.addView(createHeaderView());

        // Auth & Admin Bar
        root.addView(createAuthBarView());

        // Search & Filter Bar
        root.addView(createSearchAndFilterBar());

        // Feed container inside ScrollView
        ScrollView feedScrollView = new ScrollView(this);
        feedScrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        feedScrollView.setFillViewport(true);

        postsFeedContainer = new LinearLayout(this);
        postsFeedContainer.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        postsFeedContainer.setPadding(pad, pad, pad, dp(40));

        // Empty state view
        emptyFeedView = new TextView(this);
        emptyFeedView.setText(getString(R.string.empty_feed_subtitle));
        emptyFeedView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        emptyFeedView.setTextColor(Color.parseColor("#64748B"));
        emptyFeedView.setGravity(Gravity.CENTER);
        int padEmpty = dp(32);
        emptyFeedView.setPadding(padEmpty, padEmpty, padEmpty, padEmpty);
        emptyFeedView.setVisibility(View.GONE);
        postsFeedContainer.addView(emptyFeedView);

        feedScrollView.addView(postsFeedContainer);
        root.addView(feedScrollView);

        return root;
    }

    private View createHeaderView() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundResource(R.drawable.bg_header_gradient);
        int padH = dp(16);
        int padV = dp(18);
        header.setPadding(padH, padV, padH, padV);

        // Top row: App Name & Broadcast icon
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_launcher);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.setMargins(dp(12), 0, 0, 0);
        iconView.setLayoutParams(iconParams);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView appTitle = new TextView(this);
        appTitle.setText(R.string.app_name);
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        appTitle.setTypeface(Typeface.DEFAULT_BOLD);
        appTitle.setTextColor(Color.WHITE);

        TextView subTitle = new TextView(this);
        subTitle.setText(R.string.channel_subtitle);
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subTitle.setTextColor(Color.parseColor("#BBDEFB"));

        titles.addView(appTitle);
        titles.addView(subTitle);

        topRow.addView(titles);
        topRow.addView(iconView);
        header.addView(topRow);

        return header;
    }

    private View createAuthBarView() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.VERTICAL);
        bar.setBackgroundColor(Color.WHITE);
        int pad = dp(12);
        bar.setPadding(pad, pad, pad, pad);

        // Top sub-row: User status info and actions
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        userStatusText = new TextView(this);
        userStatusText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));
        userStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        userStatusText.setTextColor(Color.parseColor("#334155"));
        userStatusText.setText(R.string.guest_user);

        btnGoogleAuth = new Button(this);
        btnGoogleAuth.setText(R.string.sign_in_google);
        btnGoogleAuth.setBackgroundResource(R.drawable.bg_google_btn);
        btnGoogleAuth.setTextColor(Color.parseColor("#1E293B"));
        btnGoogleAuth.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnGoogleAuth.setTypeface(Typeface.DEFAULT_BOLD);
        btnGoogleAuth.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnGoogleAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUserEmail != null) {
                    showAccountDetailsDialog();
                } else {
                    promptGoogleSignIn();
                }
            }
        });

        btnAdminAction = new Button(this);
        btnAdminAction.setText(R.string.admin_login);
        btnAdminAction.setBackgroundResource(R.drawable.bg_badge_admin);
        btnAdminAction.setTextColor(Color.parseColor("#E65100"));
        btnAdminAction.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btnAdminAction.setTypeface(Typeface.DEFAULT_BOLD);
        btnAdminAction.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams adminBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        adminBtnParams.setMargins(dp(6), 0, 0, 0);
        btnAdminAction.setLayoutParams(adminBtnParams);
        btnAdminAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAdminMode) {
                    showAdminLogoutDialog();
                } else {
                    showAdminLoginDialog();
                }
            }
        });

        row.addView(userStatusText);
        row.addView(btnGoogleAuth);
        row.addView(btnAdminAction);
        bar.addView(row);

        // New Broadcast button (Visible only when admin is logged in)
        btnNewBroadcast = new Button(this);
        btnNewBroadcast.setText("✍️  " + getString(R.string.new_broadcast));
        btnNewBroadcast.setBackgroundResource(R.drawable.bg_btn_primary);
        btnNewBroadcast.setTextColor(Color.WHITE);
        btnNewBroadcast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnNewBroadcast.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams postBtnParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        postBtnParams.topMargin = dp(8);
        btnNewBroadcast.setLayoutParams(postBtnParams);
        btnNewBroadcast.setVisibility(View.GONE);
        btnNewBroadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateBroadcastDialog();
            }
        });
        bar.addView(btnNewBroadcast);

        return bar;
    }

    private View createSearchAndFilterBar() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(12);
        int padV = dp(6);
        container.setPadding(padH, padV, padH, padV);

        // Search edit text
        searchEditText = new EditText(this);
        searchEditText.setHint("חיפוש בהודעות הערוץ...");
        searchEditText.setBackgroundResource(R.drawable.bg_input);
        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        searchEditText.setPadding(dp(12), dp(8), dp(12), dp(8));
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString();
                reloadPosts();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        container.addView(searchEditText);

        // Filter chips horizontal scroll
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollParams.topMargin = dp(6);
        chipScroll.setLayoutParams(scrollParams);

        LinearLayout chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);

        chipAll = makeFilterChip(getString(R.string.filter_all), true);
        chipUpdates = makeFilterChip(getString(R.string.filter_updates), false);
        chipAnnouncements = makeFilterChip(getString(R.string.filter_announcements), false);
        chipImportant = makeFilterChip(getString(R.string.filter_important), false);

        chipsRow.addView(chipAll);
        chipsRow.addView(chipUpdates);
        chipsRow.addView(chipAnnouncements);
        chipsRow.addView(chipImportant);

        chipScroll.addView(chipsRow);
        container.addView(chipScroll);

        return container;
    }

    private TextView makeFilterChip(final String title, boolean selected) {
        final TextView chip = new TextView(this);
        chip.setText(title);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        chip.setPadding(dp(14), dp(6), dp(14), dp(6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(params);

        applyChipStyle(chip, selected);

        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCategory = title;
                applyChipStyle(chipAll, "הכל".equals(selectedCategory));
                applyChipStyle(chipUpdates, "עדכונים".equals(selectedCategory));
                applyChipStyle(chipAnnouncements, "הודעות".equals(selectedCategory));
                applyChipStyle(chipImportant, "חשוב ⚡".equals(selectedCategory));
                reloadPosts();
            }
        });

        return chip;
    }

    private void applyChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(Color.WHITE);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(Color.parseColor("#475569"));
            chip.setTypeface(Typeface.DEFAULT);
        }
    }

    private void refreshAuthState() {
        if (currentUserEmail != null) {
            String displayName = (currentUserName != null && !currentUserName.isEmpty())
                    ? currentUserName
                    : currentUserEmail.split("@")[0];

            if (isAdminMode) {
                userStatusText.setText("👑 " + displayName + " (מנהל)");
                btnAdminAction.setText("יציאה ממנהל");
                btnAdminAction.setBackgroundColor(Color.parseColor("#FEE2E2"));
                btnAdminAction.setTextColor(Color.parseColor("#DC2626"));
                btnNewBroadcast.setVisibility(View.VISIBLE);
            } else {
                userStatusText.setText("👤 " + displayName);
                btnAdminAction.setText(R.string.admin_login);
                btnAdminAction.setBackgroundResource(R.drawable.bg_badge_admin);
                btnAdminAction.setTextColor(Color.parseColor("#E65100"));
                btnNewBroadcast.setVisibility(View.GONE);
            }
            btnGoogleAuth.setText("חשבון מחובר");
        } else {
            userStatusText.setText(R.string.guest_user);
            btnGoogleAuth.setText(R.string.sign_in_google);
            btnAdminAction.setText(R.string.admin_login);
            btnAdminAction.setBackgroundResource(R.drawable.bg_badge_admin);
            btnAdminAction.setTextColor(Color.parseColor("#E65100"));
            btnNewBroadcast.setVisibility(View.GONE);
        }
    }

    // Google Sign-In handling
    private void promptGoogleSignIn() {
        // Try reading registered Google accounts on device via AccountManager
        AccountManager am = AccountManager.get(this);
        Account[] accounts = null;
        try {
            accounts = am.getAccountsByType("com.google");
        } catch (SecurityException ignored) {
            // Permission GET_ACCOUNTS not granted at runtime on older/certain systems
        }

        if (accounts != null && accounts.length > 0) {
            final String[] accountNames = new String[accounts.length + 1];
            for (int i = 0; i < accounts.length; i++) {
                accountNames[i] = accounts[i].name;
            }
            accountNames[accounts.length] = "הזן חשבון Google ידנית...";

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("בחר חשבון Google להתחברות");
            builder.setItems(accountNames, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which < accountNames.length - 1) {
                        saveUserSession(accountNames[which], accountNames[which].split("@")[0]);
                        Toast.makeText(MainActivity.this, "התחברת בהצלחה עם " + accountNames[which], Toast.LENGTH_SHORT).show();
                    } else {
                        showManualGoogleInputDialog();
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel_button, null);
            builder.show();
        } else {
            // Alternatively launch system account picker or direct manual input
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                try {
                    Intent chooseIntent = AccountManager.newChooseAccountIntent(
                            null, null, new String[]{"com.google"}, false, null, null, null, null);
                    startActivityForResult(chooseIntent, REQUEST_CHOOSE_ACCOUNT);
                    return;
                } catch (Exception ignored) {
                }
            }
            showManualGoogleInputDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHOOSE_ACCOUNT && resultCode == RESULT_OK && data != null) {
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (accountName != null && !accountName.trim().isEmpty()) {
                saveUserSession(accountName, accountName.split("@")[0]);
                Toast.makeText(this, "התחברת עם " + accountName, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showManualGoogleInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("התחברות עם חשבון Google");
        builder.setMessage("הזן את כתובת ה-Gmail שלך כדי להתחבר לערוץ:");

        final EditText emailInput = new EditText(this);
        emailInput.setHint("example@gmail.com");
        emailInput.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setBackgroundResource(R.drawable.bg_input);
        emailInput.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        container.setPadding(pad, pad, pad, pad);
        container.addView(emailInput);
        builder.setView(container);

        builder.setPositiveButton("התחבר", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = emailInput.getText().toString().trim();
                if (email.isEmpty() || !email.contains("@")) {
                    Toast.makeText(MainActivity.this, "נא להזין כתובת דוא\"ל תקינה", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveUserSession(email, email.split("@")[0]);
                Toast.makeText(MainActivity.this, "התחברת בהצלחה כ-" + email, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel_button, null);
        builder.show();
    }

    private void saveUserSession(String email, String name) {
        currentUserEmail = email;
        currentUserName = name;
        prefs.edit()
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_NAME, name)
                .apply();
        refreshAuthState();
    }

    private void showAccountDetailsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("פרטי חשבון Google");
        builder.setMessage("מחובר כרגע:\n" + currentUserEmail + "\n\nתפקיד: " + (isAdminMode ? "מנהל הערוץ (הרשאות פרסום)" : "עוקב (קריאה בלבד)"));
        builder.setPositiveButton("סגור", null);
        builder.setNegativeButton("התנתק מחשבון Google", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logoutGoogleUser();
            }
        });
        builder.show();
    }

    private void logoutGoogleUser() {
        currentUserEmail = null;
        currentUserName = null;
        isAdminMode = false;
        prefs.edit().clear().apply();
        refreshAuthState();
        Toast.makeText(this, "התנתקת בהצלחה", Toast.LENGTH_SHORT).show();
    }

    // Admin Verification (Only the channel creator can post)
    private void showAdminLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("כניסת מנהל הערוץ");
        builder.setMessage("רק מנהל הערוץ מורשה להעלות תוכן.\nהזן את קוד המנהל שלך כדי לאמת שליטה:");

        final EditText pinInput = new EditText(this);
        pinInput.setHint(R.string.admin_code_hint);
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pinInput.setBackgroundResource(R.drawable.bg_input);
        pinInput.setPadding(dp(14), dp(10), dp(14), dp(10));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        container.setPadding(pad, pad, pad, pad);
        container.addView(pinInput);
        builder.setView(container);

        builder.setPositiveButton("אישור", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String enteredPin = pinInput.getText().toString().trim();
                if (DEFAULT_ADMIN_PIN.equals(enteredPin)) {
                    isAdminMode = true;
                    prefs.edit().putBoolean(KEY_IS_ADMIN, true).apply();
                    if (currentUserEmail == null) {
                        currentUserEmail = "admin@channel.com";
                        currentUserName = "מנהל הערוץ";
                        prefs.edit()
                                .putString(KEY_USER_EMAIL, currentUserEmail)
                                .putString(KEY_USER_NAME, currentUserName)
                                .apply();
                    }
                    refreshAuthState();
                    Toast.makeText(MainActivity.this, "ברוך הבא, מנהל הערוץ! כעת תוכל לפרסם הודעות.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, "קוד מנהל שגוי. הגישה נדחתה.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel_button, null);
        builder.show();
    }

    private void showAdminLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("יציאה ממצב מנהל");
        builder.setMessage("האם ברצונך לחזור למצב עוקב (קריאה בלבד)?");
        builder.setPositiveButton("כן, עבור למצב עוקב", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isAdminMode = false;
                prefs.edit().putBoolean(KEY_IS_ADMIN, false).apply();
                refreshAuthState();
                Toast.makeText(MainActivity.this, "עברת למצב עוקב", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel_button, null);
        builder.show();
    }

    // Creating Broadcast Message (Restricted to Admin)
    private void showCreateBroadcastDialog() {
        if (!isAdminMode) {
            Toast.makeText(this, "רק מנהל הערוץ יכול לפרסם תוכן!", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("פרסום עדכון חדש לערוץ");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        container.setPadding(pad, pad, pad, pad);

        final EditText titleInput = new EditText(this);
        titleInput.setHint(R.string.msg_title_hint);
        titleInput.setBackgroundResource(R.drawable.bg_input);
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        titleInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        container.addView(titleInput);

        final EditText contentInput = new EditText(this);
        contentInput.setHint(R.string.msg_content_hint);
        contentInput.setBackgroundResource(R.drawable.bg_input);
        contentInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        contentInput.setMinLines(4);
        contentInput.setGravity(Gravity.TOP | Gravity.START);
        contentInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.topMargin = dp(10);
        contentInput.setLayoutParams(contentParams);
        container.addView(contentInput);

        // Category options
        final TextView categoryLabel = new TextView(this);
        categoryLabel.setText("קטגוריית פרסום:");
        categoryLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        categoryLabel.setTypeface(Typeface.DEFAULT_BOLD);
        categoryLabel.setTextColor(Color.parseColor("#475569"));
        LinearLayout.LayoutParams catLabelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catLabelParams.topMargin = dp(10);
        categoryLabel.setLayoutParams(catLabelParams);
        container.addView(categoryLabel);

        final LinearLayout catRow = new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams catRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        catRowParams.topMargin = dp(4);
        catRow.setLayoutParams(catRowParams);

        final String[] categories = new String[]{"עדכונים", "הודעות", "חשוב ⚡"};
        final String[] chosenCategory = new String[]{"עדכונים"};
        final Button[] catButtons = new Button[categories.length];

        for (int i = 0; i < categories.length; i++) {
            final String cat = categories[i];
            final Button catBtn = new Button(this);
            catBtn.setText(cat);
            catBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            catBtn.setPadding(dp(10), dp(4), dp(10), dp(4));
            catButtons[i] = catBtn;

            if (i == 0) {
                catBtn.setBackgroundResource(R.drawable.bg_chip_selected);
                catBtn.setTextColor(Color.WHITE);
            } else {
                catBtn.setBackgroundResource(R.drawable.bg_chip_unselected);
                catBtn.setTextColor(Color.parseColor("#475569"));
            }

            final int idx = i;
            catBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    chosenCategory[0] = cat;
                    for (int j = 0; j < catButtons.length; j++) {
                        if (j == idx) {
                            catButtons[j].setBackgroundResource(R.drawable.bg_chip_selected);
                            catButtons[j].setTextColor(Color.WHITE);
                        } else {
                            catButtons[j].setBackgroundResource(R.drawable.bg_chip_unselected);
                            catButtons[j].setTextColor(Color.parseColor("#475569"));
                        }
                    }
                }
            });

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            if (i > 0) btnParams.setMargins(dp(6), 0, 0, 0);
            catBtn.setLayoutParams(btnParams);
            catRow.addView(catBtn);
        }
        container.addView(catRow);

        final CheckBox pinCheckbox = new CheckBox(this);
        pinCheckbox.setText("נעץ הודעה זו בראש הערוץ 📌");
        pinCheckbox.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pinParams.topMargin = dp(10);
        pinCheckbox.setLayoutParams(pinParams);
        container.addView(pinCheckbox);

        builder.setView(container);
        builder.setPositiveButton(R.string.publish_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String title = titleInput.getText().toString();
                String content = contentInput.getText().toString().trim();
                if (content.isEmpty()) {
                    Toast.makeText(MainActivity.this, "לא ניתן לפרסם הודעה ריקה", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean pinned = pinCheckbox.isChecked();
                long id = dbHelper.addPost(title, content, chosenCategory[0], pinned);
                if (id > 0) {
                    Toast.makeText(MainActivity.this, "ההודעה פורסמה בהצלחה בערוץ!", Toast.LENGTH_SHORT).show();
                    reloadPosts();
                } else {
                    Toast.makeText(MainActivity.this, "שגיאה בפרסום ההודעה", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel_button, null);
        builder.show();
    }

    // Feed reload & dynamic card generation
    private void reloadPosts() {
        postsFeedContainer.removeAllViews();
        List<DatabaseHelper.Post> posts = dbHelper.getAllPosts(selectedCategory, currentSearchQuery);

        if (posts.isEmpty()) {
            emptyFeedView.setVisibility(View.VISIBLE);
            postsFeedContainer.addView(emptyFeedView);
            return;
        }

        emptyFeedView.setVisibility(View.GONE);

        for (final DatabaseHelper.Post post : posts) {
            View card = createPostCardView(post);
            postsFeedContainer.addView(card);
        }
    }

    private View createPostCardView(final DatabaseHelper.Post post) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = dp(16);
        card.setPadding(pad, pad, pad, pad);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(cardParams);

        // Header row inside card: Category & Pinned & Timestamp & Delete (if admin)
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        // Category Tag
        TextView catTag = new TextView(this);
        catTag.setText(post.category != null ? post.category : "עדכונים");
        catTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        catTag.setTypeface(Typeface.DEFAULT_BOLD);
        catTag.setTextColor(Color.parseColor("#1976D2"));
        catTag.setBackgroundResource(R.drawable.bg_chip_unselected);
        catTag.setPadding(dp(8), dp(2), dp(8), dp(2));
        headerRow.addView(catTag);

        if (post.pinned) {
            TextView pinTag = new TextView(this);
            pinTag.setText(" נעוץ 📌");
            pinTag.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            pinTag.setTextColor(Color.parseColor("#E65100"));
            pinTag.setTypeface(Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams pinParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pinParams.setMargins(dp(6), 0, 0, 0);
            pinTag.setLayoutParams(pinParams);
            headerRow.addView(pinTag);
        }

        // Spacer
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
        headerRow.addView(spacer);

        // Timestamp
        TextView timeView = new TextView(this);
        timeView.setText(dateFormat.format(new Date(post.timestamp)));
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        timeView.setTextColor(Color.parseColor("#94A3B8"));
        headerRow.addView(timeView);

        // Admin delete button
        if (isAdminMode) {
            TextView deleteBtn = new TextView(this);
            deleteBtn.setText("🗑️");
            deleteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            deleteBtn.setPadding(dp(8), 0, 0, 0);
            deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmDeletePost(post.id);
                }
            });
            headerRow.addView(deleteBtn);
        }

        card.addView(headerRow);

        // Post Title (if available)
        if (post.title != null && !post.title.trim().isEmpty()) {
            TextView titleView = new TextView(this);
            titleView.setText(post.title);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            titleView.setTypeface(Typeface.DEFAULT_BOLD);
            titleView.setTextColor(Color.parseColor("#0F172A"));
            LinearLayout.LayoutParams tParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tParams.setMargins(0, dp(8), 0, dp(4));
            titleView.setLayoutParams(tParams);
            card.addView(titleView);
        }

        // Post Content
        TextView contentView = new TextView(this);
        contentView.setText(post.content);
        contentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        contentView.setLineSpacing(dp(3), 1.15f);
        contentView.setTextColor(Color.parseColor("#334155"));
        LinearLayout.LayoutParams cParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cParams.setMargins(0, dp(4), 0, dp(12));
        contentView.setLayoutParams(cParams);
        card.addView(contentView);

        // Divider
        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        card.addView(divider);

        // Reactions and interaction row
        LinearLayout reactionsRow = new LinearLayout(this);
        reactionsRow.setOrientation(LinearLayout.HORIZONTAL);
        reactionsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rParams.topMargin = dp(8);
        reactionsRow.setLayoutParams(rParams);

        // Reaction buttons
        final Button btnLike = createReactionButton("👍 " + post.likes);
        btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAuthAndReact(post.id, "like", btnLike);
            }
        });

        final Button btnHeart = createReactionButton("❤️ " + post.hearts);
        btnHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAuthAndReact(post.id, "heart", btnHeart);
            }
        });

        final Button btnFire = createReactionButton("🔥 " + post.fires);
        btnFire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAuthAndReact(post.id, "fire", btnFire);
            }
        });

        reactionsRow.addView(btnLike);
        reactionsRow.addView(btnHeart);
        reactionsRow.addView(btnFire);

        // Share/Copy text button
        View reactionSpacer = new View(this);
        reactionSpacer.setLayoutParams(new LinearLayout.LayoutParams(0, 0, 1.0f));
        reactionsRow.addView(reactionSpacer);

        Button btnShare = new Button(this);
        btnShare.setText("שתף ↗");
        btnShare.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        btnShare.setTextColor(Color.parseColor("#1565C0"));
        btnShare.setBackgroundColor(Color.TRANSPARENT);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareText = (post.title != null && !post.title.isEmpty() ? post.title + "\n\n" : "")
                        + post.content + "\n\n(נשלח מתוך " + getString(R.string.app_name) + ")";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, "שתף הודעה זו באמצעות"));
            }
        });
        reactionsRow.addView(btnShare);

        card.addView(reactionsRow);

        return card;
    }

    private Button createReactionButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setBackgroundResource(R.drawable.bg_chip_unselected);
        btn.setTextColor(Color.parseColor("#334155"));
        btn.setPadding(dp(10), dp(4), dp(10), dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, dp(6), 0);
        btn.setLayoutParams(params);
        return btn;
    }

    private void checkAuthAndReact(long postId, String reactionType, Button btn) {
        if (currentUserEmail == null) {
            Toast.makeText(this, "התחבר עם חשבון Google כדי להגיב על הודעות הערוץ", Toast.LENGTH_SHORT).show();
            promptGoogleSignIn();
            return;
        }

        dbHelper.incrementReaction(postId, reactionType);
        reloadPosts();
    }

    private void confirmDeletePost(final long postId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("מחיקת הודעה");
        builder.setMessage("האם אתה בטוח שברצונך למחוק הודעה זו מהערוץ?");
        builder.setPositiveButton("מחק", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dbHelper.deletePost(postId);
                Toast.makeText(MainActivity.this, "ההודעה נמחקה", Toast.LENGTH_SHORT).show();
                reloadPosts();
            }
        });
        builder.setNegativeButton(R.string.cancel_button, null);
        builder.show();
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}
