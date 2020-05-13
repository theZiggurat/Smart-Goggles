package c2020.smartgoggles.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.core.content.FileProvider
import c2020.smartgoggles.R
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class AddItemActivity : AppCompatActivity() {


    val REQUEST_IMAGE_CAPTURE = 1
    val NUM_PICTURES = 2

    private lateinit var editText: EditText
    private lateinit var setNameButton: Button
    private lateinit var resetButton: Button
    private lateinit var takePictureButton: Button
    private lateinit var remainingText: TextView
    private lateinit var finishButton: Button
    private lateinit var progressBar: ProgressBar

    private var objectName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_item)

        editText = findViewById(R.id.edittext)
        setNameButton = findViewById(R.id.setbutton)
        resetButton = findViewById(R.id.resetbutton) as Button
        takePictureButton = findViewById(R.id.takepicturebutton)
        remainingText = findViewById(R.id.remaining)
        finishButton = findViewById(R.id.finishbutton)
        progressBar = findViewById(R.id.progressbar)

        resetButton.isEnabled = false
        finishButton.isEnabled = false
        takePictureButton.isEnabled = false

        remainingText.setText("Remaining: $NUM_PICTURES")
        progressBar.visibility = View.INVISIBLE

        setNameButton.setOnClickListener {
            if(editText.text.toString().isNotBlank()){
                if(!noDirectoryExists(editText.text.toString())){
                    Toast.makeText(applicationContext, "Object name already exists", Toast.LENGTH_SHORT).show()
                } else {
                    objectName = editText.text.toString()
                    resetButton.isEnabled = true
                    takePictureButton.isEnabled = true
                }
            }
            else
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
        }

        resetButton.setOnClickListener {
            editText.setText("")
            objectName = ""
            resetButton.isEnabled = false
            takePictureButton.isEnabled = false
        }

        takePictureButton.setOnClickListener {
            resetButton.isEnabled = false
            setNameButton.isEnabled = false
            editText.isEnabled = false

            dispatchTakePictureIntent()
        }

        finishButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            uploadToS3()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {

        val id = AWSMobileClient.getInstance().identityId!!

        val timestamp = SimpleDateFormat("YYYYMMddhhmmss").format(Date())

        val idDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, id)
        if(!idDir.exists())
            idDir.mkdir()

        val objectDir = File(idDir, objectName)
        if(!objectDir.exists())
            objectDir.mkdir()

        return File.createTempFile(
            timestamp,
            ".jpg",
            objectDir
        )
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                    return
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this, "com.example.android.fileprovider", it!!
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            val files = getObjectImageFiles()

            if (files.size == NUM_PICTURES) {
                finishButton.isEnabled = true
                takePictureButton.isEnabled = false
                remainingText.setText("Remaining: 0")
            } else {
                remainingText.setText("Remaining: ${NUM_PICTURES-files.size}")
            }
        }
    }

    private fun getObjectImageFiles(): List<File> {
        val id = AWSMobileClient.getInstance().identityId
        val idDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, id)
        val objectDir = File(idDir, objectName)
        if(!objectDir.exists())
            return listOf()
        return objectDir.listFiles()!!.asList()
    }

    private fun noDirectoryExists(subpath: String): Boolean {
        val id = AWSMobileClient.getInstance().identityId
        val idDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!, id)
        val objectDir = File(idDir, subpath)
        return !objectDir.exists()
    }

    private fun uploadToS3() {
        val transferUtility = TransferUtility.builder()
            .context(this.applicationContext)
            .awsConfiguration(AWSMobileClient.getInstance().configuration)
            .s3Client(AmazonS3Client(AWSMobileClient.getInstance().credentialsProvider))
            .build()

        val map = HashMap<Int, Long>(NUM_PICTURES)
        var totalBytes = 0L
        for(file in getObjectImageFiles()) {
            totalBytes += file.length()
        }
        var completed = 0

        val transferListener = object: TransferListener {
            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                map.put(id, bytesCurrent)

                var currentBytes = 0L
                for (bytes in map) {
                    currentBytes += bytes.value
                }
                val done = (((currentBytes.toDouble() / totalBytes) * 100.0).toInt())
                progressBar.setProgress(done)
                progressBar.invalidate()
            }

            override fun onStateChanged(id: Int, state: TransferState?) {
                if(state == TransferState.COMPLETED) {
                    completed++
                    if(completed == NUM_PICTURES)
                        finalize()
                }
            }

            override fun onError(id: Int, ex: Exception?) {
                Toast.makeText(applicationContext, "Error uploading to cloud", Toast.LENGTH_SHORT).show()
            }
        }

        val id = AWSMobileClient.getInstance().identityId
        for (file in getObjectImageFiles()) {
            val uploadObserver = transferUtility.upload("public/$id/$objectName/${file.name}", file)
            uploadObserver.setTransferListener(transferListener)
        }
    }

    private fun finalize() {

        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
    }
}
