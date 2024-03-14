package com.example.widgetapp.activities

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.widgetapp.widgets.CallWidget
import com.example.widgetapp.ContactsAdapter
import com.example.widgetapp.R
import com.example.widgetapp.bottomSheet.ImageUploadBottomSheet
import com.example.widgetapp.bottomSheet.ImageUploadBottomSheet.Companion.imageSizeTooLarge
import com.example.widgetapp.bottomSheet.ImageUploadBottomSheet.Companion.notPickedMessge
import com.example.widgetapp.bottomSheet.ImageUploadBottomSheet.Companion.uploaded
import com.example.widgetapp.bottomSheet.ImageUploadBottomSheet.Companion.wrongMessage
import com.example.widgetapp.bottomSheet.OnImageUpload
import com.example.widgetapp.daoClasses.Contact
import com.google.android.material.snackbar.Snackbar


class WidgetActivity : AppCompatActivity(), OnImageUpload {

    private var contactList = mutableListOf<Contact?>()
    private var namesToDisplay = mutableListOf<String?>()
    private var searchList = mutableListOf<String?>()
    private lateinit var recyclerView: RecyclerView
    private var widgetAppId: Int? = null
    private var audioContact: Contact? = null
    private var videoContact: Contact? = null
    private var networkCallContact: Contact? = null
    private lateinit var searchBar: EditText
    private lateinit var adapter: ContactsAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_widget)
        recyclerView = findViewById(R.id.recyclerViewer)
        searchBar = findViewById(R.id.searchView)

        searchBar.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchWord = s.toString()
                if(searchWord.isEmpty()) {
                    searchList.clear()
                    searchList.addAll(namesToDisplay)
                }
                else {
                    searchList.clear()
                    for (name in namesToDisplay) {
                        if (name?.contains(searchWord.trim(), ignoreCase = true) == true) {
                            searchList.add(name)
                        }
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        setWidgetAppId()
        getContactsList()
        setRecyclerViewData()
    }

    private fun setRecyclerViewData() {
        adapter = ContactsAdapter(searchList, ::contactClickListener)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(
            recyclerView.context,
            LinearLayoutManager.VERTICAL
        )
        recyclerView.addItemDecoration(dividerItemDecoration)
        recyclerView.adapter = adapter
    }

    private fun contactClickListener(contact: String?) {
        for(data in contactList) {
            if (data?.displayName == contact) {
                if (data?.type == voipCall) {
                    Log.d(WIDGET_LOG, "Got the audio contact")
                    audioContact = data
                } else if (data?.type == videoCall) {
                    Log.d(WIDGET_LOG, "Got the video contact")
                    videoContact = data
                } else if (data?.type == networkCall) {
                    setContactNumberAsType(data)
                }
            }
        }

        val bottomSheet = ImageUploadBottomSheet()
        bottomSheet.show(supportFragmentManager, "ModalBottomSheet")
//        val intent = Intent(this, CallWidget::class.java)
//        intent.action = actionNameSelected
//        intent.putExtra(valueForValue, widgetAppId)
//        intent.putExtra(AUDIO_VAL, audioContact)
//        intent.putExtra(VIDEO_VAL, videoContact)
//        sendBroadcast(intent)
//        finish()
    }

    private fun setContactNumberAsType(data: Contact) {
        networkCallContact = data
        networkCallContact?.type = getPhoneNumberFromContactId(networkCallContact?.id)
        Log.d(WIDGET_LOG, "Logging Phone Number: " + networkCallContact?.type)
    }

    private fun getPhoneNumberFromContactId(contactId: Long?): String? {
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


    private fun setWidgetAppId() {
        val bundle = intent?.extras
        widgetAppId = bundle?.getInt(valueForValue)
        Log.d(WIDGET_LOG, "Widget Value received in Widget Activity is $widgetAppId")
    }

    private fun getContactsList() {
        val resolver: ContentResolver = applicationContext.contentResolver
        val cursor: Cursor? = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME
        )

        cursor?.let {
            val idColumnIndex = it.getColumnIndex(ContactsContract.Data._ID)
            val displayNameColumnIndex = it.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
            val mimeTypeColumnIndex = it.getColumnIndex(ContactsContract.Data.MIMETYPE)

            while (it.moveToNext()) {
                if (idColumnIndex >= 0 && displayNameColumnIndex >= 0 && mimeTypeColumnIndex >= 0) {
                    val id = it.getLong(idColumnIndex)
                    val displayName = it.getString(displayNameColumnIndex)
                    val mimeType = it.getString(mimeTypeColumnIndex)
                    val contact = Contact(id, displayName, mimeType)
                    Log.d("Data", contact.toString())
                    if (contact.type == voipCall || contact.type == videoCall || contact.type == networkCall) {
                        contactList.add(contact)
                        if(!namesToDisplay.contains(contact.displayName)) {
                            namesToDisplay.add(contact.displayName)
                            searchList.add(contact.displayName)
                        }
                    }
                }
            }
            it.close()
        }
    }

    companion object {
        const val WIDGET_LOG = "checking"
        const val AUDIO_VAL = "audioValue"
        const val VIDEO_VAL = "videoValue"
        const val NETWORK_VAL = "networkValue"
        const val IMG_VAL = "imageValue"
        const val actionNameSelected = "This is for widget"
        const val valueForValue = "ThisValue"
        const val voipCall = "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
        const val videoCall = "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
        const val networkCall = "vnd.android.cursor.item/phone_v2"
    }

    override fun onImageUpload(selectedImage: Uri?, message: String) {

        val intent = Intent(this, CallWidget::class.java)
        intent.action = actionNameSelected
        intent.putExtra(valueForValue, widgetAppId)
        intent.putExtra(AUDIO_VAL, audioContact)
        intent.putExtra(VIDEO_VAL, videoContact)
        intent.putExtra(NETWORK_VAL, networkCallContact)
        if ((selectedImage != null && message == uploaded)) {
            intent.putExtra(IMG_VAL, selectedImage)
        } else if (message == wrongMessage) {
            showSnackBar("Something went wrong. Please try again.")
            return
        } else if (message == notPickedMessge) {
            showSnackBar("Image not selected")
            return
        } else if (message == imageSizeTooLarge) {
            showSnackBar(message)
            return
        }
        sendBroadcast(intent)
        finish()
    }

    private fun showSnackBar(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
