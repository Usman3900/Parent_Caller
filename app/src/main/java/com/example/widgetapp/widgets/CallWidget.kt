package com.example.widgetapp.widgets

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import com.example.widgetapp.R
import com.example.widgetapp.activities.WidgetActivity
import com.example.widgetapp.activities.WidgetActivity.Companion.AUDIO_VAL
import com.example.widgetapp.activities.WidgetActivity.Companion.IMG_VAL
import com.example.widgetapp.activities.WidgetActivity.Companion.NETWORK_VAL
import com.example.widgetapp.activities.WidgetActivity.Companion.VIDEO_VAL
import com.example.widgetapp.activities.WidgetActivity.Companion.WIDGET_LOG
import com.example.widgetapp.activities.WidgetActivity.Companion.actionNameSelected
import com.example.widgetapp.activities.WidgetActivity.Companion.valueForValue
import com.example.widgetapp.daoClasses.Contact
import java.io.ByteArrayOutputStream
import java.io.InputStream

class CallWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(WIDGET_LOG, "onUpdate is Called")

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context?) {
        Log.d(WIDGET_LOG, "onEnabled is Called")
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        Log.d(WIDGET_LOG, "OnReceived is Called")
        Log.d(WIDGET_LOG, "Intent Action For compare: " + intent?.action)

        if(intent?.action == actionNameSelected) {

            Log.d(WIDGET_LOG, "Intent Received")

            val appId = intent.getIntExtra(valueForValue, 0)
            val audioContact = intent.getSerializableExtra(AUDIO_VAL) as? Contact
            val videoContact = intent.getSerializableExtra(VIDEO_VAL) as? Contact
            val networkContact = intent.getSerializableExtra(NETWORK_VAL) as? Contact

            val imageBitmap = intent.getParcelableExtra(IMG_VAL) as? Uri
            var selectedImage: Bitmap? = null

            if (imageBitmap != null) {
                val imageStream: InputStream? =
                    context?.contentResolver?.openInputStream(imageBitmap)
                selectedImage = BitmapFactory.decodeStream(imageStream)
            }

            Log.d(WIDGET_LOG, "This is app id received from the activity: $appId")
            Log.d(WIDGET_LOG, "This is value audio received from the activity: $audioContact")
            Log.d(WIDGET_LOG, "This is value video received from the activity: $videoContact")

            updateSelectedWidgetTitle(appId, audioContact, videoContact, context, selectedImage, networkContact)
        }
    }

    private fun updateSelectedWidgetTitle(
        appId: Int?,
        audioContact: Contact?,
        videoContact: Contact?,
        context: Context?,
        selectedImage: Bitmap?,
        networkContact: Contact?
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(context?.let {
            ComponentName(
                it,
                CallWidget::class.java
            )
        })
        Log.d(WIDGET_LOG, "Came to update title")
        Log.d(WIDGET_LOG, appWidgetIds.toString())
        for (appWidgetId in appWidgetIds) {
            Log.d(WIDGET_LOG, appWidgetId.toString())
            if(appWidgetId == appId) {
                Log.d(WIDGET_LOG, "Got the app id to update")

                val views = RemoteViews(context?.packageName, R.layout.call_widget)
                views.setTextViewText(R.id.NameView, audioContact?.displayName)

                views.setViewVisibility(R.id.setContact, View.GONE)
//                views.setViewVisibility(R.id.audioCall, View.VISIBLE)
                views.setViewVisibility(R.id.videoCall, View.VISIBLE)
                views.setViewVisibility(R.id.networkCall, View.VISIBLE)
                views.setViewVisibility(R.id.contactImage, View.VISIBLE)

//                setAudioButtonClickListeners(audioContact, views, appWidgetId, context)
                setVideoButtonClickListeners(videoContact, views, appWidgetId, context)
                setNetworkButtonClickListeners(networkContact, views, appWidgetId, context)
                setContactImage(selectedImage, views)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

    }

    private fun setContactImage(
        selectedImage: Bitmap?,
        views: RemoteViews) {
        if (selectedImage != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
            views.setImageViewBitmap(
                R.id.contactImage,
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            )

        } else {
            views.setImageViewResource(R.id.contactImage, R.drawable.sample_image)
        }
    }

    private fun setNetworkButtonClickListeners(networkContact: Contact?,
                                               views: RemoteViews,
                                               appWidgetId: Int,
                                               context: Context?) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:${networkContact?.type}")

        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, callIntent, PendingIntent.FLAG_IMMUTABLE)

        views.setOnClickPendingIntent(R.id.networkCall, pendingIntent)

    }

    private fun setVideoButtonClickListeners(videoContact: Contact?,
                                             views: RemoteViews,
                                             appWidgetId: Int,
                                             context: Context?
    ) {
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW

        intent.setDataAndType(Uri.parse("content://com.android.contacts/data/${videoContact?.id}"), videoContact?.type)
        intent.setPackage("com.whatsapp")

        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE)
        views.setOnClickPendingIntent(R.id.videoCall, pendingIntent)
    }

//    private fun setAudioButtonClickListeners(
//        audioContact: Contact?,
//        views: RemoteViews,
//        appWidgetId: Int,
//        context: Context?
//    ) {
//        val intent = Intent()
//        intent.action = Intent.ACTION_VIEW
//
//        intent.setDataAndType(Uri.parse("content://com.android.contacts/data/${audioContact?.id}"), audioContact?.type)
//        intent.setPackage("com.whatsapp")
//
//        val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE)
//        views.setOnClickPendingIntent(R.id.audioCall, pendingIntent)
//    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val views = RemoteViews(context.packageName, R.layout.call_widget)
    val intent = Intent(context, WidgetActivity::class.java)

    setContactIntent(appWidgetId, intent, context, views)
//    views.setViewVisibility(R.id.audioCall, View.GONE)
    views.setViewVisibility(R.id.videoCall, View.GONE)
    views.setViewVisibility(R.id.networkCall, View.GONE)
    views.setViewVisibility(R.id.contactImage, View.GONE)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private fun setContactIntent(
    appWidgetId: Int,
    intent: Intent,
    context: Context,
    views: RemoteViews
) {
    val bundle = Bundle()
    bundle.putInt(valueForValue, appWidgetId)
    intent.putExtras(bundle)
    Log.d(WIDGET_LOG, "Value add in intent: $appWidgetId")
    val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.setContact, pendingIntent)
}