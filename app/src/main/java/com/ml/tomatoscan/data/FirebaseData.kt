package com.ml.tomatoscan.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ml.tomatoscan.models.ScanResult
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseData {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }

    suspend fun saveScanResult(result: ScanResult) {
        firestore.collection("scans").document(getUserId()).collection("results").add(result).await()
    }

    suspend fun uploadImage(imageUri: Uri): String {
        val storageRef = storage.reference.child("images/${getUserId()}/${UUID.randomUUID()}")
        storageRef.putFile(imageUri).await()
        return storageRef.downloadUrl.await().toString()
    }

    suspend fun getScanHistory(): List<ScanResult> {
        val snapshot = firestore.collection("scans").document(getUserId()).collection("results")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.toObjects(ScanResult::class.java)
    }
}
