package com.cem.cookbookprpject

import android.annotation.SuppressLint
import android.app.ListFragment
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ListFragment : Fragment() {
    var yemekIsmiListesi =ArrayList<String>()
    var yemekIdListesi =ArrayList<Int>()
    private lateinit var listeAdapter:ListeRecyclerAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listeAdapter= ListeRecyclerAdapter(yemekIsmiListesi,yemekIdListesi)
        val recyclerView=view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager=LinearLayoutManager(context)
        recyclerView.adapter=listeAdapter


        sqlVeriAlma()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun sqlVeriAlma(){
        try {
            activity?.let{
                val database=it.openOrCreateDatabase("tarifListesi",Context.MODE_PRIVATE,null)

                val cursor=database.rawQuery("SELECT * FROM yemekler", null)
                val yemekIsmiIndex= cursor.getColumnIndex("yemekIsim")
                val yemekIdIndex= cursor.getColumnIndex("id")
                yemekIsmiListesi.clear()
                yemekIdListesi.clear()

                while (cursor.moveToNext()){
                    yemekIsmiListesi.add(cursor.getString(yemekIsmiIndex))
                    yemekIdListesi.add(cursor.getInt(yemekIdIndex))

                }
                listeAdapter.notifyDataSetChanged()
                cursor.close()
            }

        }
        catch (e:Exception){
            e.printStackTrace()
        }

    }

}