package com.example.android.myinventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.myinventoryapp.data.ProductContract.ProductEntry;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Tudor Cristian on 18.07.2017.
 */

public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.myinventoryapp.data.ProductContract.ProductEntry";
    private static final int IMAGE_REQUEST = 0;

    private Uri mCurrentProductUri;
    private EditText mNameEditText;
    private EditText mPriceEditText;
    private EditText mQuantityEditText;
    private EditText mQuantityChangeValueEditText;
    private boolean mProductHasChanged = false;

    private ImageView mProductImage;
    private Bitmap mBitmap;
    private Uri mUri;
    private boolean galleryPic = false;
    private String uriString;

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        if (mCurrentProductUri == null) {
            setTitle(getString(R.string.add_new_product));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.edit_product));
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mProductImage = (ImageView) findViewById(R.id.add_image);
        mQuantityEditText = (EditText) findViewById(R.id.edit_product_quantity);
        mQuantityChangeValueEditText = (EditText) findViewById(R.id.change_quantity);

        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mProductImage.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mQuantityChangeValueEditText.setOnTouchListener(mTouchListener);

        Button orderButton = (Button) findViewById(R.id.orderButton);
        orderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String subject = "Order - New Products";
                if (checkForName() && checkForQuantity()) {
                    String message = "New order:\n" + mNameEditText.getText().toString().trim();
                    message = message + "\n" + mQuantityEditText.getText().toString().trim();
                    Intent sendOrder = new Intent(Intent.ACTION_SENDTO);
                    sendOrder.setData(Uri.parse("mailto:"));
                    sendOrder.putExtra(Intent.EXTRA_SUBJECT, subject);
                    sendOrder.putExtra(Intent.EXTRA_TEXT, message);
                    if (sendOrder.resolveActivity(getPackageManager()) != null) {
                        startActivity(sendOrder);
                    }
                } else {
                    Toast.makeText(DetailActivity.this, "You need to insert product name and quantity to send order.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button plusButton = (Button) findViewById(R.id.plus_button);
        plusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String quantity = mQuantityEditText.getText().toString().trim();
                String quantityChanger = mQuantityChangeValueEditText.getText().toString().trim();
                if (!checkForQuantityChanger() || !checkForQuantity()) {
                    Toast.makeText(DetailActivity.this, "Please Insert both: quantity and quantity changer.", Toast.LENGTH_SHORT).show();
                } else {
                    int qChanger = Integer.parseInt(quantityChanger);
                    int q = Integer.parseInt(quantity);
                    q = q + qChanger;
                    String totalQuantity = String.valueOf(q);
                    mQuantityEditText.setText(totalQuantity);

                }
            }
        });

        final Button minusButton = (Button) findViewById(R.id.minus_button);
        minusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String quantity = mQuantityEditText.getText().toString().trim();
                String quantityChanger = mQuantityChangeValueEditText.getText().toString().trim();
                if (!checkForQuantityChanger() || !checkForQuantity()) {
                    Toast.makeText(DetailActivity.this, "Please Insert both: quantity and quantity changer.", Toast.LENGTH_SHORT).show();
                } else {
                    int qChanger = Integer.parseInt(quantityChanger);
                    int q = Integer.parseInt(quantity);
                    if (q > 0 && q >= qChanger) {
                        q = q - qChanger;
                        String totalQuantity = String.valueOf(q);
                        mQuantityEditText.setText(totalQuantity);
                    } else {
                        Toast.makeText(DetailActivity.this, "Quantity can not be less than 0.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        final Button addImageButton = (Button) findViewById(R.id.add_image_button);
        addImageButton.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                image();
            }
        });
    }

    /**
     * Get user input from editor and save product into database.
     */

    private void saveProduct() {
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String imageString = uriString;

        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(imageString)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_IMAGE, imageString);

        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }

        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        // Determine if this is a new or existing product by checking if mCurrentProductUri is null or not
        if (mCurrentProductUri == null) {
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, getString(R.string.product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.insert_product_successful), Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.update_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.update_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_product, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    private boolean checkForName() {
        return (mNameEditText.getText().toString().trim().length() != 0);
    }

    private boolean checkForQuantity() {
        return (mQuantityEditText.getText().toString().trim().length() != 0);
    }

    private boolean checkForQuantityChanger() {
        return (mQuantityChangeValueEditText.getText().toString().trim().length() != 0);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (!checkForName()) {
                    Toast.makeText(this, "Please insert product name.", Toast.LENGTH_SHORT).show();
                } else if (!checkForQuantityChanger()) {
                    Toast.makeText(this, "Please insert valid quantity changer.", Toast.LENGTH_SHORT).show();
                } else {
                    saveProduct();
                    finish();
                }
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.

                onOptionsItemSelected(item);

    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,       // Parent activity context
                mCurrentProductUri,         // Provider content URI to query
                projection,                 // Columns to include in the resulting Cursor
                null,                       // No selection clause
                null,                       // No selection arguments
                null);                      // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {

            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int imageColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE);

            String name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String img = cursor.getString(imageColumnIndex);
            if (img != null) {
                Uri imgUri = Uri.parse(img);
                mProductImage.setImageURI(imgUri);
            }

            mNameEditText.setText(name);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {

        if (mCurrentProductUri != null) {

            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            if (rowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.delete_product_failed), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.delete_product_successful), Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }

    private void image() {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCodes, Intent resultData) {

        if (resultCodes == Activity.RESULT_OK) {

            if (resultData != null) {
                mUri = resultData.getData();
                mBitmap = getBitmapFromCurrentItemURI(mUri);
                mProductImage.setImageBitmap(mBitmap);
                uriString = getShareableImageUri().toString();
                galleryPic = true;
            }
        }
    }

    private Bitmap getBitmapFromCurrentItemURI(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = null;
            if (parcelFileDescriptor != null) {
                fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            }
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            return image;
        } catch (Exception e) {
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Uri getShareableImageUri() {
        Uri imagesUri;

        if (galleryPic) {
            String filename = PathFinder();
            savingInFile(getCacheDir(), filename, mBitmap, Bitmap.CompressFormat.JPEG, 100);
            File imagesFile = new File(getCacheDir(), filename);

            imagesUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, imagesFile);

        } else {
            imagesUri = mUri;
        }

        return imagesUri;
    }

    private String PathFinder() {
        Cursor returnCursor =
                getContentResolver().query(mUri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);

        if (returnCursor != null) {
            returnCursor.moveToFirst();
        }
        String fileNames = null;
        if (returnCursor != null) {
            fileNames = returnCursor.getString(returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        }
        if (returnCursor != null) {
            returnCursor.close();
        }
        return fileNames;
    }


    private boolean savingInFile(File dir, String fileName, Bitmap bm,
                                 Bitmap.CompressFormat format, int quality) {
        File imagesFile = new File(dir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imagesFile);
            bm.compress(format, quality, fos);
            fos.close();

            return true;
        } catch (IOException e) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }
}