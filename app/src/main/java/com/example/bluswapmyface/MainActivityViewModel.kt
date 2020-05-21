package com.example.bluswapmyface

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel

class BluSwapViewModel: ViewModel() {
    // Tracks the score for Team A

    var resizedBitmap1: Bitmap? = null
    var resizedBitmap2: Bitmap? = null
    var face1detected = 0
    var face2detected = 0
    var imageblurred1 = 0
    var imageblurred2 = 0
    var loadcheck1 = 0
    var loadcheck2 = 0
    var face1left = 0
    var face1right = 0
    var face1top = 0
    var face1bottom = 0
    var face2left = 0
    var face2right = 0
    var face2top = 0
    var face2bottom = 0


    override fun onCleared() {
        super.onCleared()
        Log.d("tag", "ViewModel instance about to be destroyed")
    }

    fun getData1() = resizedBitmap1
    //fun getData2(): Bitmap {
    //    return resizedBitmap2
    //}
    fun updateBitmap1(bitmap:Bitmap){
        resizedBitmap1 = bitmap
    }
    fun updateBitmap2(bitmap:Bitmap){
        resizedBitmap2 = bitmap
    }
    fun updatechecks1(ufd:Int,imgb:Int,lck:Int){
        face1detected = ufd
        imageblurred1 = imgb
        loadcheck1 = lck
    }
    fun updatechecks2(ufd:Int,imgb:Int,lck:Int){
        face2detected = ufd
        imageblurred2 = imgb
        loadcheck2 = lck
    }
    fun updateface1paras(left:Int, right:Int, top:Int, bottom:Int) {
        face1left = left
        face1right = right
        face1top = top
        face1bottom = bottom
    }
    fun updateface2paras(left:Int, right:Int, top:Int, bottom:Int) {
        face2left = left
        face2right = right
        face2top = top
        face2bottom = bottom
    }
}