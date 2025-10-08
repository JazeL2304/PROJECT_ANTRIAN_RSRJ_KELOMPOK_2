package com.example.project_antrian_rsrj_kelompok_2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_antrian_rsrj_kelompok_2.adapter.NewsAdapter
import com.example.project_antrian_rsrj_kelompok_2.api.NewsApiService
import com.example.project_antrian_rsrj_kelompok_2.model.NewsResponse
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

// Gunakan nama class sama seperti file: fragment_news
class fragment_news : Fragment() {

    private lateinit var recyclerNews: RecyclerView
    private val apiKey = "pub_1f56deb4f0334aff8befe1f8ad74e5cb"

    // Argument default (biarkan saja)
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_news, container, false)

        // setup RecyclerView
        recyclerNews = view.findViewById(R.id.recyclerNews)
        recyclerNews.layoutManager = LinearLayoutManager(requireContext())

        // load data dari API
        loadNews()

        return view
    }

    private fun loadNews() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://newsdata.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(NewsApiService::class.java)
        val call = service.getHealthNews(apiKey, "health")

        call.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                if (response.isSuccessful) {
                    val newsList = response.body()?.results ?: emptyList()
                    recyclerNews.adapter = NewsAdapter(newsList)
                } else {
                    Toast.makeText(context, "Gagal memuat berita (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    companion object {
        private const val ARG_PARAM1 = "param1"
        private const val ARG_PARAM2 = "param2"

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            fragment_news().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}