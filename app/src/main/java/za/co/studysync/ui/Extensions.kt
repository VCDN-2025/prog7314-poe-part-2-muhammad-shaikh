package za.co.studysync.ui

import android.view.View
import android.widget.AdapterView
import android.widget.Spinner

fun Spinner.setOnItemSelectedListenerCompat(onSelected: (position: Int) -> Unit) {
    this.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onSelected(position)
        }
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}
