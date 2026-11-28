package com.prayagi.netraassistant.fileassistant

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class IndexedDoc(val uri: String, val name: String, val addedAt: Long, val pages: Int)
data class Passage(val docUri: String, val docName: String, val page: Int, val snippet: String)

/** Turns a user question into a safe FTS4 MATCH query: quoted terms ANDed together. */
object QuerySanitizer {
    fun toMatchQuery(raw: String): String {
        val terms = raw.trim()
            .split(Regex("\\s+"))
            .map { it.replace("\"", "") }
            .filter { t -> t.any { it.isLetterOrDigit() } }
        return terms.joinToString(" AND ") { "\"$it\"" }
    }
}

class DocumentStore(context: Context) : SQLiteOpenHelper(context, "file_assistant.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE docs(uri TEXT PRIMARY KEY, name TEXT NOT NULL, added_at INTEGER NOT NULL, pages INTEGER NOT NULL)")
        db.execSQL("CREATE VIRTUAL TABLE paragraphs USING fts4(doc_uri TEXT NOT NULL, page INTEGER NOT NULL, seq INTEGER NOT NULL, text TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addDocument(uri: String, name: String, pages: Int) {
        val v = ContentValues()
        v.put("uri", uri)
        v.put("name", name)
        v.put("added_at", System.currentTimeMillis())
        v.put("pages", pages)
        writableDatabase.insertWithOnConflict("docs", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeDocument(uri: String) {
        writableDatabase.delete("docs", "uri=?", arrayOf(uri))
        writableDatabase.delete("paragraphs", "doc_uri=?", arrayOf(uri))
    }

    fun documents(): List<IndexedDoc> {
        val out = mutableListOf<IndexedDoc>()
        readableDatabase.rawQuery("SELECT uri, name, added_at, pages FROM docs ORDER BY added_at DESC", null).use { c ->
            while (c.moveToNext()) out.add(IndexedDoc(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3)))
        }
        return out
    }

    fun clearParagraphs(docUri: String) {
        writableDatabase.delete("paragraphs", "doc_uri=?", arrayOf(docUri))
    }

    fun insertParagraph(docUri: String, page: Int, seq: Int, text: String) {
        val v = ContentValues()
        v.put("doc_uri", docUri)
        v.put("page", page)
        v.put("seq", seq)
        v.put("text", text)
        writableDatabase.insert("paragraphs", null, v)
    }

    fun search(rawQuery: String, limit: Int = 8): List<Passage> {
        val match = QuerySanitizer.toMatchQuery(rawQuery)
        if (match.isBlank()) return emptyList()
        val out = mutableListOf<Passage>()
        try {
            readableDatabase.rawQuery(
                "SELECT paragraphs.doc_uri, docs.name, paragraphs.page, snippet(paragraphs, '', '', ' ... ', -1, 40) " +
                    "FROM paragraphs JOIN docs ON docs.uri = paragraphs.doc_uri " +
                    "WHERE paragraphs MATCH ? LIMIT ?",
                arrayOf(match, limit.toString())
            ).use { c ->
                while (c.moveToNext()) out.add(Passage(c.getString(0), c.getString(1), c.getInt(2), c.getString(3)))
            }
        } catch (t: Throwable) {
            return emptyList()
        }
        return out
    }
}
