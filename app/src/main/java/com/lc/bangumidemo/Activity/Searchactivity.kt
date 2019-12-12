package com.lc.bangumidemo.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.lc.bangumidemo.Adapter.Recadapt
import com.lc.bangumidemo.KT.imglist
import com.lc.bangumidemo.KT.list
import com.lc.bangumidemo.MyRetrofit.ResClass.BookDetail
import com.lc.bangumidemo.MyRetrofit.ResClass.BookResult
import com.lc.bangumidemo.MyRetrofit.Retrofit.Retrofitcall
import com.lc.bangumidemo.R
import kotlinx.android.synthetic.main.search.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.ArrayList

class Searchactivity :BaseActivity() {
    lateinit var searchItem:MenuItem
    lateinit var searchView:SearchView
    override fun setRes(): Int {
        return R.layout.search
    }

    override fun initview() {
        super.initview()
        setSupportActionBar(toolbar_search)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {android.R.id.home -> finish()}
        return super.onOptionsItemSelected(item)
    }

    fun initsearchlistener() {
        //

        searchView = searchItem.actionView as SearchView
        searchView!!.isSubmitButtonEnabled = false // 提交按钮
        searchView!!.queryHint = "少年, 要来个兔子么"
        searchView!!.onActionViewExpanded()
        searchView!!.isIconified = false
        searchView!!.clearFocus() // 收起键盘
        //
        val bundle = this.intent.extras //读取intent的数据给bundle对象
        val tag = bundle!!.getString("tag") //通过key得到value
        when(tag){
            "小说"->{searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchbook(query)//查找书本
                    searchView!!.clearFocus() // 收起键盘
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                return true
                }
            })}
            "影视"->{}
            "动漫"->{}
            "综合"->{}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menusearch, menu)
        searchItem = menu!!.findItem(R.id.action_search)
        initsearchlistener()
        return super.onCreateOptionsMenu(menu)
    }
    fun initadapt(){

        var adapt = Recadapt(list,this)
        initlistener(adapt)
        listview.setLayoutManager(LinearLayoutManager (this@Searchactivity))
        listview.adapter = adapt
        listview.adapter?.notifyDataSetChanged()
    }
    private fun initlistener(adapter:Recadapt) {
        adapter.setOnItemClickListener(object :Recadapt.OnItemClickListener{
            override fun onItemClick(view: View, position: Int) {
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                //view!!.findViewById<CardView>(R.id.cardview)
                var urls= list[position].url
                var start=Intent(this@Searchactivity,BookIndex::class.java)
                var bundle=Bundle()
                bundle.putString("url",urls)
                bundle.putInt("position",position)
                start.putExtras(bundle)
                startActivity(start)
            }
        })
    }
    fun searchbook(name: String?) {
        val mHamdler1 = object : Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    2 -> {
                        var result= msg.obj as BookResult
                        getpicture(result)
                    }
                    else -> {
                    }
                }
            }

        }
        Thread(Runnable {
            var message = Message()

            val call = Retrofitcall().getAPIService().getCall(name)
            call.enqueue(object : Callback<BookResult> {
                override fun onResponse(call: Call<BookResult>, response: Response<BookResult>) {
                    val st = response.body()
                    println(st)
                    message.obj=st
                    message.what=2
                    mHamdler1.sendMessage(message)
                }
                override fun onFailure(call: Call<BookResult>, t: Throwable) {
                    println("连接失败")
                    message.obj=null
                    message.what=2
                    mHamdler1.sendMessage(message)
                }

            })

        }).start()
    }
    fun getpicture(result:BookResult) {

        val hand = object : Handler() {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {

                    3 -> {
                        var map = msg.obj as MutableList<Bitmap>
                        for (i in map) {
                            imglist.add(i)
                        }
                        for(i in result!!.list)
                        {
                            list.add(i)

                        }
                        initadapt()
                    }
                    else -> {
                    }
                }
            }

        }
        Thread(Runnable {
            var bitmaplist : MutableList<Bitmap> = ArrayList<Bitmap>()
            var ms=Message()
            ms.obj=bitmaplist
            ms.what=3
            hand.sendMessage(ms)
        }).start()
    }
}