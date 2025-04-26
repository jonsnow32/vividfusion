package cloud.app.vvf.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import cloud.app.vvf.common.models.ImageHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

fun ImageHolder?.loadInto(
    imageView: ImageView, placeholder: Int? = null, errorDrawable: Int? = null
) = tryWith {
  val builder = Glide.with(imageView).asDrawable().transition(DrawableTransitionOptions.withCrossFade())
  val request = createRequest(builder, placeholder, errorDrawable)
  request.into(imageView)
}

fun ImageHolder?.loadWith(
  imageView: ImageView, thumbnail: ImageHolder? = null,
  error: Int? = null, onDrawable: (Drawable?) -> Unit = {}
) = tryWith {
  val builder = Glide.with(imageView).asDrawable()
  if (this == null) {
    thumbnail.loadInto(imageView, error)
    return@tryWith
  }
  val request = createRequest(builder, error)
  request.into(ViewTarget(imageView) {
    imageView.setImageDrawable(it)
    tryWith(false) { onDrawable(it) }
  })
}

fun <T : View> ImageHolder?.loadWith(
  view: T, placeholder: Int? = null, errorDrawable: Int? = null, onDrawable: (Drawable?) -> Unit
) = tryWith {
  val builder = Glide.with(view).asDrawable()
  val request = createRequest(builder, placeholder, errorDrawable)
  request.circleCrop().into(ViewTarget(view) {
    tryWith(false) { onDrawable(it) }
  })
}

suspend fun ImageHolder?.loadBitmap(
  context: Context, placeholder: Int? = null
) = tryWithSuspend {
  val builder = Glide.with(context).asBitmap()
  val request = createRequest(builder, placeholder)
  coroutineScope {
    withContext(Dispatchers.IO) {
      tryWithSuspend(false) { request.submit().get() }
    }
  }
}

fun ImageView.load(placeHolder: Int) = tryWith {
  Glide.with(this).load(placeHolder).into(this)
}

fun ImageView.load(drawable: Drawable?) = tryWith {
  Glide.with(this).load(drawable).into(this)
}

fun ImageView.load(drawable: Drawable?, size: Int) = tryWith {
  Glide.with(this).load(drawable).override(size).into(this)
}

fun Context.loadBitmap(placeHolder: Int) = tryWith {
  Glide.with(this).asBitmap().load(placeHolder).submit().get()
}

private fun <T> createRequest(
  imageHolder: ImageHolder,
  requestBuilder: RequestBuilder<T>,
) = imageHolder.run {
  when (this) {
    is ImageHolder.UriImageHolder -> requestBuilder.load(uri)
    is ImageHolder.UrlRequestImageHolder ->
      requestBuilder.load(request)
  }
}

fun <T> ImageHolder?.createRequest(
  requestBuilder: RequestBuilder<T>, placeholder: Int? = null, errorDrawable: Int? = null
): RequestBuilder<T> {
  var error = errorDrawable
  if (error == null) error = placeholder

  if (this == null) return requestBuilder.load(error)

  var request = createRequest(this, requestBuilder)
  request = placeholder?.let { request.apply(RequestOptions.placeholderOf(it)) } ?: request
  request = error?.let { request.error(it) } ?: request
  return if (crop) request.transform(SquareBitmapTransformation()) else request
}
//
//val circleCrop = CircleCropTransformation()
//val squareCrop = SquareCropTransformation()
//fun <T : View> ImageHolder?.loadAsCircle(
//  view: T, placeholder: Int? = null, errorDrawable: Int? = null, onDrawable: (Drawable?) -> Unit
//) = tryWith {
//  val request = createRequest(view.context, placeholder, errorDrawable, circleCrop)
//  fun setDrawable(image: Image?) {
//    val drawable = image?.asDrawable(view.resources)
//    tryWith(false) { onDrawable(drawable) }
//  }
//  request.target(::setDrawable, ::setDrawable, ::setDrawable)
//  view.enqueue(request)
//}
class ViewTarget<T : View>(val target: T, private val onDrawable: (Drawable?) -> Unit) :
  CustomViewTarget<View, Drawable>(target) {
  override fun onLoadFailed(errorDrawable: Drawable?) {
    onDrawable(errorDrawable)
  }

  override fun onResourceCleared(placeholder: Drawable?) {
    onDrawable(placeholder)
  }

  override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
    onDrawable(resource)
  }
}

