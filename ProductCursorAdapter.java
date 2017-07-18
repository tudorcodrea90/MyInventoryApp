package com.example.android.myinventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;


import com.example.android.myinventoryapp.data.ProductContract.ProductEntry;
/**
 * Created by Tudor Cristian on 18.07.2017.
 */

public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {

        TextView nameTextView = (TextView) view.findViewById(R.id.name_text_view);
        TextView priceTextView = (TextView) view.findViewById(R.id.price_text_view);
        TextView quantityTextView = (TextView) view.findViewById(R.id.quantity_text_view);

        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);

        final int quantityValue = cursor.getInt(cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY));
        final int idValue = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));

        final String productName = cursor.getString(nameColumnIndex);
        int productPrice = cursor.getInt(priceColumnIndex);
        int productQuantity = cursor.getInt(quantityColumnIndex);

        String productPriceString = Integer.toString(productPrice);
        String productQuantityString = Integer.toString(productQuantity);

        nameTextView.setText(productName);
        priceTextView.setText(productPriceString);
        quantityTextView.setText(productQuantityString);

        if (productPrice == 0 || TextUtils.isEmpty(productPriceString)) {
            priceTextView.setText(R.string.no_price);
        }

        final Button saleButton = (Button) view.findViewById(R.id.sale_button);
        saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues values = new ContentValues();
                String saleMessage;
                if (quantityValue > 0) {
                    int newQuantity;
                    newQuantity = quantityValue - 1;
                    values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, newQuantity);
                    Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, idValue);
                    context.getContentResolver().update(uri, values, null, null);
                    saleMessage = "You just sold one " + productName + "!";
                    Toast.makeText(context, saleMessage, Toast.LENGTH_SHORT).show();
                } else {
                    saleMessage = "You have no more: " + productName + ".";
                    Toast.makeText(context, saleMessage, Toast.LENGTH_SHORT).show();
                }
                context.getContentResolver().notifyChange(ProductEntry.CONTENT_URI, null);
            }
        });
    }
}