package com.example.projectantrianrsrjkelompok2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projectantrianrsrjkelompok2.adapter.NewsAdapter
import com.example.projectantrianrsrjkelompok2.api.NewsApiService
import com.example.projectantrianrsrjkelompok2.model.NewsResponse
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class fragment_news : Fragment() {

    private lateinit var recyclerNews: RecyclerView
    private lateinit var progressBar: ProgressBar
    private val apiKey = "pub_1f56deb4f0334aff8befe1f8ad74e5cb"

    // ✅ TAMBAHAN: Flag untuk cek apakah fragment masih attached
    private var isFragmentAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ Set flag saat fragment dibuat
        isFragmentAttached = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)
        recyclerNews = view.findViewById(R.id.recyclerNews)
        recyclerNews.layoutManager = LinearLayoutManager(requireContext())
        progressBar = view.findViewById(R.id.progressBar)

        loadNews()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ✅ Set flag saat fragment di-destroy
        isFragmentAttached = false
    }

    private fun loadNews() {
        // ✅ FIXED: Cek fragment masih attached sebelum show progress
        if (!isFragmentAttached || !isAdded) return

        progressBar.visibility = View.VISIBLE

        val retrofit = Retrofit.Builder()
            .baseUrl("https://newsdata.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(NewsApiService::class.java)
        val call = service.getHealthNews(apiKey, "health,medical,medicine,hospital,doctor")

        call.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                // ✅ FIXED: Cek fragment masih attached
                if (!isFragmentAttached || !isAdded) return

                progressBar.visibility = View.GONE

                if (response.isSuccessful) {
                    val newsList = response.body()?.results ?: emptyList()
                    recyclerNews.adapter = NewsAdapter(newsList)
                } else {
                    // ✅ FIXED: Gunakan context yang safe
                    context?.let {
                        Toast.makeText(it, "Gagal memuat berita (${response.code()})", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                // ✅ FIXED: Cek fragment masih attached sebelum akses view/context
                if (!isFragmentAttached || !isAdded) return

                progressBar.visibility = View.GONE

                // ✅ FIXED: Gunakan context yang safe dengan null check
                context?.let {
                    Toast.makeText(it, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}