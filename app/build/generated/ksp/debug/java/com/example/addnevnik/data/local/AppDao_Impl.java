package com.example.addnevnik.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDao_Impl implements AppDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BloodPressureEntity> __insertionAdapterOfBloodPressureEntity;

  private final EntityInsertionAdapter<NoteEntity> __insertionAdapterOfNoteEntity;

  private final EntityDeletionOrUpdateAdapter<BloodPressureEntity> __deletionAdapterOfBloodPressureEntity;

  private final EntityDeletionOrUpdateAdapter<NoteEntity> __deletionAdapterOfNoteEntity;

  private final EntityDeletionOrUpdateAdapter<BloodPressureEntity> __updateAdapterOfBloodPressureEntity;

  private final EntityDeletionOrUpdateAdapter<NoteEntity> __updateAdapterOfNoteEntity;

  public AppDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBloodPressureEntity = new EntityInsertionAdapter<BloodPressureEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `blood_pressure` (`id`,`systolic`,`diastolic`,`pulse`,`timestamp_ms`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BloodPressureEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSystolic());
        statement.bindLong(3, entity.getDiastolic());
        statement.bindLong(4, entity.getPulse());
        statement.bindLong(5, entity.getTimestamp_ms());
      }
    };
    this.__insertionAdapterOfNoteEntity = new EntityInsertionAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notes` (`id`,`text`,`timestamp_ms`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        statement.bindLong(3, entity.getTimestamp_ms());
      }
    };
    this.__deletionAdapterOfBloodPressureEntity = new EntityDeletionOrUpdateAdapter<BloodPressureEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `blood_pressure` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BloodPressureEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfNoteEntity = new EntityDeletionOrUpdateAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `notes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfBloodPressureEntity = new EntityDeletionOrUpdateAdapter<BloodPressureEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `blood_pressure` SET `id` = ?,`systolic` = ?,`diastolic` = ?,`pulse` = ?,`timestamp_ms` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BloodPressureEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSystolic());
        statement.bindLong(3, entity.getDiastolic());
        statement.bindLong(4, entity.getPulse());
        statement.bindLong(5, entity.getTimestamp_ms());
        statement.bindLong(6, entity.getId());
      }
    };
    this.__updateAdapterOfNoteEntity = new EntityDeletionOrUpdateAdapter<NoteEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notes` SET `id` = ?,`text` = ?,`timestamp_ms` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NoteEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        statement.bindLong(3, entity.getTimestamp_ms());
        statement.bindLong(4, entity.getId());
      }
    };
  }

  @Override
  public Object insertBloodPressure(final BloodPressureEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBloodPressureEntity.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNoteEntity.insert(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBloodPressure(final BloodPressureEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfBloodPressureEntity.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfNoteEntity.handle(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateBloodPressure(final BloodPressureEntity entry,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfBloodPressureEntity.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNote(final NoteEntity note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfNoteEntity.handle(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BloodPressureEntity>> getAllBloodPressure() {
    final String _sql = "SELECT * FROM blood_pressure ORDER BY timestamp_ms DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blood_pressure"}, new Callable<List<BloodPressureEntity>>() {
      @Override
      @NonNull
      public List<BloodPressureEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfPulse = CursorUtil.getColumnIndexOrThrow(_cursor, "pulse");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp_ms");
          final List<BloodPressureEntity> _result = new ArrayList<BloodPressureEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpPulse;
            _tmpPulse = _cursor.getInt(_cursorIndexOfPulse);
            final long _tmpTimestamp_ms;
            _tmpTimestamp_ms = _cursor.getLong(_cursorIndexOfTimestampMs);
            _item = new BloodPressureEntity(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpPulse,_tmpTimestamp_ms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllBloodPressureOnce(
      final Continuation<? super List<BloodPressureEntity>> $completion) {
    final String _sql = "SELECT * FROM blood_pressure ORDER BY timestamp_ms DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BloodPressureEntity>>() {
      @Override
      @NonNull
      public List<BloodPressureEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfPulse = CursorUtil.getColumnIndexOrThrow(_cursor, "pulse");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp_ms");
          final List<BloodPressureEntity> _result = new ArrayList<BloodPressureEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpPulse;
            _tmpPulse = _cursor.getInt(_cursorIndexOfPulse);
            final long _tmpTimestamp_ms;
            _tmpTimestamp_ms = _cursor.getLong(_cursorIndexOfTimestampMs);
            _item = new BloodPressureEntity(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpPulse,_tmpTimestamp_ms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<BloodPressureEntity> getLatestBloodPressure() {
    final String _sql = "SELECT * FROM blood_pressure ORDER BY timestamp_ms DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blood_pressure"}, new Callable<BloodPressureEntity>() {
      @Override
      @Nullable
      public BloodPressureEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfPulse = CursorUtil.getColumnIndexOrThrow(_cursor, "pulse");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp_ms");
          final BloodPressureEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpPulse;
            _tmpPulse = _cursor.getInt(_cursorIndexOfPulse);
            final long _tmpTimestamp_ms;
            _tmpTimestamp_ms = _cursor.getLong(_cursorIndexOfTimestampMs);
            _result = new BloodPressureEntity(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpPulse,_tmpTimestamp_ms);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<BloodPressureEntity>> getBloodPressureAfter(final long since) {
    final String _sql = "SELECT * FROM blood_pressure WHERE timestamp_ms >= ? ORDER BY timestamp_ms DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blood_pressure"}, new Callable<List<BloodPressureEntity>>() {
      @Override
      @NonNull
      public List<BloodPressureEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfSystolic = CursorUtil.getColumnIndexOrThrow(_cursor, "systolic");
          final int _cursorIndexOfDiastolic = CursorUtil.getColumnIndexOrThrow(_cursor, "diastolic");
          final int _cursorIndexOfPulse = CursorUtil.getColumnIndexOrThrow(_cursor, "pulse");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp_ms");
          final List<BloodPressureEntity> _result = new ArrayList<BloodPressureEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BloodPressureEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpSystolic;
            _tmpSystolic = _cursor.getInt(_cursorIndexOfSystolic);
            final int _tmpDiastolic;
            _tmpDiastolic = _cursor.getInt(_cursorIndexOfDiastolic);
            final int _tmpPulse;
            _tmpPulse = _cursor.getInt(_cursorIndexOfPulse);
            final long _tmpTimestamp_ms;
            _tmpTimestamp_ms = _cursor.getLong(_cursorIndexOfTimestampMs);
            _item = new BloodPressureEntity(_tmpId,_tmpSystolic,_tmpDiastolic,_tmpPulse,_tmpTimestamp_ms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<Integer> getBloodPressureCount() {
    final String _sql = "SELECT COUNT(*) FROM blood_pressure";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"blood_pressure"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<NoteEntity>> getAllNotes() {
    final String _sql = "SELECT * FROM notes ORDER BY timestamp_ms DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<NoteEntity>>() {
      @Override
      @NonNull
      public List<NoteEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfTimestampMs = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp_ms");
          final List<NoteEntity> _result = new ArrayList<NoteEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NoteEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final long _tmpTimestamp_ms;
            _tmpTimestamp_ms = _cursor.getLong(_cursorIndexOfTimestampMs);
            _item = new NoteEntity(_tmpId,_tmpText,_tmpTimestamp_ms);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
