package com.renown.whiskeyvictor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout

class SessionAdapter(
    private val context: Context,
    private val sessionList: List<Session>
): RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    var sessionAdapterClickCallback: SessionAdapterClickCallback? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun getItemCount(): Int {
        return sessionList.size
    }
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val currentSession = sessionList[position]
        holder.listItemSessionUserName.text = currentSession.userName
        holder.listItemSessionUrl.text = currentSession.url
        holder.listItemSessionCookies.text = currentSession.cookiesJson
        holder.listItemSessionFlexBox.setOnClickListener {
            sessionAdapterClickCallback?.onSessionClicked(
                currentSession,
                holder.adapterPosition
            )
        }
        holder.listItemSessionDeleteButton.setOnClickListener {
            sessionAdapterClickCallback?.onSessionDeleteClicked(
                currentSession,
                holder.adapterPosition
            )
        }

    }

    inner class SessionViewHolder(sessionView: View): RecyclerView.ViewHolder(sessionView) {
        val listItemSessionFlexBox = sessionView.findViewById<FlexboxLayout>(R.id.list_item_session_flex_box)
        val listItemSessionUserName = sessionView.findViewById<TextView>(R.id.list_item_session_user_name)
        val listItemSessionUrl = sessionView.findViewById<TextView>(R.id.list_item_session_url)
        val listItemSessionCookies = sessionView.findViewById<TextView>(R.id.list_item_session_cookies)
        val listItemSessionDeleteButton = sessionView.findViewById<FlexboxLayout>(R.id.list_item_session_delete_button)
    }

}