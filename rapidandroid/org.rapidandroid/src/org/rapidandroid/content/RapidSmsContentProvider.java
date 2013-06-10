/*
 * Copyright (C) 2009 Dimagi Inc., UNICEF
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.rapidandroid.content;

import java.util.Map;

import org.rapidandroid.content.translation.MessageTranslator;
import org.rapidandroid.content.translation.ModelTranslator;
import org.rapidandroid.data.RapidSmsDBConstants;
import org.rapidandroid.data.SmsDbHelper;
import org.rapidsms.java.core.model.Form;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Main Content provider for the RapidAndroid project.
 * 
 * It should be the universal URI accessible content provider that's accessible
 * via the GetContentResolver().query()<br>
 * 
 * The definitions for the URI are linked via the RapidSmsDBConstants static
 * properties.<br>
 * <br>
 * 
 * This content provider should provide all functionality to do the following:<br>
 * <ul>
 * <li>insert/query SMS messages that are known to be related to this app</li>
 * <li>insert/query monitors (senders) in a table associated with the SMSs</li>
 * <li>insert/query Form/Field/Fieldtype definitions in SQL that reflect the
 * structures defined in the org.rapidsms.java.core.model.*</li>
 * <li>Create and store and query data tables generated by the Form definition.</li>
 * </ul>
 * 
 * 
 * @author Daniel Myung dmyung@dimagi.com
 * @created Jan 09, 2009
 * 
 * 
 */

public class RapidSmsContentProvider extends ContentProvider {
	/**
	 * @param context
	 * @param name
	 * @param factory
	 * @param version
	 */

	public static final Uri CONTENT_URI = Uri.parse("content://" + RapidSmsDBConstants.AUTHORITY);

	private static final String TAG = "RapidSmsContentProvider";

	private SmsDbHelper mOpenHelper;

	private static final int MESSAGE = 1;
	private static final int PROJECT = 13;
	private static final int SURVEY = 14;
	private static final int MESSAGE_ID = 2;
	private static final int MONITOR = 3;
	private static final int MONITOR_ID = 4;
	private static final int MONITOR_MESSAGE_ID = 5;

	private static final int FORM = 6;
	private static final int FORM_ID = 7;

	private static final int FIELD = 8;
	private static final int FIELD_ID = 9;

	private static final int FIELDTYPE = 10;
	private static final int FIELDTYPE_ID = 11;

	private static final int FORMDATA_ID = 12;
	// private static final int FORMDATA_ID = 13;

