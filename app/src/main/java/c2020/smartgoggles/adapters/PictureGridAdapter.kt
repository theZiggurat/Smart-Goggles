package c2020.smartgoggles.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import c2020.smartgoggles.R
import java.io.File

class PictureGridAdapter constructor(val mContext: Context, val width: Int): BaseAdapter() {

    private var fileList: List<File> = listOf()

    override fun getView(p0: Int, view: View?, parent: ViewGroup?): View {
        val inflater = mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.adapter_picture_grid, null)

        val imageView: ImageView = view.findViewById(R.id.image)
        imageView.rotation = 90f

        val file = getItem(p0) as File

        imageView.setImageBitmap(decodeSampleBitmap(file, width / 2))

        return view
    }

    override fun getItem(p0: Int): Any {
        return fileList[p0]
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return fileList.size
    }

    fun setFileList(list: List<File>){
        this.fileList = list
    }

    fun decodeSampleBitmap(
        path: File,
        width: Int
    ): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(path.path)
            inSampleSize = calculateInSampleSize(this, width)
            Log.d("sampleSize", "$inSampleSize")

            inJustDecodeBounds = false
            BitmapFactory.decodeFile(path.path)
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int): Int {
        val width: Int = options.run { outWidth }
        var inSampleSize = 1

        Log.d("width", "$width $reqWidth")

        if(width > reqWidth) {
            val halfWidth = width / 2



            while(halfWidth / inSampleSize >= reqWidth)
                inSampleSize *=2
        }

        return inSampleSize
    }

}