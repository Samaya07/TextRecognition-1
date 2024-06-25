package com.android.example.textrecognitionlive

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import com.android.example.textrecognitionlive.databinding.ActivityMainBinding
import com.android.example.textrecognitionlive.main.MainFragment


class MainActivity : AppCompatActivity(), LifecycleOwner {


    private lateinit var viewBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.main_idkwhat)
        if(savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container,MainFragment.newInstance())
                .commitNow()
        }

    }

}





















