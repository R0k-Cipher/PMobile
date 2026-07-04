package com.example.projectmobile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.projectmobile.model.HistoryItem
import com.example.projectmobile.security.PasswordHasher

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "leanmass_secure.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                weight REAL NOT NULL,
                height REAL NOT NULL,
                gender TEXT NOT NULL,
                lbm REAL NOT NULL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun registerUser(username: String, password: String): Boolean {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hashPassword(password, salt)

        return try {
            val values = ContentValues().apply {
                put("username", username)
                put("password_hash", hash)
                put("salt", salt)
            }

            writableDatabase.insertOrThrow("users", null, values)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loginUser(username: String, password: String): Pair<Boolean, Int> {
        val cursor = readableDatabase.rawQuery(
            "SELECT id, password_hash, salt FROM users WHERE username = ?",
            arrayOf(username)
        )

        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(0)
                val hash = it.getString(1)
                val salt = it.getString(2)

                val valid = PasswordHasher.verifyPassword(password, salt, hash)
                return Pair(valid, if (valid) id else -1)
            }
        }

        return Pair(false, -1)
    }

    fun addHistory(
        userId: Int,
        weight: Double,
        height: Double,
        gender: String,
        lbm: Double,
        status: String
    ) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("weight", weight)
            put("height", height)
            put("gender", gender)
            put("lbm", lbm)
            put("status", status)
            put("created_at", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.FRANCE).format(java.util.Date()))
        }

        writableDatabase.insert("history", null, values)
    }

    fun getHistory(userId: Int): List<HistoryItem> {
        val list = mutableListOf<HistoryItem>()

        val cursor = readableDatabase.rawQuery(
            """
            SELECT id, weight, height, gender, lbm, status, created_at
            FROM history
            WHERE user_id = ?
            ORDER BY id DESC
            """.trimIndent(),
            arrayOf(userId.toString())
        )

        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryItem(
                        id = it.getInt(0),
                        weight = it.getDouble(1),
                        height = it.getDouble(2),
                        gender = it.getString(3),
                        lbm = it.getDouble(4),
                        status = it.getString(5),
                        createdAt = it.getString(6)
                    )
                )
            }
        }

        return list
    }

    fun deleteHistoryItem(id: Int) {
        writableDatabase.delete("history", "id = ?", arrayOf(id.toString()))
    }

    fun deleteAllHistory(userId: Int) {
        writableDatabase.delete("history", "user_id = ?", arrayOf(userId.toString()))
    }
}