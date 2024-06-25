package com.android.example.textrecognitionlive

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.android.example.textrecognitionlive.main.MainFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FallBActivity : AppCompatActivity() {

    private lateinit var prodInput:String
    private lateinit var mrpInput:String
    private lateinit var mfgInput:String
    private lateinit var expInput:String

    private var db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fall_bactivity)

        val confirmBt = findViewById<Button>(R.id.confirm_button2)
        val prodInView = findViewById<EditText>(R.id.productIn)
        val mrpInView = findViewById<EditText>(R.id.mrpIn)
        val mfgInView = findViewById<EditText>(R.id.mfgIn)
        val expInView = findViewById<EditText>(R.id.expIn)

        confirmBt.setOnClickListener {
            prodInput = prodInView.text.toString()
            mrpInput = mrpInView.text.toString()
            mfgInput = mfgInView.text.toString()
            expInput = expInView.text.toString()

            val prodInArray = prodInput.split(" ").toTypedArray()
            for(prod in prodInArray)
            {
                MainFragment.labels.add(Pair(prod, "Product"))
            }
            //MainFragment.labels.add(Pair(prodInput, "Product"))
            MainFragment.labels.add(Pair(mrpInput, "MRP"))
            MainFragment.labels.add(Pair(mfgInput, "MfgDate"))
            MainFragment.labels.add(Pair(expInput, "ExpDate"))

            MainFragment.tokens = MainFragment.tokens.distinct() as ArrayList<Any>


            val data = hashMapOf(
                "labels" to MainFragment.labels,
                "tokens" to MainFragment.tokens
            )


            db.collection("data").add(data)
                .addOnSuccessListener {
                    //Toast.makeText(this, "Successfully added!", Toast.LENGTH_SHORT).show()
                    MainFragment.labels.clear()
                    MainFragment.tokens.clear()

                }
                .addOnFailureListener{
                    //Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show()
                }

            Log.i(TAG, "labelsfallb: ${MainFragment.labels}")
            Log.i(TAG, "tokensfallb: ${MainFragment.tokens}")

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


    }
}