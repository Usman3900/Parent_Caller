package com.example.widgetapp.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.TextView
import com.example.widgetapp.R
import org.w3c.dom.Text
import java.util.Locale.Category

class MainActivity : AppCompatActivity() {

    private val PERMISSIONS_REQUEST_READ_CONTACTS = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkForPermission()
        findViewById<TextView>(R.id.textTest).setOnClickListener {
            val intent = Intent(Intent.ACTION_CALL)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val id = 670
            val uri = Uri.parse("tel:" + getPhoneNumberFromContactId(id))
            intent.data = uri

            startActivity(intent)
        }
    }
    private fun getPhoneNumberFromContactId(contactId: Int): String? {
        val contentResolver = contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            ContactsContract.CommonDataKinds.Phone._ID + " = ?",
            arrayOf(contactId.toString()),
            null
        )

        var phoneNumber: String? = null

        cursor?.use {
            if (it.moveToFirst()) {
                val phoneNumberIndex =
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                phoneNumber = it.getString(phoneNumberIndex)
            }
        }

        return phoneNumber
    }

    private fun checkForPermission() {
        val readContactsPermission = Manifest.permission.READ_CONTACTS
        val callPhonePermission = Manifest.permission.CALL_PHONE

        if (checkSelfPermission(readContactsPermission) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(callPhonePermission) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(readContactsPermission, callPhonePermission),
                PERMISSIONS_REQUEST_READ_CONTACTS
            )
        }
    }
}
