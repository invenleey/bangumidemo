package com.lc.bangumidemo.Activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import com.lc.bangumidemo.KT.*
import com.lc.bangumidemo.MyRetrofit.ResClass.BookDetail
import com.lc.bangumidemo.MyRetrofit.Retrofit.Retrofitcall
import com.lc.bangumidemo.R
import com.lc.bangumidemo.Sqlite.*
import com.lc.bangumidemo.Util.SingleClick
import com.ramotion.foldingcell.FoldingCell
import kotlinx.android.synthetic.main.bookindex.*
import kotlinx.android.synthetic.main.bookindex.listview
import org.jetbrains.anko.startActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.io.InputStream
import java.lang.NullPointerException
import java.net.URL
import kotlin.Exception



class BookDetailActivity : BaseActivity() {
    lateinit var contentView: LinearLayout
    private var isload=false    //forbit touch more
    override fun setRes(): Int {
        return R.layout.bookindex
    }

    override fun initview() {
        super.initview()
        lockscreen(true)
        setSupportActionBar(toolbar_index)
        supportActionBar!!.setHomeButtonEnabled(true)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowTitleEnabled(false)


    }


    override fun initlistener() {
        super.initlistener()
        startread.setOnClickListener(object : View.OnClickListener{
            @SingleClick
            override fun onClick(v: View?) {
                if(!isload) {
                    isload = true
                    lockscreen(true)
                    startActivity<ReadActivity>()
                }
            }
        })
        cell_title_view2.setOnClickListener {
            text_view.isVisible = false
            findViewById<FoldingCell>(R.id.folding_cell2).toggle(false)
        }
    }

    override fun initaction() {
        super.initaction()
        var bundle = intent.extras
        val urls = bundle!!.getString("url")
        val call = Retrofitcall().getAPIServerdetail().getCall(urls)
        call.enqueue(object : Callback<BookDetail> {
            override fun onFailure(call: Call<BookDetail>, t: Throwable) {
                println("连接失败")
            }

            override fun onResponse(
                call: Call<BookDetail>,
                response: Response<BookDetail>
            ) {
                val st = response.body()
                println(st)
                try {
                    loaddata(st)
                    loadlist(st)
                } catch (e: Exception) {
                    var intent = Intent(this@BookDetailActivity, ErrorActivity::class.java)
                    intent.putExtra("msg","书本信息无法请求")
                    intent.putExtra("error",e.message)
                    intent.putExtra("tag","BookDetailActivity")
                    startActivity(intent)
                }


            }
        })
    }

    fun loadlist(data: BookDetail?) {
             var adapter: ArrayAdapter<String> =
             ArrayAdapter<String>(this, R.layout.textview, data?.getbooknum() as MutableList<String>)
             listview.adapter = adapter
             listview.setOnItemClickListener(object :AdapterView.OnItemClickListener{
                 @SingleClick
                 override fun onItemClick(
                     parent: AdapterView<*>?,
                     view: View?,
                     position: Int,
                     id: Long
                 ) {
                     if(!isload) {
                         isload=true
                         lockscreen(true)
                         var db = MyDatabaseHelper(this@BookDetailActivity, "bookstore", null, 1)
                         var updata = BookIndexclass(
                             null,
                             bookDetail!!.data.author,
                             bookDetail!!.data.name,
                             position,
                             0,
                             bookDetail!!.list.size,
                             position,
                             0
                         )
                         Bookupdata.updata(db, updata)
                         db.close()
                         startActivity<ReadActivity>()
                     }
                 }
             })
    }

    fun loaddata(data: BookDetail?) {
        if (data == null) return
        bookDetail = data
        try {
            initThreadtoupdatapicture(data.data.cover)
        } catch (e: NullPointerException) {
            Log.e("NullPointerException", e.toString())
        }
        try {
            bookname.setText("书名:" + data.data.name)
            bookauthor.setText("作者:" + data.data.author)
            booknum.setText("最新章节:" + data.data.num)
            bookstatus.setText("状态:" + data.data.status)
            booktime.setText("更新时间:" + data.data.time)
            booktag.setText("类型:" + data.data.tag)
            text_view.setText("简介:" + data.data.introduce)
            listsize.setText("共${data.list.size}章 >  ")

            lockscreen(false)
            //查询是否存在索引
            var db = MyDatabaseHelper(this, "bookstore", null, 1)
            var selectindex = Selectclass(
                bookDetail!!.data.name,
                bookDetail!!.data.author,
                bookDetail!!.list.size
            )
            var returnsult = Bookselect.selectindex(db, selectindex)
            //如果没索引则加载前三章并插入索引
            if (returnsult == null) {
                var insert = BookIndexclass(
                    null,
                    bookDetail!!.data.author,
                    bookDetail!!.data.name,
                    0,
                    0,
                    bookDetail!!.list.size,
                    0,
                    0
                )
                Bookinsert.insertindex(db, insert)
                db.close()
            }else{
                db.close()
            }
        } catch (e: Exception) {
            Log.e("初始化书本信息错误", e.toString())
            Toast.makeText(this, "this booksource is valid", Toast.LENGTH_LONG).show()
        }


    }

    override fun startaction() {
        super.startaction()

    }

    private fun initThreadtoupdatapicture(urls: String) {
        val mHamdler1 = object : Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    1 -> {
                        if (msg.obj != null) {
                            val picture = msg.obj as Bitmap
                            bookcover.setImageBitmap(picture)
                        }
                    }

                }
            }
        }
        Thread(Runnable {
            val message = Message()
            var image: Bitmap?
            var url: URL? = null
            try {
                url = URL(urls)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            var io: InputStream? = null
            try {
                io = url!!.openConnection().getInputStream()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            image = BitmapFactory.decodeStream(io)
            message.what = 1
            message.obj = image
            mHamdler1.sendMessage(message)
        }).start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onRestart() {
        super.onRestart()
        try {
            isload=false
            bookDetail!!.data.url
        }catch (e:Exception){
            finish()
        }
    }
}