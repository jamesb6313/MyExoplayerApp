package com.example.myexoplayerapp.util

import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
fun AppCompatActivity.checkPermission(permission: String) =
    ActivityCompat.checkSelfPermission(this, permission)
fun AppCompatActivity.shouldRequestPermissionRationale(permission: String) =
    ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
fun AppCompatActivity.requestAllPermissions(
    permissionsArray: Array<String>,
    requestCode: Int
) {
    ActivityCompat.requestPermissions(this, permissionsArray, requestCode)
}