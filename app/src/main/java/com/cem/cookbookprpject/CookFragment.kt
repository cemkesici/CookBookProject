package com.cem.cookbookprpject

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import java.io.ByteArrayOutputStream

@Suppress("DEPRECATION")
class CookFragment : Fragment() {
    private var secilenGorsel: Uri?= null
    private var secilenBitmap: Bitmap?=null
    private var button: Button ?=null
    private var imageview: ImageView?=null

    @SuppressLint("CutPasteId", "Recycle")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_cook, container, false)
        button= v.findViewById(R.id.cookAddButton1)
        imageview=v.findViewById(R.id.imageView1)
        button?.let{
            it.setOnClickListener {

                save(v)
            }
        }

        imageview?.let{
            it.setOnClickListener {
                photoSave()
            }
        }
        arguments?.let{
            val gelenBilgi=CookFragmentArgs.fromBundle(it).bilgi
            if(gelenBilgi == "menudengeldim"){
                //yeni yemek eklemeye geldi
                v.findViewById<TextView>(R.id.cookNameText).text = ""
                v.findViewById<TextView>(R.id.cookDetailsText).text = ""
                v.findViewById<Button>(R.id.cookAddButton1).visibility=View.VISIBLE

                val gorselArkaPlan= BitmapFactory.decodeResource(context?.resources,R.drawable.defaultimg)
                v.findViewById<ImageView>(R.id.imageView1).setImageBitmap(gorselArkaPlan)

            }
            else{
                //oluşturulmuş yemeği görmeye geldi
                v.findViewById<Button>(R.id.cookAddButton1).visibility=View.INVISIBLE
                val secilenId=CookFragmentArgs.fromBundle(it).id
                context?.let{
                    try {
                        val database=it.openOrCreateDatabase("tarifListesi",Context.MODE_PRIVATE,null)
                        val cursor=database.rawQuery("SELECT * FROM yemekler WHERE id=?", arrayOf(secilenId.toString()) )

                        val yemekIsmiIndex=cursor.getColumnIndex(("yemekIsim"))
                        val yemekMalzemeIndex=cursor.getColumnIndex("yemekIcerik")
                        val yemekGorselIndex=cursor.getColumnIndex("yemekGorsel")

                        while (cursor.moveToNext()){
                            v.findViewById<TextView>(R.id.cookNameText).text=cursor.getString(yemekIsmiIndex)
                            v.findViewById<TextView>(R.id.cookDetailsText).text =cursor.getString(yemekMalzemeIndex)

                            val byteDizisi=cursor.getBlob(yemekGorselIndex)
                            val gorsel=BitmapFactory.decodeByteArray(byteDizisi,0,byteDizisi.size)
                            v.findViewById<ImageView>(R.id.imageView1).setImageBitmap(gorsel)
                        }
                        cursor.close()
                    }
                    catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
        return v
    }

    private fun bitmapKucult(userBitmap:Bitmap, maxBoyut:Int): Bitmap{
        //Proje için büyük boyutlu bitmap yüklenirse güvenlik amaçlı küçültmemiz gerekiyor.
        var width=userBitmap.width
        var height=userBitmap.height

        val bitmapOran:Double=width.toDouble()/height.toDouble()

        if (bitmapOran>1){
            //yatay görsel
            width= maxBoyut
            val kisaltilmisHeight=width/bitmapOran
            height=kisaltilmisHeight.toInt()
        }
        else{
            //görsel dikey
            height= maxBoyut
            val kisaltilmisHeight=height*bitmapOran
            width=kisaltilmisHeight.toInt()
        }
        return Bitmap.createScaledBitmap(userBitmap,width,height,true)
    }


    private fun save (view: View){
        //SQLite kayıt

        val textView1=view.findViewById<TextView>(R.id.cookNameText)
        val textView2=view.findViewById<TextView>(R.id.cookDetailsText)

        val yemekIsmi= textView1.text.toString()
        val yemekIcerik=textView2.text.toString()
        if (secilenBitmap!=null){
            val kucukBitmap= bitmapKucult(secilenBitmap!!,300)
            val outputStream=ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteDizisi=outputStream.toByteArray()

            try{
                context?.let{
                    val database=it.openOrCreateDatabase("tarifListesi",Context.MODE_PRIVATE,null)

                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler (id INTEGER PRIMARY KEY,yemekIsim VARCHAR,yemekIcerik VARCHAR,yemekGorsel BLOB )")

                    //Veriler sabit olmadığı, sürekli değiştiği için statement denen bir fonksiyon kullanmamız gerekli
                    val sqlString="INSERT INTO yemekler(yemekIsim,yemekIcerik,yemekGorsel) VALUES(?,?,?)"//sorguyu string olarak kayıt ediyoruz ve değerler yerine soru işareti koyuyoruz
                    val statement=database.compileStatement(sqlString)//stringi statement fonksiyonuna gönderdik
                    statement.bindString(1, yemekIsmi)//Birinci soru  string değer ve yemek ismi
                    statement.bindString(2, yemekIcerik)//İkinci soru işareti string ve yemek tarifi
                    statement.bindBlob(3,byteDizisi)//Üçüncü soru işareti görsel/blob ve yemek görseli
                    statement.execute()

                }
            }
            catch (e:Exception){
                e.printStackTrace()
            }
            val action=CookFragmentDirections.actionCookFragmentToListFragment()
            Navigation.findNavController(view).navigate(action)
        }
    }


    private fun photoSave() {
        //Fotoğraf almak için kullanıcı izinleri gerekli
        activity?.let{
            if(ContextCompat.checkSelfPermission(it.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                //üstte daha önce izin verildi mi onu kontrol ettik
                //izin verilmediyse çalışır
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
            }
            else{
                //izin zaten verilmişse
                val galeriIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent,2)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        //izin verildiyse çalışan fonksiyon. Bize aşağıdaki değerleri döndürür
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==1){//request kodu biz belirlemiştik, eğer code 1 ise çalışacak
            if(grantResults.isNotEmpty() &&grantResults[0]== PackageManager.PERMISSION_GRANTED){
                //grantResult yani verilen sonuçlar birden büyükse yani değer döndü ve dönen değerin ilk satırı onaylanmış mı
                val galeriIntent= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //galeriye gidilince ne olacağını kontrol eden fonksiyon
        if(requestCode==2 && resultCode== Activity.RESULT_OK && data !=null){
            secilenGorsel=data.data//secilen görselin adresini aldık
            try{
                context?.let {
                    if (secilenGorsel != null) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source= ImageDecoder.createSource(it.contentResolver, secilenGorsel!!)
                            secilenBitmap= ImageDecoder.decodeBitmap(source)
                            view?.findViewById<ImageView>(R.id.imageView1)?.setImageBitmap(secilenBitmap)
                        }
                        else{
                            secilenBitmap=
                                MediaStore.Images.Media.getBitmap(it.contentResolver,secilenGorsel)
                            view?.findViewById<ImageView>(R.id.imageView1)?.setImageBitmap(secilenBitmap)
                        }
                    }
                }
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}