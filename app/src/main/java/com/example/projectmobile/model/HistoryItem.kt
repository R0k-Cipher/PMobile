package com.example.projectmobile.model

data class HistoryItem(
    val id: Int,
    val weight: Double,
    val height: Double,
    val gender: String,
    val lbm: Double,
    val status: String,
    val createdAt: String
)