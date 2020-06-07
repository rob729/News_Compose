package com.example.jetpacknews

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.ui.core.*
import androidx.ui.foundation.*
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Canvas
import androidx.ui.layout.*
import androidx.ui.material.Card
import androidx.ui.material.CircularProgressIndicator
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TopAppBar
import androidx.ui.material.ripple.ripple
import androidx.ui.unit.Dp
import androidx.ui.unit.PxSize
import androidx.ui.unit.dp
import coil.Coil
import coil.request.LoadRequest
import coil.size.Scale
import com.example.jetpacknews.model.Articles
import com.example.jetpacknews.model.News
import com.example.jetpacknews.model.NewsResource
import com.example.jetpacknews.network.NewsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {

    val news = MutableLiveData<NewsResource<News>>()

    val _news: LiveData<NewsResource<News>>
        get() = news

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val news = remember {
                NewsModel(NewsResource.Loading())
            }
            _news.observe(this, Observer {
                news.state = it
            })

            news.state.let {
                when (it.status) {
                    NewsResource.Status.LOADING -> {
                        MaterialTheme() {
                            loadingComponent()
                        }
                    }
                    NewsResource.Status.SUCCESS -> {
                        MaterialTheme() {
                            Column() {
                                TopAppBar(title = { Text(text = "News Compose") })
                                displayList(data = it.data?.articles!!)
                            }
                        }
                    }
                    NewsResource.Status.ERROR -> {
                    }
                }
            }

        }

        CoroutineScope(Dispatchers.IO).launch {
            getNews()
        }
    }

    private fun getNews() {
        val request = NewsService().initalizeRetrofit()
            .getNews("top-headlines?sources=the-verge&apiKey=4663b6001744472eaac1f5aa16076a7a")
        request.enqueue(object : Callback<News> {
            override fun onFailure(call: Call<News>, t: Throwable) {
                news.value = NewsResource.Loading()
            }

            override fun onResponse(call: Call<News>, response: Response<News>) {
                if (response.isSuccessful) {
                    news.value = NewsResource.Success(data = response.body()!!)
                } else {
                    news.value = NewsResource.Loading()
                }
            }
        })
    }
}

@Composable
fun displayList(data: List<Articles>) {
    MaterialTheme {
        VerticalScroller(modifier = Modifier.fillMaxHeight()) {
            Column() {
                data.forEach {
                    Row(modifier = Modifier.fillMaxWidth() + Modifier.padding(8.dp)) {
                        rowLayout(articles = it)
                    }
                }
            }
        }
    }
}


@Composable
fun rowLayout(articles: Articles) {
    val typography = MaterialTheme.typography
    val img = imageLoader(url = articles.urlToImage, width = Dp(1080f), height = Dp(180f))
    Card(
        shape = RoundedCornerShape(Dp(4f)),
        modifier = Modifier.drawShadow(
            shape = RoundedCornerShape(Dp(4f)),
            elevation = 3.dp
        ) + Modifier.ripple()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (img != null) {
                Box(
                    modifier = Modifier.preferredHeight(180.dp).fillMaxWidth()
                        .clip(shape = RoundedCornerShape(4.dp))
                ) {
                    Draw { canvas: Canvas, _: PxSize ->
                        canvas.nativeCanvas.drawBitmap(img, 0f, 0f, null)
                    }
                }
            }
            Spacer(
                modifier = Modifier.preferredHeight(12.dp)
            )
            Text(
                text = articles.title,
                maxLines = 1,
                style = typography.h6,
                modifier = Modifier.padding(4.dp)
            )
            Text(
                text = articles.description,
                maxLines = 2,
                style = typography.body2,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}


@Composable
fun loadingComponent() {
    Column() {
        TopAppBar(title = { Text(text = "News Compose") })
        Box(modifier = Modifier.fillMaxSize(), gravity = ContentGravity.Center) {
            CircularProgressIndicator(modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally))
        }
    }

}

@Model
data class NewsModel(
    var state: NewsResource<News> = NewsResource.Loading()
)

@Composable
fun imageLoader(
    url: String,
    width: Dp,
    height: Dp
): Bitmap? {
    val image = state<Bitmap?> { null }
    val context = ContextAmbient.current

    val loadResource = LoadRequest.Builder(context)
        .data(url)
        .size(width = width.value.toInt(), height = height.value.toInt())
        .scale(Scale.FILL)
        .target(onSuccess = { image.value = (it as? BitmapDrawable)?.bitmap },
            onError = { image.value = null })
        .build()

    onCommit(url, width, height, context) {
        val requestDisposable = Coil.imageLoader(context).execute(loadResource)

        onDispose { requestDisposable.dispose() }
    }

    return image.value
}
