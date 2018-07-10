package ng.com.fastpay.fastpay.Utils

import android.app.Activity
import android.content.Context
import android.support.design.widget.Snackbar
import android.view.View

/**
 * Created by Emem on 7/4/18.
 */
class Extensions {

}


fun Activity.showAppSnackbar(mainTextStringId: String, showTime:Int = Snackbar.LENGTH_SHORT, actionString: String = "",
                         listener: View.OnClickListener? = null){
    Snackbar.make(
            this.findViewById(android.R.id.content),
            mainTextStringId,
            Snackbar.LENGTH_INDEFINITE)
            .setAction(actionString, listener).show()

}