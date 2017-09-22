package com.example.android.pets.database;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.android.pets.database.PetContract.PetEntry;

/**
 * Created by nawaf on 2/10/17.
 */

public class PetProvider extends ContentProvider {
    /** Tag for the log messages */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();
    private PetDbHelper mPetDbHelper;
    /** URI matcher code for the content URI for the pets table */
    private static final int PETS = 1;
    /** URI matcher code for the content URI for a single pet in the pets table */
    private static final int PET_ID = 2;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.

        // (content://com.example.android.pets/pets)
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS,PETS);
        // (content://com.example.android.pets/pets/#)
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS+"/#",PET_ID);

    }

    @Override
    public boolean onCreate() {
        // TODO: Create and initialize a PetDbHelper object to gain access to the pets database.
        // Make sure the variable is a global variable, so it can be referenced from other
        // ContentProvider methods.
        mPetDbHelper = new PetDbHelper(getContext());

        return true;    }

    @Nullable
    @Override
    public Cursor query( @NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Get readable database
        SQLiteDatabase database = mPetDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor = database.query(PetEntry.TABLE_NAME,projection,selection,null,null,null,null);
                break;
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.pets/pets/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver() , uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(  @NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE+"/"+PetContract.CONTENT_AUTHORITY+PetContract.PATH_PETS;
            case PET_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE+"/"+PetContract.CONTENT_AUTHORITY+PetContract.PATH_PETS;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
    @Nullable
    @Override
    public Uri insert( @NonNull Uri uri, ContentValues values) {

         int match = sUriMatcher.match(uri);

        switch (match){
            case PETS:
                try {
                    return insertPet(uri, values);
                }catch (IllegalArgumentException e){
                    e.printStackTrace();
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();return null;
                }
                default: throw new IllegalArgumentException("Can't Insert: "+uri);
        }

    }
    private Uri insertPet(Uri uri , ContentValues values) throws IllegalArgumentException{
        // Check that the name is not null
        String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        // Check that the breed is not null
        String breed = values.getAsString(PetEntry.COLUMN_PET_BREED);
        if (breed == null) {
            throw new IllegalArgumentException("Pet requires a breed");
        }

        // Check that the weight is not null
        int weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
        if ( weight < 1) {
            throw new IllegalArgumentException("Pet requires a weight");
        }

        // TODO: Finish sanity checking the rest of the attributes in ContentValues



        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();

      long id =   database.insert(PetEntry.TABLE_NAME,null,values);

        getContext().getContentResolver().notifyChange(uri,null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        // No need for validation
        // Get writeable database
        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        int rowDeleted;
        switch (match) {
            case PETS:
                // Delete all rows that match the selection and selection args
                 rowDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
                database.execSQL("delete from sqlite_sequence where name='"+PetEntry.TABLE_NAME+"';");
                getContext().getContentResolver().notifyChange(uri,null);
                break;
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                getContext().getContentResolver().notifyChange(uri,null);

                rowDeleted = database.delete(PetEntry.TABLE_NAME, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
        if(rowDeleted != 0) getContext().getContentResolver().notifyChange(uri,null);

        return rowDeleted;
    }

    @Override
    public int update( @NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int match = sUriMatcher.match(uri);
        switch (match) {

            case PETS:
                return updatePet(uri, values, selection, selectionArgs);
            case PET_ID:
                selection = PetEntry._ID + "=?";
                selectionArgs = new String[1];
                selectionArgs[0] = String.valueOf(ContentUris.parseId(uri));
                try {
                    return updatePet(uri, values, selection, selectionArgs);
                } catch (IllegalArgumentException e) {
                    return 0;
                }

            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePet( Uri uri , ContentValues values  , String selection, String[] selectionArgs ){
        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // If the {@link PetEntry#COLUMN_PET_NAME} key is present,
        // check that the name value is not null.
        if (values.containsKey(PetEntry.COLUMN_PET_NAME)) {
            String name = values.getAsString(PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }

        // If the {@link PetEntry#COLUMN_PET_GENDER} key is present,
        // check that the gender value is valid.
        if (values.containsKey(PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetEntry.COLUMN_PET_GENDER);
            if (gender == null  ) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }

        // If the {@link PetEntry#COLUMN_PET_WEIGHT} key is present,
        // check that the weight value is valid.
        if (values.containsKey(PetEntry.COLUMN_PET_WEIGHT)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer weight = values.getAsInteger(PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        // No need to check the breed, any value is valid (including null).

        SQLiteDatabase database = mPetDbHelper.getWritableDatabase();
        int num = database.update(PetEntry.TABLE_NAME,values,selection,selectionArgs);
        if(num != 0) getContext().getContentResolver().notifyChange(uri,null);

        return num;

    }


}