	private static final UriMatcher sUriMatcher;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Message.URI_PART, MESSAGE);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Message.URI_PART + "/#", MESSAGE_ID);

		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Monitor.URI_PART, MONITOR);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Monitor.URI_PART + "/#", MONITOR_ID);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, "messagesbymonitor/#", MONITOR_MESSAGE_ID);

		// form field data stuffs
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Form.URI_PART, FORM);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Form.URI_PART + "/#", FORM_ID);

		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Field.URI_PART, FIELD);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Field.URI_PART + "/#", FIELD_ID);

		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.FieldType.URI_PART, FIELDTYPE);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.FieldType.URI_PART + "/#", FIELDTYPE_ID);

		// actual form data
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.FormData.URI_PART + "/#", FORMDATA_ID);
	
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Project.URI_PART, PROJECT);
		sUriMatcher.addURI(RapidSmsDBConstants.AUTHORITY, RapidSmsDBConstants.Survey.URI_PART, SURVEY);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#getType(android.net.Uri)
	 */
	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case MESSAGE:
				return RapidSmsDBConstants.Message.CONTENT_TYPE;
			case PROJECT:
				return RapidSmsDBConstants.Project.CONTENT_TYPE;
			case SURVEY:
				return RapidSmsDBConstants.Survey.CONTENT_TYPE;
			case MESSAGE_ID:
				return RapidSmsDBConstants.Message.CONTENT_ITEM_TYPE;
			case MONITOR:
				return RapidSmsDBConstants.Monitor.CONTENT_TYPE;
			case MONITOR_ID:
				return RapidSmsDBConstants.Monitor.CONTENT_ITEM_TYPE;
			case MONITOR_MESSAGE_ID:
				// this is similar to Monitor, but is filtered
				return RapidSmsDBConstants.Monitor.CONTENT_TYPE;

			case FORM:
				return RapidSmsDBConstants.Form.CONTENT_TYPE;
			case FORM_ID:
				return RapidSmsDBConstants.Form.CONTENT_ITEM_TYPE;

			case FIELD:
				return RapidSmsDBConstants.Field.CONTENT_TYPE;
			case FIELD_ID:
				return RapidSmsDBConstants.Field.CONTENT_ITEM_TYPE;

			case FIELDTYPE:
				return RapidSmsDBConstants.FieldType.CONTENT_TYPE;
			case FIELDTYPE_ID:
				return RapidSmsDBConstants.FieldType.CONTENT_ITEM_TYPE;

			case FORMDATA_ID:
				return RapidSmsDBConstants.FormData.CONTENT_TYPE;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
				// return sUriMatcher.match(uri)+"";
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#insert(android.net.Uri,
	 * android.content.ContentValues)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Validate the requested uri
		// if (sUriMatcher.match(uri) != MESSAGE || sUriMatcher.match(uri) !=
		// MONITOR) {
		// throw new IllegalArgumentException("Unknown URI " + uri);
		// }

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		switch (sUriMatcher.match(uri)) {
			case MESSAGE:
				return insertMessage(uri, values);
			case PROJECT:
				return insertProject(uri, values);
			case SURVEY:
				return insertSurvey(uri, values);
			case MONITOR:
				return insertMonitor(uri, values);
			case FIELDTYPE:
				return insertFieldType(uri, values);
			case FIELD:
				return insertField(uri, values);
			case FORM:
				return insertForm(uri, values);
			case FORMDATA_ID:
				return insertFormData(uri, values);
				// other stuffs not implemented for insertion yet.

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);

		}
	}

	private Uri insertFormData(Uri uri, ContentValues values) {
		// sanity check, see if the table exists
		String formid = uri.getPathSegments().get(1);
		String formprefix = ModelTranslator.getFormById(Integer.valueOf(formid).intValue()).getPrefix().replace("@", "");
		// SQLiteDatabase dbr = mOpenHelper.getReadableDatabase();
		// Cursor table_exists = dbr.rawQuery("select count(*) from formdata_"
		// + formprefix, null);
		// if (table_exists.getCount() != 1) {
		// throw new SQLException("Failed to insert row into " + uri
		// + " :: table doesn't exist.");
		// }
		// table_exists.close();

		// doInsert doesn't apply well here.
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(RapidSmsDBConstants.FormData.TABLE_PREFIX + formprefix,
								RapidSmsDBConstants.FormData.MESSAGE, values);
		if (rowId > 0) {
			Uri fieldUri = ContentUris.withAppendedId(RapidSmsDBConstants.Form.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(fieldUri, null);
			return Uri.parse(uri.toString() + "/" + rowId);
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	private Uri insertForm(Uri uri, ContentValues values) {
		if (values.containsKey(RapidSmsDBConstants.Form.FORMNAME) == false
				|| values.containsKey(RapidSmsDBConstants.Form.DESCRIPTION) == false
				|| values.containsKey(RapidSmsDBConstants.Form.PARSEMETHOD) == false) {
			throw new SQLException("Insufficient arguments for Form insert " + uri);
		}
		return doInsert(uri, values, RapidSmsDBConstants.Form.TABLE, RapidSmsDBConstants.Form.FORMNAME);
	}

	/**
	 * @param uri
	 * @param values
	 * @return
	 */
	private Uri doInsert(Uri uri, ContentValues values, String tablename, String nullvalue) {

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		
		Log.i("RapidSmsContentProvider", "tablename: " + tablename);
		for (Map.Entry<String, Object> pair : values.valueSet()) {
			Log.i("RapidSmsContentProvider", "cv " + pair.getKey() + " " + pair.getValue());
		}
		long rowId = db.insert(tablename, nullvalue, values);
		if (rowId > 0) {
			Uri retUri = ContentUris.withAppendedId(uri, rowId);
			getContext().getContentResolver().notifyChange(retUri, null);
			return retUri;
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	// Insert Methods
	private Uri insertField(Uri uri, ContentValues values) {
		if (values.containsKey(RapidSmsDBConstants.Field.FORM) == false
				|| values.containsKey(RapidSmsDBConstants.Field.NAME) == false
				|| values.containsKey(RapidSmsDBConstants.Field.FIELDTYPE) == false
				|| values.containsKey(RapidSmsDBConstants.Field.PROMPT) == false
				|| values.containsKey(RapidSmsDBConstants.Field.SEQUENCE) == false) {
			throw new SQLException("Insufficient arguments for field insert " + uri);
		}

		return doInsert(uri, values, RapidSmsDBConstants.Field.TABLE, RapidSmsDBConstants.Field.NAME);
	}

	private Uri insertFieldType(Uri uri, ContentValues values) {
		if (values.containsKey(BaseColumns._ID) == false
				|| values.containsKey(RapidSmsDBConstants.FieldType.NAME) == false
				|| values.containsKey(RapidSmsDBConstants.FieldType.REGEX) == false
				|| values.containsKey(RapidSmsDBConstants.FieldType.DATATYPE) == false) {

			throw new SQLException("Insufficient arguments for fieldtype insert " + uri);
		}
		return doInsert(uri, values, RapidSmsDBConstants.FieldType.TABLE, RapidSmsDBConstants.FieldType.NAME);
	}

	public void ClearFormDataDebug() {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		Cursor formsCursor = db.query("rapidandroid_form", new String[] { "prefix" }, null, null, null, null, null);
		// ("select prefix from rapidandroid_form");

		// iterate and blow away
		formsCursor.moveToFirst();
		do {
			String prefix = formsCursor.getString(0);
			String dropstatement = "drop table formdata_" + prefix + ";";
			db.execSQL(dropstatement);
		} while (formsCursor.moveToNext());
		formsCursor.close();
	}

	/**
	 * @param uri
	 * @param values
	 */
	private Uri insertMessage(Uri uri, ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(RapidSmsDBConstants.Message.TIME) == false) {
			values.put(RapidSmsDBConstants.Message.TIME, now);
		}

		if (values.containsKey(RapidSmsDBConstants.Message.MESSAGE) == false) {
			throw new SQLException("No message");
		}

		if (values.containsKey(RapidSmsDBConstants.Message.MONITOR) == false) {
			throw new SQLException("Must set a monitor for insertion");
		}
		// else {
		// //check if the monitor exists.
		// Cursor monitorCursor = query(RapidSmsDBConstants.Monitor.CONTENT_URI,
		// null, "phone='" +
		// values.getAsString(RapidSmsDBConstants.Message.PHONE) + "'" , null,
		// null);
		// if(monitorCursor.getCount() == 0) {
		// ContentValues monitorValues = new ContentValues();
		// monitorValues.put(RapidSmsDBConstants.Message.MESSAGE, values
		// .getAsString(RapidSmsDBConstants.Message.MESSAGE));
		// Uri monitorUri =
		// insertMonitor(RapidSmsDBConstants.Monitor.CONTENT_URI,
		// monitorValues);
		// // ok, so we insert the mMonitorString into the mMonitorString table.
		// // get the URI back and assign the foreign key into the values as
		// // part of the message insert
		// values.put(RapidSmsDBConstants.Message.MONITOR, monitorUri
		// .getPathSegments().get(1));
		// values.remove(RapidSmsDBConstants.Message.PHONE);
		// }
		//			
		//			
		// }

		if (values.containsKey(RapidSmsDBConstants.Message.IS_OUTGOING) == false) {
			throw new SQLException("No direction");
		}

		if (values.containsKey(RapidSmsDBConstants.Message.IS_VIRTUAL) == false) {
			values.put(RapidSmsDBConstants.Message.IS_VIRTUAL, false);
		}

		return doInsert(uri, values, RapidSmsDBConstants.Message.TABLE, RapidSmsDBConstants.Message.MESSAGE);
	}
	
	
	/**
	 * @param uri
	 * @param values
	 */
	private Uri insertSurvey(Uri uri, ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(RapidSmsDBConstants.Message.TIME) == false) {
			values.put(RapidSmsDBConstants.Message.TIME, now);
		}
/*
		if (values.containsKey(RapidSmsDBConstants.Message.MESSAGE) == false) {
			throw new SQLException("No message");
		}

		if (values.containsKey(RapidSmsDBConstants.Message.MONITOR) == false) {
			throw new SQLException("Must set a monitor for insertion");
		}
		// else {
		// //check if the monitor exists.
		// Cursor monitorCursor = query(RapidSmsDBConstants.Monitor.CONTENT_URI,
		// null, "phone='" +
		// values.getAsString(RapidSmsDBConstants.Message.PHONE) + "'" , null,
		// null);
		// if(monitorCursor.getCount() == 0) {
		// ContentValues monitorValues = new ContentValues();
		// monitorValues.put(RapidSmsDBConstants.Message.MESSAGE, values
		// .getAsString(RapidSmsDBConstants.Message.MESSAGE));
		// Uri monitorUri =
		// insertMonitor(RapidSmsDBConstants.Monitor.CONTENT_URI,
		// monitorValues);
		// // ok, so we insert the mMonitorString into the mMonitorString table.
		// // get the URI back and assign the foreign key into the values as
		// // part of the message insert
		// values.put(RapidSmsDBConstants.Message.MONITOR, monitorUri
		// .getPathSegments().get(1));
		// values.remove(RapidSmsDBConstants.Message.PHONE);
		// }
		//			
		//			
		// }

		if (values.containsKey(RapidSmsDBConstants.Message.IS_OUTGOING) == false) {
			throw new SQLException("No direction");
		}

		if (values.containsKey(RapidSmsDBConstants.Message.IS_VIRTUAL) == false) {
			values.put(RapidSmsDBConstants.Message.IS_VIRTUAL, false);
		}
*/
		// TOD remove the hardcoding of nullhack keyword
		return doInsert(uri, values, RapidSmsDBConstants.Survey.TABLE, "surveyname");
	}
	
	

	private Uri insertProject(Uri uri, ContentValues values) {
		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(RapidSmsDBConstants.Message.TIME) == false) {
			values.put(RapidSmsDBConstants.Message.TIME, now);
		}

		// TODO these are all hard coded, should put them in constants file
		if (!values.containsKey("name")) {
			throw new SQLException("No message");
		}
		// TODO what is this monitor?
/*
		if (values.containsKey(RapidSmsDBConstants.Message.MONITOR) == false) {
			throw new SQLException("Must set a monitor for insertion");
		}
*/


		if (!values.containsKey("is_active")) {
			values.put("is_active", true);
		}

		return doInsert(uri, values, RapidSmsDBConstants.Project.TABLE, "name");
	}
	
	/**
	 * @param uri
	 * @param values
	 */
	private Uri insertMonitor(Uri uri, ContentValues values) {
		// Make sure that the fields are all set
		if (values.containsKey(RapidSmsDBConstants.Monitor.PHONE) == false) {
			throw new SQLException("No phone");
		}

		if (values.containsKey(RapidSmsDBConstants.Monitor.ALIAS) == false) {
			values.put(RapidSmsDBConstants.Monitor.ALIAS, values.getAsString(RapidSmsDBConstants.Monitor.PHONE));
		}

		if (values.containsKey(RapidSmsDBConstants.Monitor.EMAIL) == false) {
			values.put(RapidSmsDBConstants.Monitor.EMAIL, "");
		}

		if (values.containsKey(RapidSmsDBConstants.Monitor.FIRST_NAME) == false) {
			values.put(RapidSmsDBConstants.Monitor.FIRST_NAME, "");
		}

		if (values.containsKey(RapidSmsDBConstants.Monitor.LAST_NAME) == false) {
			values.put(RapidSmsDBConstants.Monitor.LAST_NAME, "");
		}

		if (values.containsKey(RapidSmsDBConstants.Monitor.INCOMING_MESSAGES) == false) {
			values.put(RapidSmsDBConstants.Monitor.INCOMING_MESSAGES, 0);
		}

		// Check if mMonitorString exists, if it doesn't insert a new one, else
		// return the old one.
		Cursor exists = query(uri, null, RapidSmsDBConstants.Monitor.PHONE + "='"
				+ values.getAsString(RapidSmsDBConstants.Monitor.PHONE) + "'", null, null);

		if (exists.getCount() == 1) {
			exists.moveToFirst();
			int existingMonitorId = exists.getInt(0);
			exists.close();
			return ContentUris.withAppendedId(RapidSmsDBConstants.Monitor.CONTENT_URI, existingMonitorId);
		} else {
			exists.close();
		}

		Uri ret = doInsert(uri, values, RapidSmsDBConstants.Monitor.TABLE, RapidSmsDBConstants.Monitor.PHONE);
		MessageTranslator.updateMonitorHash(getContext());
		return ret;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String table;
		String finalWhere = "";

		switch (sUriMatcher.match(uri)) {
			case MESSAGE:
				table = RapidSmsDBConstants.Message.TABLE;
				break;

			case PROJECT:
				table = RapidSmsDBConstants.Project.TABLE;
				break;
				
			case SURVEY:
				table = RapidSmsDBConstants.Survey.TABLE;
				break;
				
			case MESSAGE_ID:
				table = RapidSmsDBConstants.Message.TABLE;
				finalWhere = BaseColumns._ID + "=" + uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
				break;
			case MONITOR:
				table = RapidSmsDBConstants.Monitor.TABLE;
				break;

			case MONITOR_ID:
				table = RapidSmsDBConstants.Monitor.TABLE;
				finalWhere = BaseColumns._ID + "=" + uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
				break;
			case MONITOR_MESSAGE_ID:
				table = RapidSmsDBConstants.Message.TABLE;
				// qb.appendWhere(RapidSmsDBConstants.Message.MONITOR + "="
				// + uri.getPathSegments().get(1));

				finalWhere = RapidSmsDBConstants.Message.MONITOR + "=" + uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");
				break;
			case FORMDATA_ID:
				// need to set the table to the FieldData + form_prefix
				// this is possible via querying hte forms to get the
				// formname/prefix from the form table definition
				// and appending that to do the qb.setTables
				String formid = uri.getPathSegments().get(1);
				Form f = ModelTranslator.getFormById(Integer.valueOf(formid).intValue());
				table = RapidSmsDBConstants.FormData.TABLE_PREFIX + f.getPrefix().replace("@", "");
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (finalWhere == "") {
			finalWhere = where;
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		int result = db.delete(table, finalWhere, whereArgs);

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#query(android.net.Uri,
	 * java.lang.String[], java.lang.String, java.lang.String[],
	 * java.lang.String)
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
			case MESSAGE:
				qb.setTables(RapidSmsDBConstants.Message.TABLE);
				break;

			case PROJECT:
				qb.setTables(RapidSmsDBConstants.Project.TABLE);
				break;
			
			case SURVEY:
				Log.i("ContentProvider.query", "selection: \"" + selection + "\"");
				qb.setTables(RapidSmsDBConstants.Survey.TABLE);
				/*SQLiteDatabase db1 = mOpenHelper.getReadableDatabase();
				Cursor c_all = qb.query(db1, null, null, null, null, null, null);
				c_all.moveToFirst();
				for (int i = 1; i < c_all.getCount(); i++) {
					Log.i("ContentProvider.query", "Survey name: \"" + c_all.getString(c_all.getColumnIndex("surveyname")) + "\"");
					c_all.moveToNext();
				}
				*/
				break;
				
				
			case MESSAGE_ID:
				qb.setTables(RapidSmsDBConstants.Message.TABLE);
				qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
				break;
			case MONITOR:
				qb.setTables(RapidSmsDBConstants.Monitor.TABLE);
				break;

			case MONITOR_ID:
				qb.setTables(RapidSmsDBConstants.Monitor.TABLE);
				qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
				break;
			case MONITOR_MESSAGE_ID:
				qb.setTables(RapidSmsDBConstants.Message.TABLE);
				qb.appendWhere(RapidSmsDBConstants.Message.MONITOR + "=" + uri.getPathSegments().get(1));
				break;
			case FORM:
				qb.setTables(RapidSmsDBConstants.Form.TABLE);
				break;
			case FORM_ID:
				qb.setTables(RapidSmsDBConstants.Form.TABLE);
				qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
				break;
			case FIELD:
				qb.setTables(RapidSmsDBConstants.Field.TABLE);
				break;
			case FIELD_ID:
				qb.setTables(RapidSmsDBConstants.Field.TABLE);
				qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));
				break;
			case FIELDTYPE:
				qb.setTables(RapidSmsDBConstants.FieldType.TABLE);
				break;
			case FIELDTYPE_ID:
				qb.setTables(RapidSmsDBConstants.FieldType.TABLE);
				qb.appendWhere(BaseColumns._ID + "=" + uri.getPathSegments().get(1));

				break;
			case FORMDATA_ID:
				// need to set the table to the FieldData + form_prefix
				// this is possible via querying hte forms to get the
				// formname/prefix from the form table definition
				// and appending that to do the qb.setTables
				String formid = uri.getPathSegments().get(1);
				Form f = ModelTranslator.getFormById(Integer.valueOf(formid).intValue());
				StringBuilder query = new StringBuilder();
				query.append("select " + RapidSmsDBConstants.FormData.TABLE_PREFIX);
				query.append(f.getPrefix().replace("@", "") + ".*");
				query.append(" from " + RapidSmsDBConstants.FormData.TABLE_PREFIX + f.getPrefix().replace("@", ""));
				query.append(" join rapidandroid_message on (");
				query.append(RapidSmsDBConstants.FormData.TABLE_PREFIX + f.getPrefix().replace("@", ""));
				query.append(".message_id = rapidandroid_message._id");
				query.append(") ");

				if (selection != null) {
					query.append(" WHERE " + selection);
					query.append(" ORDER BY rapidandroid_message.time DESC");
				} else {
					query.append(" ORDER BY RAPIDANDROID_MESSAGE.time DESC");
				}
				SQLiteDatabase db = mOpenHelper.getReadableDatabase();
				Cursor c = db.rawQuery(query.toString(), null);
				c.setNotificationUri(getContext().getContentResolver(), uri);

				return c;

				// throw new IllegalArgumentException(uri +
				// " query handler not implemented.");

			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy = sortOrder;

		// if (TextUtils.isEmpty(sortOrder)) {
		// orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
		// } else {
		// orderBy = sortOrder;
		// }

		// Get the database and run the query
		Log.i("getting database", "");
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		
		Log.i("doing query", "");
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		/*if (c == null) {
			Log.i("ContentProvider.query", "results null!");
		} else if (c.getCount() == 0) {
			Log.i("ContentProvider.query", "results empty!");
		}*/
		
		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.ContentProvider#update(android.net.Uri,
	 * android.content.ContentValues, java.lang.String, java.lang.String[])
	 */
	@Override
	public int update(Uri uri, ContentValues valuesToChange, String selection, String[] selectionArgs) {
		
		// Taken from remove method
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		String table;
		String finalWhere = "";

		switch (sUriMatcher.match(uri)) {
			case MESSAGE:
				table = RapidSmsDBConstants.Message.TABLE;
				break;
				
			case PROJECT:
				table = RapidSmsDBConstants.Project.TABLE;
				break;
				
			case SURVEY:
				table = RapidSmsDBConstants.Survey.TABLE;
				break;

			case FORM:
				table = RapidSmsDBConstants.Form.TABLE;
				break;
				
			case MESSAGE_ID:
				table = RapidSmsDBConstants.Message.TABLE;
				/*finalWhere = BaseColumns._ID + "=" + uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");*/
				break;
			case MONITOR:
				table = RapidSmsDBConstants.Monitor.TABLE;
				break;

			case MONITOR_ID:
				table = RapidSmsDBConstants.Monitor.TABLE;
				/*finalWhere = BaseColumns._ID + "=" + uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");*/
				break;
			case MONITOR_MESSAGE_ID:
				table = RapidSmsDBConstants.Message.TABLE;
				// qb.appendWhere(RapidSmsDBConstants.Message.MONITOR + "="
				// + uri.getPathSegments().get(1));

				/*finalWhere = RapidSmsDBConstants.Message.MONITOR + "=" + uri.getPathSegments().get(1)
						+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : "");*/
				break;
			case FORMDATA_ID:
				// need to set the table to the FieldData + form_prefix
				// this is possible via querying hte forms to get the
				// formname/prefix from the form table definition
				// and appending that to do the qb.setTables
				String formid = uri.getPathSegments().get(1);
				Form f = ModelTranslator.getFormById(Integer.valueOf(formid).intValue());
				table = RapidSmsDBConstants.FormData.TABLE_PREFIX + f.getPrefix().replace("@", "");
				break;
			default:
				throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Log.i("RapidSmsContentProvider", "update: selection " + selection);
		return db.update(table, valuesToChange, selection, selectionArgs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite
	 * .SQLiteDatabase)
	 */
	@Override
	public boolean onCreate() {
		mOpenHelper = new SmsDbHelper(getContext());
		return true;
	}

}
