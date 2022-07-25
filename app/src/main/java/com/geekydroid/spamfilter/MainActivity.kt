package com.geekydroid.spamfilter

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Build.ID
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.geekydroid.spamfilter.databinding.ActivityMainBinding
import kotlinx.coroutines.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private var cursor:Cursor? = null

    private lateinit var binding: ActivityMainBinding
    val permissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                startReadingMesages()
            }
        }
    private lateinit var adapter:Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        binding.btnReadMessage.setOnClickListener {
            startReadingMesages()
        }

        binding.btnPagination.setOnClickListener {
            loadMoreMessages()
        }

    }

    private fun loadMoreMessages() {
        CoroutineScope(Dispatchers.Main).launch {
            val result = getMessages()
            adapter.loadMoreItems(result)
        }
    }

    private fun startReadingMesages() {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                "android.permission.READ_SMS"
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            CoroutineScope(Dispatchers.Main).launch {
                val result = getMessages()
                adapter = Adapter(result)
                binding.recyclerView.adapter = adapter
                adapter.notifyDataSetChanged()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && shouldShowRequestPermissionRationale(
                Manifest.permission.READ_SMS
            )
        ) {
            showEducationalUI()
        } else {
            permissionResult.launch(Manifest.permission.READ_SMS)
        }

    }

    private fun showEducationalUI() {
        val builder = AlertDialog.Builder(this)
            .setTitle("Read sms permission")
            .setMessage("Please allow us to read the messages")
            .setPositiveButton(
                "Ok"
            ) { _, _ ->
                startReadingMesages()
            }.create()
        builder.show()
    }

    private suspend fun getMessages() : ArrayList<Message> {

        val messageList = mutableListOf<Message>()
        val finalList = withContext(Dispatchers.Default) {
            val inboxUri = Uri.parse("content://sms/inbox")
            val selection = "1=1 GROUP BY ${Telephony.Sms.THREAD_ID}"
            val cr = contentResolver
            cursor = cr.query(inboxUri,null,null,null,null)
            cursor?.let { c ->
                for (i in 0 until c.columnCount) {
                    Log.d(TAG, "getMessages: ${c.getColumnName(i)}")
                }
                val idIndex = c.getColumnIndex("_id")
                val threadIdIndex = c.getColumnIndex("thread_id")
                val addressIndex = c.getColumnIndex("address")
                val personIndex = c.getColumnIndex("person")
                val subjectIdx = c.getColumnIndex("subject")
                val bodyIdx = c.getColumnIndex("body")
                val phoneIdIndex = c.getColumnIndex("phone_id")
                val breakPoint = 0
                while (c.moveToNext()) {
                    Log.d(TAG, "getMessages: ${c.getString(subjectIdx)}")
                    val currentMessage = Message(
                        c.getString(idIndex),
                        c.getString(threadIdIndex),
                        c.getString(addressIndex),
                        c.getInt(personIndex),
                        "",
                        c.getString(bodyIdx),
                        c.getString(phoneIdIndex)
                    )
                    messageList.add(currentMessage)
                    if (breakPoint > 99) {
                        break
                    }
                }
            }
            messageList as ArrayList<Message>
        }
        return finalList
    }
}