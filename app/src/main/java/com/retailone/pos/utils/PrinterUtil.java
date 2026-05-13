package com.retailone.pos.utils;


import static android.provider.MediaStore.Images.Media.getBitmap;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.common.apiutil.CommonException;
import com.common.apiutil.printer.NewUsbThermalPrinter;
import com.common.apiutil.printer.UsbThermalPrinter;
//import com.common.apiutil.util.SDKUtil;
import com.common.apiutil.util.StringUtil;
import com.common.apiutil.util.SystemUtil;
import com.retailone.pos.R;
import com.retailone.pos.localstorage.SharedPreference.LocalizationHelper;
import com.retailone.pos.models.LocalizationModel.LocalizationData;
import com.retailone.pos.models.PosSalesDetailsModel.PosSalesDetails;
import com.retailone.pos.models.PosSalesDetailsModel.SalesItem;
import com.retailone.pos.models.PrinterModel.ReceiptData;
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnSaleRes;
import com.retailone.pos.models.ReturnSalesItemModel.ReturnSaleResModel.ReturnedItem;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;

public class PrinterUtil {

    private String printVersion;
    private final int NOPAPER = 3;
    private final int LOWBATTERY = 4;
    private final int PRINTVERSION = 5;
    private final int PRINTBARCODE = 6;
    private final int PRINTQRCODE = 7;
    private final int PRINTPAPERWALK = 8;
    private final int PRINTCONTENT = 9;
    private final int CANCELPROMPT = 10;
    private final int PRINTERR = 11;
    private final int OVERHEAT = 12;
    private final int MAKER = 13;
    private final int PRINTPICTURE = 14;
    private final int NOBLACKBLOCK = 15;
    private final int PRINTSHORTCONTENT = 16;
    private final int PRINTLONGPICTURE = 17;
    private final int PRINTLONGTEXT = 18;
    private final int PRINTBLACK = 19;
    private final int PRINTCOLUMNS = 20;

    private final int TSIZE20 = 36;
    private final int TSIZE22 = 34;
    private final int TSIZE24 = 31;
    private final int TSIZE26 = 29;
    private final int TSIZE28 = 27;
    private final int TSIZE30 = 26;
    private final int TSIZE32 = 23;


    private NewUsbThermalPrinter mUsbThermalPrinter;
    private ProgressDialog dialog;
    private ProgressDialog progressDialog;
    private MyHandler handler;
    private boolean LowBattery = false;
    private Context context;
    private int deviceType;
    private boolean nopaper = false;
    private boolean looseOil = false;
    ArrayList<MyItem> itemx ;

    private String Result;

    private String currency;
    private String zone;
    String printType = "";
    LocalizationData localizationData;

    String productname = "";

    int numberOfItems = 0;


    PosSalesDetails posSalesDetails;
    ReturnSaleRes returnSaleRes;
    ReceiptData receipt_data;

    public PrinterUtil(Context context) {
        this.context = context;
        mUsbThermalPrinter = new NewUsbThermalPrinter(context);
        deviceType = SystemUtil.getDeviceType();
        handler = new MyHandler();
        currency =  new LocalizationHelper(context).getLocalizationData().getCurrency();
        zone =  new LocalizationHelper(context).getLocalizationData().getTimezone();

        /// SDKUtil.getInstance(context).initSDK();
        initializePrinter();
    }

    private void initializePrinter() {
        dialog = new ProgressDialog(context);
        dialog.setTitle("Initializing Printer");
        dialog.setMessage("Please wait...");
        dialog.setCancelable(false);
        dialog.show();

        new Thread(() -> {
            try {
                mUsbThermalPrinter.start(0);
                mUsbThermalPrinter.reset();
            } catch (CommonException e) {
                e.printStackTrace();
            } finally {
                dialog.dismiss();
            }
        }).start();
    }

    public void printReceiptData(PosSalesDetails _posSalesDetails) {
        printType = "SALE";

        //  receipt_data = receiptData;
        posSalesDetails = _posSalesDetails;

        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
                // handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));

                handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                //Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
            }
        }

    }

    public void printReturnReceiptData( ReturnSaleRes _returnSaleRes) {
        printType = "RETURN";

        // Toast.makeText(context, "return print 2", Toast.LENGTH_LONG).show();


        returnSaleRes = _returnSaleRes;

        if (LowBattery) {
            handler.sendMessage(handler.obtainMessage(LOWBATTERY, 1, 0, null));
        } else {
            if (!nopaper) {
                // handler.sendMessage(handler.obtainMessage(PRINTPICTURE, 1, 0, null));

                handler.sendMessage(handler.obtainMessage(PRINTCONTENT, 1, 0, null));
                //Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
            }
        }

    }

