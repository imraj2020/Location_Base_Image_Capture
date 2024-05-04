package com.cse.locationbaseimagecapture

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cse.locationbaseimagecapture.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.Locale


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    // Define request code for permissions
    private val PERMISSION_REQUEST_CODE = 100

    // Define a global variable to store the image URI
    var imageUri: Uri? = null


    private val PICK_IMAGE_REQUEST = 123
    private val filepath: Uri? = null
    private val mStorageRef: StorageReference? = null

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize FusedLocationProviderClient

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        checkPermissions()
        enableEdgeToEdge()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        binding.ivTakePicture.setOnClickListener {
            getLastKnownLocation()
            Toast.makeText(this@MainActivity, "Opening Camera", Toast.LENGTH_SHORT).show()
            dispatchTakePictureIntent()
        }
        binding.btnSendPicture.setOnClickListener {

            uploadFile()
            //       uploadImageToFirebaseStorage()
        }


    }

    @SuppressLint("SuspiciousIndentation")
    private fun uploadFile() {

        val etReqLocation = binding.etReqLocation.text.toString().trim()
        var etSelectedLocation = binding.etLocation.text.toString().trim()




        // Iterate through each word in etReqLocation and check if it exists in etSelectedLocation

        if (etSelectedLocation.contains(etReqLocation, ignoreCase = true)) {

            val Message = "upload file successfull"
            showToast(Message)
        } else{
            val Message = "location not matched"
            showToast(Message)
        }
    }

    // Method to convert Bitmap to Uri
    private fun bitmapToUriConverter(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir("Images", MODE_PRIVATE)
        file = File(file, "unique_image_filename" + ".jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("TAG", "bitmap To Uri Convert failed: $e")
            val errorMessage = "bitmap To Uri Convert failed"
            showToast(errorMessage)
        }
        return Uri.parse(file.absolutePath)
    }

    // Method to show toast message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentLocations() {
        Log.e("MSG", "Error")
    }


    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            binding.ivCapturedImage.setImageBitmap(imageBitmap)  //error in this line





            //convert image to bitmap
            imageUri = bitmapToUriConverter(imageBitmap)
        }
    }


    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            // Request permissions
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Check if all permissions are granted
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (!allPermissionsGranted) {
                // Handle permission denied
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
                // You might want to disable functionality that requires these permissions
            }
        }
    }

    fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    // Use your location object here (e.g., get latitude, longitude, etc.)
                    val latitude = location.latitude
                    val longitude = location.longitude
                    binding.etLatitude.setText(latitude.toString())
                    binding.etLongitude.setText(longitude.toString())

                    var cityName: String? = null
                    val geoCoder = Geocoder(this@MainActivity, Locale.getDefault())
                    val addressList = geoCoder.getFromLocation(latitude, longitude, 1)

                    if (addressList != null) {
                        if (addressList.isNotEmpty()) {
                            val address = addressList?.get(0)
                            if (address != null) {

                                binding.etLocation.setText(addressList[0].getAddressLine(0))
                                //  cityName = address.adminArea ?: address.locality ?: address.subAdminArea
                                //  binding.etLocation.setText(cityName)
                            }
                        }
                    }
                    // Do something with the location data
                } else {
                    // Handle the case when the location is null
                }
            }
    }

}
