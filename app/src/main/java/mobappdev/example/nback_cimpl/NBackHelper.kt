package mobappdev.example.nback_cimpl

import android.util.Log

/**
 * This class provides the connection to the C-model
 *
 * The class loads the JniBridge when it is initialized.
 * Secondly, it provides the funtion generateNBackString to the rest of the code.
 *
 * Date: 25-08-2023
 * Version: Version 1.1
 * Author: Yeetivity
 *
 */



class NBackHelper {
    // Native function declaration
    private external fun createNBackString(size: Int, combinations: Int, percentMatch: Int, nBack: Int): IntArray

    fun generateNBackString(size: Int, combinations: Int, percentMatch: Int, nBack: Int): IntArray {
        Log.d("NBackHelper", "generateNBackString called with size=$size, combinations=$combinations, percentMatch=$percentMatch, nBack=$nBack")
        return try {
            val result = createNBackString(size, combinations, percentMatch, nBack)
            Log.d("NBackHelper", "Generated sequence: ${result.contentToString()}")
            result
        } catch (e: Exception) {
            Log.e("NBackHelper", "Error in generateNBackString: ${e.message}", e)
            IntArray(0) // Return an empty array if there’s an error
        }
    }

    companion object {
        init {
            try {
                System.loadLibrary("JniBridge") // Load the native C library
                Log.d("NBackHelper", "JniBridge library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("NBackHelper", "Failed to load JniBridge library: ${e.message}")
            }
        }
    }
}
