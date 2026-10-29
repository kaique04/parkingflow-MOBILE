package com.riobranco.parkingflow.mobile

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun temConexaoInternet(context: Context): Boolean {
    val gerenciador = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return false
    val rede = gerenciador.activeNetwork ?: return false
    val capacidades = gerenciador.getNetworkCapabilities(rede) ?: return false
    return capacidades.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}