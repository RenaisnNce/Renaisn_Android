package com.renaisn.reader.ui.about


import android.os.Bundle
import com.renaisn.reader.R
import com.renaisn.reader.base.BaseActivity
import com.renaisn.reader.databinding.ActivityDonateBinding
import com.renaisn.reader.utils.viewbindingdelegate.viewBinding

/**
 * Created by GKF on 2018/1/13.
 * 捐赠页面
 */

class DonateActivity : BaseActivity<ActivityDonateBinding>() {

    override val binding by viewBinding(ActivityDonateBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val fTag = "donateFragment"
        var donateFragment = supportFragmentManager.findFragmentByTag(fTag)
        if (donateFragment == null) donateFragment = DonateFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fl_fragment, donateFragment, fTag)
            .commit()
    }

}
