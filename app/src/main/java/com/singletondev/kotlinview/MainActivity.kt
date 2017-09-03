package com.singletondev.kotlinview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*

class MainActivity : AppCompatActivity() {

    val SELECT_PICTURE = 1
    lateinit var buttonView : Button
    lateinit var imageView : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = img_foto
        buttonView = btn_pickFoto

    }

    fun pickImage(v : View){
        var intent = Intent();
        intent.setType("image/*")
        intent.setAction(Intent.ACTION_GET_CONTENT)
        startActivityForResult(Intent.createChooser(intent,"Memilih Foto"),SELECT_PICTURE);
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        var  bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, data.data)
        Log.e("BitmapCountBefore",bitmap.byteCount.toString())
        when (resultCode){
            Activity.RESULT_OK -> when (requestCode) {
                SELECT_PICTURE -> reduceImageinLambda(data.data)
                    //imageView.setImageBitmap(reduceImageSizeMemory(data.data))
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun getRealPath_API19(context: Context, uri: Uri): String {
        var file = ""
        if (uri.host.contains("com.android.providers.media")) {
            val column = arrayOf(MediaStore.Images.Media.DATA)

            //pilih image dari recent file
            val idID = DocumentsContract.getDocumentId(uri)

            //pisahkah kolom, gunakan char array selanjutnya
            val id = idID.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]

            //terapkan kondisi persamaan ID
            val sel = MediaStore.Images.Media._ID + "=?"

            val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    column, sel, arrayOf(id), null)

            val colInd = cursor!!.getColumnIndex(column[0])
            if (cursor.moveToFirst()) {
                file = cursor.getString(colInd)
            }
            cursor.close()
            return file
        } else {
            return getRealPathFromUri(context, uri)
        }
    }

    fun getRealPathFromUri(context: Context, contentUri: Uri): String {
        var cursor: Cursor? = null
        try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val column_index = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(column_index)
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
    }

    /**
     * Reducing image size in memory different Compress image
     * The image is compressed when it is on disk (stored in a JPG, PNG, or similar format).
     * Once you load the image into memory,
     * it is no longer compressed and takes up as much memory as is necessary for all the pixels.
     * return value Bitmap
     */

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun reduceImageSizeMemory(uri : Uri) : Bitmap{
        var bitmap = null;

        var urlPath : String = getRealPath_API19(baseContext,uri)
        Log.e("UrlPath",urlPath)
        var bitmapFactory = BitmapFactory.Options()
        bitmapFactory.inJustDecodeBounds = false
        bitmapFactory.inSampleSize = 3
        try {
            var bitmap : Bitmap = BitmapFactory.decodeFile(urlPath,bitmapFactory)
            Log.e("byteCountAfterCompress",bitmap.byteCount.toString())
            return bitmap
        } catch (e: Exception){
            Log.e("Failed",e.message)
            var  bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
            return bitmap
        }

//        Log.e("BitmapCountImageSize",bitmap.byteCount.toString())

    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun reduceImageinLambda(uri : Uri){
        var bitmapFactory = BitmapFactory.Options()

        bitmapFactory.apply {
            inJustDecodeBounds = false
            inSampleSize = 3
        }

        Observable.just(uri)
                .observeOn(Schedulers.io())
                .map { getRealPath_API19(baseContext,uri) }
                .observeOn(AndroidSchedulers.mainThread())
                .map { t -> BitmapFactory.decodeFile(t,bitmapFactory) }
                .doOnNext { t -> run {
                    imageView.setImageBitmap(t)
                    Log.e("byteCountAfterCompress",t.byteCount.toString())
                } }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

}