//    public void printReceipt(@NotNull PosSalesDetails posSaleData) {
//
//        posSalesDetails = posSaleData;
//
//        if (LowBattery) {
//            handler.sendMessage(handler.obtainMessage(4, 1, 0, null));
//        } else {
//            if (!nopaper) {
//                handler.sendMessage(handler.obtainMessage(9, 1, 0, null));
//                Toast.makeText(context, "Paper Available", Toast.LENGTH_LONG).show();
//
//            } else {
//                Toast.makeText(context, "No paper detected", Toast.LENGTH_LONG).show();
//            }
//        }
//
//    }

    private class MyHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case NOPAPER:
                    //NOPAPER
                    noPaperDlg();
                    break;
                case LOWBATTERY:
                    //LOWBATTERY
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
                    alertDialog.setTitle("Operation Result");
                    alertDialog.setMessage("Low Battery");
                    alertDialog.setPositiveButton("OK", (dialog, which) -> {
                    });
                    alertDialog.show();
                    break;
                case PRINTCONTENT:
                    //Toast.makeText(context, "printContent", Toast.LENGTH_LONG).show();
                    new contentPrintThread().start();
                    break;

                case PRINTPICTURE:
                    new printPicture().start();
                    break;

                case NOBLACKBLOCK:
                    //NOBLACKBLOCK
                    Toast.makeText(context, R.string.maker_not_find, Toast.LENGTH_SHORT).show();
                    break;
