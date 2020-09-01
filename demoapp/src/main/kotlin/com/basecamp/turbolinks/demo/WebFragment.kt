package com.basecamp.turbolinks.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.basecamp.turbolinks.TurbolinksNavGraphDestination
import com.basecamp.turbolinks.TurbolinksView
import com.basecamp.turbolinks.TurbolinksWebFragment
import kotlinx.android.synthetic.main.error.view.*

@TurbolinksNavGraphDestination(uri = "turbolinks://fragment/web")
open class WebFragment : TurbolinksWebFragment(), Destination {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_web, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        pageViewModel.title.observe(viewLifecycleOwner) {
            toolbarForNavigation()?.title = it
        }
    }

    override fun onResume() {
        super.onResume()
        animateBottomNavVisibility()
    }

    override fun createProgressView(location: String): View {
        return layoutInflater.inflate(R.layout.progress, null)
    }

    override fun createErrorView(statusCode: Int): View {
        return layoutInflater.inflate(R.layout.error, null).apply {
            error_message.text = Error.getMessage(statusCode)
        }
    }

    override fun shouldEnablePullToRefresh(): Boolean {
        return true
    }

    override fun toolbarForNavigation(): Toolbar? {
        return view?.findViewById(R.id.toolbar)
    }

    override val turbolinksView: TurbolinksView?
        get() = view?.findViewById(R.id.turbolinks_view)
}
