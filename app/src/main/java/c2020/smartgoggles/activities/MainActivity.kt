package c2020.smartgoggles.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import c2020.smartgoggles.Constants
import c2020.smartgoggles.R
import com.amazonaws.ClientConfiguration
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobile.client.Callback
import com.amazonaws.mobile.client.UserState
import com.amazonaws.mobile.client.UserStateDetails
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ListObjectsRequest
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var signOutButton: Button
    private lateinit var takePictureButton: Button
    private lateinit var s3Client: AmazonS3Client
    private lateinit var welcomeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AWSMobileClient.getInstance().initialize(applicationContext, object: Callback<UserStateDetails> {
            override fun onResult(result: UserStateDetails?) {
                when(result!!.userState) {
                    UserState.SIGNED_IN -> { }
                    else -> {
                        val i = Intent(applicationContext, AuthenticationActivity::class.java)
                        startActivity(i)
                    }
                }
            }
            override fun onError(e: Exception?) {
                Log.e("error", e.toString())
            }
        })
        initialize()
    }



    private fun initialize() {
        //s3Client = AmazonS3Client(AWSMobileClient.getInstance().credentials)

        val usr = AWSMobileClient.getInstance().username
        welcomeText = findViewById(R.id.hello)
        welcomeText.text = "Hello $usr, welcome to your Smart Goggles control application"


        takePictureButton = findViewById(R.id.object_button)
        takePictureButton.setOnClickListener {
            val i = Intent(this, AddItemActivity::class.java)
            startActivity(i)
        }

        //retrieveObjectsFromS3()
    }

    private fun retrieveObjectsFromS3() {

        val id = AWSMobileClient.getInstance().identityId

        val listObjectrequest = ListObjectsRequest()
            .withBucketName(Constants.BUCKET_NAME)
            .withPrefix("public")

        val listing = s3Client.listObjects(listObjectrequest)
        Log.d("summaries", "start")
        for(summary in listing.objectSummaries) {
            Log.d("summaries", summary.key)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.signout_button -> {
            AWSMobileClient.getInstance().signOut()
            val i = Intent(this, AuthenticationActivity::class.java)
            startActivity(i)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }
}