//                case 10:
//                    //CANCELPROMPT
//                    if (progressDialog != null && !UsbPrinterActivity.this.isFinishing()) {
//                        progressDialog.dismiss();
//                        progressDialog = null;
//                    }
//                    break;
                default:
                    // Toast.makeText(context, "Print Error!", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private void noPaperDlg() {
        AlertDialog.Builder dlg = new AlertDialog.Builder(context);
        dlg.setTitle("No Paper");
        dlg.setMessage("Please load paper and try again.");
        dlg.setCancelable(false);
        dlg.setPositiveButton("OK", (dialog, which) -> {
        });
        dlg.show();
    }



    private class printPicture extends Thread {

        public void run() {
            super.run();
            try {
                mUsbThermalPrinter.reset();
                mUsbThermalPrinter.setGray(3);
                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
                //File file = new File(picturePath);
                //if (file.exists()) {
                mUsbThermalPrinter.printLogo(drawableToBitmap(ContextCompat.getDrawable(context,R.drawable.mlogo)), false);
                mUsbThermalPrinter.walkPaper(20);
				/*} else {
					runOnUiThread(new Runnable() {


						public void run() {
							Toast.makeText(UsbPrinterActivity.this, getString(R.string.not_find_picture),
									Toast.LENGTH_LONG).show();
						}
					});
				}*/
            } catch (Exception e) {
                e.printStackTrace();
                Result = e.toString();
                if (Result.contains("NoPaperException")) {
                    nopaper = true;
                } else if (Result.contains("OverHeatException")) {
                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
                } else {
                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
                }
            } finally {
                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
                if (nopaper) {
                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                    nopaper = false;
                    return;
                }
            }
        }
    }


    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            // If the drawable is a BitmapDrawable, just return its bitmap
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // Otherwise, create a new bitmap and draw the drawable on it
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            // Ensure dimensions are valid, otherwise, use default dimensions
            width = width > 0 ? width : 1;
            height = height > 0 ? height : 1;

            // Create a bitmap with the specified width and height
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // Create a canvas to draw on the bitmap
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        return bitmap;
    }


    private class contentPrintThread extends Thread {
        public void run() {
            super.run();
            if(printType.equals("SALE")){

                printSaleType(posSalesDetails);

            }else if(printType.equals("RETURN")){

                printReturnType(returnSaleRes);


            }
        }
    }

    private void printReturnType(ReturnSaleRes details) {
        try {

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString(context.getString(R.string.print_internal_invoice));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(32);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);

            mUsbThermalPrinter.addString(details.getData().getStore().getStore_name().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString(details.getData().getStore().getAddress().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.addString(context.getString(R.string.print_credit_note));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);
           /* mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.addString("VAT NO:" + padLeft(details.getData().getVat_no(), TSIZE22-7));
            mUsbThermalPrinter.printString();*/
            mUsbThermalPrinter.setGray(6);
            String tinLabel = context.getString(R.string.print_tin_n0_label);
            mUsbThermalPrinter.addString(tinLabel + padLeft(details.getData().getTpin_no(), TSIZE22 - tinLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setGray(6);
            //mUsbThermalPrinter.addString("DATE:" + padLeft(DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getReturned_date(),zone), TSIZE22-7));
            ///mUsbThermalPrinter.addString("DATE:" + padLeft(DateTimeFormatting.Companion.formatReturndate(details.getData().getReturned_date(),zone), TSIZE22-7));
            String dateLabel = context.getString(R.string.print_date_label);
            mUsbThermalPrinter.addString(dateLabel + padLeft((details.getData().getReturned_date()), TSIZE22 - dateLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.setGray(6);
            String sdcLabel = context.getString(R.string.print_sdc_receipt_label);
            mUsbThermalPrinter.addString(sdcLabel + padLeft(details.getData().getReturned_invoice_id(), TSIZE22 - sdcLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setGray(6);
            String buyerNameLabel = context.getString(R.string.print_buyers_name_label);
            mUsbThermalPrinter.addString(buyerNameLabel + padLeft(details.getData() != null && details.getData().getCustomer_name() != null ? details.getData().getCustomer_name() : "", TSIZE22 - buyerNameLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setGray(6);
            String buyerTinLabel = context.getString(R.string.print_buyers_tin_label);
            mUsbThermalPrinter.addString(buyerTinLabel + padLeft(details.getData().getBuyers_tpin(), TSIZE22 - buyerTinLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString(context.getString(R.string.print_description));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString(context.getString(R.string.print_qty_rate_amount));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            for (ReturnedItem item : details.getData().getReturned_items()){
                productname = item.getProduct_name();
                if (productname.toLowerCase().startsWith("bulk oil")){

                }
                mUsbThermalPrinter.setTextSize(24);
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.addString(item.getProduct_name());
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setBold(false);

                mUsbThermalPrinter.setTextSize(22);
                String qtyRate = FunUtils.INSTANCE.DtoString(item.getReturn_quantity()) + " X " + FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getRetail_price()));
                String amount = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_returned_amount()));
                String Line2 = qtyRate + padLeft(amount, TSIZE22 - (qtyRate.length()));
                mUsbThermalPrinter.addString(Line2);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }

            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setBold(true);
            String totalLabel = context.getString(R.string.print_total_format, currency);
            mUsbThermalPrinter.addString(totalLabel + padLeft(FunUtils.INSTANCE.formatPrintPrice(Double.toString(details.getData().getTotal())), TSIZE26 - totalLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.setTextSize(22);
            String itemsLabel = context.getString(R.string.print_items_label);
            mUsbThermalPrinter.addString(itemsLabel + padLeft(Integer.toString(details.getData().getReturned_items().size()), TSIZE22 - itemsLabel.length()));
            mUsbThermalPrinter.printString();

            String taxExLabel = context.getString(R.string.print_tax_ex_label);
            mUsbThermalPrinter.addString(taxExLabel + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex()), TSIZE22 - taxExLabel.length()));
            mUsbThermalPrinter.printString();
            String taxVatLabel = context.getString(R.string.print_tax_vat_label);
            mUsbThermalPrinter.addString(taxVatLabel + padLeft(details.getData().getTax() + "%", TSIZE22 - taxVatLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(true);
            String totalVatLabel = context.getString(R.string.print_total_vat_label);
            mUsbThermalPrinter.addString(totalVatLabel + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()), TSIZE22 - totalVatLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setTextSize(26);
            String payableLabel = context.getString(R.string.print_payable_label);
            mUsbThermalPrinter.addString(payableLabel + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()), TSIZE26 - payableLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(20);
//        String cashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
//                FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
//        mUsbThermalPrinter.addString("CASH:" + padLeft(cashAmt, TSIZE20 - 5));
//        mUsbThermalPrinter.printString();
//        String nonCashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
//                details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
//                FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
//        mUsbThermalPrinter.addString("M-MONEY:" + padLeft(nonCashAmt, TSIZE20 - 8));
//        mUsbThermalPrinter.printString();
//        mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

//            mUsbThermalPrinter.setTextSize(24);
//            mUsbThermalPrinter.addString("Sign/Stamp_____________________");
//            mUsbThermalPrinter.printString();
//            mUsbThermalPrinter.walkPaper(4);

            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.addString(context.getString(R.string.print_end_label));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(15);
            mUsbThermalPrinter.reset();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }
    }

/// for debug invoice print
   /* private void printReturnType(ReturnSaleRes   details) {

        try {
            String printStr = "";

            Log.d("Line 1", "* START OF LEGEAL RECEIPT *");
            printStr = details.getData().getStore().getStore_name().toString().toUpperCase();
            Log.d("Line 1", printStr);


            printStr = details.getData().getStore().getAddress().toString().toUpperCase();
            Log.d("Line 1", printStr);

            printStr = "VAT NO:" + padLeft(details.getData().getVat_no(), TSIZE22-7);
            Log.d("Line 1", printStr);
            printStr = "TPIN N0:" + padLeft(details.getData().getTpin_no(), TSIZE22-8);
            Log.d("Line 1", printStr);
            //printStr = "DATE:" + padLeft(DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getReturned_date(),zone), TSIZE22-7);
            printStr = "DATE:" + padLeft(details.getData().getReturned_date(), TSIZE22-7);
            Log.d("Line 1", printStr);

            printStr = "RECEIPT NO:" + padLeft(details.getData().getReturned_invoice_id(), TSIZE22-11);
            Log.d("Line 1", printStr);
            printStr = "BUYER’S NAME:" + padLeft(details.getData() != null && details.getData().getCustomer_name() != null ? details.getData().getCustomer_name() : "", TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "BUYER’S TPIN:" + padLeft(details.getData().getBuyers_tpin(), TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "Description";
            Log.d("Line 1", printStr);
            printStr = "Qty X Rate                 Amount";
            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            for ( ReturnedItem item : details.getData().getReturned_items()){
                printStr = item.getProduct_name();
                Log.d("Line 1", printStr);

                //String qtyRate = FunUtils.INSTANCE.DtoString(item.getQuantity()) + " X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String qtyRate = FunUtils.INSTANCE.DtoString(item.getReturn_quantity())+" X " + FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getRetail_price()));
                String amount = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_returned_amount()));
                String Line2 = qtyRate + padLeft(amount, TSIZE22 - (qtyRate.length()));
                printStr = Line2;
                Log.d("Line 1", printStr);
            }

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "TOTAL (" + currency + ") :" + padLeft(FunUtils.INSTANCE.formatPrintPrice(Double.toString(details.getData().getTotal())), TSIZE26 - (("TOTAL (" + currency + ") :").length()));
            Log.d("Line 1", printStr);


            printStr = "Items:" + padLeft(Integer.toString(details.getData().getReturned_items().size()), TSIZE22 - 6 );
            Log.d("Line 1", printStr);
            printStr = "TAX EX:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex()), TSIZE22 - 7 );
            Log.d("Line 1", printStr);
            printStr = "TAX VAT @" + padLeft(details.getData().getTax()+"%", TSIZE22 - 9 );
            Log.d("Line 1", printStr);
            printStr = "TOTAL VAT:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()), TSIZE22 - 10 );
            Log.d("Line 1", printStr);
            printStr = "----------------------------------";
            Log.d("Line 1", printStr);
            printStr = "PAYABLE:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()), TSIZE26-8);
            Log.d("Line 1", printStr);



//            String cashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
//                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
//            printStr = "CASH:" + padLeft(cashAmt, TSIZE20 - 5);
//            Log.d("Line 1", printStr);
//            String nonCashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
//                    details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
//                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
//            printStr = "M-MONEY:" + padLeft(nonCashAmt, TSIZE20 - 8);
//            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "Sign/Stamp_________";
            Log.d("Line 1", printStr);

            printStr = "* END OF LEGEAL RECEIPT *";
            Log.d("Line 1", printStr);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }

    }*/


    public static String padRight(String s, int n) {
        return String.format("%-" + n + "s", s);
    }

    public static String padLeft(String s, int n) {
        return String.format("%" + n + "s", s);
    }

    /// for debug invoice print

    /*private void printSaleType(PosSalesDetails details) {

        try {
            String printStr = "";

            Log.d("Line 1", "* START OF LEGEAL RECEIPT *");
            printStr = details.getData().getStore().getStore_name().toString().toUpperCase();
            Log.d("Line 1", printStr);


            printStr = details.getData().getStore().getAddress().toString().toUpperCase();
            Log.d("Line 1", printStr);

            printStr = "VAT NO:" + padLeft(details.getData().getVat_no(), TSIZE22-7);
            Log.d("Line 1", printStr);
            printStr = "TPIN N0:" + padLeft(details.getData().getVat_no(), TSIZE22-8);
            Log.d("Line 1", printStr);
            printStr = "DATE:" + padLeft(DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getPurchase_date_time(),zone), TSIZE22-7);
            Log.d("Line 1", printStr);

            printStr = "RECEIPT NO:" + padLeft(details.getData().getInvoice_id(), TSIZE22-11);
            Log.d("Line 1", printStr);
            printStr = "BUYER’S NAME:" + padLeft(details.getData() != null && details.getData().getCustomer_name() != null ? details.getData().getCustomer_name() : "", TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "BUYER’S TPIN:" + padLeft(details.getData().getBuyers_tpin(), TSIZE22-13);
            Log.d("Line 1", printStr);

            printStr = "Description";
            Log.d("Line 1", printStr);
            printStr = "Qty X Rate                 Amount";
            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            for (SalesItem item : details.getData().getSalesItem()){
                printStr = item.getProduct_name();
                Log.d("Line 1", printStr);

                //String qtyRate = FunUtils.INSTANCE.DtoString(item.getQuantity()) + " X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String qtyRate = FunUtils.INSTANCE.DtoString(Double.parseDouble(item.getQuantity()))+" (" + item.getUom()+ ") X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String amount = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_amount()));
                String Line2 = qtyRate + padLeft(amount, TSIZE22 - (qtyRate.length()));
                printStr = Line2;
                Log.d("Line 1", printStr);
            }

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "TOTAL (" + currency + ") :" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getSub_total()), TSIZE26 - (("TOTAL (" + currency + ") :").length()));
            Log.d("Line 1", printStr);


            printStr = "Items:" + padLeft(Integer.toString(details.getData().getSalesItem().size()), TSIZE22 - 6 );
            Log.d("Line 1", printStr);
            printStr = "TAX EX:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex()), TSIZE22 - 7 );
            Log.d("Line 1", printStr);
            printStr = "TAX VAT @" + padLeft(details.getData().getTax()+"%", TSIZE22 - 9 );
            Log.d("Line 1", printStr);
            printStr = "TOTAL VAT:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()), TSIZE22 - 10 );
            Log.d("Line 1", printStr);
            printStr = "----------------------------------";
            Log.d("Line 1", printStr);
            printStr = "PAYABLE:" + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()), TSIZE26-8);
            Log.d("Line 1", printStr);



            String cashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
            printStr = "CASH:" + padLeft(cashAmt, TSIZE20 - 5);
            Log.d("Line 1", printStr);
            String nonCashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
                    details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
            printStr = "M-MONEY:" + padLeft(nonCashAmt, TSIZE20 - 8);
            Log.d("Line 1", printStr);

            printStr = "----------------------------------";
            Log.d("Line 1", printStr);


            printStr = "Sign/Stamp_________";
            Log.d("Line 1", printStr);

            printStr = "* END OF LEGEAL RECEIPT *";
            Log.d("Line 1", printStr);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }

    }*/


    private void printSaleType(PosSalesDetails details) {

        try {
            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString(context.getString(R.string.print_internal_invoice));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(3);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(32);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);

            mUsbThermalPrinter.addString(details.getData().getStore().getStore_name().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString(details.getData().getStore().getAddress().toString().toUpperCase());
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.reset();
            mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
            mUsbThermalPrinter.setTextSize(22);


//            mUsbThermalPrinter.setGray(6);
//            mUsbThermalPrinter.addString("VAT NO:" + padLeft(details.getData().getVat_no(), TSIZE22-7));
//            mUsbThermalPrinter.printString();
            String tinLabelStr = context.getString(R.string.print_tin_n0_label);
            mUsbThermalPrinter.addString(tinLabelStr + padLeft(details.getData().getTpin_no(), TSIZE22 - tinLabelStr.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);
            String dateLabelStr = context.getString(R.string.print_date_label);
            mUsbThermalPrinter.addString(dateLabelStr + padLeft(DateTimeFormatting.Companion.formatSaleReturndate(details.getData().getPurchase_date_time(), zone), TSIZE22 - dateLabelStr.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.setGray(6);
            String sdcLabel = context.getString(R.string.print_sdc_receipt_label);
            String sdcValue = details.getData().getInvoice_id() != null && !details.getData().getInvoice_id().isEmpty()
                    ? details.getData().getInvoice_id() : "N/A";
            mUsbThermalPrinter.addString(sdcLabel + padLeft(sdcValue, TSIZE22 - sdcLabel.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setGray(6);
            String nameLabel = context.getString(R.string.print_buyers_name_label);
            mUsbThermalPrinter.addString(nameLabel + padLeft(
                    details.getData() != null && details.getData().getCustomer_name() != null && !details.getData().getCustomer_name().isEmpty()
                            ? details.getData().getCustomer_name() : "N/A",
                    TSIZE22 - nameLabel.length()));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.setGray(6);

            String tinLabel = context.getString(R.string.print_buyers_tin_label);
            mUsbThermalPrinter.addString(tinLabel + padLeft(
                    details.getData().getBuyers_tpin() != null && !details.getData().getBuyers_tpin().isEmpty()
                            ? details.getData().getBuyers_tpin() : "N/A",
                    TSIZE22 - tinLabel.length()));
            mUsbThermalPrinter.printString();

            String mobLabel = context.getString(R.string.print_buyers_mobile_label);
            mUsbThermalPrinter.addString(mobLabel + padLeft(
                    details.getData().getCustomer_mob_no() != null && !details.getData().getCustomer_mob_no().isEmpty()
                            ? details.getData().getCustomer_mob_no() : "N/A",
                    TSIZE22 - mobLabel.length()));
            mUsbThermalPrinter.printString();

            String vatLabel = context.getString(R.string.print_vat_no_label);
            mUsbThermalPrinter.addString(vatLabel + padLeft(
                    details.getData().getVat_no() != null && !details.getData().getVat_no().isEmpty()
                            ? details.getData().getVat_no() : "N/A",
                    TSIZE22 - vatLabel.length()));
            mUsbThermalPrinter.printString();




            if (details.getData() != null
                    && details.getData().getCustomer_address() != null
                    && !details.getData().getCustomer_address().isEmpty()) {

                String addressLabel = context.getString(R.string.print_buyers_address_label);
                String addressValue = details.getData().getCustomer_address();

                // Check if label + address fits within the line width
                if ((addressLabel.length() + addressValue.length()) <= TSIZE22) {
                    // Short address — print inline on same line
                    mUsbThermalPrinter.addString(
                            addressLabel + padLeft(addressValue, TSIZE22 - addressLabel.length())
                    );
                    mUsbThermalPrinter.printString();
                } else {
                    // Long address — label on first line, address on next line centered
                    mUsbThermalPrinter.addString(addressLabel);
                    mUsbThermalPrinter.printString();

                    mUsbThermalPrinter.setAlgin(1);
                    mUsbThermalPrinter.addString(addressValue);
                    mUsbThermalPrinter.printString();
                    mUsbThermalPrinter.setAlgin(0);
                }

            } else {

                String addressLabel = context.getString(R.string.print_buyers_address_label);
                mUsbThermalPrinter.addString(addressLabel + padLeft("N/A", TSIZE22 - addressLabel.length()));
                mUsbThermalPrinter.printString();

            }




            mUsbThermalPrinter.walkPaper(2);

            mUsbThermalPrinter.setGray(6);
            mUsbThermalPrinter.setBold(true);
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString(context.getString(R.string.print_description));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.addString(context.getString(R.string.print_qty_rate_amount));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            for (SalesItem item : details.getData().getSalesItem()){
                productname = item.getProduct_name();
                numberOfItems++;
                if (productname.toLowerCase().startsWith("bulk oil") ){
                    looseOil = true;
                }else {
                    looseOil = false;
                }
                mUsbThermalPrinter.setTextSize(24);
                mUsbThermalPrinter.setGray(6);
                mUsbThermalPrinter.setBold(true);
                mUsbThermalPrinter.addString(item.getProduct_name());
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.setBold(false);

                mUsbThermalPrinter.setTextSize(22);
                String qtyRate = FunUtils.INSTANCE.DtoString(Double.parseDouble(item.getQuantity())) + " (" + item.getUom()+ ") X " + FunUtils.INSTANCE.formatPrintPrice(item.getRetail_price());
                String amount = FunUtils.INSTANCE.formatPrintPrice(Double.toString(item.getTotal_amount()));
                String Line2 = qtyRate + padLeft(amount, TSIZE22 - (qtyRate.length()));
                mUsbThermalPrinter.addString(Line2);
                mUsbThermalPrinter.printString();
                mUsbThermalPrinter.walkPaper(1);
            }
            mUsbThermalPrinter.setTextSize(22);
            String sptdiscount = context.getString(R.string.print_on_spot_discount_label, details.getData().getSpot_discount_percentage());
            mUsbThermalPrinter.addString(sptdiscount + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getSpot_discount_amount()), TSIZE22 - sptdiscount.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setTextSize(26);
            mUsbThermalPrinter.setBold(true);
            String totalLabelStr = context.getString(R.string.print_total_format, currency);
            mUsbThermalPrinter.addString(totalLabelStr + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getSub_total()), TSIZE26 - totalLabelStr.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.walkPaper(2);


            mUsbThermalPrinter.setTextSize(22);
            String itemsLabelStr = context.getString(R.string.print_items_label);
            mUsbThermalPrinter.addString(itemsLabelStr + padLeft(Integer.toString(details.getData().getSalesItem().size()), TSIZE22 - itemsLabelStr.length()));
            mUsbThermalPrinter.printString();
            String ivaExLabel = context.getString(R.string.print_iva_ex_label);
            mUsbThermalPrinter.addString(ivaExLabel + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_ex()), TSIZE22 - ivaExLabel.length()));
            mUsbThermalPrinter.printString();
            String ivaVatLabel = context.getString(R.string.print_iva_vat_label);
            if (looseOil && numberOfItems == 1){
                mUsbThermalPrinter.addString(ivaVatLabel + padLeft(context.getString(R.string.print_inc_16), TSIZE22 - ivaVatLabel.length() ));
            }else {
                mUsbThermalPrinter.addString(ivaVatLabel + padLeft(details.getData().getTax()+"%", TSIZE22 - ivaVatLabel.length() ));
            }
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setBold(true);
            String totalIvaLabel = context.getString(R.string.print_total_iva_label);
            mUsbThermalPrinter.addString(totalIvaLabel + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getTax_amount()), TSIZE22 - totalIvaLabel.length() ));
            mUsbThermalPrinter.printString();

            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);
            mUsbThermalPrinter.setTextSize(26);
            String payableLabelStr = context.getString(R.string.print_payable_label);
            mUsbThermalPrinter.addString(payableLabelStr + padLeft(FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()), TSIZE26 - payableLabelStr.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(1);

            mUsbThermalPrinter.setBold(false);
            mUsbThermalPrinter.setTextSize(20);
            String cashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CASH") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
            String cashLabelStr = context.getString(R.string.print_cash_label);
            mUsbThermalPrinter.addString(cashLabelStr + padLeft(cashAmt, TSIZE20 - cashLabelStr.length()));
            mUsbThermalPrinter.printString();
            String nonCashAmt = details.getData().getPayment_type().trim().toUpperCase().equals("CARD") ||
                    details.getData().getPayment_type().trim().toUpperCase().equals("M-MONEY") ?
                    FunUtils.INSTANCE.formatPrintPrice(details.getData().getGrand_total()) : "";
            String mMoneyLabelStr = context.getString(R.string.print_m_money_label);
            mUsbThermalPrinter.addString(mMoneyLabelStr + padLeft(nonCashAmt, TSIZE20 - mMoneyLabelStr.length()));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.setTextSize(22);
            mUsbThermalPrinter.addString("----------------------------------");
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(2);

//            mUsbThermalPrinter.setTextSize(24);
//            mUsbThermalPrinter.addString("Sign/Stamp_____________________");
//            mUsbThermalPrinter.printString();
//            mUsbThermalPrinter.walkPaper(4);

            mUsbThermalPrinter.setLeftIndent(1);
            mUsbThermalPrinter.setLineSpace(3);
            mUsbThermalPrinter.setTextSize(20);
            mUsbThermalPrinter.addString(context.getString(R.string.print_end_label));
            mUsbThermalPrinter.printString();
            mUsbThermalPrinter.walkPaper(15);
            mUsbThermalPrinter.reset();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.toString());

            Result = e.toString();
            if (Result.contains("NoPaperException")) {
                nopaper = true;
            } else if (Result.contains("OverHeatException")) {
                handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
            } else {
                handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
            }
        }finally {
            handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
            if (nopaper) {
                handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
                nopaper = false;
                return;
            }
        }

    }


//    private class contentPrintThread extends Thread {
//        public void run() {
//            super.run();
//            try {
//
////                val top: String,
////                        val storeName: String,
////                        val storeAddress: String,
////                        val vatInfo: String,
////                        val dateTime: String,
////                        val receiptInfo: String,
////                        val itemInfo:String,
////                        val totalPrice: String,
////                        val itemCount: String,
////                        val taxInfo: String,
////                        val totalVat: String,
////                        val payableAmount: String,
////                        val paymentModeInfo: String,
////                        val ejInfo: String,
////                        val bottom: String
//
//
//                ReceiptData receipt = receipt_data;
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setLeftIndent(1);
////                mUsbThermalPrinter.setLineSpace(3);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(3);
////                mUsbThermalPrinter.setBold(false);
////               // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTop());
////                mUsbThermalPrinter.printString();
////               mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getStoreName());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getStoreAddress());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getVatInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getDateTime());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getReceiptInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.walkPaper(1);
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getItemInfo());
////                mUsbThermalPrinter.setMonoSpace(true);
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTotalPrice());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(2);
////
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getItemCount());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTaxInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getTotalVat());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(22);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(true);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getPayableAmount());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
////
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getPaymentModeInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
//
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getEjInfo());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////                mUsbThermalPrinter.setTextSize(20);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setBold(false);
////                // mUsbThermalPrinter.setItalic(true);
////                mUsbThermalPrinter.addString(receipt.getBottom());
////                mUsbThermalPrinter.printString();
////                mUsbThermalPrinter.walkPaper(1);
//
//
//                mUsbThermalPrinter.reset();
//                //mUsbThermalPrinter.set(1);
//
//                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
//               // mUsbThermalPrinter.setTextSize(20);
//                mUsbThermalPrinter.setGray(6);
//                mUsbThermalPrinter.setBold(false);
//
//                // mUsbThermalPrinter.setItalic(true);
//
////                for  ( SalesItem item : receipt.getProduct_item()  ){
////
////                    String[] colsTestArr = {item.getProduct_name()+"-"+item.getDistribution_pack().getProduct_description()
////                            , item.getQuantity() +" X "+item.getRetail_price(), item.getTotal_amount()+"0000"};
////                    int[] colsWidthArr = {100, 50, 50};
////                    int[] colsAlign = {0, 1, 2}; // Example alignment values
////                    int colsTextSize = 20;
////
////                     mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
////
////                     mUsbThermalPrinter.printString();
////                     mUsbThermalPrinter.walkPaper(1);
////
////
////                }
//
//
////                   mUsbThermalPrinter.printColumnsString(new String[]{"Item 1","$2000.00",}, new int[]{6,2}, new int[]{0,0}, 20);
////                mUsbThermalPrinter.reset();
////                mUsbThermalPrinter.setBold(true);
////                mUsbThermalPrinter.setGray(6);
////                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_MIDDLE);
////
////                mUsbThermalPrinter.printColumnsString(new String[]{"Item 1123456","$200023.00",}, new int[]{6,3}, new int[]{0,0}, 22);
//
//                mUsbThermalPrinter.reset();
//                mUsbThermalPrinter.setBold(true);
//                mUsbThermalPrinter.setGray(6);
//                mUsbThermalPrinter.setAlgin(UsbThermalPrinter.ALGIN_LEFT);
//
//                mUsbThermalPrinter.setUnderline(false);
//                mUsbThermalPrinter.setAlgin(1);
//                mUsbThermalPrinter.printStringAndWalk(0,0,2);
//
//
//
//                mUsbThermalPrinter.printColumnsString(new String[]{"COLUMN Aaaaaaa","Culoaaa B","20000aaaa"}, new int[]{6,3,3}, new int[]{0,1,2}, 22);
//
//
//
//                mUsbThermalPrinter.walkPaper(1);
//
//                mUsbThermalPrinter.autoBreakSet(true);
//                mUsbThermalPrinter.addString("----------------------------");
//                mUsbThermalPrinter.printString();
//
//
////                String[] colsTestArr = {"Column1", "Column2", "Column3"};
//////                int[] colsWidthArr = {100, 200, 150};
//////                int[] colsAlign = {1, 2, 3}; // Example alignment values
//////                int colsTextSize = 12;
//
//                // Calling the method
//               // mUsbThermalPrinter.addColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
//               // mUsbThermalPrinter.printColumnsString(colsTestArr, colsWidthArr, colsAlign, colsTextSize);
//              //  mUsbThermalPrinter.addColumnsString(new String[]{"AAA","BBB","CCC"}, new int[]{6,6,6}, new int[]{0,0,0}, 16);
//
//               // mUsbThermalPrinter.printString();
//                mUsbThermalPrinter.walkPaper(1);
//
//
//
//                mUsbThermalPrinter.walkPaper(1);
//                mUsbThermalPrinter.reset();
//
//
//                Toast.makeText(context, "Printing initiated", Toast.LENGTH_LONG).show();
//
//            } catch (Exception e) {
//                e.printStackTrace();
//                System.out.println(e.toString());
//
//                Result = e.toString();
//                if (Result.contains("NoPaperException")) {
//                    nopaper = true;
//                } else if (Result.contains("OverHeatException")) {
//                    handler.sendMessage(handler.obtainMessage(OVERHEAT, 1, 0, null));
//                } else {
//                    handler.sendMessage(handler.obtainMessage(PRINTERR, 1, 0, null));
//                }
//            }finally {
//                handler.sendMessage(handler.obtainMessage(CANCELPROMPT, 1, 0, null));
//                if (nopaper) {
//                    handler.sendMessage(handler.obtainMessage(NOPAPER, 1, 0, null));
//                    nopaper = false;
//                    return;
//                }
//            }
//        }
//    }

    private String generateReceipt(ArrayList<MyItem> items) {
        StringBuilder printContent = new StringBuilder();
        printContent.append("\n             RetailOne\n")
                .append("---------------------------\n")
                .append("Date：2015-01-01 16:18:20\n")
                .append("invoice：12378945664\n")
                .append("id：1001000000000529142\n")
                .append("---------------------------\n")
                .append("    item        quantity   Price  total\n");

        double total = 0;
        for (SalesItem item : posSalesDetails.getData().getSalesItem()) {
            // double itemTotal = item.getQuantity() * item.getPrice();
            printContent.append(String.format("%-14s %8d %10.2f %10.2f\n", formatItemName(item.getProduct_name()), item.getQuantity(), item.getRetail_price(), item.getTotal_amount()));
            //total += itemTotal;
        }

        printContent.append("----------------------------\n")
                .append(String.format(" tax：%10.2f\n", 1000.00))
                .append("----------------------------\n")
                .append(String.format("paid：%10.2f\n", 10000.00))
                .append(String.format("tender：%10.2f\n", 1000.00))
                .append(String.format("paid：%10.2f\n", 9000.00))
                .append("----------------------------\n")
                .append(" Thanks for shopping with us\n")
                .append("tel :1111111111\n");

        return printContent.toString();
    }

    private static String formatItemName(String name) {
        if (name.length() > 14) {
            return name.substring(0, 14);
        } else {
            return String.format("%-14s", name);
        }
    }

    public void registerBatteryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction("android.intent.action.BATTERY_CAPACITY_EVENT");
        context.registerReceiver(printReceive, filter);
    }

    public void unregisterBatteryReceiver() {
        context.unregisterReceiver(printReceive);
    }

    private final BroadcastReceiver printReceive = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_NOT_CHARGING);
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);

                if (deviceType == StringUtil.DeviceModelEnum.TPS390.ordinal()) {
                    LowBattery = level * 5 <= scale;
                } else if (SystemUtil.getInternalModel().equals("M8")) {
                    LowBattery = level * 10 <= scale;
                } else {
                    LowBattery = status != BatteryManager.BATTERY_STATUS_CHARGING && level * 5 <= scale;
                }
            } else if (action.equals("android.intent.action.BATTERY_CAPACITY_EVENT")) {
                int status = intent.getIntExtra("action", 0);
                int level = intent.getIntExtra("level", 0);
                LowBattery = status == 0 && level < 1;
            }
        }
    };
}
