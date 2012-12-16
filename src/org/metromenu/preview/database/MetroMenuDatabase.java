/*
 * Copyright (C) 2011 Moko365 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metromenu.preview.database;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MetroMenuDatabase extends SQLiteOpenHelper {

	private static final String TAG = "MetroMenuDatabase";

	private static final String DATABASE_NAME = "metromenu_preview.db";
	private static final String TABLE_NAME = "items";
	private static final int DATABASE_VERSION = 56; 
    private static final String TABLE_CREATE =
        "CREATE TABLE " + TABLE_NAME + "("
    	     + "_ID INTEGER PRIMARY KEY,"
    	     + "package_name TEXT,"
    	     + "app_name TEXT,"
    	     + "activity_name TEXT,"
    	     + "module TEXT,"
    	     + "image TEXT,"
    	     + "size TEXT"
        + ");";
    
	private SQLiteDatabase db;
	private Cursor mCursor;
	private MetroMenuDatabase mMokoWebDatabase;
	
	// Not Singleton at this project
	public MetroMenuDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		open();
	}
	
	public synchronized void open() {
		db = this.getWritableDatabase();
		Log.i(TAG, "on open");
	}
	
	@Override
	public void close() {
		synchronized (db) {
			db.close();
			mMokoWebDatabase = null;			
		}
		Log.i(TAG, "on close");
	}	

	@Override
	public void onCreate(SQLiteDatabase db) {
		synchronized (db) {
			db.execSQL(TABLE_CREATE);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop old
		db.execSQL("DROP TABLE IF EXISTS items");
		// Create new one
		onCreate(db);
	}
	
	public void saveItem(String package_name, 
			String app_name, String activity_name) {
		put(package_name, app_name, activity_name);
	}
	
	public void saveItem(String package_name, 
			String app_name, String activity_name, String module_name, String image) {
		put(package_name, app_name, activity_name, module_name, image);		
	}

	private synchronized Cursor get(String package_name) throws SQLException {
		Cursor cursor = db.query(true,
			"items",
			new String[] {"_ID", 
							"package_name", 
							"app_name",
							"activity_name",
				    	     "module",	
							"image",
							"size"},
			"package_name=\"" + package_name + "\"",	// WHERE
			null, 						// Parameters to WHERE
			null, 						// GROUP BY
			null, 						// HAVING
			null, 						// ORDOR BY
			null  						// Max num of return rows
		);
 
		// Must check
		if (0 != cursor.getCount()) {
			cursor.moveToFirst(); // Move to first row
		} else {
			return null;
		}
		return cursor;
	}
	
	private synchronized long put(String package_name, 
			String app_name, String activity_name) {
		ContentValues args = new ContentValues();
		args.put("package_name", package_name);
		args.put("app_name", app_name);		
		args.put("activity_name", activity_name);
		args.put("image", "");
		args.put("module", "");
		args.put("size", "");
		
		long id = db.insert("items", null, args);
		Log.i(TAG, "Insert to: " + id + ", " + activity_name);

		return id;
    }

	private synchronized long put(String package_name, 
			String app_name, String activity_name, String module_name, String image) {
		ContentValues args = new ContentValues();
		args.put("package_name", package_name);
		args.put("app_name", app_name);		
		args.put("activity_name", activity_name);
		args.put("image", image);
		args.put("module", module_name);
		args.put("size", "");
		
		long id = db.insert("items", null, args);
		Log.i(TAG, "Insert to: " + id + ", " + activity_name + " (Module: " + module_name + ")");

		return id;
    }
	
	/**
	 * checkItemExist
	 * @param url
	 * @return true: exist in DB
	 */
	public boolean checkItemNoExist(String package_name) {
		if (db.isOpen())Log.i(TAG, "db is open");
		else Log.i(TAG,"db is close");
		return get(package_name) == null ? true : false;
	}
	
	/**
	 * Get all tiles except modules
	 * @return
	 */
	private synchronized Cursor getAll() {
		Cursor cursor = db.query(true,
				"items",
				new String[] {"_ID", 
								"package_name", 
								"app_name",
								"activity_name",
								"module",
								"image",
								"size"},
								"module=\"" + "" + "\"",	// WHERE
				null, 						// Parameters to WHERE
				null, 						// GROUP BY
				null, 						// HAVING
				null, 						// ORDOR BY
				null  						// Max num of return rows
			);
	 
			// Must check
			if (0 != cursor.getCount()) {
				cursor.moveToFirst(); // Move to first row
			}
			return cursor;	
	}
	
	public String getJSON() {
		Cursor items = getAll();
		String json_code;
		String packageName, appName, activityName, moduleName, image, size;
		
		if (items.getCount() == 0) {
			return null;
		}
		
		json_code = "[";
		
		for (int i = 0; i < items.getCount(); i++) {
			packageName = items.getString(1);
			appName = items.getString(2);
			activityName = items.getString(3);
			moduleName = items.getString(4);
			image = items.getString(5);
			size = items.getString(6);
			
			json_code += "{";
			json_code += "bgcolor: \"#97c02c\",";
			json_code += "package: \"" + packageName + "\","; 		// package name
			json_code += "app: \"" + appName + "\",";				// application name
			json_code += "activity: \"" + activityName + "\",";		// activity name
			json_code += "module: \"" + moduleName + "\",";			// module name
			json_code += "image: \"" + image + "\",";				// image URL
			json_code += "size: \"" + size + "\"";					// tile size
			
			json_code += "}";
			if (i < (items.getCount() - 1))
				json_code += ",";
			items.moveToNext();
		}
		
		json_code += "]";
		
		return json_code;
	}

	public synchronized String getPackageByModule(String moduleName) {
		Cursor cursor = db.query(true,
				"items",
				new String[] {"_ID", 
								"package_name", 
								"app_name",
								"activity_name",
								"module",
								"image",
								"size"},
				"module=\"" + moduleName + "\"",	// WHERE
				null, 						// Parameters to WHERE
				null, 						// GROUP BY
				null, 						// HAVING
				null, 						// ORDOR BY
				null  						// Max num of return rows
			);
	 
			// Must check
			if (0 != cursor.getCount()) {
				cursor.moveToFirst(); 		// Move to first row
				return cursor.getString(1); // package name
			}
			return null;
	}

	public synchronized String getActivityByModule(String moduleName) {
		Cursor cursor = db.query(true,
				"items",
				new String[] {"_ID", 
								"package_name", 
								"app_name",
								"activity_name",
								"module",
								"image",
								"size"},
								"module=\"" + moduleName + "\"",	// WHERE
				null, 						// Parameters to WHERE
				null, 						// GROUP BY
				null, 						// HAVING
				null, 						// ORDOR BY
				null  						// Max num of return rows
			);
	 
			// Must check
			if (0 != cursor.getCount()) {
				cursor.moveToFirst(); 		// Move to first row
				return cursor.getString(3); // activity name
			}
			return null;
	}

	public void reset() {
		this.removeAll();
	}

	private void removeAll() {
		int rows = db.delete(TABLE_NAME, null, null);
		Log.i(TAG, "Delete " + rows + " rows.");
	}

	public void setTileSize(String packageName, String activityName, String size) {
        ContentValues cv = new ContentValues();
        cv.put("size", size);

		int result = db.update(TABLE_NAME,
				cv,
				"package_name=\"" + packageName + "\"",
				null
			);		
		
		Log.i(TAG, packageName + ": setTileSize = " + size + ", result: " + result);
	}

}
