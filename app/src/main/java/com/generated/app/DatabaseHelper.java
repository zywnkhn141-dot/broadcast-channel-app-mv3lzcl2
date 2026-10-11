package com.generated.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "broadcast_channel.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_POSTS = "posts";
    public static final String COL_ID = "id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_CATEGORY = "category";
    public static final String COL_TIMESTAMP = "timestamp";
    public static final String COL_PINNED = "pinned";
    public static final String COL_LIKES = "likes";
    public static final String COL_HEARTS = "hearts";
    public static final String COL_FIRES = "fires";

    public static class Post {
        public long id;
        public String title;
        public String content;
        public String category;
        public long timestamp;
        public boolean pinned;
        public int likes;
        public int hearts;
        public int fires;
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createPosts = "CREATE TABLE " + TABLE_POSTS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_CONTENT + " TEXT NOT NULL, " +
                COL_CATEGORY + " TEXT, " +
                COL_TIMESTAMP + " INTEGER, " +
                COL_PINNED + " INTEGER DEFAULT 0, " +
                COL_LIKES + " INTEGER DEFAULT 0, " +
                COL_HEARTS + " INTEGER DEFAULT 0, " +
                COL_FIRES + " INTEGER DEFAULT 0" +
                ");";
        db.execSQL(createPosts);

        // Preload welcoming broadcast message from the channel owner
        long now = System.currentTimeMillis();
        ContentValues welcome = new ContentValues();
        welcome.put(COL_TITLE, "ברוכים הבאים לערוץ הרשמי שלי!");
        welcome.put(COL_CONTENT, "שלום לכולם וברוכים הבאים!\nזהו ערוץ עדכונים רשמי. רק מנהל הערוץ יכול לפרסם כאן הודעות ועדכונים, וכולכם יכולים לקרוא ולהגיב עם אימוג'ים בלחיצה אחת.\n\nהתחברו באמצעות חשבון ה-Google שלכם כדי להישאר מעודכנים!");
        welcome.put(COL_CATEGORY, "עדכונים");
        welcome.put(COL_TIMESTAMP, now);
        welcome.put(COL_PINNED, 1);
        welcome.put(COL_LIKES, 14);
        welcome.put(COL_HEARTS, 28);
        welcome.put(COL_FIRES, 9);
        db.insert(TABLE_POSTS, null, welcome);

        ContentValues tip = new ContentValues();
        tip.put(COL_TITLE, "איך פועלת האפליקציה?");
        tip.put(COL_CONTENT, "• המשתמשים מחוברים כצופים בטוחים.\n• כדי לפרסם תוכן חדש, לחצו על כפתור כניסת מנהל (סיסמת ברירת מחדל: 1234).\n• כל הודעה נשמרת וזמינה מיידית לכל העוקבים.");
        tip.put(COL_CATEGORY, "הודעות");
        tip.put(COL_TIMESTAMP, now - 3600000L);
        tip.put(COL_PINNED, 0);
        tip.put(COL_LIKES, 8);
        tip.put(COL_HEARTS, 11);
        tip.put(COL_FIRES, 3);
        db.insert(TABLE_POSTS, null, tip);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    public long addPost(String title, String content, String category, boolean pinned) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title != null ? title.trim() : "");
        cv.put(COL_CONTENT, content.trim());
        cv.put(COL_CATEGORY, category != null ? category : "עדכונים");
        cv.put(COL_TIMESTAMP, System.currentTimeMillis());
        cv.put(COL_PINNED, pinned ? 1 : 0);
        cv.put(COL_LIKES, 0);
        cv.put(COL_HEARTS, 0);
        cv.put(COL_FIRES, 0);
        return db.insert(TABLE_POSTS, null, cv);
    }

    public boolean deletePost(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_POSTS, COL_ID + "=?", new String[]{String.valueOf(id)}) > 0;
    }

    public void incrementReaction(long id, String reactionType) {
        SQLiteDatabase db = this.getWritableDatabase();
        String column;
        if ("like".equals(reactionType)) {
            column = COL_LIKES;
        } else if ("heart".equals(reactionType)) {
            column = COL_HEARTS;
        } else {
            column = COL_FIRES;
        }
        db.execSQL("UPDATE " + TABLE_POSTS + " SET " + column + " = " + column + " + 1 WHERE " + COL_ID + " = " + id);
    }

    public List<Post> getAllPosts(String categoryFilter, String searchQuery) {
        List<Post> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder query = new StringBuilder("SELECT * FROM " + TABLE_POSTS);
        List<String> args = new ArrayList<>();
        boolean hasFilter = categoryFilter != null && !categoryFilter.equals("הכל");
        boolean hasSearch = searchQuery != null && !searchQuery.trim().isEmpty();

        if (hasFilter || hasSearch) {
            query.append(" WHERE ");
            if (hasFilter) {
                query.append(COL_CATEGORY).append(" = ? ");
                args.add(categoryFilter);
            }
            if (hasSearch) {
                if (hasFilter) query.append(" AND ");
                query.append("(").append(COL_TITLE).append(" LIKE ? OR ").append(COL_CONTENT).append(" LIKE ?)");
                String pattern = "%" + searchQuery.trim() + "%";
                args.add(pattern);
                args.add(pattern);
            }
        }

        query.append(" ORDER BY ").append(COL_PINNED).append(" DESC, ").append(COL_TIMESTAMP).append(" DESC");

        Cursor c = db.rawQuery(query.toString(), args.toArray(new String[0]));
        if (c != null) {
            while (c.moveToNext()) {
                Post p = new Post();
                p.id = c.getLong(c.getColumnIndexOrThrow(COL_ID));
                p.title = c.getString(c.getColumnIndexOrThrow(COL_TITLE));
                p.content = c.getString(c.getColumnIndexOrThrow(COL_CONTENT));
                p.category = c.getString(c.getColumnIndexOrThrow(COL_CATEGORY));
                p.timestamp = c.getLong(c.getColumnIndexOrThrow(COL_TIMESTAMP));
                p.pinned = c.getInt(c.getColumnIndexOrThrow(COL_PINNED)) == 1;
                p.likes = c.getInt(c.getColumnIndexOrThrow(COL_LIKES));
                p.hearts = c.getInt(c.getColumnIndexOrThrow(COL_HEARTS));
                p.fires = c.getInt(c.getColumnIndexOrThrow(COL_FIRES));
                list.add(p);
            }
            c.close();
        }
        return list;
    }
}
