package com.example.widgetapp.bottomSheet

import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.example.widgetapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream


class ImageUploadBottomSheet: BottomSheetDialogFragment() {

    var pictureUpload: OnImageUpload? = null;
    private lateinit var cancelButton:Button
    private lateinit var uploadImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.image_upload_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancelButton = view.findViewById(R.id.cancelUpload)
        uploadImage = view.findViewById(R.id.uploadImage)

        cancelButton.setOnClickListener {
            pictureUpload?.onImageUpload(null, cancelledMessage)
            activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
        }

        uploadImage.setOnClickListener{
            val photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            startActivityForResult(photoPickerIntent, 123)
        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is OnImageUpload) {
            pictureUpload = context
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            try {
                val imageUri: Uri? = data?.data
                if(context != null && data?.data != null) {
                    if (checkImageSize(requireActivity(), data.data!!, 20736000L)) {
                        pictureUpload?.onImageUpload(imageUri, uploaded)
                    } else {
                        pictureUpload?.onImageUpload(null, imageSizeTooLarge)
                    }
                } else {
                    pictureUpload?.onImageUpload(null, wrongMessage)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                pictureUpload?.onImageUpload(null, wrongMessage)
            }
        } else {
            pictureUpload?.onImageUpload(null, notPickedMessge)
        }
        activity?.supportFragmentManager?.beginTransaction()?.remove(this)?.commit()
    }

    private fun checkImageSize(context: Context, imageUri: Uri, maxAllowedSize: Long): Boolean {
        var inputStream: InputStream? = null
        try {
            val contentResolver: ContentResolver = context.contentResolver
            inputStream = contentResolver.openInputStream(imageUri)
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            val memoryUsage = options.outHeight * options.outWidth * 4 // Assuming ARGB_8888 format
            return if (memoryUsage <= maxAllowedSize) {
                Log.d("ImageSizeCheck", "Image size within the allowed range")
                true
            } else {
                Log.d("ImageSizeCheck", "Image size exceeds the allowed range")
                false
            }
        } catch (e: IOException) {
            Log.e("ImageSizeCheck", "Error reading image URI", e)
            return false
        } finally {
            try {
                inputStream?.close()
            } catch (e: IOException) {
                Log.e("ImageSizeCheck", "Error closing InputStream", e)
            }
        }
    }

    companion object {
        const val uploaded = "Image uploaded"
        const val wrongMessage = "Something went wrong"
        const val notPickedMessge = "You haven't picked Image"
        const val cancelledMessage = "Image uploaded canceled"
        const val imageSizeTooLarge = "Image size too large. Kindly select another."
    }
}

interface OnImageUpload {
    fun onImageUpload(selectedImage: Uri?, message: String);
}

